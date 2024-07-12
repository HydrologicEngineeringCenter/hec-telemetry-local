package mil.army.usace.hec.opentelemetry.telemetryvue.model.actions;

import java.util.Map;

public interface PickDatabaseAction<T> {

    Class<T> supportedDatabaseObject();
    T pickDatabase(Map<String, Object> additionalParameters);

}
