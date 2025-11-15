Команды запуска (Виндовс 10):
```
 mvn clean package -DskipTests

java -jar target\api-security-testing-framework-1.0-SNAPSHOT.jar scan --target https://vbank.open.bankingapi.ru --auth-header "Authorization: Bearer JJqqH33ePjnfCMlyHFfz7Px09SMWvzhO" --openapi vbank-openapi.json --output-file scan_results.json

```





копирование файлов :
```
.\SaveFilesContent.ps1
```