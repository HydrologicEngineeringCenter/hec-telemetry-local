package mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.ConnectionAdapter;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CloseDatabaseAction extends AbstractAction {

    private TelemetryVueModel _model;

    public CloseDatabaseAction(TelemetryVueModel model) {
        super("Close Selected Database");
        setEnabled(false);
        _model = model;
        _model.addConnectionListener(new CloseConnectionAdapter());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        _model.closeSelectedConnection();
    }

    private class CloseConnectionAdapter extends ConnectionAdapter {
        @Override
        public void connectionSelected(TelemetryConnection connection) {
            setEnabled(connection != null);
        }
    }
}
