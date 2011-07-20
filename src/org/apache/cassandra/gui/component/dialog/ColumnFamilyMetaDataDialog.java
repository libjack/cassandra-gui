package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.model.ColumnFamilyMetaDataModel;
import org.apache.cassandra.thrift.IndexType;
import org.apache.cassandra.unit.ColumnFamily;
import org.apache.cassandra.unit.ColumnFamilyMetaData;

public class ColumnFamilyMetaDataDialog extends JDialog {
    private static final long serialVersionUID = -2295468150799941163L;

    private static final int BUTTON_COLUMN = 4;

    public ColumnFamilyMetaDataDialog(final ColumnFamily columnFamily) {
        final ColumnFamilyMetaDataModel model = new ColumnFamilyMetaDataModel();
        final JTable table = new JTable(model);
        TableRowSorter<ColumnFamilyMetaDataModel> sorter = new TableRowSorter<ColumnFamilyMetaDataModel>(model);
        table.setRowSorter(sorter);
        sorter.setSortable(BUTTON_COLUMN, false);

        TableColumn colColumnName = table.getColumnModel().getColumn(0);
        colColumnName.setMinWidth(60);
        colColumnName.setMaxWidth(60);
        colColumnName.setResizable(false);

        TableColumn column = table.getColumnModel().getColumn(BUTTON_COLUMN);
        column.setCellRenderer(new DeleteButtonRenderer());
        column.setCellEditor(new DeleteButtonEditor(table));
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column.setResizable(false);

        TableColumn colValidationClass = table.getColumnModel().getColumn(1);
        JComboBox validationClassCb = new JComboBox(Client.getValidationClassMap().values().toArray());

        validationClassCb.setBorder(BorderFactory.createEmptyBorder());
        colValidationClass.setCellEditor(new DefaultCellEditor(validationClassCb));

        TableColumn colIndexType = table.getColumnModel().getColumn(2);
        JComboBox indexTypeCb = new JComboBox(IndexType.values());
        indexTypeCb.setBorder(BorderFactory.createEmptyBorder());
        colIndexType.setCellEditor(new DefaultCellEditor(indexTypeCb));

        for (ColumnFamilyMetaData metaData : columnFamily.getMetaDatas()) {
            metaData.setValiDationClass(Client.getValidationClassMap().get(metaData.getValiDationClass()));
            model.add(metaData);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(new JButton(new AbstractAction("add") {
            private static final long serialVersionUID = 5064662528876805962L;

            @Override public void actionPerformed(ActionEvent e) {
                model.add(new ColumnFamilyMetaData());
            }
        }));

        buttonPanel.add(new JButton(new AbstractAction("apply") {
            private static final long serialVersionUID = 5064662528876805962L;

            @Override public void actionPerformed(ActionEvent e) {
                columnFamily.getMetaDatas().clear();
                for (int i = 0; i < model.getRowCount(); i++) {
                    ColumnFamilyMetaData metaData = new ColumnFamilyMetaData();
                    String columnName = (String) model.getValueAt(i, ColumnFamilyMetaDataModel.COLUMN_COLUMN_NAME);
                    if (columnName == null || columnName.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Enter Column Name.");
                        return;
                    }
                    metaData.setColumnName(columnName);

                    String valiDationClass = (String) model.getValueAt(i, ColumnFamilyMetaDataModel.COLUMN_VALIDATION_CLASS);
                    if (valiDationClass == null || valiDationClass.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Enter ValiDation Class.");
                        return;
                    }
                    metaData.setValiDationClass(valiDationClass);

                    IndexType indexType = (IndexType) model.getValueAt(i, ColumnFamilyMetaDataModel.COLUMN_INDEX_TYPE);
                    if (indexType != null) {
                        metaData.setIndexType(indexType);
                    }

                    metaData.setIndexName((String) model.getValueAt(i, ColumnFamilyMetaDataModel.COLUMN_INDEX_NAME));
                    columnFamily.getMetaDatas().add(metaData);
                }
                setVisible(false);
            }
        }));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        add(panel);

        setPreferredSize(new Dimension(320, 200));

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("metadata detail");
        setLocationRelativeTo(null);
        setModal(true);
    }

    private class DeleteButton extends JButton {
        private static final long serialVersionUID = -6863132480711592499L;

        @Override
        public void updateUI() {
            super.updateUI();
            setBorder(BorderFactory.createEmptyBorder());
            setFocusable(false);
            setRolloverEnabled(false);
            setText("X");
        }
    }

    private class DeleteButtonRenderer extends DeleteButton implements TableCellRenderer {
        private static final long serialVersionUID = -4965652160429644303L;

        public DeleteButtonRenderer() {
            super();
            setName("Table.cellRenderer");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            return this;
        }
    }

    private class DeleteButtonEditor extends DeleteButton implements TableCellEditor {
        private static final long serialVersionUID = 7489312671827408326L;

        public DeleteButtonEditor(final JTable table) {
            super();
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    int row = table.convertRowIndexToModel(table.getEditingRow());
                    fireEditingStopped();
                    ((DefaultTableModel) table.getModel()).removeRow(row);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            return this;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            fireEditingStopped();
            return true;
        }

        @Override
        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            listenerList.add(CellEditorListener.class, l);
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(CellEditorListener.class, l);
        }

        protected void fireEditingStopped() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >=0; i -= 2) {
                if (listeners[i] == CellEditorListener.class) {
                    if (changeEvent == null) {
                        changeEvent = new ChangeEvent(this);
                    }
                    ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
                }
            }
        }

        protected void fireEditingCanceled() {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == CellEditorListener.class) {
                    if (changeEvent == null) {
                        changeEvent = new ChangeEvent(this);
                    }
                    ((CellEditorListener) listeners[i + 1]).editingCanceled(changeEvent);
                }
            }
        }
    }
}
