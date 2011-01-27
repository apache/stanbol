package org.apache.stanbol.enhancer.interaction.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/*
 * Copyright 2010
 * German Research Center for Artificial Intelligence (DFKI)
 * Department of Intelligent User Interfaces
 * Germany
 *
 *     http://www.dfki.de/web/forschung/iui
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 *     Sebastian Germesin
 *     Massimo Romanelli
 *     Tilman Becker
 */

public class ResultPanel extends JPanel {

    private static final long serialVersionUID = -5932078826093367287L;

    private JComboBox resultDocuments;
    private ResultDocumentsListener resultDocumentsListener;
    private JTextArea documentArea;

    public ResultPanel () {
        super();

        init();
    }

    private void init () {
        resultDocuments = new JComboBox();
        resultDocumentsListener = new ResultDocumentsListener();
        resultDocuments.addActionListener(resultDocumentsListener);

        Box resultBox = Box.createHorizontalBox();
        resultBox.add(new JLabel("Found documents:"));
        resultBox.add(resultDocuments);

        documentArea = new JTextArea();
        documentArea.setLineWrap(true);
        documentArea.setWrapStyleWord(true);


        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, resultBox);
        add(BorderLayout.CENTER, new JScrollPane(documentArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
    }

    public void addContent (final String title, final String content) {
        Runnable runner = new Runnable () {

            @Override
            public void run() {
                documentArea.setText(content); //TODO!
            }

        };

        resultDocumentsListener.addCase(title, runner);
        resultDocuments.addItem(title);
    }

    class ResultDocumentsListener implements ActionListener {

        private Map<String, Runnable> cases = new HashMap<String, Runnable>();


        @Override
        public void actionPerformed(ActionEvent e) {
            String item = (String)((JComboBox)e.getSource()).getSelectedItem();

            Runnable runner = cases.get(item);

            if (runner != null) {
                SwingUtilities.invokeLater(runner);
            }

        }

        public void addCase (String item, Runnable runner) {
            cases.put(item, runner);
        }

        public void clear () {
            cases.clear();
        }

        public void removeCase (String item) {
            cases.remove(item);
        }

    }

}
