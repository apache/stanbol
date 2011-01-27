package org.apache.stanbol.enhancer.interaction.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.event.UploadFileEvent;


public class UploadPanel extends JPanel {

    private static final long serialVersionUID = 4946503829842270168L;

    private String baseAddress;

    public UploadPanel (String server, int port) {
        setServer(server, port);

        init();
    }

    public void setServer (String server, int port) {
        this.baseAddress = server + ":" + port;
    }

    private void init () {
        final JTextField uploadFilenameField = new JTextField("<please choose a file to upload>");
        uploadFilenameField.setEditable(false);

        final JTextField uriField = new JTextField("<please set a URI for uploading>");

        final JButton browseStructureButton = new JButton ("Browse");
        final JButton uploadButton = new JButton ("Upload");

        browseStructureButton.addActionListener(new ActionListener () {

            @Override
            public void actionPerformed(ActionEvent e) {

                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int returnVal = fc.showOpenDialog(UploadPanel.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    uploadButton.setEnabled(true);
                    File file = fc.getSelectedFile();
                    uploadFilenameField.setText(file.getAbsolutePath());
                }
            }
        });


        uploadButton.setEnabled(false);
        uploadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                uploadButton.setEnabled(false);

                UploadFileEvent ufe = new UploadFileEvent(uploadFilenameField.getText(), uriField.getText());

                EventManager.eventOccurred(ufe);
                uploadFilenameField.setText("<please choose a file to upload>");
            }

        });


        Box hb = Box.createHorizontalBox();

        hb.add(new JLabel("File:"));
        hb.add(uploadFilenameField);
        hb.add(browseStructureButton);

        Box hb2 = Box.createHorizontalBox();
        hb2.add(new JLabel("URI:"));
        hb2.add(uriField);
        hb2.add(uploadButton);

        Box vb = Box.createVerticalBox();

        vb.add(hb);
        vb.add(hb2);

        setLayout(new GridLayout(5, 1));
        add(new JPanel());
        add(new JPanel());
        add(vb);
    }


}
