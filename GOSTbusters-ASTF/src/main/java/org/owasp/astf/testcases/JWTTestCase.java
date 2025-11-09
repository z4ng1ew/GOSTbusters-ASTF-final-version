package org.owasp.astf.testcases;

import org.owasp.astf.core.EndpointInfo;
import org.owasp.astf.core.http.HttpClient;
import org.owasp.astf.core.result.Finding;
import org.owasp.astf.core.result.Severity;

import java.io.IOException;
import java.util.*;

public class JWTTestCase implements TestCase {
    @Override
    public String getId() { return "JWT"; }
    
    @Override
    public String getName() { return "JWT Vulnerabilities"; }
    
    @Override
    public String getDescription() { 
        return "Tests for JWT token vulnerabilities (none algorithm, weak secrets, etc.)"; 
    }
    
    @Override
    public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
        List<Finding> findings = new ArrayList<>();
        // Implementation here
        return findings;
    }
}