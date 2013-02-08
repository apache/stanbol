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

import java.util.List;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a client for an UIMA Simple Servlet, accessed trough HTTP Rest.
 *
 * @author Mihály Héder <mihaly.heder@sztaki.hu>
 */
public class UIMASimpleServletClient {

    
    UIMAServletClient usc;

    String uri;
    String sourceName;
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    public UIMASimpleServletClient() {
        this.usc = new UIMAServletClient();
    }   

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    

    /**
     * Processes an String
     * @param xec
     */
    public List<FeatureStructure> process(String sofaString) {
        List<FeatureStructure> fsList = usc.getFSList(uri, sourceName, sofaString);
        //the FeatureSetLists is a Map so this move simply deletes the old list:
        return fsList;
    }

    /**
     * Sets the source name of this processor
     * @param sourceName
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Returns the source name of this processor
     * @return
     */
    public String getSourceName() {
        return sourceName;
    }
}
