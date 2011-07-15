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
