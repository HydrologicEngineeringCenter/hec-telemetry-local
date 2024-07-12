package mil.army.usace.hec.opentelemetry.telemetryvue.model;

import io.opentelemetry.sdk.trace.data.SpanData;
import mil.army.usace.hec.opentelemetry.objects.Trace;

import java.util.List;
import java.util.Map;

// Functionally equivalent to a Tree of SpanData
public class NestedTraceData {

    private Trace _trace;
    private List<SpanData> _spanDataList;
    private Map<SpanData, List<SpanData>> _nestedSpanDataMap;

    public NestedTraceData(Trace trace, List<SpanData> spanDataList, Map<SpanData, List<SpanData>> nestedSpanDataMap) {
        _trace = trace;
        _spanDataList = spanDataList;
        _nestedSpanDataMap = nestedSpanDataMap;
    }

    public Trace getTrace() {
        return _trace;
    }

    public List<SpanData> getRootSpanDataList() {
        return _spanDataList;
    }

    public List<SpanData> getNestedSpan(SpanData parentSpanData) {
        return _nestedSpanDataMap.get(parentSpanData);
    }
}
