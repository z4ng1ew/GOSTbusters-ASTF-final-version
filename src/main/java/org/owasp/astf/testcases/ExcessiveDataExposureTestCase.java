package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExcessiveDataExposureTestCase implements TestCase {
    @Override
    public String getId() {
        return "EXCESSIVE-DATA-EXPOSURE";
    }

    @Override
    public String getName() {
        return "Excessive Data Exposure";
    }

    @Override
    public String getDescription() {
        return "Tests for excessive data exposure in API responses";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Только для GET-запросов
        if (!"GET".equalsIgnoreCase(endpoint.getMethod())) {
            return findings;
        }

        // Выполняем запрос без авторизации
        Map<String, String> headers = Map.of();
        try {
            String response = client.get(endpoint.getFullUrl(), headers);

            // Проверяем, есть ли лишние поля (например, password, secret, token)
            if (response.contains("password") || response.contains("secret") || response.contains("token")) {
                findings.add(new Finding(
                    "EXCESSIVE-DATA-01",
                    "Excessive Data Exposure",
                    "Response contains sensitive fields like 'password', 'secret', or 'token'",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Remove sensitive data from API responses. Use DTOs to control what data is returned."
                ));
            }
        } catch (Exception e) {
            // 403/401 — OK
        }

        return findings;
    }
}