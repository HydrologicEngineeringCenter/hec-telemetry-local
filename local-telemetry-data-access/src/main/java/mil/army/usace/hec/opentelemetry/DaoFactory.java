package mil.army.usace.hec.opentelemetry;

import rma.util.lookup.Lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A factory for creating connections to a telemetry backend, and for creating DAOs for that backend.
 * @param <T> The type of connection that this factory creates
 */
public interface DaoFactory<T extends TelemetryConnection> {

    /**
     * @return A unique user-friendly name for the connection type
     */
    String connectionType();

    /**
     * @return A map of connection parameters and their types
     */
    Map<String, Class<?>> getConnectionParameters();

    /**
     * Get a connection to the backend for the given parameters
     * @param parameters A map of connection parameters, see getConnectionParameters()
     * @return A connection to the backend, or null if the parameters are invalid
     */
    T getConnection(Map<String, Object> parameters);

    /**
     * @return An instance of the SpanDao compatible with the connections returned by getConnection() from this factory
     */
    SpanDao<T> getSpanDao();

    /**
     * @return An instance of the TraceDao compatible with the connections returned by getConnection() from this factory
     */
    TraceDao<T> createTraceDao();

    /**
     * Attempt to force the specified connection to flush to disk. Note that this operation may no-op for some backends,
     * such as those that use a remote database or other non-file-based storage.
     * @param connection The connection to flush
     * @throws TelemetryDataAccessException If the connection is invalid or the flush fails
     */
    void flushToDisk(TelemetryConnection connection) throws TelemetryDataAccessException;

    /**
     * Initialize the backend for the given connection. This may involve creating tables, indexes, or other structures.
     * @param connection The connection to initialize
     * @throws TelemetryDataAccessException If the connection is invalid or the initialization fails (such as already completed)
     */
    void initBackend(TelemetryConnection connection) throws TelemetryDataAccessException;

    /**
     * Check if the backend needs initialization. This may involve checking for the existence of tables, indexes, or other
     * structures.
     * @param connection The connection to check
     * @return True if the backend needs initialization, false otherwise
     * @throws TelemetryDataAccessException If the connection is invalid or the check fails
     */
    boolean backendNeedsInit(TelemetryConnection connection) throws TelemetryDataAccessException;

    /**
     * Initialize the backend for the given connection if it has not already been initialized. This may involve creating
     * tables, indexes, or other structures.
     * @param connection The connection to initialize
     * @throws TelemetryDataAccessException If the connection is invalid or the initialization fails
     */
    default void initIfNeeded(TelemetryConnection connection) throws TelemetryDataAccessException {
        if(backendNeedsInit(connection)){
            initBackend(connection);
        }
    }

    /**
     * @return A list of all known DaoFactories
     */
    static List<? extends DaoFactory<?>> getDaoFactories() {
        Collection<? extends DaoFactory<?>> factories = (Collection<? extends DaoFactory<?>>) Lookup.getDefault().lookupAll(DaoFactory.class);
        return new ArrayList<>(factories);
    }
    /**
     * @return The DaoFactory for the given connection type, or null if a matching one isn't found
     */
    static DaoFactory<?> getDaoFactory(String type) {
        return getDaoFactories().stream()
                .filter(factory -> factory.connectionType().equals(type))
                .findFirst()
                .orElse(null);
    }

}
