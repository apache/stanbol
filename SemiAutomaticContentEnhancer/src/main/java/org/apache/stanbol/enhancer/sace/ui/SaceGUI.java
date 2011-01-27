package org.apache.stanbol.enhancer.sace.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.stanbol.enhancer.sace.Sace;
import org.apache.stanbol.enhancer.sace.util.DocumentAnnotation;
import org.apache.stanbol.enhancer.sace.util.EntityAnnotation;
import org.apache.stanbol.enhancer.sace.util.IAnnotation;
import org.apache.stanbol.enhancer.sace.util.ImageAnnotation;
import org.apache.stanbol.enhancer.sace.util.TextAnnotation;


public class SaceGUI extends JFrame {

    private static final long serialVersionUID = -1599015661183868734L;

    private Sace sace;

    private JButton newButton;
    private JButton annotateContentButton;

    private SacePanelText sacePaneltxt;
    private SacePanelImage sacePanelimg;

    public SaceGUI(Sace sace, String windowTitle) {
        super(windowTitle);

        this.sace = sace;

        init();
    }

    private void init() {
        setSize(700, 400);
        setLocationRelativeTo(null);

        sacePaneltxt = new SacePanelText(this);
        sacePanelimg = new SacePanelImage(this);

        newButton = new JButton("Create new content");
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                annotateContentButton.setEnabled(true);
                sacePaneltxt.clear();
                sacePanelimg.clear();
            }

        });

        annotateContentButton = new JButton("Send content to Stanbol Enhancer");
        annotateContentButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final String text = sacePaneltxt.getText();
                annotateContentButton.setEnabled(false);

                annotateTextWithStanbolEnhancer(text);
            }

        });

        JPanel sace = new JPanel();

        sace.setLayout(new GridLayout(1, 2));

        sace.add(sacePaneltxt);
        sace.add(sacePanelimg);

        getContentPane().add(BorderLayout.NORTH, newButton);
        getContentPane().add(BorderLayout.CENTER, sace);
        getContentPane().add(BorderLayout.SOUTH, annotateContentButton);

        setVisible(true);
    }

    public void annotateTextWithStanbolEnhancer(final String text) {
        Runnable runner = new Runnable() {
            public void run() {

                List<IAnnotation> result = sace.annotateTextWithStanbolEnhancer(text);
                // add the annotations to SACE
                for (IAnnotation annot : result) {
                    if (annot instanceof DocumentAnnotation) {
                        sacePaneltxt
                                .addDocumentAnnotation((DocumentAnnotation) annot);
                    } else if (annot instanceof TextAnnotation) {
                        sacePaneltxt.addTextAnnotation((TextAnnotation) annot);
                    }
                }
                sacePaneltxt.finalizeText();
            }
        };
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(runner);
    }

    public void submitTextAnnotationToStanbolEnhancer(TextAnnotation annotation) {
        Runnable runner = new Runnable() {
            public void run() {
                // TODO
                System.err.println("TODO: user wants to send text annotations to Stanbol Enhancer!");
            }
        };
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(runner);
    }

    public void submitDocumentAnnotationToStanbolEnhancer(DocumentAnnotation annotation) {
        Runnable runner = new Runnable() {
            public void run() {
                // TODO
                System.err.println("TODO: user wants to send document annotations to Stanbol Enhancer!");
            }
        };
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(runner);
    }

    public void submitEntityAnnotationToStanbolEnhancer(EntityAnnotation annotation) {
        Runnable runner = new Runnable() {
            public void run() {
                // TODO
                System.err.println("TODO: user wants to send entity annotations to Stanbol Enhancer!");
            }
        };
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(runner);
    }

    public void submitImageAnnotationToStanbolEnhancer(ImageAnnotation annotation) {
        Runnable runner = new Runnable() {
            public void run() {
                // TODO
                System.err.println("TODO: user wants to send image annotations to Stanbol Enhancer!");
            }
        };
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(runner);

    }

}
