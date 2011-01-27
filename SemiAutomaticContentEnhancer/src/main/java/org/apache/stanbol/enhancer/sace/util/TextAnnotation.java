package org.apache.stanbol.enhancer.sace.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class TextAnnotation implements IAnnotation {

    /**
     *
     */
    private static final long serialVersionUID = 5865576334353943605L;
    private String name;
    private String creator;
    private String created;
    private String selectionContext;
    private String selectedText;
    private int startIndex;
    private int endIndex;

    private List<EntityAnnotation> entityAnnotations;
    private Hashtable<String, List<String>> attributes;

    public TextAnnotation() {
        entityAnnotations = new LinkedList<EntityAnnotation>();
        attributes = new Hashtable<String, List<String>>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getCreated() {
        return created;
    }

    public void setSelectionContext(String selectionContext) {
        this.selectionContext = selectionContext;
    }

    public String getSelectionContext() {
        return selectionContext;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }

    public String getSelectedText() {
        return selectedText;
    }

    public void addEntityAnnotation(EntityAnnotation ea) {
        entityAnnotations.add(ea);
    }

    public List<EntityAnnotation> getEntityAnnotations() {
        return entityAnnotations;
    }

    public void addAttribute(String key, String value) {
        if (attributes.containsKey(key)) {
            List<String> val = attributes.remove(key);
            val.add(value);
            attributes.put(key, val);
        } else {
            List<String> val = new LinkedList<String>();
            val.add(value);
            attributes.put(key, val);
        }
    }

    public List<String> getAttribute(String key) {
        return attributes.get(key);
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        return this;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TextAnnotation) {
            return ((TextAnnotation) o).name.equals(name);
        }
        return false;
    }

    public void retainTypeAnnotation(String type) {
        List<String> t = new LinkedList<String>();
        t.add(type);
        attributes.put("type", t);
    }

    public void clearEntityAnnotation() {
        entityAnnotations.clear();
    }

    public void clearTypeAnnotation() {
        attributes.remove("type");
    }
}
