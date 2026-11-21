package helpdesk.database;

import helpdesk.core.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resilient, high-performance pooled database connection manager.
 * Strictly respects cloud database connection quotas (max_user_connections = 5 on filess.io)
 * by capping client-side active physical connections to at most 2, recycling connections,
 * and backing off gracefully if connection limits are temporarily reached.
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static DBConnection instance;

    // Strict cap: filess.io free tier max connection limit is 5 across all clients.
    // Setting MAX_CONNECTIONS to 2 guarantees we never exhaust cloud quotas.
    private static final int MAX_CONNECTIONS = 2;
    private final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(MAX_CONNECTIONS);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL Driver not found on classpath: {}", e.getMessage());
        }
    }

    private DBConnection() {
        // Pool is initialized on-demand to avoid opening unnecessary connections at startup
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    private Connection createPhysicalConnection() throws SQLException {
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUser();
        String password = AppConfig.getDbPassword();
        return DriverManager.getConnection(url, user, password);
    }

    private Connection createPhysicalConnectionWithRetry() throws SQLException {
        SQLException lastEx = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                Connection conn = createPhysicalConnection();
                if (conn != null) return conn;
            } catch (SQLException e) {
                lastEx = e;
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("max_user_connections") || msg.contains("too many connections")) {
                    logger.warn("Database connection quota reached, backing off attempt {}/4...", attempt);
                    try {
                        Thread.sleep(attempt * 1200L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw lastEx != null ? lastEx : new SQLException("Failed to establish database connection after retries.");
    }

    /**
     * Returns a pooled Connection.
     * When caller calls conn.close() in try-with-resources, the physical connection
     * is returned to the pool for reuse rather than closed over the network.
     */
    public Connection getConnection() throws SQLException {
        Connection physicalConn = pool.poll();

        // If pool was empty and active physical count < MAX_CONNECTIONS, create a new one
        if (physicalConn == null && activeConnections.get() < MAX_CONNECTIONS) {
            synchronized (this) {
                if (activeConnections.get() < MAX_CONNECTIONS) {
                    try {
                        physicalConn = createPhysicalConnectionWithRetry();
                        activeConnections.incrementAndGet();
                    } catch (SQLException e) {
                        logger.warn("Could not create new physical connection: {}", e.getMessage());
                    }
                }
            }
        }

        // If still null (reached max limit or creation failed), wait for an existing one to be released
        if (physicalConn == null) {
            try {
                physicalConn = pool.poll(8, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted waiting for available DB connection", e);
            }
        }

        // Validate physical connection health
        if (physicalConn != null) {
            boolean valid = false;
            try {
                valid = !physicalConn.isClosed() && physicalConn.isValid(2);
            } catch (Exception ignored) {}

            if (!valid) {
                try {
                    physicalConn.close();
                } catch (Exception ignored) {}
                activeConnections.decrementAndGet();
                physicalConn = createPhysicalConnectionWithRetry();
                activeConnections.incrementAndGet();
            }
        } else {
            // As a last fallback, retry creating one
            physicalConn = createPhysicalConnectionWithRetry();
            activeConnections.incrementAndGet();
        }

        final Connection finalConn = physicalConn;

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    private boolean closed = false;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        if ("close".equals(methodName)) {
                            if (!closed) {
                                closed = true;
                                if (!finalConn.isClosed()) {
                                    boolean offered = pool.offer(finalConn);
                                    if (!offered) {
                                        try {
                                            finalConn.close();
                                        } catch (Exception ignored) {}
                                        activeConnections.decrementAndGet();
                                    }
                                } else {
                                    activeConnections.decrementAndGet();
                                }
                            }
                            return null;
                        }
                        if ("isClosed".equals(methodName)) {
                            return closed || finalConn.isClosed();
                        }
                        return method.invoke(finalConn, args);
                    }
                }
        );
    }

    public static boolean testConnection() {
        try (Connection conn = getInstance().getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
