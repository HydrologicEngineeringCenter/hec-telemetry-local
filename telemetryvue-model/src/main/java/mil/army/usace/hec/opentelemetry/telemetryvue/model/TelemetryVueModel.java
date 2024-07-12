package mil.army.usace.hec.opentelemetry.telemetryvue.model;

import com.google.common.flogger.FluentLogger;
import io.opentelemetry.sdk.trace.data.SpanData;
import mil.army.usace.hec.opentelemetry.SpanDao;
import mil.army.usace.hec.opentelemetry.SpanSelectionOptions;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.objects.SpanID;
import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.OperatingMode;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.actions.OpenDatabaseAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.ConnectionListener;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.TelemtryListener;
import rma.util.lookup.Lookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TelemetryVueModel {

    private final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private final List<OpenDatabaseAction<?>> _openActions = new ArrayList<>();
    private final OperatingMode _operatingMode;
    private final List<TelemetryConnection> _connections = new ArrayList<>();
    private final List<ConnectionListener> _connectionListeners = new ArrayList<>();
    private final List<TelemtryListener> _traceListeners = new ArrayList<>();
    private final ExecutorService _executorService = Executors.newFixedThreadPool(4);
    private final Map<TelemetryConnection, List<NestedTraceData>> _displayedTraces = new HashMap<>();
    private TelemetryConnection _selectedConnection;

    public TelemetryVueModel(OperatingMode operatingMode) {
        _operatingMode = operatingMode;
        populateOpenActions();
    }

    public OperatingMode getOperatingMode() {
        return _operatingMode;
    }

    public List<OpenDatabaseAction<?>> getOpenDatabaseActions() {
        return Collections.unmodifiableList(_openActions);
    }

    private void populateOpenActions() {
        _openActions.clear();
        for (OpenDatabaseAction<?> openDatabaseAction : Lookup.getDefault().lookupAll(OpenDatabaseAction.class)) {
            if (openDatabaseAction.getDatabaseObjectType() != null) {
                _openActions.add(openDatabaseAction);
            }
        }
    }

    public void tryOpenDatabase(OpenDatabaseAction<?> openDatabaseAction, Map<String, Object> extraOpenParameters) {
        TelemetryConnection connection = openDatabaseAction.openDatabase(extraOpenParameters);
        if (connection != null) {
            _connections.add(connection);
            _connectionListeners.forEach(listener -> listener.connectionAdded(connection));
        }
    }

    public void closeAllConnections() {
        for(int i = _connections.size() - 1; i >= 0; i--) {
            closeConnection(_connections.get(i));
        }
        _connections.clear();
    }

    public void setSelectedConnection(TelemetryConnection connection) {
        _selectedConnection = connection;
        _connectionListeners.forEach(listener -> listener.connectionSelected(connection));
    }

    public TelemetryConnection getSelectedConnection() {
        return _selectedConnection;
    }

    public void closeSelectedConnection() {
        if (_selectedConnection != null) {
            closeConnection(_selectedConnection);
        }
    }

    public void closeConnection(TelemetryConnection connection) {
        try {
            if (_connections.contains(connection)) {
                if (getSelectedConnection() == connection) {
                    setSelectedConnection(null);
                }
                for (NestedTraceData traceData : new ArrayList<>(_displayedTraces.getOrDefault(connection, Collections.emptyList()))) {
                    unshowTrace(connection, traceData.getTrace());
                }
                connection.close();
                _connections.remove(connection);
                _connectionListeners.forEach(listener -> listener.connectionRemoved(connection));
            } else {
                LOGGER.atWarning().log("Attempting to close connection not managed by TelemetryVue!");
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error closing connection");
        }
    }

    public List<NestedTraceData> getAllDisplayedTraces() {
        return _displayedTraces.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<NestedTraceData> getDisplayedTraces(TelemetryConnection connection) {
        return _displayedTraces.getOrDefault(connection, Collections.emptyList());
    }

    public void showTrace(TelemetryConnection connection, Trace trace) {
        AtomicBoolean traceAlreadyShown = new AtomicBoolean(false);
        if (_displayedTraces.containsKey(connection)) {
            List<NestedTraceData> displayedTraces = _displayedTraces.get(connection);
            displayedTraces.stream()
                    .filter(traceData -> traceData.getTrace().getTraceID().equals(trace.getTraceID()))
                    .findFirst()
                    .ifPresent(traceData -> {
                        traceAlreadyShown.set(true);
                        _traceListeners.forEach(listener -> listener.showTrace(traceData));
                    });
        }
        if (!traceAlreadyShown.get()) {
            executeOnExecutorService(() -> {
                try {
                    SpanDao<?> spanDao = connection.getTelemetryDaoFactory().getSpanDao();
                    String nullSpanId = SpanID.nullSpanID().getId();
                    List<SpanData> topLevelSpans = spanDao.retrieveSpans(connection, SpanSelectionOptions.byTraceId(trace.getTraceID())).stream()
                            .filter(spanData -> nullSpanId.equals(spanData.getParentSpanId()))
                            .collect(Collectors.toList());
                    Map<SpanData, List<SpanData>> nestedSpanData = new HashMap<>();

                    List<SpanData> spanDataToRetrieve = new ArrayList<>(topLevelSpans);
                    while (!spanDataToRetrieve.isEmpty()) {
                        SpanData spanData = spanDataToRetrieve.remove(0);
                        List<SpanData> childSpans = spanDao.retrieveSpans(connection, SpanSelectionOptions.byParentSpanId(SpanID.of(spanData.getSpanId())));
                        nestedSpanData.put(spanData, childSpans);
                        spanDataToRetrieve.addAll(childSpans);
                    }
                    return new NestedTraceData(trace, topLevelSpans, nestedSpanData);
                } catch (TelemetryDataAccessException e) {
                    LOGGER.atSevere().withCause(e).log("Error retrieving trace %s from connection %s", trace.getTraceID(), connection);
                    return null;
                }
            }, (traceData) -> {
                if (traceData != null) {
                    _displayedTraces.computeIfAbsent(connection, c -> new ArrayList<>()).add(traceData);
                    _traceListeners.forEach(listener -> listener.showTrace(traceData));
                }
            });
        }
    }

    public void unshowTrace(TelemetryConnection connection, Trace trace) {
        if (_displayedTraces.containsKey(connection)) {
            List<NestedTraceData> displayedTraces = _displayedTraces.get(connection);
            displayedTraces.stream()
                    .filter(traceData -> traceData.getTrace().getTraceID().equals(trace.getTraceID()))
                    .findFirst()
                    .ifPresent(traceData -> {
                        _traceListeners.forEach(listener -> listener.unshowTrace(traceData));
                        displayedTraces.remove(traceData);
                    });
        }
    }

    public void tryFlushDatabase(TelemetryConnection connection) {
        executeOnExecutorService(() -> {
            try {
                connection.getTelemetryDaoFactory().flushToDisk(connection);
            } catch (TelemetryDataAccessException e) {
                LOGGER.atSevere().withCause(e).log("Error flushing to disk.");
            }
        });
    }

    public List<TelemetryConnection> getAvailableConnections() {
        return Collections.unmodifiableList(_connections);
    }

    public void addConnectionListener(ConnectionListener listener) {
        _connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        _connectionListeners.remove(listener);
    }

    public <T> void executeOnExecutorService(Supplier<T> supplier, Consumer<T> consumer) {
        _executorService.execute(() -> {
            T result = supplier.get();
            consumer.accept(result);
        });
    }

    public void executeOnExecutorService(Runnable runnable) {
        _executorService.execute(runnable);
    }

    public void addTraceListener(TelemtryListener listener) {
        _traceListeners.add(listener);
    }

    public void removeTraceListener(TelemtryListener listener) {
        _traceListeners.remove(listener);
    }

}
