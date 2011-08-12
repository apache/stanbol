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
package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;

/**
 * This interface aims to enable different implementations to annotate external RDF data with CMS Vocabulary
 * annotations defined in {@link CMSAdapterVocabulary}.
 * 
 * @author suat
 * 
 */
public interface RDFBridge {
    /**
     * Annotates a {@link Graph} with the properties defined in {@link CMSAdapterVocabulary}. The annotated
     * graph is expected to contain annotations indicating CMS objects to be created or updated. Furthermore,
     * those CMS objects can have parent or property annotations.
     * <p>
     * In the following RDF, bold resources show possible CMS Vocabulary annotations over an external RDF
     * data.
     * <p>
     * 
     * <pre>
     * &lt;rdf:RDF
     *  xml:base="http://www.example.org#"
     *  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     *  xmlns:foaf="http://xmlns.com/foaf/0.1/"
     *  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
     *  xmlns:cms="http://org.apache.stanbol/cms/"><br>
     * &lt;rdf:Description rdf:about="#TomHanks">
     *  &lt;rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
     *  &lt;foaf:givenname rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Tom&lt;/foaf:givenname>
     *  &lt;foaf:homepage rdf:datatype="http://www.w3.org/2001/XMLSchema#string">http://www.tomhanks-online.com&lt;/foaf:homepage>
     *  &lt;rdfs:comment rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Famous actor&lt;/rdfs:comment>
     *  <b>&lt;rdf:type rdf:resource="http://org.apache.stanbol/cms/CMSObject"/>
     *  &lt;cms:name rdf:datatype="http://www.w3.org/2001/XMLSchema#string">TomHanks&lt;/cms:name>
     *  &lt;cms:hasProperty rdf:resource="#homepageProperty"/></b>
     * &lt;/rdf:Description>
     * 
     * &lt;rdf:Description rdf:about="#ColinHanks">
     *  &lt;rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
     *  &lt;foaf:givenname rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Colin&lt;/foaf:givenname>
     *  &lt;foaf:homepage rdf:datatype="http://www.w3.org/2001/XMLSchema#string">http://colin-hanks.net/&lt;/foaf:homepage>
     *  &lt;rdfs:comment rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Son of Tom Hanks&lt;/rdfs:comment>
     *  <b>&lt;rdf:type rdf:resource="http://org.apache.stanbol/cms/CMSObject"/>
     *  &lt;cms:parentRef rdf:resource="#TomHanks"/>
     *  &lt;cms:name rdf:datatype="http://www.w3.org/2001/XMLSchema#string">son&lt;/cms:name>
     *  &lt;cms:hasProperty rdf:resource="#homepageProperty"/></b>
     * &lt;/rdf:Description>
     * 
     * <b>&lt;rdf:Description rdf:about="#homepageProperty">
     *  &lt;cms:propertyName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">hasHomepage&lt;/cms:propertyName>
     *  &lt;cms:propertyURI rdf:resource="http://xmlns.com/foaf/0.1/homepage"/>
     * &lt;/rdf:Description></b>
     * &lt;/rdf:RDF>
     * </pre>
     * 
     * @param rawRDF
     *            {@link Graph} instance to be annotated
     * @return annotated {@link MGraph}.
     */
    MGraph annotateGraph(Graph rawRDF);
}
