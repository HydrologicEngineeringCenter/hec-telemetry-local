0package mil.army.usace.hec.opentelemetry;

import java.io.IOException;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
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
        var processor = SimpleSpanProcessor.create(SpanExporter.composite());
        for (var resourceSpans: request.getResourceSpansList())
        {
            for (var scopedSpanList: resourceSpans.getScopeSpansList())
            {
                
                for (Span span: scopedSpanList.getSpansList())
                {
                    var spanData = new JDBCSpan(span);
                }
            }
        }
        
        
        spanDao.insertSpans(telCon, request.getResourceSpansList().get(0).);
        System.out.println("hello" + request.toString());

        var response = ExportTraceServiceResponse.getDefaultInstance();
        responseObserver.onNext(response);
    }

    public static void main(String args[]) throws InterruptedException, IOException
    {
        var receiver = new TelemetryVueReciever();
        var server = ServerBuilder.forPort(4317).addService(receiver).build();
        server.start();
        server.awaitTermination();
    }
}
