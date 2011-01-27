package org.apache.stanbol.enhancer.sace.util;

import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ImageAnnotation implements IAnnotation {


    /**
     *
     */
    private static final long serialVersionUID = 6917081289144409008L;
    private String filename;
    private Rectangle region;
    private IAnnotation annotation;

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        // TODO Auto-generated method stub
        return false;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setRegion(Rectangle region) {
        this.region = region;
    }

    public Rectangle getRegion() {
        return region;
    }

    public void setAnnotation(IAnnotation annotation) {
        this.annotation = annotation;
    }

    public IAnnotation getAnnotation() {
        return annotation;
    }

}
