package mil.army.usace.hec.opentelemetry;

/**
 * The base interface for all Telemetry DAOs.
 * @param <T> The type of Telemetry connection that this DAO supports.
 */
public interface TelemetryDao<T extends TelemetryConnection> {

    /**
     * @return The type of connection that this DAO uses
     */
    Class<T> getConnectionType();

    /**
     * @param connectionType The type of connection to check
     * @return True if the given connection type is compatible with this DAO
     */
    default boolean isConnectionType(Class<?> connectionType) {
        return connectionType.isAssignableFrom(getConnectionType());
    }

}
