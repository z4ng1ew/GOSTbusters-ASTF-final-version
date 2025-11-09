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
        // Implementation here
        return findings;
    }
}