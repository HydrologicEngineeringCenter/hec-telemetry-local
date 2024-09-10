package mil.army.usace.hec.opentelemetry.telemetryvue.gui.tree;

import com.google.common.flogger.FluentLogger;
import mil.army.usace.hec.opentelemetry.TelemetryConnection;
import mil.army.usace.hec.opentelemetry.TelemetryDataAccessException;
import mil.army.usace.hec.opentelemetry.TraceDao;
import mil.army.usace.hec.opentelemetry.objects.Trace;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.TelemetryVueModel;
import mil.army.usace.hec.opentelemetry.telemetryvue.model.listeners.ConnectionAdapter;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TelemetryVueTreeModel extends ConnectionAdapter implements TreeModel {

    private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private final Duration miniumTraceDuration = Duration.ofNanos(50000000);
    private final List<TreeModelListener> _listeners = new ArrayList<>();
    private final TelemetryVueModel _model;
    private List<ConnectionNode> _connectionNodes = new ArrayList<>();
    private Map<ConnectionNode, LoadingNode> _loadingConnectionNodes = new HashMap<>();
    private Map<ConnectionNode, List<TraceNode>> _traceNodes = new HashMap<>();

    public TelemetryVueTreeModel(TelemetryVueModel model) {
        _model = model;
        _model.addConnectionListener(this);
    }

    @Override
    public Object getRoot() {
        return "TelemetryVue";
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent.equals(getRoot())) {
            return _connectionNodes.get(index);
        }
        if (parent instanceof ConnectionNode) {
            ConnectionNode connectionNode = (ConnectionNode) parent;
            if (_loadingConnectionNodes.containsKey(connectionNode)) {
                return _loadingConnectionNodes.get(connectionNode);
            }
            if (_traceNodes.containsKey(connectionNode)) {
                return _traceNodes.get(connectionNode).get(index);
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent.equals(getRoot())) {
            return _connectionNodes.size();
        }
        if (parent instanceof ConnectionNode) {
            ConnectionNode connectionNode = (ConnectionNode) parent;
            if (_loadingConnectionNodes.containsKey(connectionNode)) {
                return 1;
            }
            if (_traceNodes.containsKey(connectionNode)) {
                return _traceNodes.get(connectionNode).size();
            }
        }
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof TraceNode || node instanceof LoadingNode;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Tree is not user editable
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent.equals(getRoot())) {
            return _connectionNodes.indexOf(child);
        }
        if (parent instanceof ConnectionNode) {
            ConnectionNode connectionNode = (ConnectionNode) parent;
            if (_loadingConnectionNodes.containsKey(connectionNode) && _loadingConnectionNodes.get(connectionNode).equals(child)) {
                return 0;
            }
            if (_traceNodes.containsKey(connectionNode)) {
                return _traceNodes.get(connectionNode).indexOf(child);
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        _listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        _listeners.remove(l);
    }

    public TelemetryConnection getConnectionFor(Object node) {
        if (node instanceof ConnectionNode) {
            ConnectionNode connectionNode = (ConnectionNode) node;
            return connectionNode.getConnection();
        }
        if(node instanceof TraceNode) {
            TraceNode traceNode = (TraceNode) node;
            return traceNode.getConnection();
        }
        return null;
    }

    @Override
    public void connectionAdded(TelemetryConnection connection) {
        if(!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> connectionAdded(connection));
            return;
        }
        ConnectionNode connectionNode = new ConnectionNode(connection);
        _connectionNodes.add(connectionNode);
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{getRoot()}, new int[]{_connectionNodes.size() - 1}, new Object[]{connectionNode});
        for (TreeModelListener listener : _listeners) {
            listener.treeNodesInserted(event);
        }
        addTraceNodesForConnectionNode(connectionNode);
    }

    private void addTraceNodesForConnectionNode(ConnectionNode connectionNode) {
        LoadingNode loadingNode = new LoadingNode();
        _loadingConnectionNodes.put(connectionNode, loadingNode);
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{getRoot(), connectionNode}, new int[]{0}, new Object[]{loadingNode});
        for (TreeModelListener listener : _listeners) {
            listener.treeNodesInserted(event);
        }

        _model.executeOnExecutorService(() -> {
            // off edt
            try {
                LOGGER.atConfig().log("Getting traces for connection %s", connectionNode);
                return getTracesForConnection(connectionNode.getConnection(), miniumTraceDuration);
            } catch (TelemetryDataAccessException e) {
                LOGGER.atSevere().withCause(e).log("Error getting traces for connection");
            }
            return null;
        }, traces -> {
            SwingUtilities.invokeLater(() -> {
                // on edt
                _loadingConnectionNodes.remove(connectionNode);
                TreeModelEvent loadingEvent = new TreeModelEvent(this, new Object[]{getRoot(), connectionNode}, new int[]{0}, new Object[]{loadingNode});
                for (TreeModelListener listener : _listeners) {
                    listener.treeNodesRemoved(loadingEvent);
                }
                if (traces == null) {
                    return;
                }
                List<TraceNode> traceNodes = traces.stream().map(trace -> new TraceNode(connectionNode.getConnection(), trace)).collect(Collectors.toList());
                _traceNodes.put(connectionNode, traceNodes);
                int[] indexes = new int[traceNodes.size()];
                Object[] nodes = new Object[traceNodes.size()];
                for (int i = 0; i < traceNodes.size(); i++) {
                    indexes[i] = i;
                    nodes[i] = traceNodes.get(i);
                }
                TreeModelEvent traceEvent = new TreeModelEvent(this, new Object[]{getRoot(), connectionNode}, indexes, nodes);
                for (TreeModelListener listener : _listeners) {
                    listener.treeNodesInserted(traceEvent);
                }
            });
        });
    }

    private void removeLoadingNodeFromConnectionNode(ConnectionNode connectionNode) {
        LoadingNode loadingNode = _loadingConnectionNodes.get(connectionNode);
        if (loadingNode != null) {
            _loadingConnectionNodes.remove(connectionNode);
            TreeModelEvent event = new TreeModelEvent(this, new Object[]{getRoot(), connectionNode}, new int[]{0}, new Object[]{loadingNode});
            for (TreeModelListener listener : _listeners) {
                listener.treeNodesRemoved(event);
            }
        }
    }

    private void removeTraceNodesFromConnectionNode(ConnectionNode connectionNode) {
        List<TraceNode> traceNodes = _traceNodes.get(connectionNode);
        if (traceNodes != null) {
            _traceNodes.remove(connectionNode);
            int[] indexes = new int[traceNodes.size()];
            Object[] nodes = new Object[traceNodes.size()];
            for (int i = 0; i < traceNodes.size(); i++) {
                indexes[i] = i;
                nodes[i] = traceNodes.get(i);
            }
            TreeModelEvent event = new TreeModelEvent(this, new Object[]{getRoot(), connectionNode}, indexes, nodes);
            for (TreeModelListener listener : _listeners) {
                listener.treeNodesRemoved(event);
            }
        }
    }

    private void removeConnectionNode(ConnectionNode connectionNode) {
        removeLoadingNodeFromConnectionNode(connectionNode);
        removeTraceNodesFromConnectionNode(connectionNode);
        TreeModelEvent event = new TreeModelEvent(this, new Object[]{getRoot()}, new int[]{_connectionNodes.indexOf(connectionNode)}, new Object[]{connectionNode});
        _connectionNodes.remove(connectionNode);
        for (TreeModelListener listener : _listeners) {
            listener.treeNodesRemoved(event);
        }
    }

    private List<Trace> getTracesForConnection(TelemetryConnection connection, Duration minimumTraceDuration) throws TelemetryDataAccessException {
        List<Trace> traces = new ArrayList<>();
        TraceDao<?> traceDao = connection.getTelemetryDaoFactory().createTraceDao();
        for (Trace trace : traceDao.getAvailableTraces(connection)) {
            if (trace.getTraceDuration().compareTo(minimumTraceDuration) >= 0) {
                traces.add(trace);
            } else {
                LOGGER.atFiner().log("Skipping trace %s because duration %s too short", trace.getTraceName(), trace.getTraceDuration().getSeconds());
            }
        }
        return traces;

    }


    @Override
    public void connectionRemoved(TelemetryConnection connection) {
        if(!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> connectionAdded(connection));
            return;
        }
        ConnectionNode removedNode = null;
        for (ConnectionNode node : _connectionNodes) {
            if (node.getConnection().equals(connection)) {
                removedNode = node;
            }
        }
        if (removedNode == null) {
            return;
        }
        removeConnectionNode(removedNode);
    }
}
