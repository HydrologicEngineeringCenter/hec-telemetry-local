package mil.army.usace.hec.opentelemetry.h2.DAOObjects;

import mil.army.usace.hec.opentelemetry.objects.SpanID;
import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.objects.TraceID;

public class JDBCTrace implements Trace {

    private final String _traceName;
    private final long _traceDurationNanos;
    private final long _traceStartTimeNanos;
    private final TraceID _traceID;
    private final SpanID _primarySpanID;

    public JDBCTrace(String traceName, long traceDurationNanos, long traceStartTimeNanos, TraceID traceID, SpanID primarySpanID) {
        _traceName = traceName;
        _traceDurationNanos = traceDurationNanos;
        _traceStartTimeNanos = traceStartTimeNanos;
        _traceID = traceID;
        _primarySpanID = primarySpanID;
    }

    @Override
    public TraceID getTraceID() {
        return _traceID;
    }

    @Override
    public SpanID getPrimarySpanID() {
        return _primarySpanID;
    }

    @Override
    public String getTraceName() {
        return _traceName;
    }

    @Override
    public long getTraceDurationNanos() {
        return _traceDurationNanos;
    }

    @Override
    public long getTraceStartTimeNanos() {
        return _traceStartTimeNanos;
    }
}
