# Архитектура GOSTbusters ASTF

## Обзор

GOSTbusters ASTF — это модульный фреймворк для автоматизированного тестирования безопасности API, разработанный специально для хакатона **VTB API Security Hackathon 2025**. Архитектура построена на принципах модульности, расширяемости и соответствия современным Java-стандартам (Java 21).

---

## Компонентная архитектура

```
+----------------------------------+
|            CLI Layer             |
|  (ASTFCli, CICommand)            |
+----------------------------------+
                   |
+----------------------------------+
|          Scanner Core            |
|  (Scanner, ConfigLoader)         |
+----------------------------------+
       /            |             \
      /             |              \
+--------+   +---------------+   +---------------+
| HTTP   |   | Test Cases    |   | Reporting     |
| Client |   | & Plugins     |   | & Integrations|
+--------+   +---------------+   +---------------+
```

### Ключевые компоненты

1. **CLI Layer** (`org.owasp.astf.cli`)
   - `ASTFCli`: основной интерфейс для запуска сканирования с поддержкой аргументов (`--target`, `--openapi`, `--use-gost`, `--header` и др.)
   - `CICommand`: интеграция с CI/CD (GitHub Actions) через `picocli`
   - Обработка аутентификации, заголовков и спецификаций OpenAPI

2. **Scanner Core** (`org.owasp.astf.core`)
   - `Scanner`: оркестратор сканирования, запускает виртуальные потоки (Java 21)
   - `ConfigLoader`: загрузка конфигурации из файлов, переменных окружения, CLI
   - `EndpointDiscoveryService`: автоматическое обнаружение эндпоинтов (резервная стратегия)
   - `OpenApiLoader`: парсинг OpenAPI 3.1+ спецификаций

3. **HTTP Client** (`org.owasp.astf.core.http`)
   - Унифицированный клиент на основе OkHttp3
   - Поддержка всех HTTP-методов, заголовков, тел запросов
   - Обработка статус-кодов и таймаутов
   - Встроенная поддержка куки

4. **Test Cases & Plugins** (`org.owasp.astf.testcases`, `plugin-api`)
   - **Встроенные тест-кейсы**: полное покрытие OWASP API Security Top 10 2023
   - **Плагинная архитектура**: динамическая загрузка плагинов через SPI (`PluginLoader`)
   - `TestCaseRegistry`: управление тест-кейсами (встроенные + адаптированные плагины)
   - Поддержка банковской специфики (межбанковские заголовки, согласия)

5. **Reporting & Integrations** (`org.owasp.astf.reporting`, `org.owasp.astf.integrations`)
   - **Генераторы отчётов**: JSON, HTML, XML, SARIF
   - **CI/CD-интеграции**: GitHub Actions (аннотации, SARIF upload, fail-on-findings)
   - **Безопасность**: маскировка токенов, санитизация чувствительных данных

6. **Специфика хакатона**
   - `specifications/`: OpenAPI-спецификации VBank, ABank, SBank
   - `get_tokens.py`, `get_sbank_consent.py`: автоматизация получения токенов и согласий
   - Поддержка **ГОСТ-шлюза** через флаг `--use-gost`

---

## Поток данных (Data Flow)

1. Пользователь запускает CLI:  
   `java -jar astf.jar scan --target https://vbank.open.bankingapi.ru ...`
2. `ASTFCli` парсит аргументы и создаёт `ScanConfig`
3. `Scanner` обрабатывает конфигурацию:
   - Если указан `--use-gost` → перенаправляет трафик на `api.gost.bankingapi.ru:8443`
   - Если указан `--openapi` → загружает эндпоинты через `OpenApiLoader`
4. Для каждого эндпоинта запускаются все активные тест-кейсы:
   - Встроенные (из `TestCaseRegistry`)
   - Динамически загруженные плагины (из папки `plugins/`)
5. Каждый `TestCase` использует `HttpClient` для отправки запросов
6. Результаты (`Finding`) агрегируются в `ScanResult`
7. `ReportGenerator` создаёт отчёт в указанном формате
8. При запуске через `CICommand` — результаты публикуются в GitHub Actions

---

## Ключевые интерфейсы

### TestCase (встроенные тесты)

```java
public interface TestCase {
    String getId();          // Например: "BOLA", "ASTF-API2-2023"
    String getName();        // Например: "Broken Object Level Authorization"
    String getDescription();
    List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException;
}
```

### Plugin (плагинная архитектура)

```java
public interface Plugin {
    String getId();          // Уникальный ID плагина
    String getName();        // Имя плагина
    String getDescription();
    List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException;
}
```

Плагины автоматически адаптируются к `TestCase` через `PluginAsTestCaseAdapter` (временно отключено, но архитектура подготовлена).

### EndpointInfo

```java
public class EndpointInfo {
    private final String baseUrl;       // Например: https://vbank.open.bankingapi.ru
    private final String path;          // Например: /accounts/{account_id}
    private final String method;        // GET, POST и т.д.
    private final boolean requiresAuth; // Требуется ли аутентификация
    // ... геттеры и getFullUrl()
}
```

### Finding

```java
public class Finding {
    private final String id;            // Уникальный ID уязвимости
    private final String title;         // Краткое название
    private final String description;   // Подробное описание
    private final Severity severity;    // CRITICAL, HIGH, MEDIUM, LOW, INFO
    private final String testCaseId;    // ID тест-кейса или плагина
    private final String endpoint;      // Пострадавший эндпоинт
    private final String remediation;   // Рекомендации по исправлению
}
```

---

## Архитектурные принципы

1. **Модульность**: Чёткое разделение на модули через Maven (`core`, `plugin-api`, `reporting`, `cli`).
2. **Расширяемость**: 
   - Добавление тестов — через реализацию `TestCase`
   - Расширение функционала — через JAR-плагины в папке `plugins/`
3. **Соответствие стандартам**: 
   - OWASP API Security Top 10 2023
   - OpenAPI 3.1+
   - Open Banking Russia v2.1
4. **Производительность**: 
   - Виртуальные потоки (Java 21)
   - Многопоточность с настраиваемым количеством потоков (`--threads`)
5. **Безопасность по умолчанию**: 
   - Маскировка токенов в логах
   - Санитизация отчётов
   - Защита от SSRF/XXE в HTTP-клиенте

---

## Модель потоков

- Используется `Executors.newVirtualThreadPerTaskExecutor()` (Java 21)
- Один виртуальный поток на комбинацию «эндпоинт + тест-кейс»
- Максимальное количество одновременных запросов ограничено параметром `--threads` (по умолчанию: 10)
- Результаты синхронизируются через `synchronized` блоки и `ConcurrentHashMap`

---

## Как добавить новый тест-кейс

1. Создайте класс в `src/main/java/org/owasp/astf/testcases/`, реализующий `TestCase`
2. Реализуйте методы `getId()`, `getName()`, `getDescription()`, `execute()`
3. Зарегистрируйте тест в `TestCaseRegistry.registerDefaultTestCases()`
4. (Опционально) Напишите unit-тест в `src/test/java/...`

Пример:
```java
public class MyNewTestCase implements TestCase {
    @Override
    public String getId() { return "MY-NEW-TEST"; }
    @Override
    public String getName() { return "My New Security Test"; }
    @Override
    public String getDescription() { return "Тестирует новую уязвимость"; }
    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) {
        // Логика проверки...
        return findings;
    }
}
```

---

## Планы по архитектуре (до финала)

1. **Полная мультимодульность**: выделение `core`, `openapi`, `testcases`, `reporting` в отдельные Maven-модули.
2. **Улучшенная плагинная система**: поддержка версионирования, зависимостей между плагинами, горячей перезагрузки.
3. **Графический интерфейс**: разработка JavaFX UI для настройки и запуска сканирования.
4. **Расширенная валидация контракта**: полное сравнение OpenAPI-схемы и реального ответа (типы, required, enum, форматы).
5. **Фаззинг**: автоматическая генерация payload'ов для эндпоинтов.

---