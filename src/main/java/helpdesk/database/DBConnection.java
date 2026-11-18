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

/**
 * High-performance pooled database connection manager (Task 01 / Performance Optimization).
 * Reuses active physical JDBC connections to eliminate TCP + SSL/TLS handshake overhead
 * on every single query to remote/cloud MySQL.
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static DBConnection instance;

    private static final int MAX_POOL_SIZE = 8;
    private final BlockingQueue<Connection> connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL Driver not found on classpath: {}", e.getMessage());
        }
    }

    private DBConnection() {
        // Pre-warm the pool with 2 active connections in background to make login instant
        new Thread(() -> {
            try {
                for (int i = 0; i < 2; i++) {
                    Connection conn = createPhysicalConnection();
                    if (conn != null) {
                        connectionPool.offer(conn);
                    }
                }
                logger.info("Pre-warmed database connection pool ready.");
            } catch (Exception e) {
                logger.warn("Pool pre-warming exception: {}", e.getMessage());
            }
        }, "DBPoolPrewarmer").start();
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

    /**
     * Returns a pooled Connection.
     * When caller calls conn.close() in try-with-resources, the physical connection
     * is returned to the pool rather than severed, allowing 0ms connection reuse.
     */
    public Connection getConnection() throws SQLException {
        Connection physicalConn = connectionPool.poll();

        try {
            if (physicalConn == null || physicalConn.isClosed() || !physicalConn.isValid(1)) {
                physicalConn = createPhysicalConnection();
            }
        } catch (Exception e) {
            physicalConn = createPhysicalConnection();
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
                                if (!finalConn.isClosed() && connectionPool.size() < MAX_POOL_SIZE) {
                                    connectionPool.offer(finalConn);
                                } else {
                                    finalConn.close();
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
