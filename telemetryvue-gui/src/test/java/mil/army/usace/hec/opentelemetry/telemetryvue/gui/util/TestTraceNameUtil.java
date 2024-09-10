package mil.army.usace.hec.opentelemetry.telemetryvue.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTraceNameUtil {

    @Test
    public void testNanosToMillis() {
        long nanos = 11_000_000;
        assertEquals(11, TraceNameUtil.nanosToMillis(nanos));
    }

}
