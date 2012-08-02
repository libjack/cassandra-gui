package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cassandra.client.Client;

public class KeyspaceDialog extends JDialog {
    private static final long serialVersionUID = 3957978062006221011L;

    private class EnterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            enterAction();
        }
    }

    private JTextField keyspaceText = new JTextField();
    private JTextField replicationFactorText = new JTextField();
    private JComboBox strategyBox = new JComboBox();
    private JTextField optionText = new JTextField();

    private boolean cancel = true;
    private String keyspaceName;
    private int replicationFactor;
    private String strategy;
    private Map<String, String> strategyOptions = new HashMap<String, String>();

    public KeyspaceDialog(String keyspaceName,
                          int replicationFactor,
                          String strategy,
                          Map<String, String> strategyOptions) {
        keyspaceText.setText(keyspaceName);
        keyspaceText.setEditable(false);

        replicationFactorText.setText(String.valueOf(replicationFactor));

        String selectedStrategy = null;
        for (Entry<String, String> entry : Client.getStrategyMap().entrySet()) {
            if (entry.getValue().equals(strategy)) {
                selectedStrategy = entry.getKey();
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : strategyOptions.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
        }
        if (sb.length() > 0) {
            optionText.setText(sb.substring(0, sb.length() - 1));
        }

        create(selectedStrategy);
    }

    public KeyspaceDialog() {
        create(null);
    }

    public void create(String selectedStrategy) {
        keyspaceText.addActionListener(new EnterAction());
        replicationFactorText.addActionListener(new EnterAction());
        optionText.addActionListener(new EnterAction());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Keyspace Name: "));
        inputPanel.add(keyspaceText);
        inputPanel.add(new JLabel("Replication Factor: "));
        inputPanel.add(replicationFactorText);

        for (String s : Client.getStrategyMap().keySet()) {
            strategyBox.addItem(s);
        }
        if (selectedStrategy != null) {
            strategyBox.setSelectedItem(selectedStrategy);
        }

        inputPanel.add(new JLabel("Strategy: "));
        inputPanel.add(strategyBox);

        inputPanel.add(new JLabel("Strategy Options: <att1>=<value1>,<att2>=<value2>..."));
        inputPanel.add(optionText);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterAction();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("create or update keyspace");
        setLocationRelativeTo(null);
        setModal(true);
    }

    private void enterAction() {
        if (keyspaceText.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Keyspace Name.");
            keyspaceText.requestFocus();
            return;
        }
        keyspaceName = keyspaceText.getText();

        if (replicationFactorText.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter Replication Factor.");
            replicationFactorText.requestFocus();
            return;
        }
        try {
            replicationFactor = Integer.valueOf(replicationFactorText.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "number input Replication Factor.");
            replicationFactorText.requestFocus();
            return;
        }

        strategy = (String) strategyBox.getSelectedItem();

        String options = optionText.getText();
        if (options != null && !options.isEmpty()) {
            String[] split1 = options.split(",");
            for (String s : split1) {
                String[] split2 = s.split("=");
                if (split2.length != 2) {
                    JOptionPane.showMessageDialog(null, "Strategy Options format error.");
                    optionText.requestFocus();
                    return;
                }
                strategyOptions.put(split2[0], split2[1]);
            }
        }

        setVisible(false);
        cancel = false;
    }

    /**
     * @return the cancel
     */
    public boolean isCancel() {
        return cancel;
    }

    /**
     * @return the keyspaceName
     */
    public String getKeyspaceName() {
        return keyspaceName;
    }

    /**
     * @return the replicationFactor
     */
    public int getReplicationFactor() {
        return replicationFactor;
    }

    /**
     * @return the strategy
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * @return the strategyOptions
     */
    public Map<String, String> getStrategyOptions() {
        return strategyOptions;
    }
}
