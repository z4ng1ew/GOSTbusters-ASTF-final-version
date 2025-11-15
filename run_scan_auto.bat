@echo off
setlocal enabledelayedexpansion

if "%~3"=="" (
    echo Usage: run_scan_auto_v2.bat ^<bank_url^> ^<client_id^> ^<client_secret^> [output_file_prefix]
    echo Example: run_scan_auto_v2.bat https://vbank.open.bankingapi.ru team179 secret123 vbank_scan
    exit /b 1
)

set BANK_URL=%~1
set CLIENT_ID=%~2
set CLIENT_SECRET=%~3
set OUTPUT_PREFIX=%~4
if "%OUTPUT_PREFIX%"=="" set OUTPUT_PREFIX=scan_results_auto

set BANK_CODE=
if "%BANK_URL%"=="https://vbank.open.bankingapi.ru" set BANK_CODE=vbank
if "%BANK_URL%"=="https://abank.open.bankingapi.ru" set BANK_CODE=abank
if "%BANK_URL%"=="https://sbank.open.bankingapi.ru" set BANK_CODE=sbank

if "%BANK_CODE%"=="" (
    echo Unknown bank: %BANK_URL%
    exit /b 1
)

set OPENAPI_FILE=specifications\%BANK_CODE%-openapi.json
set JAR_FILE=target\api-security-testing-framework-1.0-SNAPSHOT.jar

echo.
echo --- Running scan for %BANK_CODE% (%BANK_URL%) ---
echo.

echo 1/4. Getting token for %BANK_URL%...
for /f "usebackq delims=" %%i in (`python get_tokens.py %BANK_URL% %CLIENT_ID% %CLIENT_SECRET%`) do (
    set BANK_TOKEN=%%i
)

if "!BANK_TOKEN!"=="" (
    echo Failed to get token.
    exit /b 1
)

echo Token received: !BANK_TOKEN:~0,10!...!BANK_TOKEN:~-4!
set AUTH_HEADER=Authorization: Bearer !BANK_TOKEN!

set CONSENT_ID=
set REQUESTING_BANK=
if "%BANK_CODE%"=="sbank" (
    echo Getting consent for SBank...
    for /f "usebackq delims=" %%i in (`python get_sbank_consent.py %BANK_URL%`) do (
        set CONSENT_ID=%%i
    )
    if "!CONSENT_ID!"=="" (
        echo Failed to get consent for SBank.
        exit /b 1
    )
    set REQUESTING_BANK=%CLIENT_ID%
    echo Consent received: !CONSENT_ID!
)

set HEADER_ARGS=
if defined CONSENT_ID (
    set HEADER_ARGS=--header "X-Consent-Id: !CONSENT_ID!" --header "X-Requesting-Bank: !REQUESTING_BANK!"
)

set OUTPUT_FILE=%OUTPUT_PREFIX%_%BANK_CODE%.json

echo 2/4. Running scan...
echo Auth Header: !AUTH_HEADER!
echo Headers Args: !HEADER_ARGS!
java -jar "%JAR_FILE%" scan --target %BANK_URL% --auth-header "!AUTH_HEADER!" --openapi "%OPENAPI_FILE%" !HEADER_ARGS! --output-file "!OUTPUT_FILE!"

if !errorlevel! neq 0 (
    echo Scan failed.
    exit /b 1
)

echo Scan completed. Results in !OUTPUT_FILE!.
echo.
echo --- Done ---