package mil.army.usace.hec.opentelemetry.objects;

public interface TraceID extends ID<TraceID>{

    static TraceID of(String id) {
        return new ImmutableTraceID(id);
    }

}
