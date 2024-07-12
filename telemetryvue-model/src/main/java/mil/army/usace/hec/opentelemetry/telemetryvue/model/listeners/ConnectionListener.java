package mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;

public interface ConnectionListener {

    void connectionAdded(TelemetryConnection connection);
    void connectionRemoved(TelemetryConnection connection);
    void connectionSelected(TelemetryConnection connection);

}
