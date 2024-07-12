package mil.army.usace.hec.opentelemetry.h2;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDao;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;

public abstract class AbstractH2Dao implements TelemetryDao<H2TelemetryConnection> {
    @Override
    public Class<H2TelemetryConnection> getConnectionType() {
        return H2TelemetryConnection.class;
    }

    protected H2TelemetryConnection assertConnectionType(TelemetryConnection connection) throws TelemetryDataAccessException {
        if(!isConnectionType(connection.getClass())) {
            throw new TelemetryDataAccessException("Invalid connection type. "+H2TelemetryConnection.class.getSimpleName() + " required for " + getClass().getSimpleName());
        }
        return (H2TelemetryConnection) connection;
    }

    protected static String tablePrefix(String table, String field) {
        return table + "." + field;
    }

    protected static String assembleFields(String... fields) {
        StringBuilder sb = new StringBuilder();
        for(String field : fields) {
            sb.append(field).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    protected static String renameTo(String field, String newName) {
        return field + " AS " + newName;
    }
}
