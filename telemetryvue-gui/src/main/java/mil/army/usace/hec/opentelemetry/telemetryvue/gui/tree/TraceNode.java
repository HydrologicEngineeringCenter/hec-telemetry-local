package mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.objects.Trace;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TraceNode {

    private final TelemetryConnection _connection;
    private final Trace _trace;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TraceNode(TelemetryConnection connection, Trace trace) {
        _connection = connection;
        _trace = trace;
    }

    public TelemetryConnection getConnection() {
        return _connection;
    }

    public Trace getTrace() {
        return _trace;
    }

    @Override
    public String toString() {
        LocalDateTime date = LocalDateTime.ofInstant(_trace.getTraceStartTime(), ZoneId.systemDefault());
        return _trace.getTraceDuration().getSeconds() + "s " + formatter.format(date) + " " + _trace.getTraceName();
    }

}
