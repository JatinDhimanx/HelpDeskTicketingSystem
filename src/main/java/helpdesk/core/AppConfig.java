package helpdesk.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loader.
 * Reads settings from helpdesk.properties on the classpath.
 * Never hardcodes passwords or sensitive values.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String PROPERTIES_FILE = "/helpdesk.properties";
    private static final Properties properties = new Properties();

    static {
        // 1. Load from classpath first
        try (InputStream input = AppConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
                logger.info("Configuration loaded from classpath {}", PROPERTIES_FILE);
            }
        } catch (IOException e) {
            logger.warn("Could not read classpath {}: {}", PROPERTIES_FILE, e.getMessage());
        }

        // 2. Check for external helpdesk.properties in working directory
        java.io.File externalFile = new java.io.File("helpdesk.properties");
        if (externalFile.exists() && externalFile.canRead()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(externalFile)) {
                properties.load(fis);
                logger.info("Loaded overrides from external configuration file: {}", externalFile.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Could not read external helpdesk.properties: {}", e.getMessage());
            }
        }
    }

    private AppConfig() {
        // Utility class ? no instantiation
    }

    public static String getDbUrl() {
        String env = System.getenv("DB_URL");
        if (env != null && !env.isBlank()) return env;
        return System.getProperty("db.url", properties.getProperty("db.url"));
    }

    public static String getDbUser() {
        String env = System.getenv("DB_USER");
        if (env != null && !env.isBlank()) return env;
        return System.getProperty("db.user", properties.getProperty("db.user"));
    }

    public static String getDbPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null && !env.isBlank()) return env;
        return System.getProperty("db.password", properties.getProperty("db.password"));
    }

    public static int getEscalationCheckIntervalMinutes() {
        String value = properties.getProperty("escalation.check.interval.minutes", "30");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid escalation.check.interval.minutes '{}', defaulting to 30", value);
            return 30;
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
