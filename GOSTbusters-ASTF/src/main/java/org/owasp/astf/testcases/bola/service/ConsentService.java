package org.owasp.astf.testcases.bola.service;

import org.owasp.astf.core.http.HttpClient;

import java.io.IOException;

public interface ConsentService {
    String createConsent(String baseUrl, HttpClient client) throws IOException;
}