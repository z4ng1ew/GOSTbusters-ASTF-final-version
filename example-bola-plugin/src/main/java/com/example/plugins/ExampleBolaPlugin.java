package com.example.plugins;

import org.owasp.astf.plugin.Plugin;
import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExampleBolaPlugin implements Plugin {
    @Override
    public String getId() {
        return "EXAMPLE-BOLA-PLUGIN";
    }

    @Override
    public String getName() {
        return "Example BOLA Plugin";
    }

    @Override
    public String getDescription() {
        return "Demonstrates BOLA testing through plugin architecture";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // Только для эндпоинтов с {account_id}
        if (endpoint.getPath().contains("{account_id}")) {
            // Попробуем получить чужой счёт
            String testUrl = endpoint.getFullUrl().replace("{account_id}", "acc-999-999");
            
            try {
                String response = client.get(testUrl, Map.of());
                // Если получили 200 — возможно уязвимость
                if (response.contains("account") || response.contains("balance")) {
                    findings.add(new Finding(
                        "PLUGIN-BOLA-01",
                        "BOLA via Plugin",
                        "Plugin detected possible access to foreign account via ID manipulation",
                        Severity.HIGH,
                        getId(),
                        testUrl,
                        "Implement proper ownership validation for account_id parameter"
                    ));
                }
            } catch (IOException e) {
                // 403/404 — OK
            }
        }

        return findings;
    }
}