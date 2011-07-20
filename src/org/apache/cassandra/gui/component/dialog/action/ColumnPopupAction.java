package org.apache.cassandra.gui.component.dialog.action;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.CellPropertiesDialog;
import org.apache.cassandra.node.TreeNode;
import org.apache.cassandra.unit.Cell;
import org.apache.cassandra.unit.Key;
import org.apache.cassandra.unit.SColumn;
import org.apache.cassandra.unit.Unit;

public class ColumnPopupAction extends AbstractAction {
    private static final long serialVersionUID = -4419251468566465640L;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final int OPERATION_PROPERTIES = 1;
    public static final int OPERATION_REMOVE = 2;

    private int operation;
    private boolean isSuperColumn;
    private TreeNode treeNode;
    private Client client;

    public ColumnPopupAction(String name,
                             int operation,
                             boolean isSuperColumn,
                             TreeNode treeNode) {
        this.operation = operation;
        this.treeNode = treeNode;
        this.isSuperColumn = isSuperColumn;
        this.client = treeNode.getClient();
        putValue(Action.NAME, name);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Unit u = treeNode.getUnit();
        switch (operation) {
        case OPERATION_PROPERTIES:
            if (u == null) {
                insertKeyCell();
            } else if (u instanceof Key) {
                insertCell();
            } else if (u instanceof SColumn) {
                insertSuperColumnCell();
            } else if (u instanceof Cell) {
                updateCell();
            }

            break;
        case OPERATION_REMOVE:
            String msg = "";
            if (u instanceof Key) {
                msg = "key";
            } else if (u instanceof SColumn) {
                msg = "super column";
            } else {
                msg = "column";
            }

            int status = JOptionPane.showConfirmDialog(null,
                                                       "Delete a " + msg + " " + getName() + "?",
                                                       "confirm",
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
            if (status == JOptionPane.YES_OPTION) {
                remove();
            }

            break;
        }
    }

    private void insertKeyCell() {
        CellPropertiesDialog cpdlg = new CellPropertiesDialog(isSuperColumn ?
                                                                  CellPropertiesDialog.OPERATION_KEY_SUPERCOLUMN_INSERT :
                                                                  CellPropertiesDialog.OPERATION_KEY_INSERT);
        cpdlg.setVisible(true);
        if (cpdlg.isCancel()) {
            return;
        }

        String key = cpdlg.getKey();
        String superColumn = cpdlg.getSuperColumn();
        String name = cpdlg.getName();
        String value = cpdlg.getValue();

        boolean keyFound = false;
        boolean superColumnFound = false;
        boolean cellFound = false;

        Key k =  null;
        try {
            k = (Key) treeNode.getKeyMap().get(key);
            if (k == null) {
                Map<String, Key> m = client.getKey(client.getKeyspace(),
                                                   client.getColumnFamily(),
                                                   superColumn.isEmpty() ?
                                                       null : superColumn,
                                                   key);
                k = m.get(key);
                if (k == null) {
                    k = new Key(key, new HashMap<String, SColumn>(), new HashMap<String, Cell>());
                    k.setSuperColumn(isSuperColumn);
                }
            } else {
                keyFound = true;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        SColumn s = null;
        DefaultMutableTreeNode sn = null;
        if (isSuperColumn) {
            s = k.getSColumns().get(superColumn);
            if (s == null) {
                s = new SColumn(k, superColumn, new HashMap<String, Cell>());
                sn = new DefaultMutableTreeNode(s.getName());
                s.setTreeNode(sn);
                s.setParent(k);
            } else {
                sn = s.getTreeNode();
                superColumnFound = true;
            }
        }

        Date d = null;
        try {
            d = client.insertColumn(client.getKeyspace(),
                                    client.getColumnFamily(),
                                    key,
                                    s == null ? null : superColumn,
                                    name,
                                    value);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        DefaultMutableTreeNode kn = k.getTreeNode();
        if (kn == null) {
            kn = new DefaultMutableTreeNode(key);
            k.setTreeNode(kn);
        }

        if (isSuperColumn) {
            Cell c = s.getCells().get(name);
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode(name + "=" + value + ", " +
                                                                   DATE_FORMAT.format(d));
            if (c == null) {
                c = new Cell(s, name, value, d);
                c.setTreeNode(cn);
                if (isSuperColumn) {
                    c.setParent(s);
                } else {
                    c.setParent(k);
                }
            } else {
                c.getTreeNode().setUserObject(cn);
                cellFound = true;
            }
            s.getCells().put(name, c);
            k.getSColumns().put(superColumn, s);

            if (!cellFound) {
                sn.add(cn);
            }

            if (!superColumnFound) {
                kn.add(sn);
            }

            if (!keyFound) {
                treeNode.getNode().add(kn);
            }

            treeNode.getUnitMap().put(kn, k);
            treeNode.getUnitMap().put(sn, s);
            treeNode.getUnitMap().put(cn, c);
        } else {
            Cell c = k.getCells().get(name);
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode(name + "=" + value + ", " +
                                                                   DATE_FORMAT.format(d));
            if (c == null) {
                c = new Cell(s, name, value, d);
                c.setTreeNode(cn);
                c.setParent(k);
            } else {
                c.getTreeNode().setUserObject(cn);
                cellFound = true;
            }
            k.getCells().put(name, c);

            if (!cellFound) {
                kn.add(cn);
            }

            if (!keyFound) {
                treeNode.getNode().add(kn);
            }

            treeNode.getUnitMap().put(kn, k);
            treeNode.getUnitMap().put(cn, c);
        }

        treeNode.getKeyMap().put(key, k);

        treeNode.getTreeModel().nodeChanged(treeNode.getNode());
        treeNode.getTreeModel().reload(treeNode.getNode());
    }

    private void insertCell() {
        Key k = (Key) treeNode.getUnit();

        CellPropertiesDialog cpdlg = new CellPropertiesDialog(isSuperColumn ?
                                                                  CellPropertiesDialog.OPERATION_SUPERCOLUMN_INSERT :
                                                                  CellPropertiesDialog.OPERATION_CELL_INSERT);
        cpdlg.setVisible(true);
        if (cpdlg.isCancel()) {
            return;
        }

        SColumn s = null;
        if (isSuperColumn) {
            s = new SColumn(k, cpdlg.getSuperColumn(), new HashMap<String, Cell>());
        }

        Date d = null;
        try {
            d = client.insertColumn(client.getKeyspace(),
                                    client.getColumnFamily(),
                                    k.getName(),
                                    s == null ? null : s.getName(),
                                    cpdlg.getName(),
                                    cpdlg.getValue());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
        }

        if (isSuperColumn) {
            Cell c = new Cell(s, cpdlg.getName(), cpdlg.getValue(), d);
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " +
                                                                   DATE_FORMAT.format(c.getDate()));
            s.getCells().put(c.getName(), c);

            DefaultMutableTreeNode sn = new DefaultMutableTreeNode(s.getName());
            sn.add(cn);
            treeNode.getNode().add(sn);

            treeNode.getUnitMap().put(sn, s);
            treeNode.getUnitMap().put(cn, c);
        } else {
            Cell c = new Cell(k, cpdlg.getName(), cpdlg.getValue(), d);
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " +
                                                                   DATE_FORMAT.format(c.getDate()));
            k.getCells().put(c.getName(), c);
            treeNode.getNode().add(cn);

            treeNode.getUnitMap().put(cn, c);
        }

        treeNode.getTreeModel().reload(treeNode.getNode());
    }

    private void insertSuperColumnCell() {
        SColumn s = (SColumn) treeNode.getUnit();
        CellPropertiesDialog cpdlg = new CellPropertiesDialog(CellPropertiesDialog.OPERATION_CELL_INSERT);
        cpdlg.setVisible(true);
        if (cpdlg.isCancel()) {
            return;
        }

        Key k = (Key) s.getParent();

        Date d = null;
        try {
            d = client.insertColumn(client.getKeyspace(),
                                    client.getColumnFamily(),
                                    k.getName(),
                                    s.getName(),
                                    cpdlg.getName(),
                                    cpdlg.getValue());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
        }

        Cell c = new Cell(s, cpdlg.getName(), cpdlg.getValue(), d);
        DefaultMutableTreeNode cn = new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " +
                                                               DATE_FORMAT.format(c.getDate()));
        s.getCells().put(c.getName(), c);
        treeNode.getUnitMap().put(cn, c);

        treeNode.getNode().add(cn);
        treeNode.getTreeModel().reload(treeNode.getNode());
    }

    private void updateCell() {
        Cell c = (Cell) treeNode.getUnit();
        CellPropertiesDialog cpdlg = new CellPropertiesDialog(CellPropertiesDialog.OPERATION_CELL_UPDATE,
                                                              c.getName(),
                                                              c.getValue());
        cpdlg.setVisible(true);
        if (cpdlg.isCancel()) {
            return;
        }

        Key k = null;
        SColumn s = null;

        Unit parentUnit = c.getParent();
        if (parentUnit instanceof SColumn) {
            s = (SColumn) parentUnit;
            k = (Key) s.getParent();
        } else {
            k = (Key) parentUnit;
        }

        Date d = null;
        try {
            d = client.insertColumn(client.getKeyspace(),
                                    client.getColumnFamily(),
                                    k.getName(),
                                    s == null ? null : s.getName(),
                                    cpdlg.getName(),
                                    cpdlg.getValue());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
        }

        c.setName(cpdlg.getName());
        c.setValue(cpdlg.getValue());
        c.setDate(d);

        treeNode.getNode().setUserObject(
                            new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " +
                                                       DATE_FORMAT.format(c.getDate())));
        treeNode.getTreeModel().nodeChanged(treeNode.getNode());
    }

    private void remove() {
        try {
            if (treeNode.getUnit() instanceof Key) {
                Key k = (Key) treeNode.getUnit();
                client.removeKey(client.getKeyspace(),
                                 client.getColumnFamily(),
                                 k.getName());

                if (isSuperColumn) {
                    for (SColumn s : k.getSColumns().values()) {
                        treeNode.getUnitMap().remove(s.getTreeNode());
                        for (Cell c : s.getCells().values()) {
                            treeNode.getUnitMap().remove(c.getTreeNode());
                        }
                    }
                } else {
                    for (Cell c : k.getCells().values()) {
                        treeNode.getUnitMap().remove(c.getTreeNode());
                    }
                }

                k.getCells().clear();
                k.getSColumns().clear();
                treeNode.getNode().removeAllChildren();
                treeNode.getTreeModel().reload(treeNode.getNode());
            } else if (treeNode.getUnit() instanceof SColumn) {
                SColumn s = (SColumn) treeNode.getUnit();
                Key k = (Key) s.getParent();
                client.removeSuperColumn(client.getKeyspace(),
                                         client.getColumnFamily(),
                                         k.getName(),
                                         s.getName());
                k.getSColumns().remove(s.getName());

                for (Cell c : s.getCells().values()) {
                    treeNode.getUnitMap().remove(c.getTreeNode());
                }

                removeNode((DefaultMutableTreeNode) treeNode.getNode().getParent(), treeNode.getNode());
            } else {
                Cell c = (Cell) treeNode.getUnit();
                treeNode.getUnitMap().remove(c.getTreeNode());
                Unit parent = c.getParent();
                if (parent instanceof Key) {
                    Key k = (Key) parent;
                    client.removeColumn(client.getKeyspace(),
                                        client.getColumnFamily(),
                                        k.getName(),
                                        c.getName());
                    k.getCells().remove(c.getName());

                    removeNode((DefaultMutableTreeNode) treeNode.getNode().getParent(), treeNode.getNode());
                } else if (parent instanceof SColumn) {
                    SColumn s = (SColumn) parent;
                    Key k = (Key) s.getParent();
                    client.removeColumn(client.getKeyspace(),
                                        client.getColumnFamily(),
                                        k.getName(),
                                        s.getName(),
                                        c.getName());
                    s.getCells().remove(c.getName());

                    DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode) treeNode.getNode().getParent();
                    removeNode(parentNode, treeNode.getNode());

                    if (s.getCells().isEmpty()) {
                        k.getSColumns().remove(s.getName());
                        removeNode((DefaultMutableTreeNode) parentNode.getParent(), parentNode);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeNode(DefaultMutableTreeNode parentNode,
                            DefaultMutableTreeNode node) {
        if (parentNode != null && node != null) {
            node.removeFromParent();
            treeNode.getTreeModel().reload(parentNode);
        }
    }

    private String getName() {
        if (treeNode.getUnit() instanceof Key) {
            return ((Key) treeNode.getUnit()).getName();
        } else if (treeNode.getUnit() instanceof SColumn) {
            return ((SColumn) treeNode.getUnit()).getName();
        }

        return ((Cell) treeNode.getUnit()).getName();
    }
}
