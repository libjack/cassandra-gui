package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.cassandra.node.NodeInfo;

public class NodeInfoDialog extends JDialog {
    private static final long serialVersionUID = -5238189165348274251L;

    public NodeInfoDialog(NodeInfo nodeInfo) {
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Load: "));
        inputPanel.add(new JLabel(nodeInfo.getLoad()));
        inputPanel.add(new JLabel("Generation No: "));
        inputPanel.add(new JLabel(String.valueOf(nodeInfo.getGenerationNumber())));
        inputPanel.add(new JLabel("Uptime (seconds): "));
        inputPanel.add(new JLabel(String.valueOf(nodeInfo.getUptime())));
        inputPanel.add(new JLabel("Heap Memory (MB): "));
        inputPanel.add(new JLabel(String.format("%.2f", nodeInfo.getMemUsed()) + " / " +
                                  String.format("%.2f", nodeInfo.getMemMax())));

        JScrollPane scrollPane = new JScrollPane(inputPanel);

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
        setTitle("Node Info(" + nodeInfo.getEndpoint() + ")");
        setLocationRelativeTo(null);
        setModal(true);
    }
}
