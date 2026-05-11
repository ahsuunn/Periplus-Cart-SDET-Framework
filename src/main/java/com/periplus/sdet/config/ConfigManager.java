package com.periplus.sdet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * {@code ConfigManager} is a thread-safe singleton that loads test configuration
 * values from {@code src/test/resources/config.properties}.
 *
 * <p>All environment-specific settings (base URL, credentials, timeouts) are
 * centralised here so that no magic strings are scattered across test classes.</p>
 */
public final class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_FILE = "config.properties";

    /** Volatile singleton instance (double-checked locking). */
    private static volatile ConfigManager instance;
    private final Properties props = new Properties();

    // ── Private constructor ────────────────────────────────────────────────

    private ConfigManager() {
        // 1. Load default config.properties from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IllegalStateException("Cannot find " + CONFIG_FILE
                        + " on the classpath. Place it under src/test/resources/.");
            }
            props.load(is);
            log.info("Loaded configuration from '{}'.", CONFIG_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
        }

        // 2. Load .env file from project root if it exists (overrides defaults)
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(envFile)) {
                props.load(fis);
                log.info("Detected and loaded overrides from '.env'.");
            } catch (IOException e) {
                log.warn("Found .env file but failed to read it: {}", e.getMessage());
            }
        }
    }

    // ── Singleton accessor ─────────────────────────────────────────────────

    /**
     * Returns the singleton {@code ConfigManager} instance.
     *
     * @return the singleton instance
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    // ── Property accessors ─────────────────────────────────────────────────

    /**
     * Returns the raw string value of the requested property.
     *
     * @param key the property key
     * @return the value
     * @throws IllegalArgumentException if the key does not exist
     */
    public String get(String key) {
        // 1. Check System Property (e.g., -Duser.email)
        String value = System.getProperty(key);

        // 2. Check Environment Variable (e.g., USER_EMAIL or user.email)
        if (value == null) {
            // Try standard env var format: user.email -> USER_EMAIL
            String envKey = key.replace(".", "_").toUpperCase();
            value = System.getenv(envKey);

            // Fallback to literal key as env var
            if (value == null) {
                value = System.getenv(key);
            }
        }

        // 3. Fallback to properties file
        if (value == null) {
            value = props.getProperty(key);
        }

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing required configuration key: '" + key + "'. "
                    + "Verify it exists in " + CONFIG_FILE + ", as an Environment Variable, or as a -D system property.");
        }
        return value.trim();
    }

    /** @return base URL of the application under test */
    public String getBaseUrl() { return get("app.base.url"); }

    /** @return registered test account e-mail address */
    public String getUserEmail() { return get("user.email"); }

    /** @return registered test account password */
    public String getUserPassword() { return get("user.password"); }

    /** @return explicit wait timeout in seconds */
    public int getExplicitWaitSeconds() {
        return Integer.parseInt(get("wait.explicit.seconds"));
    }

    /** @return page load timeout in seconds */
    public int getPageLoadTimeoutSeconds() {
        return Integer.parseInt(get("wait.page.load.seconds"));
    }

    /** @return browser name (chrome | firefox | edge) */
    public String getBrowser() { return get("browser").toLowerCase(); }

    /** @return whether to run in headless mode */
    public boolean isHeadless() {
        return Boolean.parseBoolean(get("browser.headless"));
    }

    /** @return search keyword to use in the Add-to-Cart scenario */
    public String getSearchKeyword() { return get("test.search.keyword"); }
}
