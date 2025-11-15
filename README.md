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

**ВАЖНО:** Токен `JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO` был найден в коде (`all_files_content.txt`) как `client_secret` для `team179`. Согласно документации, `client_secret` НЕ используется как токен Bearer. Токены Bearer выдаются отдельно при вызове `/auth/bank-token`. Предположим, что `JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO` является **валидным токеном Bearer для VBank**, как в изначальной команде, но в реальности его нужно получить через `/auth/bank-token`.

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
    *   **Токен для ABank нужно получить отдельно, вызвав `POST /auth/bank-token` у ABank, используя ваш `client_id` и `client_secret` от организаторов.**
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
    *   **Токен для SberBank нужно получить отдельно, вызвав `POST /auth/bank-token` у SberBank, используя ваш `client_id` и `client_secret` от организаторов.**
    *   **Предположим, мы получили токен `SberBankSpecificToken67890`. Замените `YOUR_SBERBANK_BEARER_TOKEN` на реальный токен.**
    ```bash
    java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://sberbank.open.bankingapi.ru --auth-header "Authorization: Bearer YOUR_SBERBANK_BEARER_TOKEN" --openapi specifications\sberbank-openapi.json --output-file scan_results_sberbank.json
    ```
 

















 ## Токены Bearer которые выдаются отдельно при вызове `/auth/bank-token`

 py get_bank_token.py https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO

 py get_bank_token.py https://abank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO

 py get_bank_token.py https://sbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO













py get_bank_token.py https://abank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO  


 py get_bank_token.py https://vbank.open.bankingapi.ru team179 JJqqH33ePjnfCMlyHFfz9Px09SMWvzhO    

 py get_sbank_consent_token.py