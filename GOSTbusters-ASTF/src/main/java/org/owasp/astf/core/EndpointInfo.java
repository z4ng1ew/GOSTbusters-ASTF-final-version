package org.owasp.astf.core;

/**
 * Represents information about an API endpoint to be tested.
 */
public class EndpointInfo {
    private final String baseUrl;       // ← новое поле
    private final String path;
    private final String method;
    private final String contentType;
    private String requestBody;
    private final boolean requiresAuthentication;

    public EndpointInfo(String baseUrl, String path, String method) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.method = method;
        this.contentType = "application/json";
        this.requiresAuthentication = true;
    }

    public EndpointInfo(String baseUrl, String path, String method, String contentType, String requestBody, boolean requiresAuthentication) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.method = method;
        this.contentType = contentType;
        this.requestBody = requestBody;
        this.requiresAuthentication = requiresAuthentication;
    }

    // ✅ Временные конструкторы для обратной совместимости
    public EndpointInfo(String path, String method) {
        this("http://localhost", path, method);
    }

    public EndpointInfo(String path, String method, String contentType, String requestBody, boolean requiresAuthentication) {
        this("http://localhost", path, method, contentType, requestBody, requiresAuthentication);
    }

    /**
     * Get the full URL by combining baseUrl and path
     * @return full URL string
     */
    public String getFullUrl() {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return path;
        }
        
        // Убираем дублирование слешей
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public boolean isRequiresAuthentication() {
        return requiresAuthentication;
    }

    @Override
    public String toString() {
        return method + " " + getFullUrl();
    }
}