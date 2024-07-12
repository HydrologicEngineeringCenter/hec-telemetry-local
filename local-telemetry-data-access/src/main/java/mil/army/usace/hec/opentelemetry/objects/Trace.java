package mil.army.usace.hec.opentelemetry.objects;

import java.time.Duration;
import java.time.Instant;

public interface Trace {

    public TraceID getTraceID();

    /**
     * Primary span is the first span in the trace, with no parent span
     */
    public SpanID getPrimarySpanID();

    /**
     * In OpenTelemetry, the trace has no name, just an ID. This is inherited from the Primary Span, to give
     * the trace hopefully a more meaningful name.
     */
    public String getTraceName();

    /**
     * The duration of the trace is the duration of the primary span, to hopefully make the trace more identifiable.
     */
    public long getTraceDurationNanos();

    /**
     * The start time of the trace is the start time of the primary span, to hopefully make the trace more identifiable.
     */
    public long getTraceStartTimeNanos();

    default Instant getTraceStartTime() {
        return Instant.EPOCH.plusNanos(getTraceStartTimeNanos());
    }

    default Duration getTraceDuration() {
        return Duration.ofNanos(this.getTraceDurationNanos());
    }

}
