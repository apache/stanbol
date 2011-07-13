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

public enum NamespaceEnum {

    // TODO: change the namespace as soon as STANBOL-3 defines a namespace to be used for stanbol
    enhancer("http://fise.iks-project.eu/ontology/"),
    dbpedia_ont("dbpedia-ont", "http://dbpedia.org/ontology/"),
    rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    rdfs("http://www.w3.org/2000/01/rdf-schema#"),
    dc("http://purl.org/dc/terms/"),
    skos("http://www.w3.org/2004/02/skos/core#"),
    foaf("http://xmlns.com/foaf/0.1/"),
    geonames("http://www.geonames.org/ontology#"),
    georss("http://www.georss.org/georss/"),
    geo("http://www.w3.org/2003/01/geo/wgs84_pos#"),
    nie("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#");

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
