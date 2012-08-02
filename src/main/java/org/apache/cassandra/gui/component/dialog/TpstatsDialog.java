package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.apache.cassandra.node.Tpstats;

public class TpstatsDialog extends JDialog {
    private static final long serialVersionUID = -5287379277192919237L;

    private static final String[] columns = {"Pool Name", "Active", "Pending", "Completed"};

    public TpstatsDialog(String endpoint, List<Tpstats> l) {
        final DefaultTableModel tableModel= new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 7088445834198028640L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Tpstats t : l) {
            tableModel.addRow(new String[] {t.getPoolName(),
                                            String.valueOf(t.getActiveCount()),
                                            String.valueOf(t.getPendingTasks()),
                                            String.valueOf(t.getCompletedTasks())});
        }

        final JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Tpstats(" + endpoint + ")");
        setLocationRelativeTo(null);
        setModal(true);
    }
}
