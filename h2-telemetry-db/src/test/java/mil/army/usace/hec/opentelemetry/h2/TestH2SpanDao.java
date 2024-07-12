package mil.army.usace.hec.opentelemetry.h2;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import mil.army.usace.hec.opentelemetry.SpanSelectionOptions;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.objects.TraceID;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import static io.opentelemetry.api.common.AttributeKey.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestH2SpanDao {

    private static final SpanData SPAN1 =
            TestSpanData.builder()
                    .setHasEnded(true)
                    .setSpanContext(
                            SpanContext.create(
                                    "12345678876543211234567887654321",
                                    "8765432112345678",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault()))
                    .setStartEpochNanos(100)
                    .setEndEpochNanos(100 + 1000)
                    .setStatus(StatusData.ok())
                    .setName("testSpan1")
                    .setKind(SpanKind.INTERNAL)
                    .setAttributes(Attributes.of(stringKey("animal"), "cat", longKey("lives"), 9L))
                    .setEvents(
                            Collections.singletonList(
                                    EventData.create(
                                            100 + 500,
                                            "somethingHappenedHere",
                                            Attributes.of(booleanKey("important"), true))))
                    .setTotalRecordedEvents(1)
                    .setTotalRecordedLinks(0)
                    .setInstrumentationScopeInfo(InstrumentationScopeInfo.create("tracer1"))
                    .build();

    private static final SpanData SPAN2 =
            TestSpanData.builder()
                    .setHasEnded(false)
                    .setSpanContext(
                            SpanContext.create(
                                    "12340000000043211234000000004321",
                                    "8765000000005678",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault()))
                    .setStartEpochNanos(500)
                    .setEndEpochNanos(500 + 1001)
                    .setStatus(StatusData.error())
                    .setName("testSpan2")
                    .setKind(SpanKind.CLIENT)
                    .setInstrumentationScopeInfo(
                            InstrumentationScopeInfo.builder("tracer2").setVersion("1.0").build())
                    .build();

    @Test
    public void testStoreRetrieveSpan() throws TelemetryDataAccessException, SQLException {
        String connectURI = "jdbc:h2:mem:ins_span;DB_CLOSE_DELAY=-1";

        H2TelemetryFactory factory = new H2TelemetryFactory();
        try(H2TelemetryConnection h2Connection = factory.connectionFor(connectURI, "H2 memory db")) {
            factory.initIfNeeded(h2Connection);

            H2SpanDao dao = (H2SpanDao) factory.getSpanDao();
            dao.insertSpan(h2Connection, SPAN1);
            dao.insertSpan(h2Connection, SPAN2);

            SpanData retrSpan1 = dao.retrieveSpans(h2Connection, SpanSelectionOptions.byTraceId(TraceID.of(SPAN1.getTraceId()))).stream().findFirst().orElseThrow();
            assertEquals(SPAN1.getSpanContext(), retrSpan1.getSpanContext());
            assertEquals(SPAN1.getName(), retrSpan1.getName());
            assertEquals(SPAN1.getStartEpochNanos(), retrSpan1.getStartEpochNanos());
            assertEquals(SPAN1.getEndEpochNanos(), retrSpan1.getEndEpochNanos());
            assertEquals(SPAN1.getAttributes(), retrSpan1.getAttributes());
            assertEquals(SPAN1.getStatus(), retrSpan1.getStatus());
        }
    }

    @Test
    public void testStoreRetrieveSpan2() throws TelemetryDataAccessException, SQLException {
        String connectURI = "jdbc:h2:mem:ins_span;DB_CLOSE_DELAY=-1";

        H2TelemetryFactory factory = new H2TelemetryFactory();
        try(H2TelemetryConnection h2Connection = factory.connectionFor(connectURI, "h2 memory")) {
            factory.initIfNeeded(h2Connection);

            H2SpanDao dao = (H2SpanDao) factory.getSpanDao();
            dao.insertSpan(h2Connection, SPAN1);
            dao.insertSpan(h2Connection, SPAN2);

            SpanData retrSpan1 = dao.retrieveSpans(h2Connection, SpanSelectionOptions.byTraceId(TraceID.of(SPAN2.getTraceId()))).stream().findFirst().orElseThrow();
            assertEquals(SPAN2.getSpanContext(), retrSpan1.getSpanContext());
            assertEquals(SPAN2.getName(), retrSpan1.getName());
            assertEquals(SPAN2.getStartEpochNanos(), retrSpan1.getStartEpochNanos());
            assertEquals(SPAN2.getEndEpochNanos(), retrSpan1.getEndEpochNanos());
            assertEquals(SPAN2.getAttributes(), retrSpan1.getAttributes());
            assertEquals(SPAN2.getStatus(), retrSpan1.getStatus());
        }
    }
}
