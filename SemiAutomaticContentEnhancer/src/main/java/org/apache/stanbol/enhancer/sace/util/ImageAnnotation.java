/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
