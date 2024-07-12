package mil.army.usace.hec.opentelemetry.telemetryvue.gui.traceviewer;

import io.opentelemetry.sdk.trace.data.SpanData;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.NestedTraceData;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraceViewerTableModel implements TableModel {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NestedTraceData _data;
    private final List<SpanData[]> _rows = new ArrayList<>();
    private final List<TableModelListener> _listeners = new ArrayList<>();

    public TraceViewerTableModel(NestedTraceData data) {
        _data = data;
        assembleModelFromData();
    }

    private void assembleModelFromData() {
        List<SpanData> rootSpanDataList = new ArrayList<>(_data.getRootSpanDataList());
        for (SpanData rootSpanData : rootSpanDataList) {
            _rows.add(new SpanData[]{rootSpanData});
            List<SpanData> path = new ArrayList<>();
            path.add(rootSpanData);
            addEachChildStepAsRow(path, rootSpanData);
        }
    }

    private void addEachChildStepAsRow(List<SpanData> path, SpanData root) {
        List<SpanData> children = _data.getNestedSpan(root);
        if (children == null) {
            return;
        }
        for (SpanData child : children) {
            List<SpanData> newPath = new ArrayList<>(path);
            newPath.add(child);

            List<SpanData> reversedNewPath = new ArrayList<>(newPath);
            Collections.reverse(reversedNewPath);
            _rows.add(reversedNewPath.toArray(new SpanData[0]));

            addEachChildStepAsRow(newPath, child);
        }
    }


    @Override
    public int getRowCount() {
        return _rows.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Span Name";
            case 1:
                return "Duration (ns)";
            case 2:
                return "Offset from Start (ns)";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Long.class;
            case 2:
                return Long.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SpanData[] row = _rows.get(rowIndex);
        SpanData spanData = row[0];
        switch (columnIndex) {
            case 0:
                StringBuilder sb = new StringBuilder();
                for(int i = 1; i < row.length; i++) {
                    sb.append("    ");
                }
                sb.append(spanData.getName());
                return sb.toString();
            case 1:
                return spanData.getEndEpochNanos() - spanData.getStartEpochNanos();
            case 2:
                SpanData root = row[row.length - 1];
                return spanData.getStartEpochNanos() - root.getStartEpochNanos();
            default:
                return "";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // no-op, table is read-only
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        _listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        _listeners.remove(l);
    }
}
