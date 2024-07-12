package mil.army.usace.hec.opentelemetry.telemetryvue.h2;

import mil.army.usace.hec.opentelemetry.DaoFactory;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.actions.OpenDatabaseAction;
import rma.services.annotations.ServiceProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@ServiceProvider(service = OpenDatabaseAction.class)
public class OpenH2DatabaseAction implements OpenDatabaseAction<Path> {


    @Override
    public TelemetryConnection openDatabase(Map<String, Object> extraOpenParameters) {
        Path file = pickDatabase(extraOpenParameters);
        if(file != null && Files.isRegularFile(file)) {
            DaoFactory<?> h2DaoFactory = DaoFactory.getDaoFactory("h2sql");
            Map<String, Object> connectionProperties = new HashMap<>(extraOpenParameters);
            String fileName = file.getFileName().toString();
            if(fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.indexOf('.'));
            }
            file = file.getParent().resolve(fileName);
            connectionProperties.put("file", file);
            return h2DaoFactory.getConnection(connectionProperties);
        }
        return null;
    }

    @Override
    public Class<Path> getDatabaseObjectType() {
        return Path.class;
    }

    @Override
    public Map<String, Object> getAdditionalChoosingParameters() {
        Map<String, Object> additonalParams = new HashMap<>();
        Map<String, String> allowedFiles = new HashMap<>();
        allowedFiles.put("H2 OpenTelemetry Database", "db");
        additonalParams.put("allowedFileTypes", allowedFiles);
        return additonalParams;
    }

    @Override
    public String databaseType() {
        return "H2 OpenTelemetry DB";
    }
}
