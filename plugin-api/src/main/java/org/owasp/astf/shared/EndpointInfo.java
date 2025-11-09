package org.owasp.astf.shared;

/**
 * ✅ Интерфейс эндпоинта, доступный плагинам
 */
public class EndpointInfo {
    private final String baseUrl;
    private final String path;
    private final String method;
    private final boolean requiresAuth;

    public EndpointInfo(String baseUrl, String path, String method, boolean requiresAuth) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.method = method.toUpperCase();
        this.requiresAuth = requiresAuth;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getPath() { return path; }
    public String getMethod() { return method; }
    public boolean isRequiresAuthentication() { return requiresAuth; }

    public String getFullUrl() {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        } else {
            return baseUrl + path;
        }
    }
}