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
package org.apache.stanbol.enhancer.servicesapi.rdf;

/**
 * 
 */
public enum NamespaceEnum {

    /**
     * The Stanbol Enhancer namespace defining Enhancer, EnhancementEngine and
     * EnhancementChain. This is NOT the namespace of the enhancement structure.
     * As EnhancementStrucutre up to now still the old FISE namespace is used.
     */
    enhancer("http://stanbol.apache.org/ontology/enhancer/enhancer#"),
    dbpedia_ont("dbpedia-ont", "http://dbpedia.org/ontology/"),
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    dc("http://purl.org/dc/terms/"),
    skos("http://www.w3.org/2004/02/skos/core#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    foaf("http://xmlns.com/foaf/0.1/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    geonames("http://www.geonames.org/ontology#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    georss("http://www.georss.org/georss/"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    /**
    * @deprecated All none core namespaces where deprecated. Users should use
    * the NamespacePrefixService (module:
    * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
    * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
    */
    nie("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"),
    /**
     * Namespace for the Stanbol Enhancer Execution Plan ontology
     */
    ep("http://stanbol.apache.org/ontology/enhancer/executionplan#"),
    /**
     * Namespace for the Stanbol Enhancer Execution Metadata ontology
     */
    em("http://stanbol.apache.org/ontology/enhancer/executionmetadata#"),
    /**
     * The FISE namespace (1st version of the Enhancement Structure).
     * Will be replaced by the Stanbol Enhancement Structure by a future
     * release (see STANBOL-3).
     */
    fise("http://fise.iks-project.eu/ontology/"),
    /**
     * The W3C Ontology for Media Resources http://www.w3.org/TR/mediaont-10/
     * @deprecated All none core namespaces where deprecated. Users should use
     * the NamespacePrefixService (module:
     * org.apache.stanbol.commons.namespaceprefixservice) instead (see also
     * <a href="https://issues.apache.org/jira/browse/STANBOL-824">STANBOL-824)</a>
     */
    media("http://www.w3.org/ns/ma-ont#"), 
    /**
     * The namespace of the Apache Stanbol Entityhub
     */
    entityhub("http://stanbol.apache.org/ontology/entityhub/entityhub#"),
    /**
     * Namespace for Disambiguation related properties and classes (added with
     * STANBOL-1053)
     */
    dis("http://stanbol.apache.org/ontology/disambiguation/disambiguation#"), 
    /**
     * Namespace used for EnhancementProperties
     * @since 0.12.1
     */
    ehp("http://stanbol.apache.org/ontology/enhancementproperties#")
    ;
    
    String ns;
    String prefix;

    NamespaceEnum(String ns) {
        if (ns == null) {
            throw new IllegalArgumentException("The namespace MUST NOT be NULL");
        }
        this.ns = ns;
    }

    NamespaceEnum(String prefix, String ns) {
        this(ns);
        this.prefix = prefix;
    }

    public String getNamespace() {
        return ns;
    }

    public String getPrefix() {
        return prefix == null ? name() : prefix;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return ns;
    }

}
