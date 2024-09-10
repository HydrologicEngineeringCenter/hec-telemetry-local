package mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions;

import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.ConnectionListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ReloadSelectedDatabaseSwingAction extends AbstractAction implements ConnectionListener {

    private final TelemetryVueModel _model;

    public ReloadSelectedDatabaseSwingAction(TelemetryVueModel model) {
        super("Reload Selected Database");
        _model = model;
        _model.addConnectionListener(this);
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        _model.reloadConnection(_model.getSelectedConnection());
    }

    @Override
    public void connectionAdded(TelemetryConnection connection) {
        // noop
    }

    @Override
    public void connectionRemoved(TelemetryConnection connection) {
        // noop
    }

    @Override
    public void connectionSelected(TelemetryConnection connection) {
        setEnabled(connection != null);
    }
}
