package org.owasp.astf.plugin;

import org.owasp.astf.shared.EndpointInfo;
import org.owasp.astf.shared.http.HttpClient;
import org.owasp.astf.shared.result.Finding;

import java.io.IOException;
import java.util.List;

/**
 * ✅ Интерфейс плагина — теперь использует shared-классы
 */
public interface Plugin {
    String getId();
    String getName();
    String getDescription();
    
    List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException;
}