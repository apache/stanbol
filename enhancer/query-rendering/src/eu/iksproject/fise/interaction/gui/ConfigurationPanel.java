package org.apache.stanbol.enhancer.interaction.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.stanbol.enhancer.interaction.event.ClerezzaServerInfoChangedEvent;
import org.apache.stanbol.enhancer.interaction.event.EventManager;

public class ConfigurationPanel extends JPanel {

    private static final long serialVersionUID = -1931789642978947740L;

    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JTextField passwordField;

    public ConfigurationPanel (String server, int port, String username, String password) {
        final JButton submitButton = new JButton("Update");
        submitButton.setEnabled(false);
        submitButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                submitButton.setEnabled(false);
                ClerezzaServerInfoChangedEvent csice = new ClerezzaServerInfoChangedEvent(getServer(), getPort(), getUsername(), getPassword());
                EventManager.eventOccurred(csice);

            }

        });

        serverField = new JTextField(server);
        serverField.getDocument().addDocumentListener(new DocumentListener () {
            @Override
            public void changedUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
        });

        portField = new JTextField("" + port);
        portField.getDocument().addDocumentListener(new DocumentListener () {
            @Override
            public void changedUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
        });

        usernameField = new JTextField(username);
        usernameField.getDocument().addDocumentListener(new DocumentListener () {
            @Override
            public void changedUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
        });

        passwordField = new JTextField(password);
        passwordField.getDocument().addDocumentListener(new DocumentListener () {
            @Override
            public void changedUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                submitButton.setEnabled(true);
            }
        });

        Box bh = Box.createHorizontalBox();
        bh.add(new JLabel("Host: "));
        bh.add(serverField);
        bh.add(new JLabel("Port: "));
        bh.add(portField);
        bh.add(new JLabel("Name: "));
        bh.add(usernameField);
        bh.add(new JLabel("PW: "));
        bh.add(passwordField);

        Box b = Box.createVerticalBox();
        b.add(new JLabel("Clerezza Server Configuration"));
        b.add(bh);

        Box bv = Box.createHorizontalBox();
        bv.add(b);
        bv.add(submitButton);

        add(bv);
    }

    private String getServer () {
        return serverField.getText();
    }

    private int getPort () {
        return Integer.parseInt(portField.getText());
    }

    private String getUsername () {
        return usernameField.getText();
    }

    private String getPassword () {
        return passwordField.getText();
    }

}
