package mil.army.usace.hec.opentelemetry.telemetryvue.gui.traceviewer;

import mil.army.usace.hec.opentelemetry.telemetryvue.model.NestedTraceData;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.TelemtryListener;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MultiTelemetryViewPanel extends JPanel implements TelemtryListener {

    private static final String NO_SELECTED_TRACE = "noSelectedTrace";
    private static final String LOADING_SELECTED_TRACE = "loadingSelectedTrace";
    private static final String TRACE_PANE = "tracePane";

    private final CardLayout _cardLayout = new CardLayout();
    private JPanel _noSelectedTracePanel;
    private JPanel _loadingTracePanel;
    private ViewTracePanel _viewTracePanel;

    public MultiTelemetryViewPanel() {
        super();
        setLayout(_cardLayout);
        setup();
    }

    private void setup() {
        _noSelectedTracePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel noTraceSelectedLabel = new JLabel("<html><h1><font color=gray>No trace selected</font></h1></html>");
        noTraceSelectedLabel.setHorizontalAlignment(JLabel.CENTER);
        _noSelectedTracePanel.add(noTraceSelectedLabel, gbc);

        _loadingTracePanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel loadingTraceSelectedLabel = new JLabel("<html><h1><font color=gray>Loading selected Trace</font></h1></html>");
        loadingTraceSelectedLabel.setHorizontalAlignment(JLabel.CENTER);
        _loadingTracePanel.add(loadingTraceSelectedLabel, gbc);

        add(_noSelectedTracePanel, NO_SELECTED_TRACE);
        add(_loadingTracePanel, LOADING_SELECTED_TRACE);
        _cardLayout.show(this, NO_SELECTED_TRACE);
    }

    @Override
    public void showTrace(NestedTraceData trace) {
        ViewTracePanel viewTracePanel = new ViewTracePanel(trace);
        add(viewTracePanel, TRACE_PANE);
        _cardLayout.show(this, TRACE_PANE);
        _viewTracePanel = viewTracePanel;
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void unshowTrace(NestedTraceData trace) {
        if(_viewTracePanel.getTraceData().getTrace().getTraceID().equals(trace.getTrace().getTraceID())) {
            _cardLayout.show(this, NO_SELECTED_TRACE);
            remove(_viewTracePanel);
            _viewTracePanel = null;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    public void loadingTrace() {
        _cardLayout.show(this, LOADING_SELECTED_TRACE);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }


}
