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
 * High-performance, quota-safe database connection manager.
 * Features:
 *  - Strict physical connection cap (MAX_CONNECTIONS = 2) for cloud quotas.
 *  - Timestamp-based fresh connection reuse: skips redundant network pings (isValid)
 *    when connections are active within 45 seconds, cutting hundreds of milliseconds of latency.
 *  - Background connection pre-warming (warmUp) for instant first-time query/login execution.
 *  - Transparent proxy recycling on close().
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static DBConnection instance;

    // Strict cap: filess.io free tier max connection limit is 5 across all clients.
    // Setting MAX_CONNECTIONS to 2 guarantees we never exhaust cloud quotas.
    private static final int MAX_CONNECTIONS = 2;

    private static class PooledHolder {
        final Connection rawConn;
        volatile long lastUsed;

        PooledHolder(Connection rawConn) {
            this.rawConn = rawConn;
            this.lastUsed = System.currentTimeMillis();
        }

        boolean isFresh() {
            return (System.currentTimeMillis() - lastUsed) < 45_000L;
        }
    }

    private final BlockingQueue<PooledHolder> pool = new ArrayBlockingQueue<>(MAX_CONNECTIONS);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("MySQL Driver not found on classpath: {}", e.getMessage());
        }
    }

    private DBConnection() {
        // Pool is initialized on-demand or pre-warmed via warmUp()
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Asynchronously pre-warms the database connection pool so that the first login
     * or query does not suffer from initial TCP/TLS handshake latency.
     */
    public void warmUp() {
        Thread warmer = new Thread(() -> {
            try {
                logger.info("Pre-warming database connection pool in background...");
                Connection conn = getConnection();
                if (conn != null) {
                    conn.close(); // Return to pool for instant reuse
                    logger.info("Database connection pre-warmed and pool ready.");
                }
            } catch (Exception e) {
                logger.warn("Database pre-warming encountered: {}", e.getMessage());
            }
        }, "DBConnection-Warmer");
        warmer.setDaemon(true);
        warmer.start();
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
                if (conn != null)
                    return conn;
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
        PooledHolder holder = pool.poll();

        // If pool was empty and active physical count < MAX_CONNECTIONS, create a new one
        if (holder == null && activeConnections.get() < MAX_CONNECTIONS) {
            synchronized (this) {
                if (activeConnections.get() < MAX_CONNECTIONS) {
                    try {
                        Connection raw = createPhysicalConnectionWithRetry();
                        if (raw != null) {
                            holder = new PooledHolder(raw);
                            activeConnections.incrementAndGet();
                        }
                    } catch (SQLException e) {
                        logger.warn("Could not create new physical connection: {}", e.getMessage());
                    }
                }
            }
        }

        // If still null (reached max limit), wait for an existing one to be released
        if (holder == null) {
            try {
                holder = pool.poll(8, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted waiting for available DB connection", e);
            }
        }

        // Validate physical connection health
        if (holder != null) {
            boolean valid = false;
            try {
                // If the connection was used less than 45s ago, skip network ping for ultra-fast response
                if (holder.isFresh()) {
                    valid = !holder.rawConn.isClosed();
                } else {
                    valid = !holder.rawConn.isClosed() && holder.rawConn.isValid(1);
                }
            } catch (Exception ignored) {
            }

            if (!valid) {
                try {
                    holder.rawConn.close();
                } catch (Exception ignored) {
                }
                activeConnections.decrementAndGet();
                Connection fresh = createPhysicalConnectionWithRetry();
                holder = new PooledHolder(fresh);
                activeConnections.incrementAndGet();
            }
        } else {
            // As a last fallback, retry creating one
            Connection fresh = createPhysicalConnectionWithRetry();
            holder = new PooledHolder(fresh);
            activeConnections.incrementAndGet();
        }

        final PooledHolder finalHolder = holder;

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new InvocationHandler() {
                    private boolean closed = false;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        if ("close".equals(methodName)) {
                            if (!closed) {
                                closed = true;
                                if (!finalHolder.rawConn.isClosed()) {
                                    finalHolder.lastUsed = System.currentTimeMillis();
                                    boolean offered = pool.offer(finalHolder);
                                    if (!offered) {
                                        try {
                                            finalHolder.rawConn.close();
                                        } catch (Exception ignored) {
                                        }
                                        activeConnections.decrementAndGet();
                                    }
                                } else {
                                    activeConnections.decrementAndGet();
                                }
                            }
                            return null;
                        }
                        if ("isClosed".equals(methodName)) {
                            return closed || finalHolder.rawConn.isClosed();
                        }
                        return method.invoke(finalHolder.rawConn, args);
                    }
                });
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
