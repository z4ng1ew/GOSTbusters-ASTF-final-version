package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;
import org.owasp.astf.testcases.bola.BolaTestContext;
import org.owasp.astf.testcases.bola.strategy.DefaultIdGenerationStrategy;
import org.owasp.astf.testcases.bola.service.ConsentServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // ✅ ДОБАВЛЕНО: Не хватало этого импорта

public class BolaTestCase implements TestCase {
    private static boolean bolaTestCompleted = false;
    private static final Object BOLA_LOCK = new Object();

    private final BolaTestContext context;

    public BolaTestCase(BolaTestContext context) {
        this.context = context;
    }

    public BolaTestCase() {
        this(new BolaTestContext(
            new DefaultIdGenerationStrategy(),
            new ConsentServiceImpl()
        ));
    }

    @Override
    public String getId() {
        return "BOLA";
    }

    @Override
    public String getName() {
        return "Broken Object Level Authorization";
    }

    @Override
    public String getDescription() {
        return "Tests for BOLA using strategy pattern";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        // ✅ Проверяем, запускался ли тест
        synchronized (BOLA_LOCK) {
            if (bolaTestCompleted) {
                System.out.println("⏩ BOLA test already completed - skipping duplicate execution");
                return Collections.emptyList();
            }
            bolaTestCompleted = true;
        }

        // ✅ Проверяем, есть ли параметр account_id в пути
        String path = endpoint.getPath();
        if (!path.contains("{account_id}") && !path.contains("account_id")) {
            System.out.println("⏭️ Skipping non-BOLA endpoint: " + path);
            return Collections.emptyList();
        }

        List<Finding> findings = new ArrayList<>();
        System.out.println("🔍 Starting BOLA test on endpoint: " + endpoint.getMethod() + " " + path);

        try {
            // ✅ Получаем наши account_id
            List<String> ourAccountIds = getOurAccountIds(endpoint, client);
            System.out.println("📝 Found our accounts: " + ourAccountIds);

            // ✅ Генерируем подозрительные ID через стратегию
            List<String> suspiciousIds = context.getIdStrategy().generateIds(ourAccountIds);
            System.out.println("🎯 Generated " + suspiciousIds.size() + " suspicious account IDs for BOLA testing");

            // ✅ Получаем consent (если возможно)
            String consentId = context.getConsentService().createConsent(endpoint.getBaseUrl(), client);

            // ✅ Тестируем каждый подозрительный ID
            int testedCount = 0;
            for (String accountId : suspiciousIds) {
                // Пропускаем свои ID
                if (ourAccountIds.contains(accountId)) {
                    continue;
                }

                String testUrl = endpoint.getFullUrl().replace("{account_id}", accountId);
                Map<String, String> headers = createHeaders(consentId);

                try {
                    String response = client.get(testUrl, headers);
                    
                    // ✅ Проверяем успешный доступ (200 + данные)
                    if (response.contains("account") || response.contains("balance") || response.contains("data")) {
                        Finding finding = new Finding(
                            "BOLA-01",
                            "Broken Object Level Authorization",
                            "🚨 CRITICAL: Successfully accessed account " + accountId + " with team179 token",
                            Severity.HIGH,
                            getId(),
                            testUrl,
                            "Add ownership validation: check that account_id belongs to authenticated user"
                        );
                        findings.add(finding);
                        System.out.println("🚨 BOLA VULNERABILITY FOUND: access to foreign account " + accountId);
                    } else {
                        System.out.println("✅ BOLA protection: access denied to account " + accountId);
                    }
                } catch (IOException e) {
                    // 403/404 — OK
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (errorMsg.contains("403") || errorMsg.contains("404"))) {
                        System.out.println("✅ BOLA protection: access blocked for account " + accountId);
                    } else {
                        System.out.println("🔧 BOLA test error for account " + accountId + ": " + e.getMessage());
                    }
                }

                testedCount++;
                if (testedCount >= 10) { // Ограничим тест для скорости
                    System.out.println("ℹ️ Stopped BOLA test after 10 attempts to avoid rate limiting");
                    break;
                }
            }

            if (findings.isEmpty()) {
                System.out.println("✅ BOLA test completed: no vulnerabilities found in tested range");
            }

        } catch (Exception e) {
            System.err.println("❌ BOLA test execution error: " + e.getMessage());
            if (isDebugMode()) {
                e.printStackTrace();
            }
        }

        return findings;
    }

    private List<String> getOurAccountIds(EndpointInfo endpoint, HttpClient client) throws IOException {
        // ✅ Временная реализация — в реальности нужно получить из API
        return List.of("acc-179-1", "acc-179-2", "acc-179-3");
    }

    private Map<String, String> createHeaders(String consentId) {
        Map<String, String> headers = new HashMap<>(); // ✅ Теперь HashMap импортирован
        headers.put("x-consent-id", consentId != null ? consentId : "consent-fake");
        headers.put("x-requesting-bank", "team179");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private boolean isDebugMode() {
        return "true".equals(System.getProperty("astf.debug"));
    }
}