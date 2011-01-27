package org.apache.stanbol.enhancer.sace.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class EntityAnnotation implements IAnnotation {

    /**
     *
     */
    private static final long serialVersionUID = -4729630243302760317L;
    private String name;
    private String creator;
    private String created;
    private String relation;

    private Hashtable<String, List<String>> attributes;

    public EntityAnnotation() {
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

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getRelation() {
        return relation;
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

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        return this;
    }

    public DataFlavor[] getTransferDataFlavors() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        // TODO Auto-generated method stub
        return false;
    }

    public void retainTypeAnnotation(String typeTmp) {
        List<String> newList = new LinkedList<String>();
        newList.add(typeTmp);
        attributes.put("entity-type", newList);
    }

}
