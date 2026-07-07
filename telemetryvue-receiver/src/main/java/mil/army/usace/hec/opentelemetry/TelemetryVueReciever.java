package mil.army.usace.hec.opentelemetry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.SpanFlags;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import mil.army.usace.hec.opentelemetry.h2.DAOObjects.JDBCSpan;

public class TelemetryVueReciever extends TraceServiceGrpc.TraceServiceImplBase {
    
    private TelemetryConnection telCon;
    

    public TelemetryVueReciever(TelemetryConnection telCon)
    {
        this.telCon = telCon;
    }

    @Override
    public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver)
    {
        final var spanDao = telCon.getTelemetryDaoFactory().getSpanDao();
        var spanData = new ArrayList<SpanData>();
        for (var resourceSpans: request.getResourceSpansList())
        {
            for (var scopedSpanList: resourceSpans.getScopeSpansList())
            {
                
                for (Span span: scopedSpanList.getSpansList())
                {
                    spanData.add(toSpanData(span));
                }
            }
        }
        
        try {
            System.out.println("hello" + request.toString());
            spanDao.insertSpans(telCon, spanData);
            var response = ExportTraceServiceResponse.getDefaultInstance();
            responseObserver.onNext(response);
        } catch (TelemetryDataAccessException e) {
            responseObserver.onError(e);
        }
    }

    private SpanData toSpanData(Span span) {
        final var name = span.getName();
        final var kind = mapKind(span.getKind());
        final var status = mapCode(span.getStatus().getCode());
        final var statusData = StatusData.create(status, span.getStatus().getMessage());
        final var start = span.getStartTimeUnixNano();
        final var end = span.getEndTimeUnixNano();
        final SpanContext context = extractContext(span);
        final SpanContext parentContext = SpanContext.getInvalid();
        final Attributes attributes = null;
        final List<EventData> events = List.of();
        final List<LinkData> links = List.of();
        final Resource resource = null;



        return new JDBCSpan(name, kind, context, parentContext, statusData, start, end, attributes, events, links, false, resource);
    }

    private SpanContext extractContext(Span span) {
        var traceId = toHex(span.getTraceId().toByteArray());
        System.out.println( "TraceId(hex) " + traceId + " TraceId (ByteString)" + span.getTraceId().toString());
        var spanId = toHex(span.getSpanId().toByteArray());
        var traceStateBuilder = TraceState.builder();
        var traceStateStr = span.getTraceState();
        if (traceStateStr != null) {
            for(var pairStr: traceStateStr.split(",")) {
                var pair = pairStr.split("=");
                if (pair.length == 2) {
                    traceStateBuilder.put(pair[0], pair[1]);
                }
            }
        }

        var traceState = traceStateBuilder.build();
        var spanFlags = (byte)(span.getFlags() & SpanFlags.SPAN_FLAGS_TRACE_FLAGS_MASK.getNumber());
        var traceFlags = TraceFlags.fromByte(spanFlags);
        return SpanContext.create(traceId, spanId, traceFlags, traceState);
    }

    public SpanKind mapKind(io.opentelemetry.proto.trace.v1.Span.SpanKind protoKind) {
        SpanKind ret = SpanKind.INTERNAL;
        if (protoKind == io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_SERVER) {
            ret = SpanKind.SERVER;
        } else if (protoKind == io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CLIENT) {
            ret = SpanKind.CLIENT;
        } else if (protoKind == io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_CONSUMER) {
            ret = SpanKind.CONSUMER;
        } else if (protoKind == io.opentelemetry.proto.trace.v1.Span.SpanKind.SPAN_KIND_PRODUCER) {
            ret = SpanKind.PRODUCER;
        }
        return ret;
    }

    public String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length*2);
        for (var b: bytes) {
            sb.append(String.format("%x", b));
        }

        return sb.toString();
    }

    public StatusCode mapCode(io.opentelemetry.proto.trace.v1.Status.StatusCode statusCode) {
        StatusCode ret = StatusCode.UNSET;
        if (statusCode == io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_OK) {
            ret = StatusCode.OK;
        } else if (statusCode == io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_ERROR) {
            ret = StatusCode.ERROR;
        }
        return ret;
    }

    public static void main(String args[]) throws InterruptedException, IOException
    {
        DaoFactory<?> h2DaoFactory = DaoFactory.getDaoFactory("h2sql");
        Map<String, Object> connectionParameters = new HashMap<>();
        connectionParameters.put("file", Path.of(args.length == 1 ? args[0] : "tcp://localhost:9092/mem:mydb"));

        TelemetryConnection connection = h2DaoFactory.getConnection(connectionParameters);
        try {
            h2DaoFactory.initIfNeeded(connection);
        } catch (TelemetryDataAccessException e) {
            throw new RuntimeException(e);
        }
        
        var receiver = new TelemetryVueReciever(connection);
        var server = ServerBuilder.forPort(4317).addService(receiver).build();
        server.start();
        server.awaitTermination();
    }
}
