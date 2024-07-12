package mil.army.usace.hec.opentelemetry.h2;

import mil.army.usace.hec.opentelemetry.DaoFactory;
import mil.army.usace.hec.opentelemetry.SpanDao;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.TraceDao;
import rma.services.annotations.ServiceProvider;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServiceProvider(service = DaoFactory.class)
public class H2TelemetryFactory implements DaoFactory<H2TelemetryConnection> {

    private final H2SpanDao _spanDao = new H2SpanDao();
    private final H2TraceDao _traceDao = new H2TraceDao();
    private final Map<String, H2TelemetryConnection> _connections = new ConcurrentHashMap<>();

    @Override
    public String connectionType() {
        return "h2sql";
    }

    @Override
    public Map<String, Class<?>> getConnectionParameters() {
        return Map.of("file", Path.class);
    }

    @Override
    public H2TelemetryConnection getConnection(Map<String, Object> parameters) {
        Path file = (Path) parameters.get("file");
        String jdbcUrl = "jdbc:h2:" + file;
        return connectionFor(jdbcUrl, "H2 " + file.getFileName().toString());
    }

    H2TelemetryConnection connectionFor(String jdbcUrl, String name) {
        H2TelemetryConnection connection = _connections.computeIfAbsent(jdbcUrl, url->H2TelemetryConnection.of(this, url, name));
        if(connection.isClosed()) {
            _connections.remove(jdbcUrl);
            return _connections.computeIfAbsent(jdbcUrl, url->H2TelemetryConnection.of(this, url, name));
        } else {
            return connection;
        }
    }

    @Override
    public SpanDao<H2TelemetryConnection> getSpanDao() {
        return _spanDao;
    }

    @Override
    public TraceDao<H2TelemetryConnection> createTraceDao() {
        return _traceDao;
    }

    @Override
    public void flushToDisk(TelemetryConnection connection) throws TelemetryDataAccessException {
        H2TelemetryConnection h2connection = assertConnectionType(connection);
        try(Connection conn = h2connection.getConnection()) {
            // Try really hard to flush the database and compact it
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.addBatch( "set EXCLUSIVE 1" );
            stmt.addBatch( "set retention_time 0" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "checkpoint" );
            stmt.addBatch( "set retention_time 45000" );
            stmt.addBatch( "set EXCLUSIVE 0" );
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }

    @Override
    public void initBackend(TelemetryConnection telemConnection) throws TelemetryDataAccessException {
        H2TelemetryConnection h2connection = assertConnectionType(telemConnection);

        try(Connection connection = h2connection.getConnection()) {
            // Span Context Table
            connection.createStatement().execute("CREATE TABLE " + H2SpanDao.SPAN_CONTEXT_TABLE_NAME + " (\n" +
                    "    " + H2SpanDao.TRACE_ID + " VARCHAR(32) NOT NULL,\n" +
                    "    " + H2SpanDao.SPAN_ID + " VARCHAR(16) NOT NULL,\n" +
                    "    " + H2SpanDao.TRACE_FLAGS + " VARCHAR(32) NOT NULL,\n" +
                    "    " + H2SpanDao.TRACE_STATE + " CLOB NOT NULL,\n" +
                    "    PRIMARY KEY ("+H2SpanDao.TRACE_ID+", "+H2SpanDao.SPAN_ID+")\n" +
                    ");");

            // Span table
            connection.createStatement().execute("CREATE TABLE " + H2SpanDao.SPAN_TABLE_NAME + " (\n" +
                    "    " + H2SpanDao.TRACE_ID + " VARCHAR(32) NOT NULL,\n" +
                    "    " + H2SpanDao.SPAN_ID + " VARCHAR(16) NOT NULL,\n" +
                    "    " + H2SpanDao.SPAN_KIND + " VARCHAR(16) NOT NULL,\n" +
                    "    " + H2SpanDao.PARENT_SPAN_ID + " VARCHAR(16),\n" +
                    "    " + H2SpanDao.NAME + " VARCHAR(255) NOT NULL,\n" +
                    "    " + H2SpanDao.START_TIME + " BIGINT NOT NULL,\n" +
                    "    " + H2SpanDao.END_TIME + " BIGINT NOT NULL,\n" +
                    "    " + H2SpanDao.ATTRIBUTES + " CLOB,\n" +
                    "    " + H2SpanDao.EVENTS + " CLOB,\n" +
                    "    " + H2SpanDao.STATUS_CODE + " VARCHAR(20),\n" +
                    "    " + H2SpanDao.STATUS_MESSAGE + " VARCHAR(255),\n" +
                    "    " + H2SpanDao.HAS_ENDED + " BOOLEAN NOT NULL,\n" +
                    "    PRIMARY KEY ("+H2SpanDao.TRACE_ID+", "+H2SpanDao.SPAN_ID+")\n" +
                    ");");
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }

    @Override
    public boolean backendNeedsInit(TelemetryConnection telemConnection) throws TelemetryDataAccessException {
        H2TelemetryConnection h2connection = assertConnectionType(telemConnection);

        try (Connection connection = h2connection.getConnection()) {
            ResultSet results = connection.getMetaData().getTables(null, null, null, null);
            List<String> tables = new ArrayList<>();
            while(results.next()) {
                tables.add(results.getString("TABLE_NAME").toLowerCase());
            }
            return !(tables.contains(H2SpanDao.SPAN_CONTEXT_TABLE_NAME.toLowerCase()) && tables.contains(H2SpanDao.SPAN_TABLE_NAME.toLowerCase()));
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }

    protected H2TelemetryConnection assertConnectionType(TelemetryConnection connection) throws TelemetryDataAccessException {
        if(!(connection instanceof H2TelemetryConnection)) {
            throw new TelemetryDataAccessException("Invalid connection type. "+H2TelemetryConnection.class.getSimpleName() + " required for " + getClass().getSimpleName());
        }
        return (H2TelemetryConnection) connection;
    }
}
