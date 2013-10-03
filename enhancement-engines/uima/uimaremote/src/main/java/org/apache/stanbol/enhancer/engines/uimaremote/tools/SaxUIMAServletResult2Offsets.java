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
package org.apache.stanbol.enhancer.engines.uimaremote.tools;

import java.util.ArrayList;
import java.util.List;
import org.apache.stanbol.commons.caslight.Feature;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX processor for processing UIMA Servlet result XML
 *
 * @author Mihály Héder
 */
public class SaxUIMAServletResult2Offsets extends DefaultHandler {

    private int elementCounter = 0;
    private List<FeatureStructure> fsList = new ArrayList<FeatureStructure>();
    private String sourceName;

    /**
     * Returns the Feature List built from the UIMA result.
     *
     * @return
     */
    public List<FeatureStructure> getFsList() {
        return fsList;
    }

    /**
     * Sets the source name of this processor. The source name is incorporated
     * into the given identifiers
     *
     * @param sourceName
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {

        if (!localName.equals("result")) {
            elementCounter++;
            String type = localName;

            FeatureStructure fs = new FeatureStructure(sourceName + "." + localName + "#" + elementCounter, type);
            for (int i = 0; i < attrs.getLength(); i++) {
                String name = attrs.getQName(i);
                String value = attrs.getValue(i);
                if (checkIfInteger(value)) {
                    Feature<Integer> f = new Feature(name, value);
                    fs.addFeature(f);
                } else {
                    Feature<String> f = new Feature(name, value);
                    fs.addFeature(f);
                }
            }
            fsList.add(fs);

        }
    }

    private boolean checkIfInteger(String value) {
        boolean ret = true;
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ret = false;
        }
        return ret;
    }
}
