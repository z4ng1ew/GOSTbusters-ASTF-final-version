package org.owasp.astf.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginLoader {
    private static final Logger logger = LogManager.getLogger(PluginLoader.class);

    public List<Plugin> loadPlugins() {
        List<Plugin> plugins = new ArrayList<>();

        File pluginDir = new File("plugins"); // Папка рядом с JAR
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            logger.warn("Plugins directory does not exist: {}", pluginDir.getAbsolutePath());
            return plugins;
        }

        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            logger.info("No plugin JAR files found in plugins directory");
            return plugins;
        }

        for (File jarFile : jarFiles) {
            try {
                URL jarUrl = jarFile.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    getClass().getClassLoader()
                );

                ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, classLoader);
                for (Plugin plugin : loader) {
                    plugins.add(plugin);
                    logger.info("Loaded plugin: {} from {}", plugin.getName(), jarFile.getName());
                }

                classLoader.close();
            } catch (Exception e) {
                logger.error("Failed to load plugin from JAR: {}", jarFile.getName(), e);
            }
        }

        return plugins;
    }
}