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
package org.apache.stanbol.enhancer.rdfentities.fise;

import java.util.Collection;
import java.util.Date;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.rdfentities.Rdf;
import org.apache.stanbol.enhancer.rdfentities.RdfEntity;


/**
 * This Interface represents a Stanbol Enhancer enhancement.
 * <p>
 * To create an instance of this interface use the following code
 * <code><pre>
 *  ContentItem ci;
 *     Graph graph = ci.getMetadata();
 *  RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
 *    String enhancementId = "http://wwww.example.com/iks-project/enhancer/example-enhancement";
 *    IRI enhancementNode = new IRI(enhancementId);
 *    Enhancement enhancement = factory.getProxy(enhancementNode, Enhancement.class);
 *    enhancement.setCreator("Rupert Westenthaler");
 *  enhancement.setCreated(new Date());
 *  ...
 * </pre></code>
 *
 * @author Rupert Westenthaler
 */
@Rdf(id="http://fise.iks-project.eu/ontology/Enhancement")
public interface Enhancement extends RdfEntity{

    @Rdf(id="http://purl.org/dc/terms/creator")
    IRI getCreator();
    @Rdf(id="http://purl.org/dc/terms/creator")
    void setCreator(IRI creator);

    @Rdf(id="http://purl.org/dc/terms/created")
    void setCreated(Date date);
    @Rdf(id="http://purl.org/dc/terms/created")
    Date getCreated();

//    @Rdf(id="http://purl.org/dc/terms/type")
//    void setDcType(Collection<URI> types);
    @Rdf(id="http://purl.org/dc/terms/type")
    Collection<IRI> getDcType();

    @Rdf(id="http://fise.iks-project.eu/ontology/confidence")
    Double getConfidence();
    @Rdf(id="http://fise.iks-project.eu/ontology/confidence")
    void setConfidence(Double value);

    @Rdf(id="http://fise.iks-project.eu/ontology/extracted-from")
    IRI getExtractedFrom();
    @Rdf(id="http://fise.iks-project.eu/ontology/extracted-from")
    void setExtractedFrom(IRI contentItem);

    @Rdf(id="http://purl.org/dc/terms/requires")
    Collection<Enhancement> getRequires();
//    @Rdf(id="http://purl.org/dc/terms/requires")
//    void setRequires(Collection<Enhancement> required);

    @Rdf(id="http://purl.org/dc/terms/relation")
    Collection<Enhancement> getRelations();
//    @Rdf(id="http://purl.org/dc/terms/relation")
//    void setRelation(Collection<Enhancement> related);
}
