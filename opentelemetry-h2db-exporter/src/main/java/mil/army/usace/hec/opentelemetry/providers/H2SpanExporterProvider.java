package mil.army.usace.hec.opentelemetry.providers;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import mil.army.usace.hec.opentelemetry.DaoFactory;
import mil.army.usace.hec.opentelemetry.H2SpanExporter;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import rma.services.annotations.ServiceProvider;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@ServiceProvider(service = ConfigurableSpanExporterProvider.class)
public class H2SpanExporterProvider  implements ConfigurableSpanExporterProvider {

    @Override
    public SpanExporter createExporter(ConfigProperties configProperties) {
        DaoFactory<?> h2DaoFactory = DaoFactory.getDaoFactory("h2sql");
        Map<String, Object> connectionParameters = new HashMap<>();
        connectionParameters.put("file", Path.of(configProperties.getString("file")));

        TelemetryConnection connection = h2DaoFactory.getConnection(connectionParameters);
        try {
            h2DaoFactory.initIfNeeded(connection);
        } catch (TelemetryDataAccessException e) {
            throw new RuntimeException(e);
        }

        return new H2SpanExporter(h2DaoFactory.getConnection(connectionParameters));
    }

    @Override
    public String getName() {
        return "h2sql";
    }
}
