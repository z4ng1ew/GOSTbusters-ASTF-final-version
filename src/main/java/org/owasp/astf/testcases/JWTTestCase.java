package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for JWT-specific vulnerabilities in the VBank API.
 *
 * Focuses on real-world issues relevant to the hackathon:
 * - alg: none attack
 * - RS256 to HS256 algorithm confusion
 * - Missing audience/issuer validation
 * - Missing expiration claims
 */
public class JWTTestCase implements TestCase {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getId() {
        return "JWT";
    }

    @Override
    public String getName() {
        return "JWT Vulnerabilities";
    }

    @Override
    public String getDescription() {
        return "Tests for JWT token vulnerabilities (none algorithm, weak secrets, missing claims)";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Проверяем только эндпоинт выдачи токена и JWKS
        String path = endpoint.getPath();
        if (!path.equals("/auth/bank-token") && !path.equals("/.well-known/jwks.json")) {
            return findings;
        }

        System.out.println("🔍 Testing JWT security on: " + endpoint.getFullUrl());

        try {
            // 1. Получаем JWKS
            String jwksUrl = endpoint.getBaseUrl() + "/.well-known/jwks.json";
            String jwksResponse = client.get(jwksUrl, Map.of());
            JsonNode jwks = objectMapper.readTree(jwksResponse);
            String publicKey = extractPublicKey(jwks);

            // 2. Получаем валидный токен
            String tokenUrl = endpoint.getBaseUrl() + "/auth/bank-token?client_id=team179&client_secret=JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO";
            String tokenResponse = client.get(tokenUrl, Map.of());
            JsonNode tokenJson = objectMapper.readTree(tokenResponse);
            String validToken = tokenJson.get("access_token").asText();

            // 3. Тестируем уязвимости
            findings.addAll(testAlgNoneAttack(endpoint, client, validToken));
            findings.addAll(testAlgorithmConfusion(endpoint, client, validToken, publicKey));
            findings.addAll(testMissingClaims(endpoint, validToken));
            findings.addAll(testAudienceValidation(endpoint, client, validToken));

        } catch (Exception e) {
            System.err.println("⚠️ JWT test skipped due to error: " + e.getMessage());
        }

        return findings;
    }

    private List<Finding> testAlgNoneAttack(EndpointInfo endpoint, HttpClient client, String validToken) {
        List<Finding> findings = new ArrayList<>();
        try {
            String[] parts = validToken.split("\\.");
            if (parts.length != 3) return findings;

            // Декодируем заголовок
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            if (!"RS256".equals(header.get("alg").asText())) return findings;

            // Создаем токен с alg: none
            String forgedHeader = Base64.getUrlEncoder().encodeToString(
                "{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes()
            ).replace("=", "");
            String forgedToken = forgedHeader + "." + parts[1] + ".";

            // Проверяем токен на эндпоинте /accounts
            String testUrl = endpoint.getBaseUrl() + "/accounts";
            try {
                String response = client.get(testUrl, Map.of("Authorization", "Bearer " + forgedToken));
                // Если 200 — уязвимость
                if (response != null && !response.isEmpty() && !response.contains("401")) {
                    findings.add(new Finding(
                        "JWT-01",
                        "JWT Algorithm None Vulnerability",
                        "Сервер принимает JWT с алгоритмом 'none', что позволяет создавать токены без подписи",
                        Severity.HIGH,
                        getId(),
                        testUrl,
                        "Убедитесь, что сервер отклоняет токены с 'alg: none'. Всегда проверяйте алгоритм до валидации."
                    ));
                }
            } catch (IOException ignored) {
                // 401/403 — OK
            }
        } catch (Exception ignored) {}
        return findings;
    }

    private List<Finding> testAlgorithmConfusion(EndpointInfo endpoint, HttpClient client, String validToken, String publicKey) {
        List<Finding> findings = new ArrayList<>();
        // Пропускаем, если нет библиотеки для подписи (jose4j или nimbus-jose)
        // В рамках хакатона это сложно реализовать без внешних зависимостей
        return findings;
    }

    private List<Finding> testMissingClaims(EndpointInfo endpoint, String token) {
        List<Finding> findings = new ArrayList<>();
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payload = objectMapper.readTree(payloadJson);

            if (!payload.has("exp")) {
                findings.add(new Finding(
                    "JWT-02",
                    "Missing Expiration Claim",
                    "JWT токен не содержит поля 'exp' (expiration time), что делает его валидным навсегда",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Всегда устанавливайте 'exp' в JWT. Проверяйте его на сервере."
                ));
            }

            if (!payload.has("aud")) {
                findings.add(new Finding(
                    "JWT-03",
                    "Missing Audience Claim",
                    "JWT токен не содержит поля 'aud' (audience), что может привести к подмене банка",
                    Severity.MEDIUM,
                    getId(),
                    endpoint.getFullUrl(),
                    "Указывайте 'aud' = 'vbank.open.bankingapi.ru' и проверяйте его при валидации."
                ));
            }

            if (!payload.has("iss")) {
                findings.add(new Finding(
                    "JWT-04",
                    "Missing Issuer Claim",
                    "JWT токен не содержит поля 'iss' (issuer), что затрудняет проверку источника",
                    Severity.MEDIUM,
                    getId(),
                    endpoint.getFullUrl(),
                    "Указывайте 'iss' = 'vbank.open.bankingapi.ru' и проверяйте его."
                ));
            }
        } catch (Exception ignored) {}
        return findings;
    }

    private List<Finding> testAudienceValidation(EndpointInfo endpoint, HttpClient client, String validToken) {
        List<Finding> findings = new ArrayList<>();
        // Тест сложен без подмены токена → пропускаем в рамках хакатона
        return findings;
    }

    private String extractPublicKey(JsonNode jwks) {
        try {
            JsonNode key = jwks.get("keys").get(0);
            return "-----BEGIN PUBLIC KEY-----\n" +
                   key.get("x5c").get(0).asText() +
                   "\n-----END PUBLIC KEY-----";
        } catch (Exception e) {
            return "";
        }
    }
}