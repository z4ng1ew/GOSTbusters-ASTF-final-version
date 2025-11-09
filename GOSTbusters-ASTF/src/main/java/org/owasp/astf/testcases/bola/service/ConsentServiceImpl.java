package org.owasp.astf.testcases.bola.service;

import org.owasp.astf.core.http.HttpClient;

import java.io.IOException;
import java.util.Map;

public class ConsentServiceImpl implements ConsentService {
    @Override
    public String createConsent(String baseUrl, HttpClient client) throws IOException {
        try {
            String consentUrl = baseUrl + "/account-consents/request";
            Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "x-requesting-bank", "team179"
            );
            String body = """
                {
                  "permissions": ["accounts", "balances"],
                  "expirationDateTime": "2025-12-31T23:59:59Z"
                }
                """;
            
            String response = client.post(consentUrl, headers, body, "application/json");
            // Извлекаем consent_id из ответа (временно)
            return "consent-38e83d9f8dca";
        } catch (Exception e) {
            System.out.println("⚠️ Cannot create consent: " + e.getMessage());
            return null; // или возвращаем заглушку
        }
    }
}