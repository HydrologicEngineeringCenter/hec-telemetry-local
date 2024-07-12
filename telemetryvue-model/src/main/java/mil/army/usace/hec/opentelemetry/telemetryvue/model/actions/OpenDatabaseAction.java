package mil.army.usace.hec.opentelemetry.telemetryvue.model.actions;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import rma.util.lookup.Lookup;

import java.util.HashMap;
import java.util.Map;

public interface OpenDatabaseAction<T> {

    TelemetryConnection openDatabase(Map<String, Object> extraOpenParameters);
    Class<T> getDatabaseObjectType();
    Map<String, Object> getAdditionalChoosingParameters();
    String databaseType();

    default T pickDatabase(Map<String, Object> extraOpenParameters) {
        Map<String, Object> additionalParameters = new HashMap<>(getAdditionalChoosingParameters());
        additionalParameters.putAll(extraOpenParameters);
        return ((PickDatabaseAction<T>)Lookup.getDefault().lookupAll(PickDatabaseAction.class).
                stream().
                filter(pickDatabaseAction -> pickDatabaseAction.supportedDatabaseObject().isAssignableFrom(getDatabaseObjectType())).
                findFirst().
                orElseThrow(() -> new RuntimeException("No PickDatabaseAction found for " + getDatabaseObjectType().getName()))).
                pickDatabase(additionalParameters);
    }
}
