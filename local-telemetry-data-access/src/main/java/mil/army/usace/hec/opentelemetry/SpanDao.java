package mil.army.usace.hec.opentelemetry;

import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.List;

/**
 * A Data Access Object (DAO) for spans. This interface provides methods for inserting and retrieving spans from a datasource
 * @param <T> The type of connection that this DAO uses
 */
public interface SpanDao<T extends TelemetryConnection> extends TelemetryDao<T> {

    /**
     * Insert a single span into the database
     * @param connection The connection to the database
     * @param span The span to insert
     * @throws TelemetryDataAccessException If the connection is invalid or the insert fails
     */
    default void insertSpan(TelemetryConnection connection, SpanData span) throws TelemetryDataAccessException {
        insertSpans(connection, List.of(span));
    }

    /**
     * Insert a list of spans into the database
     * @param connection The connection to the database
     * @param spans The spans to insert
     * @throws TelemetryDataAccessException If the connection is invalid or the insert fails
     */
    void insertSpans(TelemetryConnection connection, List<SpanData> spans) throws TelemetryDataAccessException;

    /**
     * Retrieve a list of spans from the database with the given options
     * @param connection The connection to the database
     * @param options The options to use when selecting spans
     * @return A list of spans that match the selection options
     * @throws TelemetryDataAccessException If the connection is invalid or the retrieval fails (such as filtering options
     * specifying data that does not exist)
     */
    List<SpanData> retrieveSpans(TelemetryConnection connection, SpanSelectionOptions options) throws TelemetryDataAccessException;

}
