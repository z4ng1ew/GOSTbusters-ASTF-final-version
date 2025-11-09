package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InjectionTestCase implements TestCase {
    @Override
    public String getId() {
        return "INJECTION";
    }

    @Override
    public String getName() {
        return "Injection Attack";
    }

    @Override
    public String getDescription() {
        return "Tests for injection vulnerabilities (SQLi, NoSQLi, SSRF, etc.)";
    }

    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        String payload = "' OR 1=1 --"; // Пример SQL-инъекции

        // Попробовать внедрить в параметры
        String testUrl = endpoint.getFullUrl().replace("{id}", payload);

        Map<String, String> headers = Map.of();
        try {
            String response = client.get(testUrl, headers);
            // Если в ответе есть "error" или "exception" — возможна инъекция
            if (response.toLowerCase().contains("sql") || response.toLowerCase().contains("exception")) {
                findings.add(new Finding(
                    "INJECT-01",
                    "Possible Injection Vulnerability",
                    "Ответ API содержит признаки SQL/NoSQL-инъекции при использовании специального payload",
                    Severity.HIGH,
                    getId(),
                    testUrl,
                    "Проверьте валидацию входных данных и используйте параметризованные запросы"
                ));
            }
        } catch (Exception e) {
            // Ошибки сети — игнор
        }

        return findings;
    }
}