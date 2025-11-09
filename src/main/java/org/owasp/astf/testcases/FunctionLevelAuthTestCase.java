package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionLevelAuthTestCase implements TestCase {
    @Override
    public String getId() {
        return "FUNCTION-LEVEL-AUTH";
    }

    @Override
    public String getName() {
        return "Function Level Authorization";
    }

    @Override
    public String getDescription() {
        return "Tests for function level authorization bypass";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Только для эндпоинтов с ID (например /accounts/{id})
        if (!endpoint.getPath().contains("{id}") && !endpoint.getPath().contains("{account_id}")) {
            return findings;
        }

        // Попробовать выполнить DELETE, если в OpenAPI только GET
        if ("GET".equalsIgnoreCase(endpoint.getMethod())) {
            String deleteUrl = endpoint.getFullUrl().replace("{id}", "12345").replace("{account_id}", "acc-9999");
            Map<String, String> headers = Map.of();
            try {
                String response = client.delete(deleteUrl, headers); // предполагаем, что HttpClient поддерживает delete
                if (getStatusCode(response) == 200 || getStatusCode(response) == 204) {
                    findings.add(new Finding(
                        "FLA-01",
                        "Function Level Authorization Bypass",
                        "Пользователь может выполнить DELETE на эндпоинте, где у него только GET права",
                        Severity.HIGH,
                        getId(),
                        deleteUrl,
                        "Проверьте права доступа к методам API на уровне авторизации"
                    ));
                }
            } catch (Exception e) {
                // 403/405 — OK
            }
        }

        return findings;
    }

    private int getStatusCode(String response) {
        return 200; // временно
    }
}