package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class CellPropertiesDialog extends JDialog {
    private static final long serialVersionUID = -7378362468372008181L;

    private class PopupAction extends AbstractAction {
        private static final long serialVersionUID = 4235052996425858520L;

        private static final int ACTION_COPY = 1;

        private JTextField text;
        private int action;

        public PopupAction(String name, JTextField text, int action) {
            this.text = text;
            this.action = action;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            switch (action) {
            case ACTION_COPY:
                text.copy();
                break;
            }
        }
    }

    private class MousePopup extends MouseAdapter {
        private JTextField text;

        public MousePopup(JTextField text) {
            this.text = text;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                JPopupMenu popup = new JPopupMenu();
                popup.add(new PopupAction("Copy", text, PopupAction.ACTION_COPY));
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class EnterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            enterAction();
        }
    }

    public static final int OPERATION_KEY_INSERT = 1;
    public static final int OPERATION_KEY_SUPERCOLUMN_INSERT = 2;
    public static final int OPERATION_SUPERCOLUMN_INSERT = 3;
    public static final int OPERATION_CELL_INSERT = 4;
    public static final int OPERATION_CELL_UPDATE = 5;

    private static final String KEY = "key";
    private static final String SUPER_COLUMN = "super column";
    private static final String NAME = "name";
    private static final String VAELU = "value";

    private JTextField keyText = new JTextField();
    private JTextField superColumnText = new JTextField();
    private JTextField nameText = new JTextField();
    private JTextField valueText = new JTextField();
    private boolean cancel = true;

    private Map<String, JTextField> textFieldMap = new HashMap<String, JTextField>();

    public CellPropertiesDialog(int operation) {
        this(operation, "", "");
    }

    public CellPropertiesDialog(int operation, String name, String value){
        JPanel propertiesPane = null;
        switch (operation) {
        case OPERATION_KEY_INSERT:
            propertiesPane = new JPanel(new GridLayout(3, 2));
            propertiesPane.add(new JLabel(KEY + ": "));
            propertiesPane.add(keyText);
            textFieldMap.put(KEY, keyText);
            break;
        case OPERATION_KEY_SUPERCOLUMN_INSERT:
            propertiesPane = new JPanel(new GridLayout(4, 2));
            propertiesPane.add(new JLabel(KEY + ": "));
            propertiesPane.add(keyText);
            textFieldMap.put(KEY, keyText);

            propertiesPane.add(new JLabel(SUPER_COLUMN + ": "));
            propertiesPane.add(superColumnText);
            textFieldMap.put(SUPER_COLUMN, superColumnText);
            break;
        case OPERATION_SUPERCOLUMN_INSERT:
            propertiesPane = new JPanel(new GridLayout(3, 2));
            propertiesPane.add(new JLabel(SUPER_COLUMN + ": "));
            propertiesPane.add(superColumnText);
            textFieldMap.put(SUPER_COLUMN, superColumnText);
            break;
        case OPERATION_CELL_INSERT:
            nameText.addActionListener(new EnterAction());
            propertiesPane = new JPanel(new GridLayout(2, 2));
            break;
        case OPERATION_CELL_UPDATE:
            propertiesPane = new JPanel(new GridLayout(2, 2));
            nameText.setEditable(false);
            break;
        }

        nameText.setText(name);
        valueText.setText(value);
        valueText.addActionListener(new EnterAction());

        propertiesPane.add(new JLabel(NAME + ": "));
        propertiesPane.add(nameText);
        textFieldMap.put(NAME, nameText);

        propertiesPane.add(new JLabel(VAELU + ": "));
        propertiesPane.add(valueText);
        textFieldMap.put(VAELU, valueText);

        nameText.addMouseListener(new MousePopup(nameText));
        valueText.addMouseListener(new MousePopup(valueText));

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel = false;
                setVisible(false);
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
        panel.add(propertiesPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("properties");
        setLocationRelativeTo(null);
        setModal(true);
    }

    private void enterAction() {
        for (String s : textFieldMap.keySet()) {
            JTextField t = textFieldMap.get(s);
            if (t.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter " + s);
                t.requestFocus();
                return;
            }
        }

        setVisible(false);
        cancel = false;
    }

    public boolean isCancel() {
        return cancel;
    }

    public String getKey() {
        return keyText.getText();
    }

    public String getSuperColumn() {
        return superColumnText.getText();
    }

    public String getName() {
        return nameText.getText();
    }

    public String getValue() {
        return valueText.getText();
    }
}
