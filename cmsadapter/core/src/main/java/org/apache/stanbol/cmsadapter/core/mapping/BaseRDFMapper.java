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
package org.apache.stanbol.cmsadapter.core.mapping;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;

/**
 * This class contains common methods to be used in {@link RDFMapper} implementations
 * 
 * @author suat
 * 
 */
public class BaseRDFMapper {
    /**
     * Obtains the name for the CMS object based on the RDF data provided. If
     * {@link CMSAdapterVocabulary#CMS_OBJECT_NAME} assertion is already provided, its value is returned;
     * otherwise the local name is extracted from the URI given.
     * 
     * @param subject
     *            {@link NonLiteral} representing the URI of the CMS object resource
     * @param graph
     *            {@link MGraph} holding the resources
     * @return the name for the CMS object to be created/updated in the repository
     */
    protected String getObjectName(NonLiteral subject, MGraph graph) {
        String objectName = RDFBridgeHelper.getResourceStringValue(subject,
            CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        if (objectName.contentEquals("")) {
            return RDFBridgeHelper.extractLocalNameFromURI(subject);
        } else {
            return objectName;
        }
    }

    /**
     * Obtains the path for the CMS object based on the name of the object and RDF provided. If
     * {@link CMSAdapterVocabulary#CMS_OBJECT_PATH} assertion is already provided, its value is returned;
     * otherwise its name is returned together with a preceding "/" character. This means CMS object will be
     * searched under the root path
     * 
     * @param subject
     *            {@link NonLiteral} representing the URI of the CMS object resource
     * @param name
     *            name of the CMS object to be created in the repository
     * @param graph
     *            {@link MGraph} holding the resource
     * @return the path for the CMS object to be created/updated in the repository
     */
    protected String getObjectPath(NonLiteral subject, String name, MGraph graph) {
        String objectPath = RDFBridgeHelper.getResourceStringValue(subject,
            CMSAdapterVocabulary.CMS_OBJECT_PATH, graph);
        if (objectPath.contentEquals("")) {
            return "/" + name;
        } else {
            return objectPath;
        }
    }
}
