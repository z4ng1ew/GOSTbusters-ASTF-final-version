package org.owasp.astf.openapi;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;

import org.owasp.astf.core.EndpointInfo;

import java.util.ArrayList;
import java.util.List;

public class OpenApiLoader {
    public static OpenAPI load(String urlOrPath) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        var result = new OpenAPIV3Parser().readLocation(urlOrPath, null, options);
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            System.err.println("OpenAPI parse warnings: " + result.getMessages());
        }
        return result.getOpenAPI();
    }

    public static List<EndpointInfo> getEndpoints(OpenAPI openAPI) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        
        String baseUrl = "http://localhost";
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            baseUrl = openAPI.getServers().get(0).getUrl();
        }

        for (String path : openAPI.getPaths().keySet()) {
            PathItem pathItem = openAPI.getPaths().get(path);
            
            // ✅ ИСПРАВЛЕНИЕ: Конвертируем HttpMethod в String
            for (PathItem.HttpMethod httpMethod : pathItem.readOperationsMap().keySet()) {
                Operation operation = pathItem.readOperationsMap().get(httpMethod);
                boolean requiresAuth = operation.getSecurity() != null && !operation.getSecurity().isEmpty();
                if (!requiresAuth && openAPI.getSecurity() != null && !openAPI.getSecurity().isEmpty()) {
                    requiresAuth = true;
                }

                // ✅ Конвертируем HttpMethod enum в строку в верхнем регистре
                String method = httpMethod.name().toUpperCase();
                
                endpoints.add(new EndpointInfo(
                    baseUrl,
                    path,
                    method,
                    null,
                    null,
                    requiresAuth
                ));
            }
        }
        return endpoints;
    }
}