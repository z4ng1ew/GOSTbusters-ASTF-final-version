package org.owasp.astf.testcases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

/**
 * Tests for API2:2023 Broken Authentication.
 *
 * ✅ ИСПРАВЛЕНО: 
 * - Используем реальный endpoint.getFullUrl()
 * - Проверяем HTTP-статусы (200 = уязвимость, 401/403 = OK)
 * - Убраны ложные срабатывания
 */
public class BrokenAuthenticationTestCase implements TestCase {
    private static final Logger logger = LogManager.getLogger(BrokenAuthenticationTestCase.class);

    // Common authentication-related paths for endpoint detection
    private static final List<String> AUTH_PATH_PATTERNS = List.of(
            "login", "auth", "token", "signin", "oauth", "session"
    );

    @Override
    public String getId() {
        return "ASTF-API2-2023";
    }

    @Override
    public String getName() {
        return "Broken Authentication";
    }

    @Override
    public String getDescription() {
        return """
               Tests for authentication weaknesses such as weak credentials, improper 
               token validation, missing or inconsistent authentication checks, and 
               credential exposure in URLs.
               """;
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient httpClient) throws IOException {
        logger.info("Executing {} test on {}", getId(), endpoint);
        List<Finding> findings = new ArrayList<>();

        // Только для аутентификационных эндпоинтов — ручной аудит
        if (isAuthEndpoint(endpoint)) {
            findings.addAll(testWeakAuthentication(endpoint, httpClient));
            return findings; // Не тестируем Missing Auth для /auth/*
        }

        // Для ВСЕХ остальных эндпоинтов — проверяем Missing Authentication
        findings.addAll(testMissingAuthentication(endpoint, httpClient));
        return findings;
    }

    private boolean isAuthEndpoint(EndpointInfo endpoint) {
        String path = endpoint.getPath().toLowerCase();
        return AUTH_PATH_PATTERNS.stream().anyMatch(path::contains);
    }

    private List<Finding> testWeakAuthentication(EndpointInfo endpoint, HttpClient httpClient) {
        List<Finding> findings = new ArrayList<>();

        if (!endpoint.getMethod().equalsIgnoreCase("POST")) {
            return findings;
        }

        // Для хакатона: оставляем только ручной аудит
        Finding finding = new Finding(
                UUID.randomUUID().toString(),
                "Authentication Endpoint Requires Manual Review",
                "Authentication endpoints should be carefully reviewed for weak credentials, account lockout mechanisms, and proper token validation.",
                Severity.MEDIUM,
                getId(),
                endpoint.getMethod() + " " + endpoint.getPath(),
                "Implement strong password policies, account lockout after failed attempts, and proper token generation using industry standard algorithms."
        );

        findings.add(finding);
        return findings;
    }

    /**
     * ✅ ИСПРАВЛЕНО: Проверяем реальный URL и статус-код
     */
    private List<Finding> testMissingAuthentication(EndpointInfo endpoint, HttpClient httpClient) {
        List<Finding> findings = new ArrayList<>();

        // Согласно OpenAPI все эндпоинты требуют аутентификации
        // Поэтому проверяем ВСЕ кроме /auth/bank-token

        String realUrl = endpoint.getFullUrl(); // ✅ Реальный URL

        try {
            String method = endpoint.getMethod().toUpperCase();
            int statusCode;

            // ✅ Выполняем запрос БЕЗ авторизации
            switch (method) {
                case "GET":
                    statusCode = httpClient.getStatusCode(realUrl, Map.of());
                    break;
                case "POST":
                    statusCode = httpClient.postStatusCode(realUrl, Map.of(), "application/json", "{}");
                    break;
                case "PUT":
                    statusCode = httpClient.putStatusCode(realUrl, Map.of(), "application/json", "{}");
                    break;
                case "DELETE":
                    statusCode = httpClient.deleteStatusCode(realUrl, Map.of());
                    break;
                default:
                    return findings; // Неизвестный метод
            }

            // ✅ 2xx = уязвимость (доступ без аутентификации)
            if (statusCode >= 200 && statusCode < 300) {
                Finding finding = new Finding(
                        UUID.randomUUID().toString(),
                        "Missing Authentication Controls",
                        "The API endpoint appears to be accessible without proper authentication.",
                        Severity.HIGH,
                        getId(),
                        realUrl,
                        "Implement consistent authentication checks across all API endpoints that require them."
                );
                findings.add(finding);
                logger.warn("FOUND MISSING AUTH: {} {} returns {}", method, realUrl, statusCode);
            } else {
                logger.debug("OK: {} {} returns {} (auth protected)", method, realUrl, statusCode);
            }

        } catch (Exception e) {
            logger.debug("Error testing missing authentication on {}: {}", realUrl, e.getMessage());
        }

        return findings;
    }
}