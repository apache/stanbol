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
package org.apache.stanbol.commons.web.base.format;

import javax.ws.rs.core.MediaType;

/**
 * Additional MIME types for knowledge representation formats.
 * 
 * @author andrea.nuzzolese
 * @author alexdma
 * 
 */
public class KRFormat extends MediaType {

    /**
     * "application/rdf+xml"
     */
    public static final String RDF_XML = "application/rdf+xml";

    /**
     * "application/rdf+xml"
     */
    public static final MediaType RDF_XML_TYPE = new MediaType("application", "rdf+xml");

    /**
     * "application/owl+xml"
     */
    public static final String OWL_XML = "application/owl+xml";

    /**
     * "application/owl+xml"
     */
    public static final MediaType OWL_XML_TYPE = new MediaType("application", "owl+xml");

    /**
     * "text/owl-manchester"
     */
    public static final String MANCHESTER_OWL = "text/owl-manchester";

    /**
     * "text/owl-manchester"
     */
    public static final MediaType MANCHESTER_OWL_TYPE = new MediaType("text", "owl-manchester");

    /**
     * "text/owl-functional"
     */
    public static final String FUNCTIONAL_OWL = "text/owl-functional";

    /**
     * "text/owl-functional"
     */
    public static final MediaType FUNCTIONAL_OWL_TYPE = new MediaType("text", "owl-functional");

    /**
     * "application/turtle"
     */
    public static final String TURTLE = "application/turtle";

    /**
     * "application/turtle"
     */
    public static final MediaType TURTLE_TYPE = new MediaType("application", "turtle");

    /**
     * "application/rdf+json"
     */
    public static final String RDF_JSON = "application/rdf+json";

    /**
     * "application/rdf+json"
     */
    public static final MediaType RDF_JSON_TYPE = new MediaType("application", "rdf+json");

}
