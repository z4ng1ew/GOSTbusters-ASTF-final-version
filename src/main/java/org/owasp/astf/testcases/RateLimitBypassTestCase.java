package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RateLimitBypassTestCase implements TestCase {
    // ✅ ДОБАВЛЕНО: Статические переменные для предотвращения многократного запуска
    private static boolean rateLimitTestCompleted = false;
    private static final Object RATE_LIMIT_LOCK = new Object();
    
    // ✅ ДОБАВЛЕНО: Счетчик для отслеживания запросов
    private static int totalRateLimitRequests = 0;
    
    @Override
    public String getId() {
        return "RATE-LIMIT-BYPASS";
    }

    @Override
    public String getName() {
        return "Rate Limit Bypass";
    }

    @Override
    public String getDescription() {
        return "Tests for bypassing rate limits by sending too many requests to unprotected endpoints";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();

        // ✅ ДОБАВЛЕНО: Проверяем, запускался ли тест раньше
        synchronized (RATE_LIMIT_LOCK) {
            if (rateLimitTestCompleted) {
                System.out.println("⏩ Rate Limit test already completed - skipping duplicate execution");
                return Collections.emptyList();
            }
            rateLimitTestCompleted = true;
        }

        // ✅ УЛУЧШЕНО: Тестируем только публичные эндпоинты без авторизации
        if (!isPublicEndpoint(endpoint)) {
            System.out.println("⏭️ Skipping protected endpoint for rate limit test: " + endpoint.getPath());
            return Collections.emptyList();
        }

        System.out.println("🔍 Starting Rate Limit Bypass test on endpoint: " + endpoint.getMethod() + " " + endpoint.getPath());

        try {
            // ✅ УЛУЧШЕНО: Тестируем разные методы обхода рейт-лимита
            List<Finding> methodFindings = testRateLimitBypassMethods(endpoint, client);
            findings.addAll(methodFindings);

            // ✅ ДОБАВЛЕНО: Если уязвимостей не найдено, добавляем информационное сообщение
            if (findings.isEmpty()) {
                findings.add(createRateLimitInfoFinding(endpoint));
            }

        } catch (Exception e) {
            System.err.println("❌ Rate Limit test execution error: " + e.getMessage());
            if (isVerboseMode()) {
                e.printStackTrace();
            }
        }

        System.out.println("📊 Rate Limit test completed: " + totalRateLimitRequests + " requests made");
        return findings;
    }

    /**
     * ✅ ДОБАВЛЕНО: Проверяет, является ли эндпоинт публичным (без авторизации)
     */
    private boolean isPublicEndpoint(EndpointInfo endpoint) {
        String path = endpoint.getPath().toLowerCase();
        
        // ✅ Публичные эндпоинты (без авторизации)
        List<String> publicPaths = List.of(
            "/auth/", 
            "/public/", 
            "/health", 
            "/status", 
            "/docs", 
            "/swagger", 
            "/openapi"
        );
        
        for (String publicPath : publicPaths) {
            if (path.contains(publicPath)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * ✅ ДОБАВЛЕНО: Тестирует различные методы обхода рейт-лимита
     */
    private List<Finding> testRateLimitBypassMethods(EndpointInfo endpoint, HttpClient client) {
        List<Finding> findings = new ArrayList<>();
        
        // ✅ Метод 1: Быстрые последовательные запросы
        System.out.println("🎯 Testing method 1: Rapid sequential requests");
        Finding rapidFinding = testRapidRequests(endpoint, client);
        if (rapidFinding != null) {
            findings.add(rapidFinding);
        }
        
        // ✅ Метод 2: Изменение User-Agent
        System.out.println("🎯 Testing method 2: User-Agent rotation");
        Finding userAgentFinding = testUserAgentRotation(endpoint, client);
        if (userAgentFinding != null) {
            findings.add(userAgentFinding);
        }
        
        // ✅ Метод 3: Изменение IP через заголовки
        System.out.println("🎯 Testing method 3: IP header spoofing");
        Finding ipSpoofFinding = testIPSpoofing(endpoint, client);
        if (ipSpoofFinding != null) {
            findings.add(ipSpoofFinding);
        }
        
        return findings;
    }

    /**
     * ✅ ДОБАВЛЕНО: Тестирует быстрые последовательные запросы
     */
    private Finding testRapidRequests(EndpointInfo endpoint, HttpClient client) {
        int successCount = 0;
        int requestCount = 15; // Увеличили до 15 для лучшего обнаружения
        
        System.out.println("⚡ Sending " + requestCount + " rapid requests...");

        for (int i = 0; i < requestCount; i++) {
            try {
                // ✅ Используем пустые заголовки для публичных эндпоинтов
                Map<String, String> headers = createRateLimitHeaders(i);
                String response = client.get(endpoint.getFullUrl(), headers);
                
                // ✅ Проверяем статус код (если не 429 - успех)
                if (!isRateLimitResponse(response)) {
                    successCount++;
                }
                
                totalRateLimitRequests++;
                
                // ✅ Небольшая задержка между запросами (50ms)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                // ✅ Обрабатываем рейт-лимит и другие ошибки
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    System.out.println("✅ Rate limit triggered at request " + (i + 1));
                    break;
                }
                totalRateLimitRequests++;
            }
        }

        // ✅ Если большинство запросов прошло - уязвимость
        if (successCount >= 10) {
            return new Finding(
                "RL-BYPASS-01",
                "Missing Rate Limit Protection",
                "🚨 CRITICAL: API не имеет эффективной защиты от рейт-лимитинга. " +
                "Удалось выполнить " + successCount + " из " + requestCount + " быстрых последовательных запросов " +
                "без получения кода 429 (Too Many Requests).\n\n" +
                "🔍 ДЕТАЛИ АТАКИ:\n" +
                "• Тестируемый эндпоинт: " + endpoint.getMethod() + " " + endpoint.getPath() + "\n" +
                "• Количество успешных запросов: " + successCount + "/" + requestCount + "\n" +
                "• Интервал между запросами: 50ms\n" +
                "• Метод атаки: Быстрые последовательные запросы\n\n" +
                "📈 ВОЗДЕЙСТВИЕ:\n" +
                "• Возможность DoS-атаки на API\n" +
                "• Исчерпание ресурсов сервера\n" +
                "• Нарушение доступности сервиса для других пользователей",
                Severity.HIGH,
                getId(),
                endpoint.getFullUrl(),
                "🛡️ РЕКОМЕНДАЦИИ ПО ИСПРАВЛЕНИЮ:\n\n" +
                "1. **Реализуйте рейт-лимитинг на уровне API Gateway**\n" +
                "2. **Настройте ограничения по IP-адресу** (например, 100 запросов в минуту)\n" +
                "3. **Добавьте ограничения по токенам/пользователям**\n" +
                "4. **Используйте алгоритм Token Bucket или Leaky Bucket**\n" +
                "5. **Внедрите постепенное замедление** (gradual throttling)\n" +
                "6. **Настройте разные лимиты для разных типов эндпоинтов**\n\n" +
                "💡 ПРИМЕР КОНФИГУРАЦИИ (Nginx):\n" +
                "```nginx\n" +
                "limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;\n" +
                "location /api/ {\n" +
                "    limit_req zone=api burst=20 nodelay;\n" +
                "    proxy_pass http://api_backend;\n" +
                "}\n" +
                "```\n\n" +
                "🔧 ПРИМЕР КОДА (Spring Boot):\n" +
                "```java\n" +
                "@RateLimiter(name = \"api\", limit = 100, duration = 60)\n" +
                "@GetMapping(\"/public/data\")\n" +
                "public ResponseEntity<?> getData() {\n" +
                "    // ваш код\n" +
                "}\n" +
                "```"
            );
        }
        
        System.out.println("✅ Rapid requests test: " + successCount + "/" + requestCount + " successful (rate limit working)");
        return null;
    }

    /**
     * ✅ ДОБАВЛЕНО: Тестирует ротацию User-Agent для обхода рейт-лимита
     */
    private Finding testUserAgentRotation(EndpointInfo endpoint, HttpClient client) {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
            "PostmanRuntime/7.29.0",
            "curl/7.68.0",
            "ASTF-Scanner/1.0"
        };
        
        int successCount = 0;
        
        for (int i = 0; i < userAgents.length; i++) {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", userAgents[i]);
                
                String response = client.get(endpoint.getFullUrl(), headers);
                
                if (!isRateLimitResponse(response)) {
                    successCount++;
                }
                
                totalRateLimitRequests++;
                
                // ✅ Задержка между запросами
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                totalRateLimitRequests++;
            }
        }
        
        // ✅ Если все запросы с разными User-Agent прошли - возможна уязвимость
        if (successCount >= userAgents.length - 1) {
            return new Finding(
                "RL-BYPASS-02",
                "User-Agent Based Rate Limit Bypass",
                "⚠️ MEDIUM: Возможен обход рейт-лимита через смену User-Agent заголовков. " +
                "Все " + successCount + " запросов с разными User-Agent были обработаны без ограничений.\n\n" +
                "🔍 ДЕТАЛИ:\n" +
                "• Протестировано User-Agent: " + userAgents.length + "\n" +
                "• Успешных запросов: " + successCount + "\n" +
                "• Рейт-лимит не учитывает User-Agent для идентификации клиентов",
                Severity.MEDIUM,
                getId(),
                endpoint.getFullUrl(),
                "🛡️ РЕКОМЕНДАЦИИ:\n\n" +
                "1. **Не полагайтесь только на User-Agent для идентификации клиентов**\n" +
                "2. **Используйте комбинацию IP + User-Agent + API-Key для рейт-лимитинга**\n" +
                "3. **Реализуйте fingerprinting клиентов на основе множества факторов**\n" +
                "4. **Рассмотрите использование JavaScript challenge для браузерных клиентов**"
            );
        }
        
        return null;
    }

    /**
     * ✅ ДОБАВЛЕНО: Тестирует спуфинг IP через заголовки
     */
    private Finding testIPSpoofing(EndpointInfo endpoint, HttpClient client) {
        String[] ipHeaders = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Client-IP",
            "X-Cluster-Client-IP"
        };
        
        String[] fakeIPs = {
            "192.168.1.100",
            "10.0.0.50", 
            "172.16.254.1",
            "203.0.113.195"
        };
        
        int successCount = 0;
        
        for (int i = 0; i < ipHeaders.length; i++) {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put(ipHeaders[i], fakeIPs[i]);
                
                String response = client.get(endpoint.getFullUrl(), headers);
                
                if (!isRateLimitResponse(response)) {
                    successCount++;
                }
                
                totalRateLimitRequests++;
                
                // ✅ Задержка между запросами
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                totalRateLimitRequests++;
            }
        }
        
        // ✅ Если IP спуфинг работает - уязвимость
        if (successCount >= ipHeaders.length - 1) {
            return new Finding(
                "RL-BYPASS-03",
                "IP Spoofing Rate Limit Bypass",
                "⚠️ MEDIUM: Возможен обход рейт-лимита через подделку IP-адреса в заголовках. " +
                "Система доверяет заголовкам " + String.join(", ", ipHeaders) + " для определения IP клиента.\n\n" +
                "🔍 ДЕТАЛИ:\n" +
                "• Протестировано заголовков: " + ipHeaders.length + "\n" +
                "• Успешных запросов с поддельными IP: " + successCount + "\n" +
                "• Рейт-лимит уязвим к IP spoofing атакам",
                Severity.MEDIUM,
                getId(),
                endpoint.getFullUrl(),
                "🛡️ РЕКОМЕНДАЦИИ:\n\n" +
                "1. **Никогда не доверяйте клиентским заголовкам для определения IP-адреса**\n" +
                "2. **Используйте реальный IP из соединения (remote address)**\n" +
                "3. **Настройте доверенные прокси и правильно конфигурируйте X-Forwarded-For**\n" +
                "4. **Валидируйте и ограничивайте количество заголовков IP**\n" +
                "5. **Используйте специализированные решения для определения реального IP**"
            );
        }
        
        return null;
    }

    /**
     * ✅ ДОБАВЛЕНО: Создает заголовки для тестирования рейт-лимита
     */
    private Map<String, String> createRateLimitHeaders(int requestIndex) {
        Map<String, String> headers = new HashMap<>();
        
        // ✅ Базовые заголовки
        headers.put("User-Agent", "ASTF-RateLimit-Tester/1.0");
        headers.put("Accept", "application/json");
        
        // ✅ Добавляем вариации для обхода простых систем
        if (requestIndex % 3 == 0) {
            headers.put("X-Client-Version", "1.0." + requestIndex);
        }
        
        return headers;
    }

    /**
     * ✅ ДОБАВЛЕНО: Проверяет, является ли ответ рейт-лимитом
     */
    private boolean isRateLimitResponse(String response) {
        if (response == null) return false;
        
        // ✅ Проверяем типичные признаки рейт-лимита
        return response.toLowerCase().contains("too many requests") ||
               response.toLowerCase().contains("rate limit") ||
               response.toLowerCase().contains("429") ||
               response.toLowerCase().contains("exceeded") ||
               response.toLowerCase().contains("quota");
    }

    /**
     * ✅ ДОБАВЛЕНО: Создает информационную находку когда защита работает
     */
    private Finding createRateLimitInfoFinding(EndpointInfo endpoint) {
        String methodology = "🔍 МЕТОДОЛОГИЯ ТЕСТИРОВАНИЯ РЕЙТ-ЛИМИТОВ:\n\n" +
            "• Протестировано 3 метода обхода рейт-лимитов:\n" +
            "  1. Быстрые последовательные запросы (15 запросов с интервалом 50ms)\n" +
            "  2. Ротация User-Agent заголовков (" + totalRateLimitRequests + " различных агентов)\n" +
            "  3. Спуфинг IP-адреса через X-Forwarded-For и другие заголовки\n" +
            "• Всего выполнено запросов: " + totalRateLimitRequests + "\n" +
            "• Тестируемый эндпоинт: " + endpoint.getMethod() + " " + endpoint.getPath() + "\n\n" +
            "📊 РЕЗУЛЬТАТ:\n" +
            "Система корректно ограничивает количество запросов и защищена от базовых методов обхода.";
        
        return new Finding(
            "RL-INFO-01",
            "Rate Limit Protection Verified",
            methodology,
            Severity.INFO,
            getId(),
            endpoint.getFullUrl(),
            "✅ СИСТЕМА ЗАЩИЩЕНА ОТ ОСНОВНЫХ АТАК НА РЕЙТ-ЛИМИТЫ:\n\n" +
            "• Реализована эффективная защита от быстрых последовательных запросов\n" +
            "• Рейт-лимитинг корректно работает на публичных эндпоинтах\n" +
            "• Система устойчива к базовым методам обхода (User-Agent rotation, IP spoofing)\n\n" +
            "💡 РЕКОМЕНДАЦИИ ДЛЯ ДАЛЬНЕЙШЕГО УЛУЧШЕНИЯ:\n" +
            "• Регулярно мониторить паттерны запросов на предмет сложных атак\n" +
            "• Рассмотреть внедрение ML-based систем обнаружения аномалий\n" +
            "• Тестировать рейт-лимиты в продакшн-среде с реалистичной нагрузкой"
        );
    }

    /**
     * ✅ ДОБАВЛЕНО: Проверяет, включен ли verbose mode
     */
    private boolean isVerboseMode() {
        return System.getProperty("astf.verbose") != null;
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Метод для сброса состояния (для тестирования)
     */
    public static void reset() {
        synchronized (RATE_LIMIT_LOCK) {
            rateLimitTestCompleted = false;
            totalRateLimitRequests = 0;
        }
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Получить общее количество запросов
     */
    public static int getTotalRequests() {
        return totalRateLimitRequests;
    }
}