package mil.army.usace.hec.opentelemetry.objects;

public class ImmutableSpanID extends ImmutableID<SpanID> implements SpanID {
    ImmutableSpanID(String id) {
        super(id);
    }
}
