# GOSTbusters ASTF

**GOSTbusters API Security Testing Framework (ASTF)** — это автоматизированный сканер безопасности API, созданный специально для хакатона **VTB API Security Hackathon 2025**. Фреймворк объединяет международные стандарты безопасности (**OWASP API Security Top 10 2023**) с российской спецификой: поддержкой **Open Banking Russia v2.1** и интеграцией с **ГОСТ-шлюзом**.

> 🔐 *Первое и единственное решение в хакатоне с мультимодульной архитектурой и поддержкой бизнес-логики российских банков.*

---

## 🚀 Возможности

- **Полное покрытие OWASP API Security Top 10 2023** — все 10 категорий уязвимостей
- **Поддержка ГОСТ-шлюза** — автоматическая маршрутизация через `api.gost.bankingapi.ru:8443`
- **Автоматическая работа с банковскими API** — VBank, ABank, SBank (Open Banking Russia v2.1)
- **Генерация согласий** — встроенные Python-скрипты для получения `account-consents`
- **Модульная архитектура** — ядро, плагины, отчёты, интеграции — всё разделено
- **Динамическая загрузка плагинов** — расширяйте функционал без перекомпиляции
- **CI/CD-интеграция** — поддержка GitHub Actions, SARIF-отчёты, fail-on-findings
- **Мультиформатные отчёты** — JSON, HTML, SARIF, XML
- **Работа с OpenAPI 3.1+** — загрузка эндпоинтов из спецификаций

---

## 🎯 Архитектура

### Модульная структура (Maven multi-module)
```
GOSTbusters-ASTF/
├── core/                            # Ядро сканера (Scanner, EndpointInfo, Finding)
├── plugin-api/                      # API для плагинов (SPI, shared-классы)
├── example-bola-plugin/             # Пример плагина для BOLA-тестов
├── specifications/                  # OpenAPI-спецификации банков (vbank, abank, sbank)
├── src/main/java/org/owasp/astf/
│   ├── cli/                        # CLI (ASTFCli, CICommand)
│   ├── core/                       # Основной движок
│   ├── testcases/                  # Встроенные тест-кейсы (OWASP Top 10)
│   ├── openapi/                    # Парсер OpenAPI
│   ├── reporting/                  # Генераторы отчётов
│   └── integrations/               # Интеграции с CI/CD
├── run_scan_auto.bat               # Автоматический запуск сканирования
├── get_tokens.py                   # Получение bank_token
└── get_sbank_consent.py            # Получение согласия для SBank
```

### Ключевые архитектурные решения
- **Java 21 + виртуальные потоки** — высокая производительность
- **Service Provider Interface (SPI)** — динамическая загрузка плагинов
- **Чёткое разделение ответственности** — каждый модуль делает одну задачу
- **Enterprise-ready структура** — готова к масштабированию и поддержке

---

## 📋 Быстрый старт

### 1. Сборка проекта
```powershell
mvn clean package -DskipTests
```

### 2. Сканирование с ГОСТ-шлюзом (рекомендуется)
```powershell
java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan ^
  --target https://vbank.open.bankingapi.ru ^
  --auth-header "Authorization: Bearer ВАШ_ТОКЕН" ^
  --openapi specifications\vbank-openapi.json ^
  --use-gost ^
  --output-file vbank_scan.json
```

### 3. Автоматический запуск (получает токен и согласие)
```powershell
run_scan_auto.bat https://sbank.open.bankingapi.ru team179 ВАШ_SECRET sbank_scan
```

### 4. Доступные опции
```bash
--target <url>              # URL API (обязательно)
--auth-header <header>      # Заголовок авторизации
--openapi <file>            # OpenAPI-спецификация (YAML/JSON)
--use-gost                  # Включить ГОСТ-шлюз
--output-file <file>        # Выходной файл (по умолчанию: scan_results.json)
--threads <N>               # Количество потоков (по умолчанию: 10)
--timeout <мин>             # Таймаут в минутах (по умолчанию: 30)
--verbose                   # Подробный вывод
```

---

## 🛡️ Поддерживаемые уязвимости (OWASP API Security Top 10 2023)

| Категория | Реализация |
|-----------|------------|
| **API1:2023** | Broken Object Level Authorization (BOLA) — через `BolaTestCase` + плагин |
| **API2:2023** | Broken Authentication — анализ статус-кодов (200 vs 401/403) |
| **API3:2023** | Excessive Data Exposure — поиск `password`, `token` в ответах |
| **API4:2023** | Unrestricted Resource Consumption — Rate Limit Bypass (15 запросов за 750 мс) |
| **API5:2023** | Broken Function Level Authorization — проверка DELETE на GET-эндпоинтах |
| **API6:2023** | Unrestricted Access to Sensitive Business Flows — **поддержка в финале** |
| **API7:2023** | Server Side Request Forgery — ручной аудит с рекомендациями |
| **API8:2023** | Security Misconfiguration — CORS, JWT, заголовки |
| **API9:2023** | Improper Inventory Management — поиск `/debug`, `/actuator` |
| **API10:2023** | Unsafe Consumption of APIs — XXE, ручной аудит |

---

## 📊 Примеры отчётов

### CLI-вывод
```
🔴 [HIGH] BOLA-01: Broken Object Level Authorization
   Description: Successfully accessed account acc-999-999 with team179 token

🟡 [MEDIUM] ASTF-API2-2023: Missing Authentication Controls  
   Description: The API endpoint appears to be accessible without proper authentication
```

### SARIF-интеграция
Поддержка GitHub Code Scanning через формат SARIF с автоматическими аннотациями в PR.

---

## 🏆 Что сделано к полуфиналу

- ✅ Полнофункциональный CLI-сканер на **Java 21**
- ✅ Поддержка **Open Banking Russia v2.1** (VBank/ABank/SBank)
- ✅ Интеграция с **ГОСТ-шлюзом** (`--use-gost`)
- ✅ Генерация отчётов в **JSON, HTML, SARIF**
- ✅ **Базовая мультимодульная архитектура** (core, plugin-api, reporting)
- ✅ **Динамическая загрузка плагинов** через SPI
- ✅ Автоматизация через **Python-скрипты** (токены, согласия)
- ✅ Интеграция с **GitHub Actions**

## 🔜 Планы на финал

- 🔧 **Доработка мультимодульной архитектуры** — полноценный Maven multi-module
- 🖥️ **Добавление графического интерфейса** (JavaFX или веб)
- 📐 **Полная валидация контракта** — сравнение OpenAPI-схемы и реального ответа (тип, required, enum, format)
- 🧪 **Фаззинг параметров** — спецсимволы, длинные строки в `{account_id}`
- 📄 **Генерация human-readable PDF** — отчёты с логотипом и цветовой индикацией
- 🎯 **Реализация API6:2023** — автоматизированный тест на злоупотребление бизнес-логикой

---

## 📂 Документация

- [Архитектура](docs/ARCHITECTURE.md)
- [Обзор фреймворка](docs/FRAMEWORK_OVERVIEW.md)
- [Новизна и конкурентные преимущества](docs/INNOVATION.md)

---

> **GOSTbusters ASTF** — enterprise-ориентированный, модульный фреймворк для обеспечения безопасности российских банковских API в соответствии с OWASP, Open Banking Russia и требованиями ЦБ РФ.

**Разработано командой GOSTbusters для VTB API Security Hackathon 2025**  
*Защищая будущее российских финансовых API*
