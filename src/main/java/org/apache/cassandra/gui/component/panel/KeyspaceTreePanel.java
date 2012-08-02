package org.apache.cassandra.gui.component.panel;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.ColumnFamilyDialog;
import org.apache.cassandra.gui.component.dialog.KeyDialog;
import org.apache.cassandra.gui.component.dialog.KeyRangeDialog;
import org.apache.cassandra.gui.component.dialog.KeyspaceDialog;
import org.apache.cassandra.gui.control.callback.PropertiesCallback;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.gui.control.callback.SelectedColumnFamilyCallback;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KsDef;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.unit.ColumnFamily;
import org.apache.thrift.TException;

/**
 * The keyspace panel
 */
public class KeyspaceTreePanel extends JPanel implements TreeSelectionListener {
    private static final long serialVersionUID = 5481365703729222288L;

    private class PopupAction extends AbstractAction {
        private static final long serialVersionUID = 4235052996425858520L;

        public static final int OPERATION_ROWS = 1;
        public static final int OPERATION_KEYRANGE = 2;
        public static final int OPERATION_KEY = 3;
        public static final int OPERATION_CREATE_KEYSPACE = 4;
        public static final int OPERATION_REMOVE_KEYSPACE = 5;
        public static final int OPERATION_UPDATE_KEYSPACE = 6;
        public static final int OPERAITON_CREATE_COLUMN_FAMILY = 7;
        public static final int OPERATION_REMOVE_COLUMN_FAMILY = 8;
        public static final int OPERATION_TRUNCATE_COLUMN_FAMILY = 9;
        public static final int OPERATION_UPDATE_COLUMN_FAMILY = 10;
        public static final int OPERATION_REFRESH_CLUSTER = 11;

        public static final int ROWS_1000 = 1000;

        private int operation;
        private DefaultMutableTreeNode node;

        public PopupAction(String name, int operation, DefaultMutableTreeNode node) {
            this.operation = operation;
            this.node = node;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int status = 0;
            KeyspaceDialog ksd = null;
            ColumnFamilyDialog cfd = null;
            switch (operation) {
            case OPERATION_CREATE_KEYSPACE:
                ksd = new KeyspaceDialog();
                ksd.setVisible(true);
                if (ksd.isCancel()) {
                    return;
                }

                try {
                    client.addKeyspace(ksd.getKeyspaceName(),
                                       ksd.getStrategy(),
                                       ksd.getStrategyOptions(),
                                       ksd.getReplicationFactor());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }

                node.add(new DefaultMutableTreeNode(ksd.getKeyspaceName()));
                treeModel.reload(node);
                break;
            case OPERATION_UPDATE_KEYSPACE:
                KsDef ksDef;
                try {
                    ksDef = client.describeKeyspace(lastSelectedKeysapce);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }

                ksd = new KeyspaceDialog(lastSelectedKeysapce,
                                         ksDef.getReplication_factor(),
                                         ksDef.getStrategy_class(),
                                         ksDef.getStrategy_options());
                ksd.setVisible(true);
                if (ksd.isCancel()) {
                    return;
                }

                try {
                    client.updateKeyspace(ksd.getKeyspaceName(),
                                          ksd.getStrategy(),
                                          ksd.getStrategyOptions(),
                                          ksd.getReplicationFactor());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }

                propertiesCallback.keyspaceCallback(lastSelectedKeysapce);
                break;
            case OPERATION_REMOVE_KEYSPACE:
                if (lastSelectedKeysapce == null) {
                        return;
                }
                status = JOptionPane.showConfirmDialog(null,
                                                       "Delete a keyspace " + lastSelectedKeysapce + "?",
                                                       "confirm",
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
                if (status == JOptionPane.YES_OPTION) {
                    try {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        client.dropKeyspace(lastSelectedKeysapce);
                        deletedKeyspace = lastSelectedKeysapce;
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        node.removeFromParent();
                        treeModel.reload(parent);
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } catch (Exception ex) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                        ex.printStackTrace();
                        return;
                    }
                }

                break;
            case OPERATION_REFRESH_CLUSTER:
                refreshTree();
                break;
            case OPERAITON_CREATE_COLUMN_FAMILY:
                if (lastSelectedKeysapce == null) {
                    return;
                }

                cfd = new ColumnFamilyDialog();
                cfd.setVisible(true);
                if (cfd.isCancel()) {
                    return;
                }

                try {
                    client.addColumnFamily(lastSelectedKeysapce, cfd.getColumnFamily());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }

                node.add(new DefaultMutableTreeNode(cfd.getColumnFamily().getColumnFamilyName()));
                treeModel.reload(node);
                break;
            case OPERATION_UPDATE_COLUMN_FAMILY:
                if (lastSelectedKeysapce == null ||
                    lastSelectedColumnFamily == null) {
                    return;
                }

                ColumnFamily cf = null;
                try {
                    cf = client.getColumnFamilyBean(lastSelectedKeysapce, lastSelectedColumnFamily);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }

                cfd = new ColumnFamilyDialog(cf);
                cfd.setVisible(true);
                if (cfd.isCancel()) {
                    return;
                }

                try {
                    client.updateColumnFamily(lastSelectedKeysapce, cfd.getColumnFamily());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                    ex.printStackTrace();
                    return;
                }
                break;
            case OPERATION_REMOVE_COLUMN_FAMILY:
                if (lastSelectedKeysapce == null ||
                    lastSelectedColumnFamily == null) {
                    return;
                }

                status = JOptionPane.showConfirmDialog(null,
                                                       "Delete a column family " + lastSelectedColumnFamily + "?",
                                                       "confirm",
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
                if (status == JOptionPane.YES_OPTION) {
                    try {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        client.dropColumnFamily(lastSelectedKeysapce, lastSelectedColumnFamily);
                        deletedColumnFamily = lastSelectedColumnFamily;
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                        node.removeFromParent();
                        treeModel.reload(parent);
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } catch (Exception ex) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                        ex.printStackTrace();
                        return;
                    }
                }
                break;
            case OPERATION_TRUNCATE_COLUMN_FAMILY:
                if (lastSelectedKeysapce == null ||
                    lastSelectedColumnFamily == null) {
                    return;
                }

                status = JOptionPane.showConfirmDialog(null,
                                                       "truncarte column family " + lastSelectedColumnFamily + "?",
                                                       "confirm",
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
                if (status == JOptionPane.YES_OPTION) {
                    try {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        client.truncateColumnFamily(lastSelectedKeysapce, lastSelectedColumnFamily);
                        cCallback.rangeCallback(lastSelectedKeysapce,
                                                lastSelectedColumnFamily,
                                                "",
                                                "",
                                                ROWS_1000);
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } catch (Exception ex) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        JOptionPane.showMessageDialog(null, "error: " + ex.toString());
                        ex.printStackTrace();
                        return;
                    }
                }
                break;
            case OPERATION_ROWS:
            case OPERATION_KEYRANGE:
                if (lastSelectedKeysapce == null ||
                    lastSelectedColumnFamily == null) {
                    return;
                }

                String startKey = "";
                String endKey = "";

                if (operation == OPERATION_KEYRANGE) {
                    KeyRangeDialog krd = new KeyRangeDialog();
                    krd.setVisible(true);
                    if (krd.isCancel()) {
                        return;
                    }

                    startKey = krd.getStartKey();
                    endKey = krd.getEndKey();
                }

                cCallback.rangeCallback(lastSelectedKeysapce,
                                        lastSelectedColumnFamily,
                                        startKey,
                                        endKey,
                                        ROWS_1000);
                break;
            case OPERATION_KEY:
                if (lastSelectedKeysapce == null ||
                    lastSelectedColumnFamily == null) {
                    return;
                }

                KeyDialog kd = new KeyDialog();
                kd.setVisible(true);
                if (kd.isCancel()) {
                    return;
                }

                cCallback.getCacllback(lastSelectedKeysapce,
                                       lastSelectedColumnFamily,
                                       kd.getkey());
                break;
            }
        }
    }

    private class MousePopup extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                tree.setSelectionPath(path);
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                JPopupMenu popup = new JPopupMenu();
                switch (path.getPathCount()) {
                case TREE_CLUSTER:
                    popup.add(new PopupAction("create keysapce", PopupAction.OPERATION_CREATE_KEYSPACE, node));
                    popup.add(new PopupAction("refresh", PopupAction.OPERATION_REFRESH_CLUSTER, node));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    break;
                case TREE_KEYSPACE:
                    lastSelectedKeysapce = (String) node.getUserObject();
                    popup.add(new PopupAction("properties", PopupAction.OPERATION_UPDATE_KEYSPACE, node));
                    popup.add(new PopupAction("remove", PopupAction.OPERATION_REMOVE_KEYSPACE, node));
                    popup.add(new PopupAction("create column family", PopupAction.OPERAITON_CREATE_COLUMN_FAMILY, node));
                    popup.add(new PopupAction("refresh", PopupAction.OPERATION_REFRESH_CLUSTER, node));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    break;
                case TREE_COLUMN_FAMILY:
                    String columnFamily = (String) node.getUserObject();
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    lastSelectedKeysapce = (String) parent.getUserObject();
                    lastSelectedColumnFamily = columnFamily;

                    popup.add(new PopupAction("show 1000 rows", PopupAction.OPERATION_ROWS, node));
                    popup.add(new PopupAction("key range rows", PopupAction.OPERATION_KEYRANGE, node));
                    popup.add(new PopupAction("get key", PopupAction.OPERATION_KEY, node));
                    popup.add(new PopupAction("properties", PopupAction.OPERATION_UPDATE_COLUMN_FAMILY, node));
                    popup.add(new PopupAction("truncate column family", PopupAction.OPERATION_TRUNCATE_COLUMN_FAMILY, node));
                    popup.add(new PopupAction("remove column family", PopupAction.OPERATION_REMOVE_COLUMN_FAMILY, node));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    break;
                default:
                    lastSelectedKeysapce = null;
                    lastSelectedColumnFamily = null;
                    break;
                }
            }
        }
    }

    private static final int TREE_CLUSTER = 1;
    private static final int TREE_KEYSPACE = 2;
    private static final int TREE_COLUMN_FAMILY = 3;

    private Client client;

    private PropertiesCallback propertiesCallback;
    private SelectedColumnFamilyCallback cCallback;
    private RepaintCallback rCallback;

    private JScrollPane scrollPane;
    private String lastSelectedKeysapce;
    private String lastSelectedColumnFamily;
    private String deletedKeyspace;
    private String deletedColumnFamily;
    private JTree tree;
    private DefaultTreeModel treeModel;

    public KeyspaceTreePanel(Client client) {
        this.client = client;
        createTree();
    }

    public void createTree() {
        try {
            DefaultMutableTreeNode clusterNode =
                new DefaultMutableTreeNode(client.describeClusterName());
            treeModel = new DefaultTreeModel(clusterNode);
            tree = new JTree(treeModel);
            tree.setRootVisible(true);
            tree.addMouseListener(new MousePopup());
            tree.addTreeSelectionListener(this);

            List<KsDef> ks = null;
            try {
                ks = new ArrayList<KsDef>(client.getKeyspaces());
            } catch (InvalidRequestException e) {
                //TODO - Handle eligantly
                e.printStackTrace();
            }
            Collections.sort(ks);
            for (KsDef keyspace : ks) {
                DefaultMutableTreeNode keyspaceNode = new DefaultMutableTreeNode(keyspace.getName());
                clusterNode.add(keyspaceNode);
                try {
                    Set<String> cfs = client.getColumnFamilys(keyspace.getName());
                    for (String columnFamily : cfs) {
                        keyspaceNode.add(new DefaultMutableTreeNode(columnFamily));
                    }
                } catch (NotFoundException e) {
                    JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                    e.printStackTrace();
                } catch (InvalidRequestException invReq) {
                    JOptionPane.showMessageDialog(null, "error: " + invReq.getMessage());
                    invReq.printStackTrace();
                }
            }
        } catch (TException e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        add(scrollPane);
        repaint();
    }

    public void refreshTree() {
        try {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeModel.getRoot();
            node.removeAllChildren();

            List<KsDef> ks = null;
            try {
                ks = new ArrayList<KsDef>(client.getKeyspaces());
            } catch (InvalidRequestException e) {
                e.printStackTrace();
            }
            Collections.sort(ks);
            for (KsDef keyspace : ks) {
                DefaultMutableTreeNode keyspaceNode = new DefaultMutableTreeNode(keyspace.getName());
                node.add(keyspaceNode);
                try {
                    Set<String> cfs = client.getColumnFamilys(keyspace.getName());
                    for (String columnFamily : cfs) {
                        keyspaceNode.add(new DefaultMutableTreeNode(columnFamily));
                    }
                } catch (NotFoundException e) {
                    JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                    e.printStackTrace();
                } catch (InvalidRequestException invReq) {
                    JOptionPane.showMessageDialog(null, "error: " + invReq.getMessage());
                    invReq.printStackTrace();
                }
            }
            treeModel.reload();
        } catch (TException e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        String keyspace;
        String columnFamily;

        switch (e.getPath().getPathCount()) {
        case TREE_CLUSTER:
            propertiesCallback.clusterCallback();
            break;
        case TREE_KEYSPACE:
            keyspace = e.getPath().getPath()[TREE_KEYSPACE - 1].toString();
            if (!keyspace.equals(deletedKeyspace)) {
                propertiesCallback.keyspaceCallback(keyspace);
            }
            break;
        case TREE_COLUMN_FAMILY:
            keyspace = e.getPath().getPath()[TREE_KEYSPACE - 1].toString();
            columnFamily = e.getPath().getPath()[TREE_COLUMN_FAMILY - 1].toString();
            if (!columnFamily.equals(deletedColumnFamily)) {
                propertiesCallback.columnFamilyCallback(keyspace, columnFamily);
            }
            break;
        }
    }

    @Override
    public void repaint() {
        if (scrollPane != null && rCallback != null) {
            Dimension d = rCallback.callback();
            scrollPane.setPreferredSize(new Dimension(d.width - 10,
                                                      d.height - 10));
            scrollPane.repaint();
        }
        super.repaint();
    }

    /**
     * @param propertiesCallback the propertiesCallback to set
     */
    public void setPropertiesCallback(PropertiesCallback propertiesCallback) {
        this.propertiesCallback = propertiesCallback;
    }

    /**
     * @param cCallback the cCallback to set
     */
    public void setcCallback(SelectedColumnFamilyCallback cCallback) {
        this.cCallback = cCallback;
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
