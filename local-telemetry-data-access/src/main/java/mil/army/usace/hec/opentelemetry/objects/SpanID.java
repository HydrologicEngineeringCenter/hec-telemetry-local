package mil.army.usace.hec.opentelemetry.objects;

public interface SpanID extends ID<SpanID> {

    static SpanID of(String id) {
        return new ImmutableSpanID(id);
    }

    static SpanID nullSpanID() {
        return SpanID.of("0000000000000000");
    }
}
