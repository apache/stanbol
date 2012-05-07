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

public enum NamespaceEnum {

    // TODO: change the namespace as soon as STANBOL-3 defines a namespace to be used for stanbol
    enhancer("http://fise.iks-project.eu/ontology/"),
    @Deprecated
    rickModel("http://www.iks-project.eu/ontology/rick/model/"),
    @Deprecated
    rickQuery("http://www.iks-project.eu/ontology/rick/query/"),
    entityhub("http://stanbol.apache.org/ontology/entityhub/entityhub#"),
    /**
     * The namespace used by the Entityhub to define query related concepts
     * e.g. the full text search field, semantic context field, result score ...
     */
    entityhubQuery("entityhub-query","http://stanbol.apache.org/ontology/entityhub/query#"),
    
    atom("http://www.w3.org/2005/Atom"),
    bio("dc-bio","http://purl.org/vocab/bio/0.1/"),
    cc("http://creativecommons.org/ns#"),
    dcElements("dc-elements","http://purl.org/dc/elements/1.1/"),
    dcTerms("dc","http://purl.org/dc/terms/"),
    dbpediaOnt("dbp-ont","http://dbpedia.org/ontology/"),
    dbpediaProp("dbp-prop","http://dbpedia.org/property/"),
    cmisRa("cmis-ra","http://docs.oasis-open.org/ns/cmis/restatom/200908/"),
    cmis("http://docs.oasis-open.org/ns/cmis/core/200908/"),
    foaf("http://xmlns.com/foaf/0.1/"),
    goodRelations("gr","http://purl.org/goodrelations/v1#"),
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    geonames("http://www.geonames.org/ontology#"),
    georss("http://www.georss.org/georss/"),
    jcr("jcr","http://www.jcp.org/jcr/1.0/"),
    jcrSv("jcr-sv","http://www.jcp.org/jcr/sv/1.0/"),
    jcrNt("jcr-nt","http://www.jcp.org/jcr/nt/1.0/"),
    jcrMix("jcr-mix","http://www.jcp.org/jcr/mix/1.0/"),
    nie("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#"),
    nfo("http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#"),
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    owl("http://www.w3.org/2002/07/owl#"),
    rss("http://purl.org/rss/1.0/"),
    schema("http://schema.org/"),
    sioc("http://rdfs.org/sioc/ns#"),
    siocTypes("sioc-types","http://rdfs.org/sioc/types#"),
    skos("http://www.w3.org/2004/02/skos/core#"),
    swrc("http://swrc.ontoware.org/ontology#"),
    vCal("http://www.w3.org/2002/12/cal#"),
    vCard("http://www.w3.org/2001/vcard-rdf/3.0#"),
    xml("http://www.w3.org/XML/1998/namespace#"),
    xsi("http://www.w3.org/2001/XMLSchema-instance#"),
    xsd("http://www.w3.org/2001/XMLSchema#"),
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
        return ns;
    }

}
