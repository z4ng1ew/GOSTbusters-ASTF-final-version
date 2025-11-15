package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.*;

public class XXETestCase implements TestCase {
    @Override
    public String getId() { return "XXE"; }
    
    @Override
    public String getName() { return "XML External Entity"; }
    
    @Override
    public String getDescription() { 
        return "Tests for XXE vulnerabilities in XML processing endpoints"; 
    }
    
    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        
        // XXE актуален ТОЛЬКО для эндпоинтов, которые могут принимать XML
        String method = endpoint.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return findings;
        }
        
        // Проверяем, может ли эндпоинт обрабатывать XML (наличие ключевых слов в пути)
        String path = endpoint.getPath().toLowerCase();
        if (!path.contains("xml") && !path.contains("import") && !path.contains("upload")) {
            return findings;
        }
        
        // 🔍 В рамках хакатона мы НЕ выполняем реальные XXE-атаки (это может нарушить правила)
        // Вместо этого — добавляем рекомендацию на ручной аудит
        findings.add(new Finding(
            UUID.randomUUID().toString(),
            "Potential XXE Risk",
            "Эндпоинт может быть уязвим к XXE, если обрабатывает XML-входные данные. " +
            "Требуется ручная проверка на unsafe consumption of XML.",
            Severity.MEDIUM,
            getId(),
            endpoint.getFullUrl(),
            "Disable DTDs and external entities in XML parsers. " +
            "Use JSON instead of XML where possible. " +
            "Validate and sanitize all XML inputs."
        ));
        
        return findings;
    }
}