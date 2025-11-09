package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MassAssignmentTestCase implements TestCase {
    @Override
    public String getId() {
        return "MASS-ASSIGNMENT";
    }

    @Override
    public String getName() {
        return "Mass Assignment";
    }

    @Override
    public String getDescription() {
        return "Tests for mass assignment vulnerabilities";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Только POST/PUT-запросы
        if (!endpoint.getMethod().equals("POST") && !endpoint.getMethod().equals("PUT")) {
            return findings;
        }

        // Попробовать отправить JSON с лишними полями
        String payload = "{\n" +
            "  \"name\": \"test\",\n" +
            "  \"email\": \"test@example.com\",\n" +
            "  \"isAdmin\": true,\n" +  // Поле, которое не должно быть доступно
            "  \"role\": \"admin\"       // Ещё одно подозрительное поле\n" +
            "}";

        Map<String, String> headers = Map.of("Content-Type", "application/json");
        try {
            String response = client.post(endpoint.getFullUrl(), headers, payload, "application/json");
            // Если в ответе есть isAdmin=true или role=admin — уязвимость
            if (response.contains("\"isAdmin\":true") || response.contains("\"role\":\"admin\"")) {
                findings.add(new Finding(
                    "MASS-ASSIGN-01",
                    "Mass Assignment Vulnerability",
                    "API allows setting privileged fields like 'isAdmin' or 'role'",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Use allow-lists to specify which fields can be updated by user input"
                ));
            }
        } catch (Exception e) {
            // 400/403 — OK
        }

        return findings;
    }
}