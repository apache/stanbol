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
package org.apache.stanbol.contenthub.servicesapi;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Class keeping the constants that are used in various places of Contenthub.
 * 
 * @author anil.sinaci
 * 
 */
public class Constants {

    /**
     * Default encoding
     */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * The URI of the global enhancement graph. All enhancements are stored in this graph.
     */
    public static final String ENHANCEMENTS_GRAPH_URI = "org.apache.stanbol.enhancer.standalone.store.enhancements";

    /**
     * The URI of ???
     */
    public static final String ENHANCER_ENTITIY_CACHE_GRAPH_URI = "enhancerEntityCache";

    public static final String[] RESERVED_GRAPH_URIs = {ENHANCER_ENTITIY_CACHE_GRAPH_URI,
                                                        ENHANCEMENTS_GRAPH_URI};

    public static boolean isGraphReserved(String graphURI) {
        for (String uri : RESERVED_GRAPH_URIs) {
            if (uri.equals(graphURI)) return true;
        }
        return false;
    }

    public static final String SEARCH_URI = "http://stanbol.apache.org/contenthub/search/";
    
    /**
     * Represent the RDF type of CMS Object in the content management system
     */
    public static final UriRef CMS_OBJECT = new UriRef("http://www.apache.org/stanbol/cms#CMSObject");
    
    /**
     * Represents a reference to name of a CMS Object
     */
    public static final UriRef CMS_OBJECT_NAME = new UriRef("http://www.apache.org/stanbol/cms#name");
}
