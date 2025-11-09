package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdorTestCase implements TestCase {
    @Override
    public String getId() {
        return "IDOR";
    }

    @Override
    public String getName() {
        return "Insecure Direct Object Reference";
    }

    @Override
    public String getDescription() {
        return "Tests for IDOR vulnerabilities by attempting to access resources via direct object references";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        String path = endpoint.getPath();

        // Только для эндпоинтов с ID (как BOLA, но чуть шире)
        if (path.contains("{id}") || path.contains("{resource_id}") || path.contains("{account_id}")) {
            // Попробовать получить чужой ресурс через ID
            String testUrl = endpoint.getFullUrl().replace("{id}", "12345")
                                   .replace("{resource_id}", "12345")
                                   .replace("{account_id}", "acc-9999");

            Map<String, String> headers = Map.of();
            try {
                String response = client.get(testUrl, headers);
                if (getStatusCode(response) == 200) {
                    findings.add(new Finding(
                        "IDOR-01",
                        "Insecure Direct Object Reference",
                        "Доступ к ресурсу через прямую ссылку на ID без проверки авторизации",
                        Severity.HIGH,
                        getId(),
                        testUrl,
                        "Добавьте проверку принадлежности ресурса пользователю"
                    ));
                }
            } catch (Exception e) {
                // 403/404 — OK
            }
        }

        return findings;
    }

    private int getStatusCode(String response) {
        // Извлеките статус из HttpClient.getLastStatusCode() если он есть
        // или парсите ответ, если в формате JSON
        return 200; // временно
    }
}