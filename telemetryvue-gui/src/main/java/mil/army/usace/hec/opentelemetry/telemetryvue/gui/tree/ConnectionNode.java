package mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;

public class ConnectionNode {

    private TelemetryConnection _connection;

    ConnectionNode(TelemetryConnection connection) {
        _connection = connection;
    }

    public TelemetryConnection getConnection() {
        return _connection;
    }

    @Override
    public String toString() {
        return _connection.getName();
    }

}
