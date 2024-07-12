package mil.army.usace.hec.opentelemetry.h2;

import com.google.common.flogger.FluentLogger;
import mil.army.usace.hec.opentelemetry.DaoFactory;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class H2TelemetryConnection implements TelemetryConnection {
    private static FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private AtomicBoolean _closed = new AtomicBoolean(false);
    private final ConnectionFactory _connectionFactory;
    private final PoolableConnectionFactory _poolableConnectionFactory;
    private final ObjectPool<PoolableConnection> _connectionPool;
    private final PoolingDriver _driver;
    private final String _poolName;
    private final H2TelemetryFactory _factory;
    private final String _name;

    static {
        try {
            Class.forName("org.h2.Driver");
            Class.forName("org.apache.commons.dbcp2.PoolingDriver");
        } catch (ClassNotFoundException e) {
            LOGGER.atSevere().withCause(e).log("Error loading H2 driver");
        }
    }

    private H2TelemetryConnection(H2TelemetryFactory factory, ConnectionFactory connectionFactory,
                                  PoolableConnectionFactory poolableConnectionFactory,
                                  ObjectPool<PoolableConnection> connectionPool, PoolingDriver driver, String poolName,
                                  String name) {
        _factory = factory;
        _connectionFactory = connectionFactory;
        _poolableConnectionFactory = poolableConnectionFactory;
        _connectionPool = connectionPool;
        _driver = driver;
        _poolName = poolName;
        _name = name;
    }

    public Connection getConnection() throws SQLException {
        if (_closed.get()) {
            throw new SQLException("Connection is closed");
        }
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:" + _poolName);
    }

    @Override
    public void close() {
        if (_driver != null) {
            try {
                _driver.closePool(_poolName);
            } catch (SQLException e) {
                LOGGER.atSevere().withCause(e).log("Error closing pool");
            }
        }
        _connectionPool.close();
        _closed.set(true);
    }

    @Override
    public boolean isClosed() {
        return _closed.get();
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public DaoFactory<?> getTelemetryDaoFactory() {
        return _factory;
    }

    public static H2TelemetryConnection of(H2TelemetryFactory factory, String jdbcURL, String name) {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcURL, null);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        PoolingDriver driver;
        try {
            driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        } catch (SQLException e) {
            LOGGER.atSevere().withCause(e).log("Error getting driver");
            throw new RuntimeException(e);
        }

        String poolName = jdbcURL.replace("jdbc:h2:", "");
        int beforeSemicolon = poolName.indexOf(';');
        if (beforeSemicolon > 0) {
            poolName = poolName.substring(0, beforeSemicolon);
        }
        poolName = poolName.replace(":", "_");

        driver.registerPool(poolName, connectionPool);
        return new H2TelemetryConnection(factory, connectionFactory, poolableConnectionFactory, connectionPool, driver, poolName, name);
    }
}
