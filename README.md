

### **Архитектура решения**


Вот структура нашего проекта `GOSTbusters-ASTF-final-version`:

![alt text](assets/images/GOSTbusters_logo_1.png)

```
├── src/main/java/org/owasp/astf/
│   ├── core/                   - ядро фреймворка: Scanner, ScanConfig, EndpointInfo, HttpClient
│   │   └── result/             - модели результатов: Finding, Severity, ScanResult
│   │
│   ├── cli/                    - командная строка: ASTFCli, CICommand (запуск сканирования)
│   │
│   ├── testcases/              - набор тест-кейсов для проверки уязвимостей (OWASP API Top 10)
│   │   ├── BolaTestCase.java   - Проверка на BOLA (Broken Object Level Authorization)
│   │   ├── BrokenAuthenticationTestCase.java - Проверка аутентификации
│   │   ├── JWTTestCase.java    - Проверка уязвимостей JWT
│   │   ├── MassAssignmentTestCase.java - Проверка Mass Assignment
│   │   ├── RateLimitBypassTestCase.java - Проверка обхода рейт-лимитов
│   │   ├── SSRFTestCase.java   - Проверка SSRF (Server-Side Request Forgery)
│   │   └── ...                 - и другие тесты из OWASP API Top 10
│   │
│   ├── plugin/                 - API для плагинов (Plugin, PluginLoader)
│   │
│   └── reporting/              - генерация отчетов (JsonReportGenerator)
│
├── plugin-api/                 - библиотека для создания плагинов (shared-классы)
│   └── src/main/java/org/owasp/astf/plugin/
│       ├── Plugin.java         - интерфейс плагина
│       └── PluginLoader.java   - загрузчик плагинов из папки plugins/
│
├── example-bola-plugin/        - пример плагина для тестирования BOLA
│   └── src/main/java/com/example/plugins/ExampleBolaPlugin.java
│
├── specifications/             - файлы спецификаций OpenAPI для банков
│   ├── abank-openapi.json      - спецификация для ABank
│   ├── sbank-openapi.json      - спецификация для SBank
│   └── vbank-openapi.json      - спецификация для VBank
│
├── src/test/java/org/owasp/astf/testcases/ - юнит-тесты для тест-кейсов
│   └── BrokenAuthenticationTestcaseTest.java - пример теста
│
├── src/main/resources/         - ресурсы (логгеры, конфигурации)
│
├── pom.xml                     - основной файл сборки Maven
├── dependency-reduced-pom.xml  - сокращенный POM для итогового JAR
├── run_scan_auto.bat           - скрипт для автоматического запуска сканирования всех банков
├── run_scan_vbank.bat          - скрипт для запуска сканирования VBank
├── get_tokens.py               - Python-скрипт для получения токена банка
└── get_sbank_consent.py        - Python-скрипт для получения согласия для SBank

Основное ядро: SecurityScanner (реализован в классе Scanner).
Он получает спецификацию OpenAPI (через ConfigLoader), запускает параллельно все тест-кейсы (из TestCaseRegistry),
объединяет их результаты (Finding) и сохраняет отчёт (через JsonReportGenerator).
Для расширения функционала используется механизм плагинов (Plugin API).
```

Эта структура отражает ключевые компоненты нашего проекта:
* **Ядро (`core`)**: Отвечает за логику сканирования.
* **Тест-кейсы (`testcases`)**: Реализуют конкретные проверки по стандарту OWASP API Top 10.
* **Плагины (`plugin-api`)**: Предоставляют возможность расширять функциональность без изменения основного кода.
* **Интерфейсы (`cli`)**: Позволяют запускать сканирование через командную строку или CI/CD.
* **Спецификации (`specifications`)**: Определяют API, которые будут тестироваться.
* **Вспомогательные скрипты**: Автоматизируют процесс получения токенов и запуска сканирования.


Наше решение — это расширяемый фреймворк для автоматизированного тестирования безопасности API, названный **OWASP API Security Testing Framework (ASTF)**. Он построен на принципах модульности и конвейерной обработки данных, что позволяет легко добавлять новые тесты и интегрироваться с внешними системами.

**Основные компоненты:**

1.  **Ядро (Core):**
    *   **`Scanner`**: Главный класс, управляющий процессом сканирования. Он загружает спецификации API (OpenAPI), определяет эндпоинты и запускает набор тест-кейсов.
    *   **`ScanConfig`**: Конфигурация сканирования, содержащая целевой URL, токен авторизации, путь к OpenAPI-спецификации и другие параметры.
    *   **`EndpointInfo`**: Модель данных, представляющая один эндпоинт API (базовый URL, путь, метод, требует ли аутентификации).
    *   **`HttpClient`**: Унифицированный HTTP-клиент для выполнения запросов к API.
    *   **`Finding` / `ScanResult`**: Структуры данных для хранения результатов тестирования (находки, их серьезность, описание, рекомендации).

2.  **Тест-кейсы (Test Cases):**
    *   Реализованы как отдельные классы, реализующие интерфейс `TestCase`. Каждый класс отвечает за проверку одной конкретной уязвимости (например, `BolaTestCase`, `BrokenAuthenticationTestCase`, `RateLimitBypassTestCase`).
    *   Все тест-кейсы регистрируются в `TestCaseRegistry` и запускаются параллельно через `Scanner`.
    *   **Ключевая особенность:** Поддержка плагинов. Мы реализовали механизм загрузки плагинов (через `PluginLoader` и интерфейс `Plugin`), что позволяет сторонним разработчикам добавлять свои собственные тесты без изменения основного кода фреймворка. Пример: `ExampleBolaPlugin`.

3.  **Интеграции (Integrations):**
    *   Модуль `IntegrationManager` позволяет интегрировать фреймворк с CI/CD-системами (например, GitHub Actions). Он может автоматически определять окружение, конфигурировать сканирование и публиковать результаты.

4.  **Отчетность (Reporting):**
    *   Модуль `ReportGeneratorFactory` создает отчеты в различных форматах: JSON, HTML, SARIF, XML. Это позволяет легко интегрировать результаты в системы управления безопасностью и CI/CD.

5.  **Утилиты (Utils):**
    *   Включают в себя `ConfigLoader` для загрузки конфигураций из файлов, `OpenApiLoader` для парсинга спецификаций OpenAPI, а также вспомогательные классы для работы с данными.

6.  **CLI (Command Line Interface):**
    *   Основной способ взаимодействия с фреймворком. Пользователь запускает сканирование через командную строку, передавая необходимые параметры (целевой URL, токен, путь к спецификации и т.д.).

7.  **Спецификации (Specifications):**
    *   Отдельная папка содержит файлы OpenAPI (`abank-openapi.json`, `sbank-openapi.json`, `vbank-openapi.json`), которые используются для определения эндпоинтов API банков.

8.  **Плагины (Plugins):**
    *   Отдельный подпроект `example-bola-plugin`, демонстрирующий, как можно создавать и подключать пользовательские тесты. Плагин компилируется в JAR-файл и помещается в папку `plugins/`, откуда его автоматически подгружает `PluginLoader`.

---

### **Основной флоу работы решения**

1.  **Запуск:** Пользователь запускает сканер через командную строку (например, `java -jar astf.jar scan --target https://vbank.open.bankingapi.ru --auth-header "Authorization: Bearer ..."`). В качестве аргументов передаются целевой URL, токен авторизации и путь к OpenAPI-спецификации.
2.  **Конфигурация:** Класс `ASTFCli` парсит аргументы командной строки и создает объект `ScanConfig`.
3.  **Загрузка спецификации:** `Scanner` использует `OpenApiLoader` для загрузки и парсинга OpenAPI-спецификации. На основе этой спецификации он генерирует список всех доступных эндпоинтов (`EndpointInfo`).
4.  **Запуск тестов:** `Scanner` получает список всех активных тест-кейсов из `TestCaseRegistry` (включая плагины, если они есть) и запускает их параллельно.
5.  **Выполнение тестов:** Каждый тест-кейс (`TestCase`) получает на вход один эндпоинт и HTTP-клиент. Он выполняет серию HTTP-запросов, имитируя атаки (например, попытку получить чужой аккаунт или обойти лимиты запросов), и анализирует ответы сервера.
6.  **Сбор результатов:** Если тест-кейс обнаруживает уязвимость, он создает объект `Finding` и добавляет его в общий список находок.
7.  **Генерация отчета:** После завершения всех тестов `Scanner` передает список находок в `JsonReportGenerator` (или другой генератор, в зависимости от настроек), который создает итоговый отчет в указанном формате (например, `scan_results.json`).
8.  **Вывод:** Результаты сканирования выводятся в консоль и сохраняются в файл.

---

### **Основные архитектурные особенности, выделяющие наше решение**

1.  **Механизм плагинов (Plugin Architecture):** Это ключевое отличие нашего решения. Вместо того чтобы жестко прописывать все тесты в ядре, мы предоставляем интерфейс `Plugin`, позволяя любому участнику хакатона написать свой собственный тест (например, для проверки уникальной уязвимости, не описанной в OWASP Top 10) и подключить его к фреймворку. Это делает наш инструмент универсальным и масштабируемым.
2.  **Поддержка OpenAPI-спецификаций:** Наш сканер не просто отправляет случайные запросы. Он понимает структуру API, используя спецификации OpenAPI. Это позволяет ему точно определять эндпоинты, методы и параметры, что делает тестирование более целенаправленным и эффективным.
3.  **Модульность и четкое разделение ответственности:** Архитектура построена на принципах SOLID. Каждый компонент (сканер, тест-кейс, генератор отчетов, плагин) имеет одну, четко определенную задачу. Это делает код легко читаемым, тестируемым и поддерживаемым.
4.  **Интеграция с CI/CD:** Реализована возможность автоматического запуска сканирования в CI/CD-пайплайнах (через `CICommand` и `IntegrationManager`). Это позволяет встраивать безопасность API прямо в процесс разработки.
5.  **Гибкая конфигурация:** Пользователь может детально настраивать сканирование: выбирать конкретные тест-кейсы, задавать количество потоков, время выполнения, формат отчета и т.д., что делает инструмент удобным для разных сценариев использования.


Cтруктура моего проекта:

PS C:\Users\user\Desktop\Новая папка\GOSTbusters-ASTF-final-version> tree /F /A
Структура папок
Серийный номер тома: 70D5-C1A0

```
C:.
|   .gitignore
|   dependency-reduced-pom.xml
|   get_sbank_consent.py
|   get_tokens.py
|   MANIFEST.MF
|   pom.xml
|   README.md
|   run_scan_auto.bat
|   run_scan_vbank.bat
|   SaveFilesContent.ps1
|
+---assets
|   \---images
|           GOSTbusters_logo_1.png
|           GOSTbusters_logo_2.png
|
+---core
|   \---target
|       \---maven-status
|           \---maven-compiler-plugin
|               \---compile
|                   \---default-compile
|                           createdFiles.lst
|                           inputFiles.lst
|
+---docs
|       ARCHITECTURE.md
|       FRAMEWORK_OVERVIEW.md
|       INNOVATION.md
|
+---example-bola-plugin
|   |   pom.xml
|   |
|   \---src
|       \---main
|           +---java
|           |   \---com
|           |       \---example
|           |           \---plugins
|           |                   ExampleBolaPlugin.java
|           |
|           \---resources
|               \---META-INF
|                   \---services
|                           org.owasp.astf.plugin.Plugin
|
+---META-INF
|       MANIFEST.MF
|
+---plugin-api
|   |   pom.xml
|   |
|   +---src
|   |   \---main
|   |       +---java
|   |       |   \---org
|   |       |       \---owasp
|   |       |           \---astf
|   |       |               +---plugin
|   |       |               |       Plugin.java
|   |       |               |       PluginLoader.java
|   |       |               |
|   |       |               \---shared
|   |       |                   |   EndpointInfo.java
|   |       |                   |   HttpClient.java
|   |       |                   |
|   |       |                   \---result
|   |       |                           Finding.java
|   |       |
|   |       \---resources
|   |           \---META-INF
|   |               \---services
|   |                       org.owasp.astf.plugin.Plugin
|   |
|   \---target
|       |   maven-javadoc-plugin-stale-data.txt
|       |   original-plugin-api-1.0-SNAPSHOT.jar
|       |   plugin-api-1.0-SNAPSHOT-javadoc.jar
|       |   plugin-api-1.0-SNAPSHOT-sources.jar
|       |   plugin-api-1.0-SNAPSHOT.jar
|       |
|       +---classes
|       |   +---META-INF
|       |   |   \---services
|       |   |           org.owasp.astf.plugin.Plugin
|       |   |
|       |   \---org
|       |       \---owasp
|       |           \---astf
|       |               +---plugin
|       |               |       Plugin.class
|       |               |       PluginLoader.class
|       |               |
|       |               \---shared
|       |                   |   EndpointInfo.class
|       |                   |
|       |                   +---http
|       |                   |       HttpClient.class
|       |                   |
|       |                   \---result
|       |                           Finding.class
|       |                           Severity.class
|       |
|       +---javadoc-bundle-options
|       |       javadoc-options-javadoc-resources.xml
|       |
|       +---maven-archiver
|       |       pom.properties
|       |
|       +---maven-status
|       |   \---maven-compiler-plugin
|       |       \---compile
|       |           \---default-compile
|       |                   createdFiles.lst
|       |                   inputFiles.lst
|       |
|       \---reports
|           \---apidocs
|               |   allclasses-index.html
|               |   allpackages-index.html
|               |   copy.svg
|               |   element-list
|               |   help-doc.html
|               |   index-all.html
|               |   index.html
|               |   link.svg
|               |   member-search-index.js
|               |   module-search-index.js
|               |   overview-summary.html
|               |   overview-tree.html
|               |   package-search-index.js
|               |   script.js
|               |   search-page.js
|               |   search.html
|               |   search.js
|               |   stylesheet.css
|               |   tag-search-index.js
|               |   type-search-index.js
|               |
|               +---legal
|               |       ADDITIONAL_LICENSE_INFO
|               |       ASSEMBLY_EXCEPTION
|               |       jquery.md
|               |       jqueryUI.md
|               |       LICENSE
|               |
|               +---org
|               |   \---owasp
|               |       \---astf
|               |           +---plugin
|               |           |   |   package-summary.html
|               |           |   |   package-tree.html
|               |           |   |   package-use.html
|               |           |   |   Plugin.html
|               |           |   |   PluginLoader.html
|               |           |   |
|               |           |   \---class-use
|               |           |           Plugin.html
|               |           |           PluginLoader.html
|               |           |
|               |           \---shared
|               |               |   EndpointInfo.html
|               |               |   package-summary.html
|               |               |   package-tree.html
|               |               |   package-use.html
|               |               |
|               |               +---class-use
|               |               |       EndpointInfo.html
|               |               |
|               |               \---result
|               |                   |   Finding.html
|               |                   |   package-summary.html
|               |                   |   package-tree.html
|               |                   |   package-use.html
|               |                   |
|               |                   \---class-use
|               |                           Finding.html
|               |
|               +---resources
|               |       glass.png
|               |       x.png
|               |
|               \---script-dir
|                       jquery-3.7.1.min.js
|                       jquery-ui.min.css
|                       jquery-ui.min.js
|
+---specifications
|       abank-openapi.json
|       sbank-openapi.json
|       vbank-openapi.json
|       vbank-openapi.yaml
|
\---src
    +---main
    |   \---java
    |       \---org
    |           \---owasp
    |               \---astf
    |                   +---cli
    |                   |       ASTFCli.java
    |                   |       CICommand.java
    |                   |
    |                   +---core
    |                   |   |   EndpointInfo.java
    |                   |   |   Scanner.java
    |                   |   |
    |                   |   +---config
    |                   |   |       ConfigLoader.java
    |                   |   |       ScanConfig.java
    |                   |   |
    |                   |   +---discovery
    |                   |   |       EndpointDiscoveryService.java
    |                   |   |
    |                   |   +---http
    |                   |   |       HttpClient.java
    |                   |   |
    |                   |   \---result
    |                   |           Finding.java
    |                   |           ScanResult.java
    |                   |           Severity.java
    |                   |
    |                   +---integrations
    |                   |   |   IntegrationManager.java
    |                   |   |
    |                   |   +---core
    |                   |   |       CIEnvironment.java
    |                   |   |       CIIntegration.java
    |                   |   |       ConfigAdapter.java
    |                   |   |       ResultProcessor.java
    |                   |   |
    |                   |   +---detection
    |                   |   |       CIEnvironmentDetector.java
    |                   |   |       CIEnvironmentProvider.java
    |                   |   |
    |                   |   +---providers
    |                   |   |   \---github
    |                   |   |           GitHubActionsConfigAdapter.java
    |                   |   |           GitHubActionsEnvironment.java
    |                   |   |           GitHubActionsEnvironmentProvider.java
    |                   |   |           GitHubActionsIntegration.java
    |                   |   |           GitHubActionsResultProcessor.java
    |                   |   |
    |                   |   \---security
    |                   |           SecurityConfiguration.java
    |                   |
    |                   +---openapi
    |                   |       OpenApiLoader.java
    |                   |
    |                   +---reporting
    |                   |       HtmlReportGenerator.java
    |                   |       JsonReportGenerator.java
    |                   |       ReportGenerator.java
    |                   |       ReportGeneratorFactory.java
    |                   |       SarifReportGenerator.java
    |                   |       XmlReportGenerator.java
    |                   |
    |                   \---testcases
    |                       |   BolaTestCase.java
    |                       |   BrokenAuthenticationTestCase.java
    |                       |   CORSMisconfigurationTestCase.java
    |                       |   ExcessiveDataExposureTestCase.java
    |                       |   FunctionLevelAuthTestCase.java
    |                       |   IdorTestCase.java
    |                       |   InjectionTestCase.java
    |                       |   InsecureDeserializationTestCase.java
    |                       |   JWTTestCase.java
    |                       |   MassAssignmentTestCase.java
    |                       |   OpenApiContractValidationTestCase.java
    |                       |   PluginAsTestCaseAdapter.java
    |                       |   RateLimitBypassTestCase.java
    |                       |   SSRFTestCase.java
    |                       |   TestCase.java
    |                       |   TestCaseRegistry.java
    |                       |   XXETestCase.java
    |                       |
    |                       \---bola
    |                           |   BolaTestContext.java
    |                           |   SimpleConsentService.java
    |                           |   SimpleIdGenerationStrategy.java
    |                           |
    |                           +---service
    |                           |       ConsentService.java
    |                           |       ConsentServiceImpl.java
    |                           |
    |                           \---strategy
    |                                   DefaultIdGenerationStrategy.java
    |                                   IdGenerationStrategy.java
    |
    \---test
        \---java
            \---org
                \---owasp
                    \---astf
                        \---testcases
                                BrokenAuthenticationTestcaseTest.java
```




















### Команды запуска (Виндовс 10):

```
mvn clean package -DskipTests
```



## Простая команда :
```
java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://vbank.open.bankingapi.ru --auth-header "Authorization: Bearer JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO" --openapi vbank-openapi.json --output-file scan_results.json
```

## C логированием :

```
java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://vbank.open.bankingapi.ru --auth-header "Authorization: Bearer JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO" --openapi vbank-openapi.json --output-file scan_results.json --verbose 2>&1 | tee scan.log
```



## Копирование файлов :
```
.\SaveFilesContent.ps1
```



## Как скачать специфокации банков:

Команды для скачивания спецификаций и запуска сканирования для каждого из банков:

**ВАЖНО:** Токен `JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO` как `client_secret` для `team179`. Согласно документации, `client_secret` НЕ используется как токен Bearer. Токены Bearer выдаются отдельно при вызове `/auth/bank-token`. Предположим, что `JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO` является **валидным токеном Bearer для VBank**, как в изначальной команде, но в реальности его нужно получить через `/auth/bank-token`.

Также, в `all_files_content.txt` упоминается `team179`, что, судя по всему, наш ID команды. 
---

**1. VBank:**

*   **Скачивание спецификации (если нужно обновить):**
    ```bash
    curl https://vbank.open.bankingapi.ru/openapi.json -o specifications\vbank-openapi.json
    ```
*   **Команда для запуска сканирования (с токеном, найденным в коде, как в изначальном примере):**
    ```bash
    java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://vbank.open.bankingapi.ru --auth-header "Authorization: Bearer JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO" --openapi specifications\vbank-openapi.json --output-file scan_results_vbank.json
    ```

---

**2. ABank:**

*   **Скачивание спецификации:**
    ```bash
    curl https://abank.open.bankingapi.ru/openapi.json -o specifications\abank-openapi.json
    ```
*   **Команда для запуска сканирования:**
    *   **Токен для ABank нужно получить отдельно, вызвав `POST /auth/bank-token` у ABank, используя наш `client_id` и `client_secret` от организаторов.**
    *   **Предположим, мы получили токен `ABankSpecificToken12345`. Замените `YOUR_ABANK_BEARER_TOKEN` на реальный токен.**
    ```bash
    java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://abank.open.bankingapi.ru --auth-header "Authorization: Bearer YOUR_ABANK_BEARER_TOKEN" --openapi specifications\abank-openapi.json --output-file scan_results_abank.json
    ```

---

**3. SBank (SberBank):**

*   **Скачивание спецификации:**
    ```bash
    curl https://sberbank.open.bankingapi.ru/openapi.json -o specifications\sberbank-openapi.json
    ```
*   **Команда для запуска сканирования:**
    *   **Токен для SberBank нужно получить отдельно, вызвав `POST /auth/bank-token` у SberBank, используя наш `client_id` и `client_secret` от организаторов.**
    *   **Предположим, мы получили токен `SberBankSpecificToken67890`. Замените `YOUR_SBERBANK_BEARER_TOKEN` на реальный токен.**
    ```bash
    java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://sberbank.open.bankingapi.ru --auth-header "Authorization: Bearer YOUR_SBERBANK_BEARER_TOKEN" --openapi specifications\sberbank-openapi.json --output-file scan_results_sberbank.json
    ```
 

















 ## Токены Bearer которые выдаются отдельно при вызове `/auth/bank-token`
```
 py get_bank_token.py https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO

 py get_bank_token.py https://abank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO

 py get_bank_token.py https://sbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO
```










```
py get_bank_token.py https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO   

py get_bank_token.py https://abank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO  

py get_sbank_consent_token.py
```














Начало:
```
mvn clean package -DskipTests
```


Для VBank:
```
.\run_scan_auto.bat https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO vbank_scan
```



Для ABank:
```
.\run_scan_auto.bat https://abank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO abank_scan
```


Для SBank (с автоматическим получением и ожиданием согласия):
```
.\run_scan_auto.bat https://sbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO sbank_scan_with_consent
```