package org.apache.stanbol.enhancer.sace.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;

public class DocumentAnnotation implements IAnnotation {

    /**
     *
     */
    private static final long serialVersionUID = 1461648698204231096L;

    private String name;

    private Hashtable<String, String> attributes;

    public DocumentAnnotation () {
        attributes = new Hashtable<String, String>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public Set<String> listAttributeKeys () {
        return attributes.keySet();
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


}
