package org.apache.stanbol.enhancer.sace.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.stanbol.enhancer.sace.util.DocumentAnnotation;
import org.apache.stanbol.enhancer.sace.util.EntityAnnotation;
import org.apache.stanbol.enhancer.sace.util.TextAnnotation;


public class SacePanelText extends JPanel {

    private static final long serialVersionUID = 3407471836299691149L;

    private SaceGUI saceGUI;

    private List<TextAnnotation> textAnnotations;
    private List<DocumentAnnotation> docAnnotations;

    private JTextArea editor;
    private Highlighter highlighter;
    private Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter();

    public SacePanelText(SaceGUI gui) {
        super();
        saceGUI = gui;

        init();
    }

    public void clear() {
        editor.getHighlighter().removeAllHighlights();
        editor.setText("");
        textAnnotations.clear();
        docAnnotations.clear();
    }

    private void init() {
        setLayout(new BorderLayout());
        textAnnotations = new LinkedList<TextAnnotation>();
        docAnnotations = new LinkedList<DocumentAnnotation>();

        editor = new JTextArea();
        editor.setDisabledTextColor(editor.getForeground());
        editor.setLineWrap(true);
        editor.setSelectionColor(editor.getBackground());
        editor.setWrapStyleWord(true);
        highlighter = editor.getHighlighter();

        editor.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.isMetaDown()) {
                    int index = editor.viewToModel(e.getPoint());
                    if (index >= 0 && index < editor.getText().length()) {
                        // check if there are text-annotations!
                        TextAnnotation annot = findAnnotationForIndex(index);

                        if (annot != null) {
                            // open the popup menu
                            PopupMenu pm = getPopupMenuForTextAnnotation(annot);
                            pm.show(editor, e.getX(), e.getY());
                        }
                    } else {
                        // present document annotations if available!
                        if (!docAnnotations.isEmpty()) {
                            // open the popup menu
                            PopupMenu pm = getPopupMenuForDocumentAnnotations(docAnnotations);
                            pm.show(editor, e.getX(), e.getY());
                        }
                    }
                }
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger() && !e.isMetaDown()) {
                    int index = editor.viewToModel(e.getPoint());
                    if (index >= 0 && index < editor.getText().length()) {
                        // check if there are text-annotations!
                        TextAnnotation annot = findAnnotationForIndex(index);

                        if (annot != null) {
                            ((MyTransferHandler) editor.getTransferHandler())
                                    .exportAsDrag(editor, e,
                                            TransferHandler.COPY);
                        }
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
            }
        });

        editor.setDragEnabled(true);
        editor.setTransferHandler(new MyTransferHandler());

        // DEBUG //
        editor
                .setText("In the picture on the right, you can see the line-up of the German football team. This picture seems to be an older version as Michael Ballack is not able to participate this year's World Cup championship, due to an injury.");

        add(BorderLayout.CENTER, new JScrollPane(editor,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    }

    private TextAnnotation findAnnotationForIndex(int index) {
        for (TextAnnotation ta : textAnnotations) {
            if (ta.getStartIndex() <= index && index <= ta.getEndIndex())
                return ta;
        }
        return null;
    }

    private PopupMenu getPopupMenuForTextAnnotation(
            final TextAnnotation annotation) {

        final PopupMenu pm = new PopupMenu();
        MenuItem mi = new MenuItem("Please choose one annotation for '"
                + annotation.getSelectedText() + "'!");
        mi.setEnabled(false);
        pm.add(mi);
        pm.addSeparator();

        String creatorTA = annotation.getCreator().replaceFirst(".*\\.(.*)$",
                "$1");
        if (annotation.getAttribute("type") != null) {
            for (String type : annotation.getAttribute("type")) {
                final String typeTmp = type;
                if (!type.endsWith("Thing")) {
                    String label = type.replaceFirst(".*\\/(.*)$", "$1")
                            + " <-- " + creatorTA;
                    boolean alreadyThere = false;
                    for (int i = 0; i < pm.getItemCount(); i++)
                        alreadyThere |= (pm.getItem(i).getLabel().equals(label));

                    if (!alreadyThere) {
                        mi = new MenuItem(label);
                        mi.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                SacePanelText.this.remove(pm);
                                retainTextTypeAnnotation(annotation, typeTmp);
                                submitAnnotationToStanbolEnhancer(annotation);
                            }
                        });
                        pm.add(mi);
                    }
                }
            }
        }

        for (int j = 0; j < annotation.getEntityAnnotations().size(); j++) {
            final EntityAnnotation ea = annotation.getEntityAnnotations()
                    .get(j);
            String creatorEA = ea.getCreator().replaceFirst(".*\\.(.*)$", "$1");
            for (String type : ea.getAttribute("entity-type")) {
                final String typeTmp = type;
                if (!type.endsWith("Thing")) {
                    String label = type.replaceFirst(".*\\/(.*)$", "$1")
                            + " <-- " + creatorEA;
                    boolean alreadyThere = false;
                    for (int i = 0; i < pm.getItemCount(); i++)
                        alreadyThere |= (pm.getItem(i).getLabel().equals(label));

                    if (!alreadyThere) {
                        mi = new MenuItem(label);
                        mi.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                SacePanelText.this.remove(pm);
                                retainTextEntityTypeAnnotation(annotation, ea,
                                        typeTmp);
                                submitAnnotationToStanbolEnhancer(annotation);
                            }
                        });
                        pm.add(mi);
                    }
                }
            }
        }
        this.add(pm);
        return pm;
    }

    private void retainTextTypeAnnotation(TextAnnotation textAnnot, String type) {
        textAnnot.clearEntityAnnotation();
        textAnnot.retainTypeAnnotation(type);
    }

    private void retainTextEntityTypeAnnotation(TextAnnotation textAnnot,
            EntityAnnotation ea, String entityType) {
        textAnnot.clearTypeAnnotation();
        ea.retainTypeAnnotation(entityType);
    }

    private PopupMenu getPopupMenuForDocumentAnnotations(
            List<DocumentAnnotation> docAnnotations) {
        final PopupMenu pm = new PopupMenu();
        MenuItem mi = new MenuItem(
                "Please choose one annotation for the document!");
        mi.setEnabled(false);
        pm.add(mi);
        pm.addSeparator();

        for (DocumentAnnotation da : docAnnotations) {

            mi = new MenuItem(da.getAttribute("lang")
                    + " <-- LanguageIdentifier");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SacePanelText.this.remove(pm);
                    submitAnnotationToStanbolEnhancer(null);
                }
            });
            pm.add(mi);
        }
        this.add(pm);
        return pm;
    }

    private void submitAnnotationToStanbolEnhancer(TextAnnotation ta) {
        saceGUI.submitTextAnnotationToStanbolEnhancer(ta);
    }

    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public MyHighlightPainter() {
            super(new Color(65, 137, 77));
            // super(Color.red);
        }
    }

    public void addDocumentAnnotation(DocumentAnnotation annot) {
        docAnnotations.add(annot);
    }

    public void addTextAnnotation(TextAnnotation ta) {
        textAnnotations.add(ta);

        try {
            highlighter.addHighlight(ta.getStartIndex(), ta.getEndIndex(),
                    this.myHighlightPainter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getText() {
        return editor.getText();
    }

    class MyTransferHandler extends TransferHandler {

        /**
         *
         */
        private static final long serialVersionUID = -7247031090902543381L;

        @Override
        public Transferable createTransferable(JComponent c) {

            if (c instanceof JTextArea) {
                JTextArea jta = (JTextArea) c;

                int index = editor.viewToModel(jta.getMousePosition());
                if (index >= 0 && index < editor.getText().length()) {
                    // check if there are text-annotations!
                    TextAnnotation annot = findAnnotationForIndex(index);

                    if (annot != null) {
                        return annot;
                    }
                }
            }
            return null;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

    }

    public void finalizeText() {
    }

}
