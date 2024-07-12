package mil.army.usace.hec.opentelemetry;

import mil.army.usace.hec.opentelemetry.objects.SpanID;
import mil.army.usace.hec.opentelemetry.objects.TraceID;

/**
 * Options for selecting spans from a data source.
 */
public class SpanSelectionOptions {

    private final String _id;
    private final SpanSelectionType _type;

    private SpanSelectionOptions(SpanSelectionType type, String id) {
        _type = type;
        _id = id;
    }

    public SpanSelectionType getType() {
        return _type;
    }

    public String getId() {
        return _id;
    }

    /**
     * Select all spans without filtering.
     */
    public static SpanSelectionOptions withoutFiltering() {
        return new SpanSelectionOptions(SpanSelectionType.NO_FILTERING, null);
    }

    /**
     * Select spans by trace ID.
     * @param traceId The trace ID to select spans by
     * @return SpanSelectionOptions which specifies to select spans by the given trace ID
     */
    public static SpanSelectionOptions byTraceId(TraceID traceId) {
        return new SpanSelectionOptions(SpanSelectionType.BY_TRACE_ID, traceId.getId());
    }

    /**
     * Select spans by parent span ID.
     * @param parentSpanId The parent span ID to select spans by
     * @return SpanSelectionOptions which specifies to select spans by the given parent span ID
     */
    public static SpanSelectionOptions byParentSpanId(SpanID parentSpanId) {
        return new SpanSelectionOptions(SpanSelectionType.BY_PARENT_SPAN_ID, parentSpanId.getId());
    }

    public enum SpanSelectionType {
        NO_FILTERING,
        BY_TRACE_ID,
        BY_PARENT_SPAN_ID
    }

}
