package helpdesk.database;

import helpdesk.core.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection manager.
 * Reads configuration from AppConfig (which loads helpdesk.properties).
 * Use try-with-resources on every Connection obtained from getConnection().
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static DBConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DBConnection() {
        this.url      = AppConfig.getDbUrl();
        this.user     = AppConfig.getDbUser();
        this.password = AppConfig.getDbPassword();
    }

    /** Returns the singleton instance (lazy, not thread-safe for init ? acceptable for desktop app). */
    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Opens and returns a fresh JDBC Connection.
     * Callers MUST wrap usage in try-with-resources so the connection is closed.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Verifies that a connection to the database can be established.
     * Returns true on success, false on failure (logs the error).
     */
    public static boolean testConnection() {
        try (Connection conn = getInstance().getConnection()) {
            if (conn != null && !conn.isClosed()) {
                logger.info("Database connection test successful. URL: {}", AppConfig.getDbUrl());
                return true;
            }
        } catch (SQLException e) {
            logger.error("Database connection test failed: {}", e.getMessage());
        }
        return false;
    }
}
