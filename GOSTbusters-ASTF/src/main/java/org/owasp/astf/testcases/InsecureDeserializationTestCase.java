package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsecureDeserializationTestCase implements TestCase {
    @Override
    public String getId() {
        return "INSECURE-DESERIALIZATION";
    }

    @Override
    public String getName() {
        return "Insecure Deserialization";
    }

    @Override
    public String getDescription() {
        return "Tests for insecure deserialization vulnerabilities";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Только POST/PUT-запросы
        if (!endpoint.getMethod().equals("POST") && !endpoint.getMethod().equals("PUT")) {
            return findings;
        }

        // Попробовать отправить подозрительный JSON
        String maliciousPayload = "{\"@type\":\"java.lang.Class\",\"val\":\"com.malicious.Hack\"}";

        Map<String, String> headers = Map.of("Content-Type", "application/json");
        try {
            String response = client.post(endpoint.getFullUrl(), headers, maliciousPayload, "application/json");
            // Если сервер не отклонил запрос — возможна уязвимость
            if (!response.contains("error") && !response.contains("400")) {
                findings.add(new Finding(
                    "INSECURE-DES-01",
                    "Possible Insecure Deserialization",
                    "API accepts potentially dangerous payload without validation",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Implement strict validation of input data and avoid deserializing untrusted data"
                ));
            }
        } catch (Exception e) {
            // 400/403 — OK
        }

        return findings;
    }
}