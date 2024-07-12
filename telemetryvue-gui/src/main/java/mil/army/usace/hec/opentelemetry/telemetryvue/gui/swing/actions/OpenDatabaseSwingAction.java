package mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions;

import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.actions.OpenDatabaseAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Map;

public class OpenDatabaseSwingAction extends AbstractAction {

    private final OpenDatabaseAction<?> _openDatabaseAction;
    private final TelemetryVueModel _model;
    private final Map<String, Object> _extraParameters;

    public OpenDatabaseSwingAction(OpenDatabaseAction<?> openDatabaseAction, TelemetryVueModel model, Map<String, Object> extraParameters) {
        super(openDatabaseAction.databaseType());
        _openDatabaseAction = openDatabaseAction;
        _model = model;
        _extraParameters = extraParameters;
    }

    public OpenDatabaseAction<?> getOpenDatabaseAction() {
        return _openDatabaseAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        _model.executeOnExecutorService(() -> _model.tryOpenDatabase(getOpenDatabaseAction(), _extraParameters));
    }
}
