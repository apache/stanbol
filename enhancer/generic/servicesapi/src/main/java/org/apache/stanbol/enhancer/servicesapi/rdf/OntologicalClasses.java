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

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Common entity types, a.k.a. ontological classes to be used as valuation of
 * the rdf:type ({@link Properties#RDF_TYPE}) property in the metadata graph of
 * content items.
 *
 * Copy and paste the URLs of the classes in a browser to get the definition of
 * the class.
 *
 * @author ogrisel@nuxeo.com
 */
public class OntologicalClasses {

    public static final UriRef DBPEDIA_PERSON = new UriRef(
            NamespaceEnum.dbpedia_ont+"Person");

    public static final UriRef DBPEDIA_PLACE = new UriRef(
            NamespaceEnum.dbpedia_ont+"Place");

    public static final UriRef DBPEDIA_ORGANISATION = new UriRef(
            NamespaceEnum.dbpedia_ont+"Organisation");

    public static final UriRef SKOS_CONCEPT = new UriRef(
        NamespaceEnum.skos+"Concept");
    
    public static final UriRef DC_LINGUISTIC_SYSTEM = new UriRef(
        NamespaceEnum.dc+"LinguisticSystem");

    private OntologicalClasses() {
    }

}
