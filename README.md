# HEC Telemetry Local

Integration with OpenTelemetry intended for local use.
This project provides utilities and tools for OpenTelemetry, when used in a local, non-networked environment such
as desktop applications. When running OpenTelemetry in the cloud, it is recommended to evaluate other options for
centralized OTLP data collection.

Currently, this project provides a simple implementation of an H2DB Exporter for OpenTelemetry. In addition, there is
a simple user interface (TelemetryVue) to go with it.

The code is broken into several modules:

- local-telemetry-data-access: DAO layer for a Telemetry database
- h2-telemetry-db: H2DB implementation of the Telemetry database
- opentelemetry-h2db-reporter: OpenTelemetry Reporter for H2DB using local-telemetry-data-access and h2-telemetry-db
- telemetryvue-gui: The Swing components for the user interface
- telemetryvue-model: The business logic for the user interface
- telemetryvue-h2: The H2DB specific components for the model to allow for the user interface to interact with the H2 database
