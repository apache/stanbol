package org.apache.stanbol.enhancer.interaction.gui;

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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.apache.stanbol.enhancer.interaction.event.ClerezzaResultEvent;
import org.apache.stanbol.enhancer.interaction.event.Event;
import org.apache.stanbol.enhancer.interaction.event.EventListener;
import org.apache.stanbol.enhancer.interaction.event.EventManager;
import org.apache.stanbol.enhancer.interaction.event.NotUnderstoodEvent;
import org.apache.stanbol.enhancer.interaction.event.PlaybackAudioEvent;
import org.apache.stanbol.enhancer.interaction.event.QueryEvent;
import org.apache.stanbol.enhancer.interaction.event.RecognizedSpeechEvent;
import org.apache.stanbol.enhancer.interaction.event.RecordedAudioEvent;
import org.apache.stanbol.enhancer.interaction.event.SparqlEvent;
import org.apache.stanbol.enhancer.interaction.speech.Playback;
import org.apache.stanbol.enhancer.interaction.speech.PlaybackInProgressException;
import org.apache.stanbol.enhancer.interaction.speech.Recording;
import org.apache.stanbol.enhancer.interaction.speech.RecordingInProgressException;

public class Gui extends JFrame implements EventListener {

    private static final long serialVersionUID = -8791127586299692743L;

    private static final Dimension GUI_SIZE = new Dimension (600, 400);

    private static Icon sparqlIcon;
    private static Icon speechIcon;
    private static Icon uploadIcon;

    private JTabbedPane mainPane;

    private JTextArea writingArea;
    private JTextArea sparqlArea;

    private ConfigurationPanel configurationPanel;
    private UploadPanel uploadPanel;

    private Recording recording;
    private Playback playback;

    static {
        sparqlIcon = createImageIcon("/icons/sparql.jpg");
        speechIcon = createImageIcon("/icons/speech.png");
        uploadIcon = createImageIcon("/icons/upload.png");
    }


    public Gui (String clerezzaServerHost, int clerezzaServerPort, String username, String password) {
        super ("IKS [T5.1] -Semantic Search Engine Hackathon PROTOTYPE");

        recording = new Recording();
        playback = new Playback();

        init(clerezzaServerHost, clerezzaServerPort, username, password);
    }

    private void init (String clerezzaServerHost, int clerezzaServerPort, String username, String password) {

        setSize(GUI_SIZE);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // init writing panel //
        JPanel writingPanel = createWritingPanel();
        writingPanel.setBorder(BorderFactory.createTitledBorder("Text"));

        // init speech panel //
        JPanel speechPanel = createSpeechPanel();
        speechPanel.setBorder(BorderFactory.createTitledBorder("Speech"));

        // init SPARQL panel //
        JPanel sparqlPanel = createSPARQLPanel();
        sparqlPanel.setBorder(BorderFactory.createTitledBorder("SPARQL"));

        // init tabbed main panel //
        mainPane = new JTabbedPane();

        Box textBox = Box.createVerticalBox();
        textBox.add(writingPanel);
        textBox.add(sparqlPanel);

        Box mainBox = Box.createHorizontalBox();
        mainBox.add(textBox);
        mainBox.add(speechPanel);

        uploadPanel = new UploadPanel(clerezzaServerHost, clerezzaServerPort);

        mainPane.addTab("Upload", uploadIcon, uploadPanel);
        mainPane.addTab("Query", sparqlIcon, mainBox);

        getContentPane().setLayout(new BorderLayout());

        configurationPanel = new ConfigurationPanel(clerezzaServerHost, clerezzaServerPort, username, password);

        add(BorderLayout.NORTH, configurationPanel);
        add(BorderLayout.CENTER, mainPane);

        setVisible(true);
    }

    private JPanel createWritingPanel() {
        JPanel writingPanel = new JPanel();

        writingArea = new JTextArea (5, 100);
        writingArea.setToolTipText("Please insert a (valid) query using natural language!");
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener () {

            public void actionPerformed(ActionEvent e) {
                writingArea.setEditable(false);
                String text = writingArea.getText();
                writingArea.setText("");
                RecognizedSpeechEvent rse = new RecognizedSpeechEvent(text);
                EventManager.eventOccurred(rse);
                writingArea.setEditable(true);
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener () {

            public void actionPerformed(ActionEvent e) {
                clearButtonPressed ();
            }
        });

        Box writingButtonBox = Box.createHorizontalBox();
        writingButtonBox.add(submitButton);
        writingButtonBox.add(clearButton);

        Box sparqlWholeBox = Box.createVerticalBox();

        sparqlWholeBox.add(new JScrollPane(writingArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        sparqlWholeBox.add(writingButtonBox);

        writingPanel.setLayout(new BorderLayout());

        writingPanel.add(BorderLayout.CENTER, sparqlWholeBox);

        return writingPanel;
    }

    private JPanel createSpeechPanel() {
        JPanel speechPanel = new JPanel();
        speechPanel.setLayout(new BorderLayout());

        final JLabel pttButton = new JLabel(speechIcon);
        pttButton.setToolTipText("Push button to talk and release it when you're finished!");
        pttButton.addMouseListener(new MouseListener () {

            public void mouseClicked(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}

            public void mousePressed(MouseEvent e) {
                pttButton.setEnabled(false);
                clearButtonPressed ();
                startRecording ();
            }

            public void mouseReleased(MouseEvent e) {
                stopRecording ();
                pttButton.setEnabled(true);
            }

        });

        speechPanel.add(BorderLayout.CENTER, pttButton);

        return speechPanel;
    }

    private JPanel createSPARQLPanel() {
        JPanel sparqlPanel = new JPanel();

        sparqlArea = new JTextArea (5, 100);
        sparqlArea.setToolTipText("Please insert a (valid) SPARQL Query!");
        //TODO: sparqlArea.add
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener () {

            public void actionPerformed(ActionEvent e) {
                sparqlArea.setEditable(false);
                String sparqlQuery = sparqlArea.getText();
                sparqlArea.setText("");
                SparqlEvent se = new SparqlEvent(sparqlQuery);
                EventManager.eventOccurred(se);
                sparqlArea.setEditable(true);
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener () {

            public void actionPerformed(ActionEvent e) {
                clearButtonPressed();
            }
        });


        Box sparqlButtonBox = Box.createHorizontalBox();
        sparqlButtonBox.add(submitButton);
        sparqlButtonBox.add(clearButton);

        Box sparqlWholeBox = Box.createVerticalBox();

        sparqlWholeBox.add(new JScrollPane(sparqlArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        sparqlWholeBox.add(sparqlButtonBox);

        sparqlPanel.setLayout(new BorderLayout());

        sparqlPanel.add(BorderLayout.CENTER, sparqlWholeBox);

        return sparqlPanel;
    }

    private static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = Gui.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }

    }

    private void startRecording () {
        ExecutorService es = Executors.newSingleThreadExecutor();
        Runnable runner = new Runnable() {
            public void run () {
                try {
                    Future<byte[]> capturedAudioFuture = recording.startRecording();

                    byte[] capturedAudio = capturedAudioFuture.get();
                    RecordedAudioEvent rae = new RecordedAudioEvent(capturedAudio);
                    EventManager.eventOccurred(rae);
                } catch (RecordingInProgressException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        };
        es.execute(runner);
    }

    private void stopRecording () {
        ExecutorService es = Executors.newSingleThreadExecutor();
        Runnable runner = new Runnable() {
            public void run () {
                recording.stopRecording();
            }
        };
        es.execute(runner);
    }

    private void clearButtonPressed () {
        sparqlArea.setText("");
        writingArea.setText("");
    }

    public void eventOccurred(Event e) {
        if (e instanceof QueryEvent) {
            QueryEvent qe = (QueryEvent)e;

            this.sparqlArea.setText(qe.getSparqlQuery());
            this.writingArea.setText(qe.getTextQuery());

        }
        else if (e instanceof PlaybackAudioEvent) {
            PlaybackAudioEvent pae = (PlaybackAudioEvent)e;

            try {
                playback.playAudio(pae.getData(), true);
            } catch (PlaybackInProgressException e1) {
                e1.printStackTrace();
            }
        }
        else if (e instanceof NotUnderstoodEvent) {
            this.writingArea.setText("'I did not understand you!'");
        }
        else if (e instanceof ClerezzaResultEvent) {
            ClerezzaResultEvent cre = (ClerezzaResultEvent)e;

            ResultPanel resultPanel = new ResultPanel();

            List<String> uris = extractURLs(cre.getData());

            for (String uri : uris) {
                try {
                    String content = readFileFromURL(new URL(uri));
                    resultPanel.addContent(uri, content);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                } catch (Exception eee) {
                    //TODO
                }
            }

            mainPane.addTab("Result: " + new Date().toString(), resultPanel);
        }
    }

    private static List<String> extractURLs (String result) {
        List<String> retVal = new LinkedList<String>();

        while (true) {
            String uri = result.replaceFirst("(.*?)<binding name=\"content\"><uri>(.*?)</uri></binding>(.*)", "$2");

            if (uri.equals(result)) {
                break;
            }
            else {
                if (!retVal.contains(uri))
                    retVal.add(uri);
                result = result.replaceFirst("(.*?)<binding name=\"content\"><uri>(.*?)</uri></binding>(.*?)", "$1$3");
            }
        }
        return retVal;
    }

    private static String readFileFromURL (URL url) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        String returnValue = "";

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            returnValue += (inputLine + "\n");
        }
        in.close();

        return returnValue;
    }

}
