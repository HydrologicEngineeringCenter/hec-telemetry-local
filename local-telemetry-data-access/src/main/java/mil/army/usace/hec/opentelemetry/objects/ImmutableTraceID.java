package mil.army.usace.hec.opentelemetry.objects;

public class ImmutableTraceID extends ImmutableID<TraceID> implements TraceID {
    ImmutableTraceID(String id) {
        super(id);
    }
}
