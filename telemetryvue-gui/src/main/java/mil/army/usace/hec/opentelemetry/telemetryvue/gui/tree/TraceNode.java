package mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.util.TraceNameUtil;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TraceNode {

    private final TelemetryConnection _connection;
    private final Trace _trace;


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
        return TraceNameUtil.formatTraceName(_trace);
    }



}
