package org.owasp.astf.shared.http;

import java.io.IOException;
import java.util.Map;

/**
 * ✅ Интерфейс HTTP-клиента для плагинов
 */
public interface HttpClient {
    String get(String url, Map<String, String> headers) throws IOException;
    String post(String url, Map<String, String> headers, String body, String contentType) throws IOException;
    String put(String url, Map<String, String> headers, String body, String contentType) throws IOException;
    String delete(String url, Map<String, String> headers) throws IOException;
    String patch(String url, Map<String, String> headers, String body, String contentType) throws IOException;
}