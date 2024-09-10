package mil.army.usace.hec.opentelemetry.telemetryvue.gui.util;

import mil.army.usace.hec.opentelemetry.objects.Trace;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TraceNameUtil {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat millisFormatter = new DecimalFormat("000");

    private TraceNameUtil() {

    }

    public static String formatTraceName(Trace trace) {
        LocalDateTime date = LocalDateTime.ofInstant(trace.getTraceStartTime(), ZoneId.systemDefault());
        Duration traceDuration = trace.getTraceDuration();
        return traceDuration.getSeconds() + "." + millisFormatter.format(nanosToMillis(traceDuration.getNano())) + "s "
                + formatter.format(date) + " " + trace.getTraceName();
    }

    static long nanosToMillis(long millis) {
        return millis / 1_000_000;
    }
}
