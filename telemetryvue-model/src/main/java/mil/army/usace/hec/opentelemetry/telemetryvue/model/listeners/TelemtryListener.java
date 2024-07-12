package mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners;

import mil.army.usace.hec.opentelemetry.telemetryvue.model.NestedTraceData;

public interface TelemtryListener {

    void showTrace(NestedTraceData trace);
    void unshowTrace(NestedTraceData trace);

}
