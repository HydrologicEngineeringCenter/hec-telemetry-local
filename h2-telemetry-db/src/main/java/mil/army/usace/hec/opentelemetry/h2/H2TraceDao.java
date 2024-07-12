package mil.army.usace.hec.opentelemetry.h2;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.TraceDao;
import mil.army.usace.hec.opentelemetry.h2.DAOObjects.JDBCTrace;
import mil.army.usace.hec.opentelemetry.objects.SpanID;
import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.objects.TraceID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class H2TraceDao extends AbstractH2Dao implements TraceDao<H2TelemetryConnection> {

    private static String QUERY = "SELECT " +
            assembleFields(H2SpanDao.TRACE_ID, H2SpanDao.NAME, H2SpanDao.SPAN_ID, H2SpanDao.START_TIME) + "," +
            "END_TIME - START_TIME AS DURATION_NS " +
            "FROM " + H2SpanDao.SPAN_TABLE_NAME +
            " WHERE ("+assembleFields(H2SpanDao.TRACE_ID, H2SpanDao.START_TIME)+") IN (" +
            " SELECT " + H2SpanDao.TRACE_ID + ", MIN("+ H2SpanDao.START_TIME+") " +
            " FROM "+H2SpanDao.SPAN_TABLE_NAME +
            " GROUP BY " + H2SpanDao.TRACE_ID + ")";

    @Override
    public Class<H2TelemetryConnection> getConnectionType() {
        return H2TelemetryConnection.class;
    }

    @Override
    public List<Trace> getAvailableTraces(TelemetryConnection telemConnection) throws TelemetryDataAccessException {
        H2TelemetryConnection h2Conn = assertConnectionType(telemConnection);
        try(Connection conn = h2Conn.getConnection()) {
            PreparedStatement statement = conn.prepareStatement(QUERY);
            ResultSet rs = statement.executeQuery();
            List<Trace> traces = new ArrayList<>();
            while(rs.next()){
                traces.add(new JDBCTrace(
                        rs.getString(H2SpanDao.NAME),
                        rs.getLong("DURATION_NS"),
                        rs.getLong(H2SpanDao.START_TIME),
                        TraceID.of(rs.getString(H2SpanDao.TRACE_ID)),
                        SpanID.of(rs.getString(H2SpanDao.SPAN_ID))));

            }
            return traces;
        } catch (SQLException e) {
            throw new TelemetryDataAccessException("SQL Exception", e);
        }
    }
}
