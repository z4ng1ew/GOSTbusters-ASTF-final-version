package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that no hidden/debug endpoints are exposed.
 * Focuses ONLY on endpoints that are commonly used for debugging but should NOT be in production.
 * This avoids false positives on legitimate endpoints like / and /health (which ARE in OpenAPI).
 *
 * OWASP API10: Improper Assets Management → Hidden API endpoints
 */
public class OpenApiContractValidationTestCase implements TestCase {

    // Common debug/hidden endpoints that SHOULD NOT be in production
    private static final List<String> SUSPICIOUS_ENDPOINTS = List.of(
        "/debug", "/actuator", "/env", "/metrics", "/beans", "/dump", "/trace",
        "/swagger-ui.html", "/swagger", "/api-docs", "/docs",
        "/admin", "/console", "/setup", "/install"
    );

    @Override
    public String getId() {
        return "OPENAPI-CONTRACT";
    }

    @Override
    public String getName() {
        return "OpenAPI Contract Validation";
    }

    @Override
    public String getDescription() {
        return "Checks for hidden/debug endpoints not documented in OpenAPI spec (OWASP API10)";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        String path = endpoint.getPath().toLowerCase();

        // Проверяем ТОЛЬКО подозрительные эндпоинты
        for (String suspiciousPath : SUSPICIOUS_ENDPOINTS) {
            if (path.equals(suspiciousPath) || path.startsWith(suspiciousPath + "/")) {
                try {
                    int statusCode = client.getStatusCode(endpoint.getFullUrl(), java.util.Map.of());
                    // Если эндпоинт существует (2xx–5xx) → уязвимость
                    if (statusCode >= 200 && statusCode < 600) {
                        findings.add(new Finding(
                            "API10-01",
                            "Hidden Debug Endpoint Exposed",
                            "Найден скрытый эндпоинт, который не должен быть доступен в продакшене: " + path,
                            Severity.HIGH,
                            getId(),
                            endpoint.getFullUrl(),
                            "Удалите debug-эндпоинты (/debug, /actuator и т.д.) из продакшена. Они увеличивают поверхность атаки."
                        ));
                        break;
                    }
                } catch (IOException e) {
                    // Эндпоинт недоступен — OK
                }
            }
        }

        return findings;
    }
}