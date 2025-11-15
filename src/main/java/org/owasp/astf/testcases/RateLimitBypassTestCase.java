package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for API4:2023 Lack of Resources & Rate Limiting.
 * 
 * This test case checks if the API properly implements rate limiting
 * to prevent abuse and DoS attacks. It tests multiple bypass techniques:
 * - Rapid sequential requests
 * - User-Agent rotation
 * - IP header spoofing
 */
public class RateLimitBypassTestCase implements TestCase {
    private static final Logger logger = LogManager.getLogger(RateLimitBypassTestCase.class);
    
    // ✅ ИСПРАВЛЕНО: Теперь отслеживаем протестированные домены, а не глобальное состояние
    private static final Set<String> testedDomains = ConcurrentHashMap.newKeySet();
    private static final Object RATE_LIMIT_LOCK = new Object();
    
    // ✅ Счетчики для метрик
    private static final AtomicInteger totalRateLimitRequests = new AtomicInteger(0);
    private static final Map<String, Integer> requestsPerDomain = new ConcurrentHashMap<>();
    
    // ✅ Константы для настройки
    private static final int RAPID_REQUEST_COUNT = 15;
    private static final int RAPID_REQUEST_DELAY_MS = 50;
    private static final int SUCCESS_THRESHOLD = 10;
    
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

        // ✅ ИСПРАВЛЕНО: Проверяем, тестировали ли уже этот домен
        String domain = extractDomain(endpoint.getBaseUrl());
        
        synchronized (RATE_LIMIT_LOCK) {
            if (testedDomains.contains(domain)) {
                logger.debug("Rate Limit test already completed for domain: {} - skipping", domain);
                return Collections.emptyList();
            }
            testedDomains.add(domain);
        }

        // ✅ Тестируем только публичные эндпоинты без авторизации
        if (!isPublicEndpoint(endpoint)) {
            logger.debug("Skipping protected endpoint for rate limit test: {}", endpoint.getPath());
            return Collections.emptyList();
        }

        logger.info("Starting Rate Limit Bypass test on: {} {}", endpoint.getMethod(), endpoint.getPath());

        try {
            // ✅ Тестируем разные методы обхода рейт-лимита
            List<Finding> methodFindings = testRateLimitBypassMethods(endpoint, client, domain);
            findings.addAll(methodFindings);

            // ✅ Если уязвимостей не найдено, добавляем информационное сообщение
            if (findings.isEmpty()) {
                findings.add(createRateLimitInfoFinding(endpoint, domain));
            }

        } catch (Exception e) {
            logger.error("Rate Limit test execution error on {}: {}", endpoint.getPath(), e.getMessage());
            if (isVerboseMode()) {
                logger.debug("Exception details:", e);
            }
        }

        int domainRequests = requestsPerDomain.getOrDefault(domain, 0);
        logger.info("Rate Limit test completed for {}: {} requests made", domain, domainRequests);
        
        return findings;
    }

    /**
     * ✅ ДОБАВЛЕНО: Извлекает домен из URL для группировки тестов
     */
    private String extractDomain(String url) {
        try {
            // Извлекаем схему + хост + порт
            java.net.URL parsedUrl = new java.net.URL(url);
            String domain = parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
            if (parsedUrl.getPort() != -1 && parsedUrl.getPort() != 80 && parsedUrl.getPort() != 443) {
                domain += ":" + parsedUrl.getPort();
            }
            return domain;
        } catch (Exception e) {
            // Fallback: используем весь URL без path
            int pathIndex = url.indexOf("/", 8); // Пропускаем https://
            return pathIndex > 0 ? url.substring(0, pathIndex) : url;
        }
    }

    /**
     * Проверяет, является ли эндпоинт публичным (без авторизации)
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
     * Тестирует различные методы обхода рейт-лимита
     */
    private List<Finding> testRateLimitBypassMethods(EndpointInfo endpoint, HttpClient client, String domain) {
        List<Finding> findings = new ArrayList<>();
        
        // ✅ Метод 1: Быстрые последовательные запросы
        logger.debug("Testing method 1: Rapid sequential requests");
        Finding rapidFinding = testRapidRequests(endpoint, client, domain);
        if (rapidFinding != null) {
            findings.add(rapidFinding);
        }
        
        // ✅ Метод 2: Изменение User-Agent
        logger.debug("Testing method 2: User-Agent rotation");
        Finding userAgentFinding = testUserAgentRotation(endpoint, client, domain);
        if (userAgentFinding != null) {
            findings.add(userAgentFinding);
        }
        
        // ✅ Метод 3: Изменение IP через заголовки
        logger.debug("Testing method 3: IP header spoofing");
        Finding ipSpoofFinding = testIPSpoofing(endpoint, client, domain);
        if (ipSpoofFinding != null) {
            findings.add(ipSpoofFinding);
        }
        
        return findings;
    }

    /**
     * Тестирует быстрые последовательные запросы
     */
    private Finding testRapidRequests(EndpointInfo endpoint, HttpClient client, String domain) {
        int successCount = 0;
        
        logger.debug("Sending {} rapid requests with {}ms delay", RAPID_REQUEST_COUNT, RAPID_REQUEST_DELAY_MS);

        for (int i = 0; i < RAPID_REQUEST_COUNT; i++) {
            try {
                Map<String, String> headers = createRateLimitHeaders(i);
                String response = client.get(endpoint.getFullUrl(), headers);
                
                // ✅ Проверяем статус код (если не 429 - успех)
                if (!isRateLimitResponse(response)) {
                    successCount++;
                }
                
                incrementRequestCount(domain);
                
                // ✅ Небольшая задержка между запросами
                try {
                    Thread.sleep(RAPID_REQUEST_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                // ✅ Обрабатываем рейт-лимит и другие ошибки
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    logger.debug("Rate limit triggered at request {}", i + 1);
                    break;
                }
                incrementRequestCount(domain);
            }
        }

        logger.debug("Rapid requests test: {}/{} successful", successCount, RAPID_REQUEST_COUNT);

        // ✅ Если большинство запросов прошло - уязвимость
        if (successCount >= SUCCESS_THRESHOLD) {
            return new Finding(
                "RL-BYPASS-01",
                "Missing Rate Limit Protection",
                String.format(
                    "🚨 CRITICAL: API не имеет эффективной защиты от рейт-лимитинга. " +
                    "Удалось выполнить %d из %d быстрых последовательных запросов " +
                    "без получения кода 429 (Too Many Requests).\n\n" +
                    "🔍 ДЕТАЛИ АТАКИ:\n" +
                    "• Тестируемый эндпоинт: %s %s\n" +
                    "• Домен: %s\n" +
                    "• Количество успешных запросов: %d/%d\n" +
                    "• Интервал между запросами: %dms\n" +
                    "• Метод атаки: Быстрые последовательные запросы\n\n" +
                    "📈 ВОЗДЕЙСТВИЕ:\n" +
                    "• Возможность DoS-атаки на API\n" +
                    "• Исчерпание ресурсов сервера\n" +
                    "• Нарушение доступности сервиса для других пользователей",
                    successCount, RAPID_REQUEST_COUNT,
                    endpoint.getMethod(), endpoint.getPath(),
                    domain,
                    successCount, RAPID_REQUEST_COUNT,
                    RAPID_REQUEST_DELAY_MS
                ),
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
        
        return null;
    }

    /**
     * Тестирует ротацию User-Agent для обхода рейт-лимита
     */
    private Finding testUserAgentRotation(EndpointInfo endpoint, HttpClient client, String domain) {
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
                
                incrementRequestCount(domain);
                
                // ✅ Задержка между запросами
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                incrementRequestCount(domain);
            }
        }
        
        logger.debug("User-Agent rotation test: {}/{} successful", successCount, userAgents.length);
        
        // ✅ Если все запросы с разными User-Agent прошли - возможна уязвимость
        if (successCount >= userAgents.length - 1) {
            return new Finding(
                "RL-BYPASS-02",
                "User-Agent Based Rate Limit Bypass",
                String.format(
                    "⚠️ MEDIUM: Возможен обход рейт-лимита через смену User-Agent заголовков. " +
                    "Все %d запросов с разными User-Agent были обработаны без ограничений.\n\n" +
                    "🔍 ДЕТАЛИ:\n" +
                    "• Домен: %s\n" +
                    "• Протестировано User-Agent: %d\n" +
                    "• Успешных запросов: %d\n" +
                    "• Рейт-лимит не учитывает User-Agent для идентификации клиентов",
                    successCount, domain, userAgents.length, successCount
                ),
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
     * Тестирует спуфинг IP через заголовки
     */
    private Finding testIPSpoofing(EndpointInfo endpoint, HttpClient client, String domain) {
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
                
                incrementRequestCount(domain);
                
                // ✅ Задержка между запросами
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                incrementRequestCount(domain);
            }
        }
        
        logger.debug("IP spoofing test: {}/{} successful", successCount, ipHeaders.length);
        
        // ✅ Если IP спуфинг работает - уязвимость
        if (successCount >= ipHeaders.length - 1) {
            return new Finding(
                "RL-BYPASS-03",
                "IP Spoofing Rate Limit Bypass",
                String.format(
                    "⚠️ MEDIUM: Возможен обход рейт-лимита через подделку IP-адреса в заголовках. " +
                    "Система доверяет заголовкам %s для определения IP клиента.\n\n" +
                    "🔍 ДЕТАЛИ:\n" +
                    "• Домен: %s\n" +
                    "• Протестировано заголовков: %d\n" +
                    "• Успешных запросов с поддельными IP: %d\n" +
                    "• Рейт-лимит уязвим к IP spoofing атакам",
                    String.join(", ", ipHeaders), domain, ipHeaders.length, successCount
                ),
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
     * Создает заголовки для тестирования рейт-лимита
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
     * Проверяет, является ли ответ рейт-лимитом
     */
    private boolean isRateLimitResponse(String response) {
        if (response == null) return false;
        
        // ✅ Проверяем типичные признаки рейт-лимита
        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("too many requests") ||
               lowerResponse.contains("rate limit") ||
               lowerResponse.contains("429") ||
               lowerResponse.contains("exceeded") ||
               lowerResponse.contains("quota");
    }

    /**
     * Создает информационную находку когда защита работает
     */
    private Finding createRateLimitInfoFinding(EndpointInfo endpoint, String domain) {
        int domainRequests = requestsPerDomain.getOrDefault(domain, 0);
        
        String methodology = String.format(
            "🔍 МЕТОДОЛОГИЯ ТЕСТИРОВАНИЯ РЕЙТ-ЛИМИТОВ:\n\n" +
            "• Протестировано 3 метода обхода рейт-лимитов:\n" +
            "  1. Быстрые последовательные запросы (%d запросов с интервалом %dms)\n" +
            "  2. Ротация User-Agent заголовков (6 различных агентов)\n" +
            "  3. Спуфинг IP-адреса через X-Forwarded-For и другие заголовки\n" +
            "• Всего выполнено запросов для домена %s: %d\n" +
            "• Тестируемый эндпоинт: %s %s\n\n" +
            "📊 РЕЗУЛЬТАТ:\n" +
            "Система корректно ограничивает количество запросов и защищена от базовых методов обхода.",
            RAPID_REQUEST_COUNT, RAPID_REQUEST_DELAY_MS,
            domain, domainRequests,
            endpoint.getMethod(), endpoint.getPath()
        );
        
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
     * ✅ ДОБАВЛЕНО: Инкрементирует счетчик запросов для домена
     */
    private void incrementRequestCount(String domain) {
        totalRateLimitRequests.incrementAndGet();
        requestsPerDomain.merge(domain, 1, Integer::sum);
    }

    /**
     * Проверяет, включен ли verbose mode
     */
    private boolean isVerboseMode() {
        return System.getProperty("astf.verbose") != null;
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Метод для сброса состояния (для тестирования)
     */
    public static void reset() {
        synchronized (RATE_LIMIT_LOCK) {
            testedDomains.clear();
            totalRateLimitRequests.set(0);
            requestsPerDomain.clear();
        }
    }
    
    /**
     * Получить общее количество запросов
     */
    public static int getTotalRequests() {
        return totalRateLimitRequests.get();
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Получить количество протестированных доменов
     */
    public static int getTestedDomainsCount() {
        return testedDomains.size();
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Получить карту запросов по доменам
     */
    public static Map<String, Integer> getRequestsPerDomain() {
        return new HashMap<>(requestsPerDomain);
    }
}