@echo off
setlocal enabledelayedexpansion

REM --- Настройки ---
set BANK_URL=https://vbank.open.bankingapi.ru
set CLIENT_ID=team179
set CLIENT_SECRET=JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO
set OPENAPI_FILE=specifications\vbank-openapi.json
set OUTPUT_FILE=scan_results_vbank_auto.json
set JAR_FILE=target\api-security-testing-framework-1.0-SNAPSHOT.jar

echo.
echo --- Запуск сканирования для VBank (автоматическое получение токена) ---
echo.

REM --- Получение токена с помощью Python скрипта ---
echo 1/3. Получаю токен для %BANK_URL%...
for /f "usebackq tokens=*" %%i in (`python get_bank_token.py %BANK_URL% %CLIENT_ID% %CLIENT_SECRET%`) do (
    set BANK_TOKEN=%%i
)

if "!BANK_TOKEN!"=="" (
    echo ❌ Не удалось получить токен. Завершение.
    exit /b 1
)

echo ✅ Токен получен: !BANK_TOKEN:~0,10!...!BANK_TOKEN:~-4!

REM --- Запуск сканирования с полученным токеном ---
echo.
echo 2/3. Запускаю сканирование для %BANK_URL%...
java -jar "%JAR_FILE%" scan --target %BANK_URL% --auth-header "Authorization: Bearer !BANK_TOKEN!" --openapi "%OPENAPI_FILE%" --output-file "%OUTPUT_FILE%"

if !errorlevel! neq 0 (
    echo ❌ Ошибка при выполнении сканирования.
    exit /b 1
)

echo ✅ Сканирование для %BANK_URL% завершено. Результаты в %OUTPUT_FILE%.
echo.
echo --- Завершено ---