package mil.army.usace.hec.opentelemetry;

/**
 * Exception thrown when there is an issue accessing telemetry data.
 */
public class TelemetryDataAccessException extends Exception {

    public TelemetryDataAccessException(String message) {
        super(message);
    }

    public TelemetryDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

}
