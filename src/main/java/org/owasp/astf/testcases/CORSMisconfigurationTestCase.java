package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CORSMisconfigurationTestCase implements TestCase {

    @Override
    public String getId() {
        return "CORS-MISCONFIGURATION";
    }

    @Override
    public String getName() {
        return "CORS Misconfiguration";
    }

    @Override
    public String getDescription() {
        return "Tests for insecure CORS configuration (wildcard origins, reflection, credentials)";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // CORS актуален только для эндпоинтов, которые могут вызываться из браузера
        // В Open Banking такие эндпоинты редки, но проверим GET/POST на /accounts и /payments
        String path = endpoint.getPath().toLowerCase();
        if (!path.contains("account") && !path.contains("payment") && !path.contains("product")) {
            return findings;
        }

        // 1. Тест на wildcard (*)
        findings.addAll(testWildcardOrigin(endpoint, client));

        // 2. Тест на отражение Origin (CORS reflection)
        findings.addAll(testOriginReflection(endpoint, client));

        // 3. Тест на credentials с wildcard (потенциально опасная комбинация)
        findings.addAll(testCredentialsWithWildcard(endpoint, client));

        return findings;
    }

    private List<Finding> testWildcardOrigin(EndpointInfo endpoint, HttpClient client) {
        List<Finding> findings = new ArrayList<>();
        try {
            Map<String, String> headers = Map.of("Origin", "https://attacker.com");
            String response = client.options(endpoint.getFullUrl(), headers);
            if (response != null && response.contains("Access-Control-Allow-Origin: *")) {
                // Проверим, разрешены ли credentials с wildcard — это критично
                if (response.contains("Access-Control-Allow-Credentials: true")) {
                    findings.add(new Finding(
                        "CORS-01",
                        "Insecure CORS: Wildcard Origin with Credentials",
                        "API разрешает доступ с любого домена (Access-Control-Allow-Origin: *) и при этом разрешает передачу credentials. Это позволяет атакующему украсть токены через XSS.",
                        Severity.CRITICAL,
                        getId(),
                        endpoint.getFullUrl(),
                        "Уберите wildcard (*) из Access-Control-Allow-Origin. Используйте явный список доверенных доменов. Никогда не комбинируйте wildcard с credentials."
                    ));
                } else {
                    findings.add(new Finding(
                        "CORS-02",
                        "Insecure CORS: Wildcard Origin",
                        "API разрешает доступ с любого домена (Access-Control-Allow-Origin: *). Это снижает эффективность защиты от CSRF и расширяет поверхность атаки.",
                        Severity.HIGH,
                        getId(),
                        endpoint.getFullUrl(),
                        "Ограничьте Access-Control-Allow-Origin только доверенными доменами (например, https://vbank.open.bankingapi.ru)."
                    ));
                }
            }
        } catch (Exception e) {
            // 405/403/501 — OK, CORS не поддерживается
        }
        return findings;
    }

    private List<Finding> testOriginReflection(EndpointInfo endpoint, HttpClient client) {
        List<Finding> findings = new ArrayList<>();
        try {
            String maliciousOrigin = "https://evil-bank.open.bankingapi.ru";
            Map<String, String> headers = Map.of("Origin", maliciousOrigin);
            String response = client.options(endpoint.getFullUrl(), headers);

            if (response != null && response.contains("Access-Control-Allow-Origin: " + maliciousOrigin)) {
                findings.add(new Finding(
                    "CORS-03",
                    "CORS Origin Reflection",
                    "API отражает значение заголовка Origin в Access-Control-Allow-Origin без валидации. Это позволяет обойти CORS-защиту.",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Валидируйте Origin по белому списку. Никогда не отражайте Origin напрямую в заголовке ответа."
                ));
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
        return findings;
    }

    private List<Finding> testCredentialsWithWildcard(EndpointInfo endpoint, HttpClient client) {
        // Уже проверено в testWildcardOrigin, но оставим для расширения
        return new ArrayList<>();
    }
}