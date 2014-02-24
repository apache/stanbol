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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.apache.clerezza.rdf.core.UriRef;


/**
 * Ideally this should be a dereferenceable ontology on the web. Given such 
 * an ontology a class of constant (similar to this) can be generated with
 * the org.apache.clerezza:maven-ontologies-plugin
 */
public class Ontology {
    /**
     * Resources of this type can be dereferenced and will return a description
     * of the resource of which the IRI is specified in the "iri" query parameter.
     * 
     */
    public static final UriRef ResourceResolver = new UriRef("http://example.org/service-description${symbol_pound}ResourceResolver");
    
    /**
     * Point to the resource resolved by the subject.
     */
    public static final UriRef describes = new UriRef("http://example.org/service-description${symbol_pound}describes");
    
    /**
     * The description of a Request in the log.
     */
    public static final UriRef LoggedRequest = new UriRef("http://example.org/service-description${symbol_pound}LoggedRequest");
    
    /**
     * The User Agent performing the requested described by the subject.
     */
    public static final UriRef userAgent = new UriRef("http://example.org/service-description${symbol_pound}userAgent");
    
    /**
     * The Entity of which a description was requested in the request
     * described by the subject.
     */
    public static final UriRef requestedEntity = new UriRef("http://example.org/service-description${symbol_pound}requestedEntity");
}
