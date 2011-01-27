package org.apache.stanbol.enhancer.sace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.stanbol.enhancer.sace.ui.SaceGUI;
import org.apache.stanbol.enhancer.sace.util.DocumentAnnotation;
import org.apache.stanbol.enhancer.sace.util.EntityAnnotation;
import org.apache.stanbol.enhancer.sace.util.IAnnotation;
import org.apache.stanbol.enhancer.sace.util.ImageAnnotation;
import org.apache.stanbol.enhancer.sace.util.TextAnnotation;
import org.apache.stanbol.enhancer.sace.util.Util;
import org.jdom.Document;
import org.jdom.Element;


public class Sace {

    private String serverUrl;
    private SaceGUI gui;
    public static final String DEFAULT_URL = "http://localhost:8080/engines";
    public static final String WINDOW_TITLE = "Stanbol Enhancer - S.A.C.E";

    public Sace() {
        try {
            this.serverUrl = getServerUrl();
            this.gui = new SaceGUI(this, WINDOW_TITLE + " (" + serverUrl + ")");
        } catch (IllegalStateException ise) {
            // user entered wrong or missing url -> shutdown
            System.err.println(ise.getMessage());
            System.exit(1);
        }
    }

    static String getServerUrl() {
        final String result = (String) JOptionPane.showInputDialog(null,
                "Please indicate the Stanbol Enhancer server URL", WINDOW_TITLE,
                JOptionPane.PLAIN_MESSAGE, null, null, DEFAULT_URL);

        if (result == null || result.trim().length() == 0) {
            throw new IllegalStateException("Missing server URL");
        }
        return result.trim();
    }

    public List<IAnnotation> annotateTextWithStanbolEnhancer(final String text) {
        // get stuff from the Stanbol Enhancer
        String result = sendTextToStanbolEnhancer(text);
        System.out.println(result);
        // transform Stanbol Enhancer annotations to SACE-annotations
        return transformRDFXMLToAnnotations(result);

    }

    public String sendTextToStanbolEnhancer(String text) {
        try {

            // Construct data
            String data = URLEncoder.encode("format", "UTF-8") + "="
                    + URLEncoder.encode("application/rdf+xml", "UTF-8");
            data += "&" + URLEncoder.encode("content", "UTF-8") + "="
                    + URLEncoder.encode(text, "UTF-8");
            // Send data
            URL url = new URL(serverUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn
                    .getOutputStream());
            wr.write(data);
            wr.flush();

            // Get the response
            String resultText = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn
                    .getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                // Process line...
                resultText += (line + "\n");
            }
            wr.close();
            rd.close();
            return resultText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<IAnnotation> transformRDFXMLToAnnotations(String rdfXml) {
        List<IAnnotation> retVal = new LinkedList<IAnnotation>();
        Document doc = Util.transformToXML(rdfXml);

        List<TextAnnotation> textAnnotations = new LinkedList<TextAnnotation>();
        List<DocumentAnnotation> documentAnnotations = new LinkedList<DocumentAnnotation>();
        List<EntityAnnotation> entityAnnotations = new LinkedList<EntityAnnotation>();

        for (Object o1 : doc.getRootElement().getChildren()) {
            Element node = (Element) o1;

            boolean isDocumentAnnotation = false;
            boolean isTextAnnotation = false;
            boolean isEntityAnnotation = false;
            for (Object o2 : node.getChildren()) {
                Element subject = (Element) o2;

                String name = subject.getName();
                String ns = subject.getNamespace().getURI();

                isDocumentAnnotation |= name.equals("doc-lang")
                        && ns.equals("http://iksproject.eu/ns/extraction/");
                isTextAnnotation |= name.equals("type")
                        && ns
                                .equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                        && subject.getAttribute("resource",
                                subject.getNamespace()).getValue().endsWith(
                                "TextAnnotation");
                isEntityAnnotation |= name.equals("type")
                        && ns
                                .equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                        && subject.getAttribute("resource",
                                subject.getNamespace()).getValue().endsWith(
                                "EntityAnnotation");
            }

            if (isDocumentAnnotation) {
                DocumentAnnotation docAnnot = new DocumentAnnotation();
                String name = node.getAttribute("about", node.getNamespace())
                        .getValue();
                docAnnot.setName(name);
                for (Object o2 : node.getChildren()) {
                    Element subject = (Element) o2;

                    if (subject.getName().equals("doc-lang")
                            && subject.getNamespace().getURI().equals(
                                    "http://iksproject.eu/ns/extraction/")) {
                        docAnnot.addAttribute("lang", subject.getText());
                    }
                    // TODO: fill with other stuff once available

                }
                documentAnnotations.add(docAnnot);
            } else if (isTextAnnotation) {
                TextAnnotation textAnnot = new TextAnnotation();
                String name = node.getAttribute("about", node.getNamespace())
                        .getValue();
                textAnnot.setName(name);

                for (Object o2 : node.getChildren()) {
                    Element subject = (Element) o2;

                    String sName = subject.getName();
                    String sNs = subject.getNamespaceURI();

                    if (sName.equals("creator")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String creator = subject.getText();
                        textAnnot.setCreator(creator);
                    } else if (sName.equals("created")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String created = subject.getText();
                        textAnnot.setCreated(created);
                    } else if (sName.equals("selection-context")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        String selectionContext = subject.getText();
                        textAnnot.setSelectionContext(selectionContext);
                    } else if (sName.equals("selected-text")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        String selectedText = subject.getText();
                        textAnnot.setSelectedText(selectedText);
                    } else if (sName.equals("type")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String type = subject.getAttribute("resource",
                                node.getNamespace()).getValue();
                        textAnnot.addAttribute("type", type);
                    } else if (sName.equals("start")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        int index = Integer.parseInt(subject.getText());
                        textAnnot.setStartIndex(index);
                    } else if (sName.equals("end")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        int index = Integer.parseInt(subject.getText());
                        textAnnot.setEndIndex(index);
                    }

                    // TODO: once they are available
                    double confidence = 0.0;
                }
                textAnnotations.add(textAnnot);
            } else if (isEntityAnnotation) {
                EntityAnnotation entityAnnot = new EntityAnnotation();
                String name = node.getAttribute("about", node.getNamespace())
                        .getValue();
                entityAnnot.setName(name);

                for (Object o2 : node.getChildren()) {
                    Element subject = (Element) o2;

                    String sName = subject.getName();
                    String sNs = subject.getNamespaceURI();

                    if (sName.equals("creator")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String creator = subject.getText();
                        entityAnnot.setCreator(creator);
                    } else if (sName.equals("created")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String created = subject.getText();
                        entityAnnot.setCreated(created);
                    } else if (sName.equals("entity-type")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        String entityType = subject.getAttribute("resource",
                                node.getNamespace()).getValue();
                        entityAnnot.addAttribute("entity-type", entityType);
                    } else if (sName.equals("entity-reference")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        String entityRef = subject.getAttribute("resource",
                                node.getNamespace()).getValue();
                        entityAnnot.addAttribute("entity-reference", entityRef);
                    } else if (sName.equals("entity-label")
                            && sNs
                                    .equals("http://enhancer.iks-project.eu/ontology/")) {
                        String entityLabel = subject.getText();
                        entityAnnot.addAttribute("entity-label", entityLabel);
                    } else if (sName.equals("relation")
                            && sNs.equals("http://purl.org/dc/terms/")) {
                        String relation = subject.getAttribute("resource",
                                node.getNamespace()).getValue();
                        entityAnnot.setRelation(relation);
                    }

                    // TODO: once they are available
                    double confidence = 0.0;
                }
                entityAnnotations.add(entityAnnot);
            }
        }

        for (TextAnnotation ta : textAnnotations) {
            String taName = ta.getName();
            for (EntityAnnotation ea : entityAnnotations) {
                if (ea.getRelation().equals(taName)) {
                    ta.addEntityAnnotation(ea);
                }
            }
            retVal.add(ta);
        }
        for (DocumentAnnotation da : documentAnnotations) {
            retVal.add(da);
        }

        return retVal;
    }

    public void submitTextAnnotationToStanbolEnhancer(TextAnnotation ta) {
        // TODO!
    }

    public void submitEntityAnnotationToStanbolEnhancer(EntityAnnotation ea) {
        // TODO!
    }

    public void submitDocumentAnnotationToStanbolEnhancer(DocumentAnnotation da) {
        // TODO!
    }

    public void submitImageAnnotationToStanbolEnhancer(ImageAnnotation ia) {
        // TODO!
    }

    public static void main(String[] args) {
        new Sace();
    }

}
