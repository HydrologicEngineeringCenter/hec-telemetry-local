package mil.army.usace.hec.opentelemetry.h2;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import mil.army.usace.hec.opentelemetry.SpanDao;
import mil.army.usace.hec.opentelemetry.SpanSelectionOptions;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.h2.DAOObjects.JDBCSpan;
import mil.army.usace.hec.opentelemetry.h2.json.AttributesJsonAdapter;
import mil.army.usace.hec.opentelemetry.h2.json.TraceStateJsonAdapter;
import mil.army.usace.hec.opentelemetry.objects.SpanID;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class H2SpanDao extends AbstractH2Dao implements SpanDao<H2TelemetryConnection> {

    static final String SPAN_TABLE_NAME = "span";
    static final String SPAN_CONTEXT_TABLE_NAME = "span_context";
    static final String TRACE_ID = "trace_id";
    static final String TRACE_FLAGS = "trace_flags";
    static final String PARENT_TRACE_FLAGS = "parent_trace_flags";
    static final String TRACE_STATE = "trace_state";
    static final String PARENT_TRACE_STATE = "parent_trace_state";
    static final String SPAN_ID = "span_id";
    static final String SPAN_KIND = "span_kind";
    static final String PARENT_SPAN_ID = "parent_span_id";
    static final String NAME = "name";
    static final String START_TIME = "start_time";
    static final String END_TIME = "end_time";
    static final String ATTRIBUTES = "attributes";
    static final String EVENTS = "events";
    static final String STATUS_CODE = "status_code";
    static final String STATUS_MESSAGE = "status_message";
    static final String HAS_ENDED = "has_ended";

    private static final String NULL_SPAN_ID = SpanID.nullSpanID().getId();
    private static final String SELECT_SPAN_QUERY = "SELECT " +
            assembleFields(tablePrefix("s", TRACE_ID),
                    tablePrefix("sc", TRACE_FLAGS),
                    tablePrefix("sc", TRACE_STATE),
                    tablePrefix("s", SPAN_ID),
                    SPAN_KIND,
                    PARENT_SPAN_ID,
                    renameTo(tablePrefix("psc", TRACE_FLAGS), PARENT_TRACE_FLAGS),
                    renameTo(tablePrefix("psc", TRACE_STATE), PARENT_TRACE_STATE),
                    NAME,
                    START_TIME,
                    END_TIME,
                    ATTRIBUTES,
                    EVENTS,
                    STATUS_CODE,
                    STATUS_MESSAGE,
                    HAS_ENDED
            )
            + " FROM " + SPAN_TABLE_NAME + " s "+
            "JOIN " + SPAN_CONTEXT_TABLE_NAME + " sc ON " + tablePrefix("s", TRACE_ID) + " = " + tablePrefix("sc", TRACE_ID) +
            " AND " + tablePrefix("s", SPAN_ID) + " = " + tablePrefix("sc", SPAN_ID) +
            " LEFT JOIN " + SPAN_CONTEXT_TABLE_NAME + " psc ON " + tablePrefix("s", TRACE_ID) + " = " + tablePrefix("psc", TRACE_ID) +
            " AND " + tablePrefix("s", PARENT_SPAN_ID) + " = " + tablePrefix("psc", SPAN_ID);

    private static final String UPSERT_SPAN_CONTEXT_QUERY = "MERGE INTO " + SPAN_CONTEXT_TABLE_NAME +" (" +
            TRACE_ID + ", " + SPAN_ID + ", " + TRACE_FLAGS + ", " + TRACE_STATE + ") "+
            "KEY ("+assembleFields(TRACE_ID, SPAN_ID)+ ")" + "VALUES (?, ?, ?, ?)" ;

    private static final String UPSERT_SPAN_QUERY = "MERGE INTO " + SPAN_TABLE_NAME + " (" +
            assembleFields(TRACE_ID, SPAN_ID, SPAN_KIND, PARENT_SPAN_ID, NAME, START_TIME, END_TIME, ATTRIBUTES, EVENTS, STATUS_CODE, STATUS_MESSAGE, HAS_ENDED)
            + ") KEY (" +assembleFields(TRACE_ID, SPAN_ID) +") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public void insertSpans(TelemetryConnection telemConnection, List<SpanData> spans) throws TelemetryDataAccessException {
        H2TelemetryConnection h2Connection = assertConnectionType(telemConnection);

        try(Connection conn = h2Connection.getConnection()) {
            List<SpanContext> spanContexts = new ArrayList<>(spans.stream().map(SpanData::getSpanContext).distinct().collect(Collectors.toList()));
            List<SpanContext> parentSpanContexts = spans.stream().map(SpanData::getParentSpanContext).distinct().collect(Collectors.toList());
            spanContexts.addAll(parentSpanContexts);
            spanContexts = spanContexts.stream().distinct().filter(spanContext -> !NULL_SPAN_ID.equals(spanContext.getSpanId())).collect(Collectors.toList());

            PreparedStatement spanContextStatement = conn.prepareStatement(UPSERT_SPAN_CONTEXT_QUERY);
            TraceStateJsonAdapter traceStateAdapter = new TraceStateJsonAdapter();
            for(SpanContext context : spanContexts) {
                spanContextStatement.clearParameters();
                spanContextStatement.setString(1, context.getTraceId());
                spanContextStatement.setString(2, context.getSpanId());
                spanContextStatement.setString(3, context.getTraceFlags().asHex());
                spanContextStatement.setString(4, traceStateAdapter.toJson(context.getTraceState()));
                spanContextStatement.addBatch();
            }
            spanContextStatement.executeBatch();

            PreparedStatement preparedStatement = conn.prepareStatement(UPSERT_SPAN_QUERY);

            AttributesJsonAdapter attributesAdapter = new AttributesJsonAdapter();

            for(SpanData span : spans) {
                preparedStatement.clearParameters();
                preparedStatement.setString(1, span.getTraceId());
                preparedStatement.setString(2, span.getSpanId());
                preparedStatement.setString(3, span.getKind().name());
                preparedStatement.setString(4, span.getParentSpanId());
                preparedStatement.setString(5, span.getName());
                preparedStatement.setLong(6, span.getStartEpochNanos());
                preparedStatement.setLong(7, span.getEndEpochNanos());

                preparedStatement.setString(8, attributesAdapter.toJson(span.getAttributes()));

                // TODO: Should probably serialize this better somehow?
                preparedStatement.setString(9, span.getEvents().toString());
                // TODO: Serialize Links too?

                preparedStatement.setString(10, span.getStatus().getStatusCode().toString());
                preparedStatement.setString(11, span.getStatus().getDescription());
                preparedStatement.setBoolean(12, span.hasEnded());
                preparedStatement.addBatch();
            }
            preparedStatement.clearParameters();
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }

    private Attributes attributesFromJSON(String string) throws TelemetryDataAccessException {
        AttributesJsonAdapter adapter = new AttributesJsonAdapter();
        try {
            return adapter.fromJson(string);
        } catch (IOException e) {
            throw new TelemetryDataAccessException("IO Exception", e);
        }
    }

    private TraceState traceStateFromJSON(String string) throws TelemetryDataAccessException {
        TraceStateJsonAdapter adapter = new TraceStateJsonAdapter();
        try {
            return adapter.fromJson(string);
        } catch (IOException e) {
            throw new TelemetryDataAccessException("IO Exception", e);
        }
    }

    @Override
    public List<SpanData> retrieveSpans(TelemetryConnection telemConnection, SpanSelectionOptions options) throws TelemetryDataAccessException {
        H2TelemetryConnection h2Connection = assertConnectionType(telemConnection);

        try(Connection conn = h2Connection.getConnection()) {
            String query = SELECT_SPAN_QUERY;
            boolean hasWhere = false;
            switch(options.getType()) {
                case BY_TRACE_ID:
                    query += " WHERE " + tablePrefix("s", TRACE_ID) + " = ?";
                    hasWhere = true;
                    break;
                case BY_PARENT_SPAN_ID:
                    query += " WHERE " + tablePrefix("s", PARENT_SPAN_ID) + " = ?";
                    hasWhere = true;
                    break;
                case NO_FILTERING:
                    break;
                default:
                    throw new TelemetryDataAccessException("Unknown SpanSelectionType: " + options.getType());
            }
            PreparedStatement statement = conn.prepareStatement(query);
            if(hasWhere) {
                statement.setString(1, options.getId());
            }
            ResultSet rs = statement.executeQuery();
            List<SpanData> spans = new java.util.ArrayList<>();
            while(rs.next()) {

                TraceState traceState = traceStateFromJSON(rs.getString(TRACE_STATE));
                TraceFlags flags = TraceFlags.fromHex(rs.getString(TRACE_FLAGS), 0);
                SpanContext context = SpanContext.create(rs.getString(TRACE_ID), rs.getString(SPAN_ID), flags, traceState);
                SpanContext parentContext;
                String parentSpanId = rs.getString(PARENT_SPAN_ID);
                if(NULL_SPAN_ID.equals(parentSpanId)) {
                    // Null parent span, just assume defaults
                    parentContext = SpanContext.create(rs.getString(TRACE_ID), rs.getString(PARENT_SPAN_ID), TraceFlags.getDefault(), TraceState.getDefault());
                } else {
                    TraceState parentTraceState = traceStateFromJSON(rs.getString(PARENT_TRACE_STATE));
                    TraceFlags parentFlags = TraceFlags.fromHex(rs.getString(PARENT_TRACE_FLAGS), 0);
                    parentContext = SpanContext.create(rs.getString(TRACE_ID), rs.getString(PARENT_SPAN_ID), parentFlags, parentTraceState);
                }
                StatusData status = StatusData.create(StatusCode.valueOf(rs.getString(STATUS_CODE)), rs.getString(STATUS_MESSAGE));

                JDBCSpan span = new JDBCSpan(
                        rs.getString(NAME),
                        SpanKind.valueOf(rs.getString(SPAN_KIND)),
                        context,
                        parentContext,
                        status,
                        rs.getLong(START_TIME),
                        rs.getLong(END_TIME),
                        attributesFromJSON(rs.getString(ATTRIBUTES)),
                        // TODO: Ignore events for now
                        List.of(),
                        // TODO: Ignore links for now
                        List.of(),
                        rs.getBoolean(HAS_ENDED),
                        null
                );
                spans.add(span);
            }
            return spans;
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }
}
