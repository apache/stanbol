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
package org.apache.stanbol.owl.web;

import javax.ws.rs.core.MediaType;

/**
 * Additional MIME types for knowledge representation formats.
 * 
 * @author andrea.nuzzolese
 * @author alessandro
 * 
 */
public class KRFormat extends MediaType {

    public static final String RDF_XML = "application/rdf+xml";

    public static final MediaType RDF_XML_TYPE = new MediaType("application", "rdf+xml");

    public static final String OWL_XML = "application/owl+xml";

    public static final MediaType OWL_XML_TYPE = new MediaType("application", "owl+xml");

    public static final String MANCHESTER_OWL = "application/manchester+owl";

    public static final MediaType MANCHESTER_OWL_TYPE = new MediaType("application", "manchester+xml");

    public static final String FUNCTIONAL_OWL = "application/functional+owl";

    public static final MediaType FUNCTIONAL_OWL_TYPE = new MediaType("application", "functional+xml");

    public static final String TURTLE = "application/turtle";

    public static final MediaType TURTLE_TYPE = new MediaType("application", "turtle");

    public static final String RDF_JSON = "application/rdf+json";

    public static final MediaType RDF_JSON_TYPE = new MediaType("application", "rdf+json");

}
