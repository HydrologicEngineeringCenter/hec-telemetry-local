package mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions;

import mil.army.usace.hec.opentelemetry.telemetryvue.gui.TelemetryVue;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.OperatingMode;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExitAction extends AbstractAction {

    private TelemetryVue _telemetryVue;

    public ExitAction(TelemetryVue tv) {
        super("Exit TelemetryVue");
        _telemetryVue = tv;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(_telemetryVue.getModel().getOperatingMode() == OperatingMode.EMBEDDED) {
            _telemetryVue.hide();
        } else {
            _telemetryVue.shutdown();
        }
    }
}
