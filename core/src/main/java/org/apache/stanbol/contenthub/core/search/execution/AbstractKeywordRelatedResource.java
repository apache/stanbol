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

package org.apache.stanbol.contenthub.core.search.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.contenthub.core.utils.Filters;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author cihan
 * 
 */
public abstract class AbstractKeywordRelatedResource extends AbstractScored {

    protected SearchContextFactoryImpl factory;

    AbstractKeywordRelatedResource(Node n,
                                   EnhGraph g,
                                   Double weight,
                                   Double score,
                                   Keyword relatedKeyword,
                                   SearchContextFactoryImpl factory) {
        super(n, g, weight, score);
        this.factory = factory;
        addRelatedKeyword(relatedKeyword);

    }

    private void addRelatedKeyword(Keyword relatedKeyword) {
        this.addProperty(SearchVocabulary.RELATED_KEYWORD,
            factory.getKeyword(factory.getKeywordURI(relatedKeyword)));
    }

    public List<Keyword> getRelatedKeywords() {
        List<Keyword> keywords = new ArrayList<Keyword>();
        for (RDFNode node : this.listPropertyValues(SearchVocabulary.RELATED_KEYWORD).toList()) {
            keywords.add(factory.getKeyword(node.asResource().getURI()));
        }
        return Collections.unmodifiableList(keywords);
    }

    public List<ClassResource> getRelatedClasses() {
        List<ClassResource> resources = new ArrayList<ClassResource>();
        for (Resource res : this.getModel().listResourcesWithProperty(SearchVocabulary.RELATED_CLASS, this)
                .filterKeep(Filters.CLASS_RESOURCE_FILTER).toList()) {
            resources.add(factory.getClassResource(res.getPropertyResourceValue(SearchVocabulary.CLASS_URI)
                    .getURI()));
        }
        return Collections.unmodifiableList(resources);
    }

    public List<IndividualResource> getRelatedIndividuals() {
        List<IndividualResource> resources = new ArrayList<IndividualResource>();
        for (Resource res : this.getModel()
                .listResourcesWithProperty(SearchVocabulary.RELATED_INDIVIDUAL, this)
                .filterKeep(Filters.INDIVIDUAL_RESOURCE_FILTER).toList()) {
            resources.add(factory.getIndividualResource(res.getPropertyResourceValue(
                SearchVocabulary.INDIVIDUAL_URI).getURI()));

        }
        return Collections.unmodifiableList(resources);
    }

    // FIXME if context factory does not manage this class/individual resource
    // then they need to be created

    public void addRelatedClass(ClassResource classResource) {
        this.addProperty(SearchVocabulary.RELATED_CLASS,
            factory.getClassResource(classResource.getClassURI()));

    }

    public void addRelatedIndividual(IndividualResource individualResource) {
        this.addProperty(SearchVocabulary.RELATED_INDIVIDUAL,
            factory.getIndividualResource(individualResource.getIndividualURI()));

    }

    public String getDereferenceableURI() {
        return this.getPropertyValue(SearchVocabulary.DEREFENCEABLE_URI).asLiteral().toString();
    }

    protected void setDereferenceableURI(String uri) {
        this.addProperty(SearchVocabulary.DEREFENCEABLE_URI, uri);
    }
}
