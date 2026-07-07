package mil.army.usace.hec.opentelemetry.telemetryvue.gui;

import com.google.common.flogger.FluentLogger;

import mil.army.usace.hec.opentelemetry.DaoFactory;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.TelemetryVueReciever;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.CloseDatabaseAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.ExitAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.FlushDatabaseSwingAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.OpenDatabaseSwingAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.swing.actions.ReloadSelectedDatabaseSwingAction;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.traceviewer.MultiTelemetryViewPanel;
import mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree.TelemetryVueTreePanel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.OperatingMode;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;
import io.grpc.ServerBuilder;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
        if(_model.getOperatingMode() == OperatingMode.STANDALONE) {
            // In Embedded mode, let the parent app choose when to set visible.
            _telemetryVueFrame.setVisible(true);
        }
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
        _fileMenu.add(_openMenuItem);
        _fileMenu.add(_closeMenuItem);
        _fileMenu.addSeparator();
        _fileMenu.add(new ExitAction(this));
        _menuBar.add(_fileMenu);

        _toolsMenu = new JMenu("Tools");
        _toolsMenu.add(new FlushDatabaseSwingAction(getModel()));
        _toolsMenu.add(new ReloadSelectedDatabaseSwingAction(getModel()));
        _menuBar.add(_toolsMenu);

        frame.setJMenuBar(_menuBar);
    }

    /**
     * Use the model to perform actions in the TelemetryVue UI. The UI will respond to changes in the model state
     * @return The model for this TelemetryVue frame
     */
    public TelemetryVueModel getModel() {
        return _model;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SwingUtilities.invokeLater(() -> {
            // set system laf
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                FLOGGER.atSevere().withCause(e).log("Error setting system look and feel");
            }
            new TelemetryVue(OperatingMode.STANDALONE);
        });
        DaoFactory<?> h2DaoFactory = DaoFactory.getDaoFactory("h2sql");
        Map<String, Object> connectionParameters = new HashMap<>();
        connectionParameters.put("file", Path.of(args.length == 1 ? args[0] : new File("db").getAbsolutePath()));

        TelemetryConnection connection = h2DaoFactory.getConnection(connectionParameters);
        try {
            h2DaoFactory.initIfNeeded(connection);
        } catch (TelemetryDataAccessException e) {
            throw new RuntimeException(e);
        }
        
        var receiver = new TelemetryVueReciever(connection);
        var server = ServerBuilder.forPort(4317).addService(receiver).build();
        server.start();
        server.awaitTermination();
    }

    /**
     * For embedded TelemetryVue, show the window
     */
    public void show() {
        // Make sure the frame is ready if it was disposed
        _telemetryVueFrame.pack();
        // Show it
        _telemetryVueFrame.setVisible(true);
        // Try to bring it to the front
        _telemetryVueFrame.requestFocus();
    }

    /**
     * For embedded mode, show the window behaving as a child of the parent
     * @param parent The parent window
     */
    public void show(JFrame parent) {
        _telemetryVueFrame.setLocationRelativeTo(parent);
        _telemetryVueFrame.setIconImage(parent.getIconImage());
        show();
    }

    /**
     * For embedded TelemetryVue, hide the window
     */
    public void hide() {
        // Don't waste resources when the window is hidden. All important state
        // lives in the model.
        _telemetryVueFrame.dispose();
    }

    public void shutdown() {
        getModel().closeAllConnections();
        _telemetryVueFrame.dispose();
    }

    private class TelemetryVueWindowListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            if(getModel().getOperatingMode() == OperatingMode.EMBEDDED) {
                hide();
            } else {
                shutdown();
            }
        }

        @Override
        public void windowClosed(WindowEvent e) {
            if(getModel().getOperatingMode() == OperatingMode.STANDALONE) {
                System.exit(0);
            }
        }
    }
}