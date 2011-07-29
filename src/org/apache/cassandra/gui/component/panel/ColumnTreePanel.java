package org.apache.cassandra.gui.component.panel;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.action.ColumnPopupAction;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.node.TreeNode;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.unit.Cell;
import org.apache.cassandra.unit.Key;
import org.apache.cassandra.unit.SColumn;
import org.apache.cassandra.unit.Unit;

public class ColumnTreePanel extends JPanel {
    private static final long serialVersionUID = -4236268406209844637L;

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
                Unit u = unitMap.get(node);
                TreeNode treeNode = new TreeNode(client,
                                                 node,
                                                 treeModel,
                                                 u,
                                                 unitMap,
                                                 keyMap);
                JPopupMenu popup = new JPopupMenu();
                if (u == null) {
                    popup.add(new ColumnPopupAction("add",
                                                    ColumnPopupAction.OPERATION_PROPERTIES,
                                                    superColumn,
                                                    treeNode));
                } else {
                    if (u instanceof Cell) {
                        popup.add(new ColumnPopupAction("properties",
                                                        ColumnPopupAction.OPERATION_PROPERTIES,
                                                        superColumn,
                                                        treeNode));
                    } else {
                        popup.add(new ColumnPopupAction("add",
                                                        ColumnPopupAction.OPERATION_PROPERTIES,
                                                        superColumn,
                                                        treeNode));
                    }
                    popup.add(new ColumnPopupAction("remove",
                                                    ColumnPopupAction.OPERATION_REMOVE,
                                                    superColumn,
                                                    treeNode));
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String COLUMN_FAMILY_TYPE_SUPER = "Super";

    private Client client;
    private boolean superColumn;

    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTree tree;
    private DefaultTreeModel treeModel;

    private Map<DefaultMutableTreeNode, Unit> unitMap = new HashMap<DefaultMutableTreeNode, Unit>();
    private Map<String, Unit> keyMap = new HashMap<String, Unit>();

    public ColumnTreePanel(Client client) {
        this.client = client;
        scrollPane = new JScrollPane();
        add(scrollPane);
        repaint();
    }

    @Override
    public void repaint() {
        if (scrollPane != null && rCallback != null) {
            Dimension d = rCallback.callback();
            scrollPane.setPreferredSize(new Dimension(d.width - 5,
                                                      d.height - 5));
            scrollPane.repaint();
        }
        super.repaint();
    }

    public void showRow(String keyspace, String columnFamily, String key) {
        try {
            Map<String, Object> m = client.getColumnFamily(keyspace, columnFamily);
            if (m.get(CfDef._Fields.COLUMN_TYPE.name()).equals(COLUMN_FAMILY_TYPE_SUPER)) {
                client.setSuperColumn(true);
                superColumn = true;
            } else {
                client.setSuperColumn(false);
                superColumn = false;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Map<String, Key> l =
                client.getKey(keyspace, columnFamily, null, key);
            showTree(l);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            JOptionPane.showMessageDialog(null, "error: " + ((e.getMessage() != null) ? e.getMessage() : e));
            e.printStackTrace();
        }
    }

    public void showRows(String keyspace, String columnFamily, String startKey, String endKey, int rows) {
        try {
            Map<String, Object> m = client.getColumnFamily(keyspace, columnFamily);
            if (m.get(CfDef._Fields.COLUMN_TYPE.name()).equals(COLUMN_FAMILY_TYPE_SUPER)) {
                client.setSuperColumn(true);
                superColumn = true;
            } else {
                client.setSuperColumn(false);
                superColumn = false;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Map<String, Key> l =
                client.listKeyAndValues(keyspace, columnFamily, startKey, endKey, rows);
            showTree(l);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } catch (Exception e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            JOptionPane.showMessageDialog(null, "error: " + ((e.getMessage() != null) ? e.getMessage() : e));
            e.printStackTrace();
        }
    }

    public void clear() {
        DefaultMutableTreeNode columnFamilyNode = new DefaultMutableTreeNode(client.getColumnFamily());
        treeModel = new DefaultTreeModel(columnFamilyNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        scrollPane.getViewport().setView(tree);
        repaint();
    }

    private void showTree(Map<String, Key> l) {
        DefaultMutableTreeNode columnFamilyNode = new DefaultMutableTreeNode(client.getColumnFamily());
        treeModel = new DefaultTreeModel(columnFamilyNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.addMouseListener(new MousePopup());

        for (String keyName : l.keySet()) {
            Key k = l.get(keyName);
            DefaultMutableTreeNode keyNode = new DefaultMutableTreeNode(k.getName());
            k.setTreeNode(keyNode);
            columnFamilyNode.add(keyNode);
            unitMap.put(keyNode, k);
            keyMap.put(k.getName(), k);
            if (k.isSuperColumn()) {
                for (String sName : k.getSColumns().keySet()) {
                    SColumn sc = k.getSColumns().get(sName);
                    DefaultMutableTreeNode scNode = new DefaultMutableTreeNode(sc.getName());
                    sc.setTreeNode(scNode);
                    keyNode.add(scNode);
                    unitMap.put(scNode, sc);
                    for (String cName : sc.getCells().keySet()) {
                        Cell c = sc.getCells().get(cName);
                        DefaultMutableTreeNode cellNode =
                            new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                        c.setTreeNode(cellNode);
                        scNode.add(cellNode);
                        unitMap.put(cellNode, c);
                    }
                }
            } else {
                for (String cName : k.getCells().keySet()) {
                    Cell c = k.getCells().get(cName);
                    DefaultMutableTreeNode cellNode =
                        new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                    c.setTreeNode(cellNode);
                    keyNode.add(cellNode);
                    unitMap.put(cellNode, c);
                }
            }
        }

        scrollPane.getViewport().setView(tree);
        repaint();
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
