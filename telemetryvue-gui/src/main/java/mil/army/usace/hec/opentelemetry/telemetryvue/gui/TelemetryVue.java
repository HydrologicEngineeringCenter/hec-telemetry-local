package mil.army.usace.hec.opentelemetry.telemetryvue.gui;


import com.google.common.flogger.FluentLogger;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.CloseDatabaseAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.FlushDatabaseSwingAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.OpenDatabaseSwingAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.traceviewer.MultiTelemetryViewPanel;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree.TelemetryVueTreePanel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.OperatingMode;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TelemetryVue {
    private static final FluentLogger FLOGGER = FluentLogger.forEnclosingClass();

    private TelemetryVueModel _model;
    private JFrame _telemetryVueFrame;
    private JMenuBar _menuBar;
    private JMenu _fileMenu;
    private JMenu _openMenuItem;
    private Action _closeMenuItem;
    private JMenuItem _exitMenuItem;
    private JMenu _toolsMenu;
    private JSplitPane _splitPane;
    private TelemetryVueTreePanel _treePanel;
    private MultiTelemetryViewPanel _multiTelemetryViewPanel;

    public TelemetryVue(OperatingMode operatingMode) {
        this(new TelemetryVueModel(operatingMode));
    }

    public TelemetryVue(TelemetryVueModel model) {
        _model = model;
        setupFrame();
    }

    private void setupFrame() {
        _telemetryVueFrame = new JFrame("TelemetryVue");
        _telemetryVueFrame.setSize(800, 600);
        _telemetryVueFrame.setVisible(true);
        _telemetryVueFrame.addWindowListener(new TelemetryVueWindowListener());

        setupMenuBar(_telemetryVueFrame);

        _splitPane = new JSplitPane();
        _telemetryVueFrame.add(_splitPane);

        _treePanel = new TelemetryVueTreePanel(getModel());
        _splitPane.setLeftComponent(_treePanel);
        _multiTelemetryViewPanel = new MultiTelemetryViewPanel();
        getModel().addTraceListener(_multiTelemetryViewPanel);
        _splitPane.setRightComponent(_multiTelemetryViewPanel);

        _telemetryVueFrame.validate();
        _splitPane.setDividerLocation(0.35);
    }

    private void setupMenuBar(JFrame frame) {
        _menuBar = new JMenuBar();
        _fileMenu = new JMenu("File");
        _openMenuItem = new JMenu("Open Telemetry Database");

        Map<String, Object> extraOpenParams = new HashMap<>();
        extraOpenParams.put("parentComponent", _telemetryVueFrame);
        Map<String, Object> extraOpenParamsImmutable = Collections.unmodifiableMap(extraOpenParams);
        getModel().getOpenDatabaseActions().forEach(openAction -> {
            Action swingAction = new OpenDatabaseSwingAction(openAction, getModel(), extraOpenParamsImmutable);
            _openMenuItem.add(swingAction);
        });

        _closeMenuItem = new CloseDatabaseAction(getModel());
        _exitMenuItem = new JMenuItem("Exit TelemetryVue");
        _fileMenu.add(_openMenuItem);
        _fileMenu.add(_closeMenuItem);
        _fileMenu.addSeparator();
        _fileMenu.add(_exitMenuItem);
        _menuBar.add(_fileMenu);

        _toolsMenu = new JMenu("Tools");
        _toolsMenu.add(new FlushDatabaseSwingAction(getModel()));
        _menuBar.add(_toolsMenu);

        frame.setJMenuBar(_menuBar);
    }

    private TelemetryVueModel getModel() {
        return _model;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // set system laf
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                FLOGGER.atSevere().withCause(e).log("Error setting system look and feel");
            }
            new TelemetryVue(OperatingMode.STANDALONE);
        });
    }

    private class TelemetryVueWindowListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            getModel().closeAllConnections();
            _telemetryVueFrame.dispose();
        }

        @Override
        public void windowClosed(WindowEvent e) {
            if(getModel().getOperatingMode() == OperatingMode.STANDALONE) {
                System.exit(0);
            }
        }
    }
}