@echo off
setlocal enabledelayedexpansion

REM --- Проверка аргументов ---
if "%~3"=="" (
    echo ❌ Использование: run_scan_auto.bat ^<bank_url^> ^<client_id^> ^<client_secret^> [output_file_prefix]
    echo    Пример: run_scan_auto.bat https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO vbank_scan
    echo    Пример для SBank (с согласием): run_scan_auto.bat https://sbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO sbank_scan_with_consent
    exit /b 1
)

set BANK_URL=%~1
set CLIENT_ID=%~2
set CLIENT_SECRET=%~3
set OUTPUT_PREFIX=%~4
if "%OUTPUT_PREFIX%"=="" set OUTPUT_PREFIX=scan_results_auto

REM --- Определение банка и файла спецификации ---
set BANK_CODE=
if "%BANK_URL%"=="https://vbank.open.bankingapi.ru" set BANK_CODE=vbank
if "%BANK_URL%"=="https://abank.open.bankingapi.ru" set BANK_CODE=abank
if "%BANK_URL%"=="https://sbank.open.bankingapi.ru" set BANK_CODE=sbank
REM Если нужен sberbank, добавьте условие

if "%BANK_CODE%"=="" (
    echo ❌ Неизвестный банк: %BANK_URL%. Поддерживаются: vbank, abank, sbank
    exit /b 1
)

set OPENAPI_FILE=specifications\%BANK_CODE%-openapi.json
set JAR_FILE=target\api-security-testing-framework-1.0-SNAPSHOT.jar

echo.
echo --- Запуск сканирования для %BANK_CODE% (%BANK_URL%) ---
echo.

REM --- Получение токена ---
echo 1/4. Получаю токен для %BANK_URL%...
for /f "usebackq delims=" %%i in (`python get_tokens.py %BANK_URL% %CLIENT_ID% %CLIENT_SECRET%`) do (
    set BANK_TOKEN=%%i
)

if "!BANK_TOKEN!"=="" (
    echo ❌ Не удалось получить токен. Завершение.
    exit /b 1
)

echo ✅ Токен получен: !BANK_TOKEN:~0,10!...!BANK_TOKEN:~-4!

set AUTH_HEADER=Authorization: Bearer !BANK_TOKEN!

REM --- Обработка SBank (получение согласия, если нужно) ---
set CONSENT_ID=
set REQUESTING_BANK=
if "%BANK_CODE%"=="sbank" (
    echo.
    echo --- Обработка SBank (требуется согласие для межбанковых вызовов) ---
    echo    Выполняю запрос и ожидание активации согласия...
    for /f "usebackq delims=" %%i in (`python get_sbank_consent.py %BANK_URL%`) do (
        set CONSENT_ID=%%i
    )

    if "!CONSENT_ID!"=="" (
        echo ❌ Не удалось получить активное согласие для SBank. Завершение.
        exit /b 1
    )
    set REQUESTING_BANK=%CLIENT_ID%
    echo ✅ Активное согласие получено: !CONSENT_ID!
)

REM --- Формирование списка заголовков для CLI ---
set HEADER_ARGS=
if defined CONSENT_ID (
    set HEADER_ARGS=--header "X-Consent-Id: !CONSENT_ID!" --header "X-Requesting-Bank: !REQUESTING_BANK!"
)

REM --- Определение имени файла вывода ---
set OUTPUT_FILE=%OUTPUT_PREFIX%_%BANK_CODE%.json

REM --- Запуск сканирования ---
echo.
echo 2/4. Запускаю сканирование для %BANK_URL%...
echo    AUTH_HEADER: !AUTH_HEADER!
echo    HEADER_ARGS: !HEADER_ARGS!
echo    OUTPUT_FILE: !OUTPUT_FILE!
java -jar "%JAR_FILE%" scan --target %BANK_URL% --auth-header "!AUTH_HEADER!" --openapi "%OPENAPI_FILE%" !HEADER_ARGS! --output-file "!OUTPUT_FILE!"

if !errorlevel! neq 0 (
    echo ❌ Ошибка при выполнении сканирования.
    exit /b 1
)

echo ✅ Сканирование для %BANK_CODE% завершено. Результаты в !OUTPUT_FILE!.
echo.
echo --- Завершено ---