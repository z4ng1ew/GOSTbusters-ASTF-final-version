// package org.owasp.astf.testcases;

// import org.owasp.astf.core.EndpointInfo;
// import org.owasp.astf.core.http.HttpClient;
// import org.owasp.astf.core.result.Finding;
// import org.owasp.astf.core.result.Severity;
// import org.owasp.astf.plugin.Plugin; // ✅ Исправленный путь

// import java.io.IOException;
// import java.util.List;

// public class PluginAsTestCaseAdapter implements TestCase {
//     private final Plugin plugin;

//     public PluginAsTestCaseAdapter(Plugin plugin) {
//         this.plugin = plugin;
//     }

//     @Override
//     public String getId() {
//         return plugin.getId();
//     }

//     @Override
//     public String getName() {
//         return plugin.getName();
//     }

//     @Override
//     public String getDescription() {
//         return plugin.getDescription();
//     }

//     @Override
//     public List<Finding> execute(EndpointInfo endpoint, HttpClient client) throws IOException {
//         return plugin.execute(endpoint, client);
//     }
// }