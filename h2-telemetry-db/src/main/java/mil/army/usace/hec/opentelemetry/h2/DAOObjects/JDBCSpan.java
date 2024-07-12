package mil.army.usace.hec.opentelemetry.h2.DAOObjects;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.List;

public class JDBCSpan implements SpanData {

    private final String _name;
    private final SpanKind _kind;
    private final SpanContext _spanContext;
    private final SpanContext _parentSpanContext;
    private final StatusData _status;
    private final long _startEpochNanos;
    private final Attributes _attributes;
    private final List<EventData> _events;
    private final List<LinkData> _links;
    private final long _endEpochNanos;
    private final boolean _hasEnded;
    private final Resource _resource;

    public JDBCSpan(String name, SpanKind kind, SpanContext spanContext, SpanContext parentSpanContext,
                    StatusData status, long startEpochNanos, long endEpochNanos, Attributes attributes, List<EventData> events,
                    List<LinkData> links, boolean hasEnded, Resource resource) {
        _name = name;
        _kind = kind;
        _spanContext = spanContext;
        _parentSpanContext = parentSpanContext;
        _status = status;
        _startEpochNanos = startEpochNanos;
        _attributes = attributes;
        _events = events;
        _links = links;
        _endEpochNanos = endEpochNanos;
        _hasEnded = hasEnded;
        _resource = resource;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public SpanKind getKind() {
        return _kind;
    }

    @Override
    public SpanContext getSpanContext() {
        return _spanContext;
    }

    @Override
    public SpanContext getParentSpanContext() {
        return _parentSpanContext;
    }

    @Override
    public StatusData getStatus() {
        return _status;
    }

    @Override
    public long getStartEpochNanos() {
        return _startEpochNanos;
    }

    @Override
    public Attributes getAttributes() {
        return _attributes;
    }

    @Override
    public List<EventData> getEvents() {
        return _events;
    }

    @Override
    public List<LinkData> getLinks() {
        return _links;
    }

    @Override
    public long getEndEpochNanos() {
        return _endEpochNanos;
    }

    @Override
    public boolean hasEnded() {
        return _hasEnded;
    }

    @Override
    public int getTotalRecordedEvents() {
        return _events.size();
    }

    @Override
    public int getTotalRecordedLinks() {
        return _links.size();
    }

    @Override
    public int getTotalAttributeCount() {
        return _attributes.size();
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
        return InstrumentationLibraryInfo.empty();
    }

    @Override
    public Resource getResource() {
        return _resource;
    }
}
