package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class KeyRangeDialog extends JDialog {
    private static final long serialVersionUID = -7378362468372008181L;

    private boolean cancel = true;
    private String startKey;
    private String endKey;

    public KeyRangeDialog(){
        final JTextField startKeyText = new JTextField();
        final JTextField endKeyText = new JTextField();

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        inputPanel.add(new JLabel("start key:"));
        inputPanel.add(startKeyText);
        inputPanel.add(new JLabel("end key:"));
        inputPanel.add(endKeyText);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startKeyText.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Enter start key.");
                    startKeyText.requestFocus();
                    return;
                }

                if (endKeyText.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Enter end key.");
                    endKeyText.requestFocus();
                    return;
                }

                startKey = startKeyText.getText();
                endKey = endKeyText.getText();

                setVisible(false);
                cancel = false;
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
        setTitle("key range");
        setLocationRelativeTo(null);
        setModal(true);
    }

    /**
     * @return the cancel
     */
    public boolean isCancel() {
        return cancel;
    }

    /**
     * @return the startKey
     */
    public String getStartKey() {
        return startKey;
    }

    /**
     * @return the endKey
     */
    public String getEndKey() {
        return endKey;
    }
}
