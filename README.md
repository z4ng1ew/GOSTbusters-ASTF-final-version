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
