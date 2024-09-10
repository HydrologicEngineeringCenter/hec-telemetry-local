package mil.army.usace.hec.opentelemetry.telemetryvue.gui.traceviewer;

import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.util.TraceNameUtil;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.NestedTraceData;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ViewTracePanel extends JPanel {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NestedTraceData _traceData;

    private JTable _traceTable;

    public ViewTracePanel(NestedTraceData traceData) {
        super(new GridBagLayout());
        _traceData = traceData;
        setup();
    }

    private void setup() {
        JLabel traceIdLabel = new JLabel(getTraceName());
        Font defFont = UIManager.getDefaults().getFont("Label.font");
        traceIdLabel.setFont(new Font(defFont.getName(), Font.BOLD, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,0,5);
        add(traceIdLabel, gbc);

        _traceTable = new JTable(new TraceViewerTableModel(_traceData));
        JScrollPane traceTableScrollPane = new JScrollPane(_traceTable);
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5,5,5,5);
        add(traceTableScrollPane, gbc);
    }

    private String getTraceName() {
        Trace trace = _traceData.getTrace();
        return TraceNameUtil.formatTraceName(trace);
    }

    public NestedTraceData getTraceData() {
        return _traceData;
    }
}
