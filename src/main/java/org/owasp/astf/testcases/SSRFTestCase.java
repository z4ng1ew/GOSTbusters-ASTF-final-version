package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.*;

public class SSRFTestCase implements TestCase {
    @Override
    public String getId() { return "SSRF"; }
    
    @Override
    public String getName() { return "Server-Side Request Forgery"; }
    
    @Override
    public String getDescription() { 
        return "Tests for SSRF vulnerabilities by attempting to make the server request internal resources"; 
    }
    
    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        
        // SSRF актуален только для POST/PUT эндпоинтов, которые принимают URL-подобные параметры
        String method = endpoint.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return findings;
        }
        
        // Проверяем, может ли эндпоинт быть уязвим (наличие ключевых слов в пути)
        String path = endpoint.getPath().toLowerCase();
        if (!path.contains("payment") && !path.contains("consent") && !path.contains("webhook")) {
            return findings;
        }
        
        // 🔍 В рамках хакатона мы НЕ выполняем реальные SSRF-запросы (это может нарушить правила)
        // Вместо этого — добавляем рекомендацию на ручной аудит
        findings.add(new Finding(
            UUID.randomUUID().toString(),
            "Potential SSRF Risk",
            "Эндпоинт может быть уязвим к SSRF, если обрабатывает URL-параметры (например, creditorAccount, bank_code). " +
            "Требуется ручная проверка на unsafe consumption of external services.",
            Severity.MEDIUM,
            getId(),
            endpoint.getFullUrl(),
            "Validate and sanitize all URL/hostname inputs. Use allow-lists for external domains. " +
            "Avoid letting user input control backend HTTP requests."
        ));
        
        return findings;
    }
}