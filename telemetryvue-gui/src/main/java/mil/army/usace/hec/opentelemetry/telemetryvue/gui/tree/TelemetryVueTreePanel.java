package mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree;

import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TelemetryVueTreePanel extends JPanel {

    private TelemetryVueModel _model;
    private JTree _telemetryTree;
    private TelemetryVueTreeModel _telemetryVueTreeModel;

    public TelemetryVueTreePanel(TelemetryVueModel model) {
        super(new GridBagLayout());
        _model = model;
        setup();
    }

    private void setup() {
        JLabel minimumDuration = new JLabel("Minimum Duration: ");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,0,5);
        add(minimumDuration, gbc);

        JComboBox<String> minimumDurationCombo = new JComboBox<>(new String[]{"1 Second"});
        minimumDurationCombo.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,0,5);
        add(minimumDurationCombo, gbc);

        _telemetryVueTreeModel = new TelemetryVueTreeModel(getModel());
        _telemetryTree = new JTree(_telemetryVueTreeModel);
        gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5,5,5,5);
        add(new JScrollPane(_telemetryTree), gbc);
        _telemetryTree.addTreeSelectionListener(new TelemetryVueTreeSelectionListener());
        _telemetryVueTreeModel.addTreeModelListener(new TelemetryVueTreeListener());
        _telemetryTree.addMouseListener(new TelemetryVueTreeAdapter());
    }

    protected TelemetryVueModel getModel() {
        return _model;
    }

    private class TelemetryVueTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getNewLeadSelectionPath();
            if(path != null) {
                Object node = path.getLastPathComponent();
                getModel().setSelectedConnection(_telemetryVueTreeModel.getConnectionFor(node));
            }
        }
    }

    private class TelemetryVueTreeListener implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {

        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            _telemetryTree.expandPath(e.getTreePath());
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {

        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {

        }
    }

    private class TelemetryVueTreeAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount() == 2) {
                TreePath path = _telemetryTree.getPathForLocation(e.getX(), e.getY());
                if(path != null) {
                    Object node = path.getLastPathComponent();
                    if(node instanceof TraceNode) {
                        TraceNode traceNode = (TraceNode) node;
                        _model.showTrace(traceNode.getConnection(), traceNode.getTrace());
                    }
                }
            }
        }
    }
}
