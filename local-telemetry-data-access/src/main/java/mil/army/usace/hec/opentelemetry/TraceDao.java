package mil.army.usace.hec.opentelemetry;

import mil.army.usace.hec.opentelemetry.objects.Trace;

import java.util.List;

/**
 * Interface for accessing trace data. Implementations of this interface should be able to retrieve trace data from a
 * telemetry data source.
 * @param <T> The type of Telemetry connection that this DAO supports.
 */
public interface TraceDao<T extends TelemetryConnection> extends TelemetryDao<T> {

    /**
     * Get a list of available traces from the telemetry data source
     * @param connection The connection to the telemetry data source
     * @return A list of available traces
     * @throws TelemetryDataAccessException If the connection is invalid or the retrieval fails
     */
    List<Trace> getAvailableTraces(TelemetryConnection connection) throws TelemetryDataAccessException;

}
