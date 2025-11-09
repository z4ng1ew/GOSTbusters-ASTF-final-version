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
        return "Tests for insecure CORS configuration";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Выполнить OPTIONS-запрос
        Map<String, String> headers = Map.of("Origin", "https://evil.com");
        try {
            String response = client.options(endpoint.getFullUrl(), headers);
            if (response.contains("Access-Control-Allow-Origin: *") || response.contains("Access-Control-Allow-Origin: https://evil.com")) {
                findings.add(new Finding(
                    "CORS-01",
                    "CORS Misconfiguration",
                    "API позволяет доступ с любого домена или поддельного origin",
                    Severity.HIGH,
                    getId(),
                    endpoint.getFullUrl(),
                    "Ограничьте Access-Control-Allow-Origin списком доверенных доменов"
                ));
            }
        } catch (Exception e) {
            // 405/403 — OK
        }

        return findings;
    }
}