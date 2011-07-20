package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.apache.cassandra.client.Client;

/**
 * Connection Dialogue class to connect to the Cassandra cluster
 */
public class ConnectionDialog extends JDialog {
    private static final long serialVersionUID = 8707158056959280058L;

    private class EnterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            enterAction();
        }
    }

    private Client client;
    private JButton ok = new JButton("OK");
    private JTextField hostText = new JTextField();
    private JTextField thriftPortText = new JTextField();
    private JTextField jmxPortTextField = new JTextField();

    public ConnectionDialog(JFrame owner){
        super(owner);

        thriftPortText.setText(String.valueOf(Client.DEFAULT_THRIFT_PORT));
        jmxPortTextField.setText(String.valueOf(Client.DEFAULT_JMX_PORT));

        hostText.addActionListener(new EnterAction());
        thriftPortText.addActionListener(new EnterAction());
        jmxPortTextField.addActionListener(new EnterAction());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        inputPanel.add(new JLabel("Host:"));
        inputPanel.add(hostText);
        inputPanel.add(new JLabel("Thrift Port:"));
        inputPanel.add(thriftPortText);
        inputPanel.add(new JLabel("JMX Port:"));
        inputPanel.add(jmxPortTextField);

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
                client = null;
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Connection Details"), BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        inputPanel.setBorder(BorderFactory.createEtchedBorder());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Connection Details");
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void enterAction() {
        if (hostText.getText().isEmpty()){
            JOptionPane.showMessageDialog(null, "Enter Hostname.");
            return;
        }

        String host = hostText.getText();
        int thriftPort =
            thriftPortText.getText().isEmpty() ?
                    Client.DEFAULT_THRIFT_PORT :
                    Integer.valueOf(thriftPortText.getText());
        int jmxPort =
            jmxPortTextField.getText().isEmpty() ?
                    Client.DEFAULT_JMX_PORT :
                    Integer.valueOf(jmxPortTextField.getText());

        client = new Client(host, thriftPort, jmxPort);
        try {
            client.connect();
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(null, "Connection failed.");
            e1.printStackTrace();
            return;
        }

        setVisible(false);
    }

    /**
     * @return the client
     */
    public Client getClient() {
        return client;
    }
}
