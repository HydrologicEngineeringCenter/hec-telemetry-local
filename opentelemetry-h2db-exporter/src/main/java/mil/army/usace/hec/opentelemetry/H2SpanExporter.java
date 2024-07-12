package mil.army.usace.hec.opentelemetry;

import com.google.common.flogger.FluentLogger;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class H2SpanExporter implements SpanExporter {
    private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private final AtomicBoolean isShutdown = new AtomicBoolean();

    private final TelemetryConnection _connection;

    public H2SpanExporter(TelemetryConnection connection) {
        _connection = connection;
    }

    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }
        try {
            DaoFactory<?> telemetryDaoFactory = _connection.getTelemetryDaoFactory();
            SpanDao<?> spanDao = telemetryDaoFactory.getSpanDao();
            spanDao.insertSpans(_connection, new ArrayList<>(spans));
        } catch (TelemetryDataAccessException e) {
            LOGGER.atSevere().withCause(e).log("Error exporting spans.");
            return CompletableResultCode.ofFailure();
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        try {
            _connection.getTelemetryDaoFactory().flushToDisk(_connection);
        } catch(TelemetryDataAccessException e) {
            LOGGER.atSevere().withCause(e).log("Error flushing to disk.");
            return CompletableResultCode.ofFailure();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        try {
            _connection.close();
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Error closing connection.");
            return CompletableResultCode.ofFailure();
        }
        if (!isShutdown.compareAndSet(false, true)) {
            LOGGER.atWarning().log("Calling shutdown() multiple times.");
            return CompletableResultCode.ofSuccess();
        }
        return flush();
    }
}
