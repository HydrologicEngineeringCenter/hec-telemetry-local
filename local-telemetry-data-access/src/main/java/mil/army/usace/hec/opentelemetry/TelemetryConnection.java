package mil.army.usace.hec.opentelemetry;

/**
 * A connection to a telemetry data source. Implementations of this interface are expected to be
 * thread-safe and behave like a connection pool.
 */
public interface TelemetryConnection extends AutoCloseable {

    /**
     * A reference to the telemetry DAO factory that created this connection.
     * @return the telemetry DAO factory
     */
    DaoFactory<?> getTelemetryDaoFactory();

    /**
     * Closes the connection to the telemetry data source.
     */
    boolean isClosed();

    /**
     * @return The name of this specific connection
     */
    String getName();

}
