package org.apache.cassandra.gui.component.panel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.RingDialog;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.thrift.KsDef;

public class PropertiesPanel extends JPanel {
    private static final long serialVersionUID = 1452324774722196104L;

    private class Header extends JTableHeader {
        private static final long serialVersionUID = 6540121946461922362L;

        public Header(TableColumnModel columnModel) {
            super(columnModel);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_CLICKED &&
                SwingUtilities.isLeftMouseButton(e)) {
                Cursor cur = super.getCursor();
                if (cur.getType() == Cursor.E_RESIZE_CURSOR) {
                    int cc = e.getClickCount();
                    if (cc % 2 == 1) {
                        return;
                    } else {
                        Point pt = new Point(e.getX() - 3, e.getY());
                        int vc = super.columnAtPoint(pt);
                        if (vc >= 0) {
                            sizeWidthToFitData(vc);
                            e.consume();
                            return;
                        }
                    }
                }
            }
            super.processMouseEvent(e);
        }

        public void sizeWidthToFitData(int vc) {
            JTable table = super.getTable();
            TableColumn tc = table.getColumnModel().getColumn(vc);

            int max = 0;

            int vrows = table.getRowCount();
            for (int i = 0; i < vrows; i++) {
                TableCellRenderer r = table.getCellRenderer(i, vc);
                Object value = table.getValueAt(i, vc);
                Component c = r.getTableCellRendererComponent(table, value, false, false, i, vc);
                int w = c.getPreferredSize().width;
                if (max < w) {
                    max = w;
                }
            }

            tc.setPreferredWidth(max + 1);
        }
    }

    private static final String COLUMN_SNITCH = "Snitch";
    private static final String COLUMN_PARTITIONER = "Partitioner";
    private static final String COLUMN_SCHEMA_VERSIONS = "Schema versions: ";
    private static final String COLUMN_VERSION = "api version";
    private static final String COLUMN_NUMBER_OF_KEYSPACE = "Number of Keyspace";
    private static final String COLUMN_RING = "ring";
    private static final String COLUMN_DOUBLE_CLICK_VALUE = "view the details by double-clicking";

    private static final String COLUMN_REPLICATION_STRATEGY = "Replication Strategy";
    private static final String COLUMN_REPLICATION_FACTOR = "Replication Factor";
    private static final String COLUMN_NUMBER_OF_COLUMN_FAMILY = "Number of Column Family";

    private static final String[] columns = { "name", "value" };

    private Client client;
    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTable table;

    private DefaultTableModel tableModel;

    public PropertiesPanel(final Client client) {
        this.client = client;

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 7088445834198028640L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            private static final long serialVersionUID = -5396565496344780783L;

            protected JTableHeader createDefaultTableHeader() {
                return new Header(super.columnModel);
            };
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if(me.getClickCount() == 2) {
                    Point point = me.getPoint();
                    int row = table.convertRowIndexToModel(table.rowAtPoint(point));
                    try {
                        if (tableModel.getValueAt(row, 0).equals(COLUMN_RING)) {
                            RingDialog rd = new RingDialog(client);
                            rd.setVisible(true);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });

        DefaultTableColumnModel columnModel = (DefaultTableColumnModel)table.getColumnModel();
        for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(350);
        }

        scrollPane = new JScrollPane(table);
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

    public void showClusterProperties() {
        try {
            tableModel.setRowCount(0);

            tableModel.addRow(new String[] {COLUMN_SNITCH, client.describeSnitch()});
            tableModel.addRow(new String[] {COLUMN_PARTITIONER, client.describePartitioner()});

            Map<String, List<String>> m = client.describeSchemaVersions();
            if (m != null) {
                for (Entry<String, List<String>> entry : m.entrySet()) {
                    if (entry.getKey() != null) {
                        for (String s : entry.getValue()) {
                            tableModel.addRow(new String[] {COLUMN_SCHEMA_VERSIONS + entry.getKey(), s});
                        }
                    }
                }
            }

            tableModel.addRow(new String[] {COLUMN_VERSION, client.descriveVersion()});
            int n = client.getKeyspaces().size();
            tableModel.addRow(new String[] {COLUMN_NUMBER_OF_KEYSPACE, String.valueOf(n)});
            tableModel.addRow(new String[] {COLUMN_RING, COLUMN_DOUBLE_CLICK_VALUE});

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();
    }

    public void showKeyspaceProperties(String keyspace) {
        try {
            tableModel.setRowCount(0);
            KsDef kd = client.describeKeyspace(keyspace);

            tableModel.addRow(new String[] {COLUMN_REPLICATION_FACTOR, String.valueOf(kd.getReplication_factor())});
            tableModel.addRow(new String[] {COLUMN_REPLICATION_STRATEGY, kd.getStrategy_class()});
            int n = client.getColumnFamilys(keyspace).size();
            tableModel.addRow(new String[] {COLUMN_NUMBER_OF_COLUMN_FAMILY, String.valueOf(n)});

            if (kd.getStrategy_options() != null) {
                for (Entry<String, String> entry : kd.getStrategy_options().entrySet()) {
                    tableModel.addRow(new String[] {entry.getKey(), entry.getValue()});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();
    }

    public void showColumnFamilyProperties(String keyspace, String columnFamily) {
        try {
            tableModel.setRowCount(0);
            Map<String, Object> m = client.getColumnFamily(keyspace, columnFamily);
            for (Map.Entry<String, Object> e : m.entrySet()) {
            	// FIXME ... column metadata is not showing up correctly, here... plus multi-line, or hover to see all, etc
                tableModel.addRow(new String[] {e.getKey(), (e.getValue() != null) ? e.getValue().toString() : ""});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
