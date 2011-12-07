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
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ExternalResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of {@link Keyword}.
 * 
 * @author cihan
 * 
 */
public class KeywordImpl extends AbstractScored implements Keyword {

    private String source;

    protected final SearchContextFactoryImpl factory;

    KeywordImpl(Node n,
                EnhGraph g,
                String keyword,
                Double weight,
                Double score,
                SearchContextFactoryImpl factory) {
        super(n, g, weight, score);
        this.factory = factory;
        this.addLiteral(SearchVocabulary.KEYWORD_STRING, keyword);
        this.addProperty(RDF.type, SearchVocabulary.KEYWORD);

    }

    @Override
    public String getKeyword() {
        return this.getPropertyValue(SearchVocabulary.KEYWORD_STRING).asLiteral().getLexicalForm();
    }

    @Override
    public QueryKeyword getRelatedQueryKeyword() {
        return factory.getQueryKeyword(getPropertyValue(SearchVocabulary.RELATED_QUERY_KEYWORD).asResource()
                .getURI());

    }

    @Override
    public List<ClassResource> getRelatedClassResources() {
        List<ClassResource> resources = new ArrayList<ClassResource>();
        for (Resource res : this.getModel().listResourcesWithProperty(SearchVocabulary.RELATED_KEYWORD, this)
                .filterKeep(Filters.CLASS_RESOURCE_FILTER).toList()) {
            resources.add(factory.getClassResource(res.getPropertyResourceValue(SearchVocabulary.CLASS_URI)
                    .getURI()));
        }
        return Collections.unmodifiableList(resources);
    }

    @Override
    public List<IndividualResource> getRelatedIndividualResources() {
        List<IndividualResource> resources = new ArrayList<IndividualResource>();
        for (Resource res : this.getModel().listResourcesWithProperty(SearchVocabulary.RELATED_KEYWORD, this)
                .filterKeep(Filters.INDIVIDUAL_RESOURCE_FILTER).toList()) {
            resources.add(factory.getIndividualResource(res.getPropertyResourceValue(
                SearchVocabulary.INDIVIDUAL_URI).getURI()));

        }
        return Collections.unmodifiableList(resources);
    }

    @Override
    public List<DocumentResource> getRelatedDocumentResources() {
        List<DocumentResource> resources = new ArrayList<DocumentResource>();
        for (Resource res : this.getModel().listResourcesWithProperty(SearchVocabulary.RELATED_KEYWORD, this)
                .filterKeep(Filters.DOCUMENT_RESOURCE_FILTER).toList()) {
            resources.add(factory.getDocumentResource(res.getURI()));
        }
        return Collections.unmodifiableList(resources);
    }

    @Override
    public List<ExternalResource> getRelatedExternalResources() {
        List<ExternalResource> resources = new ArrayList<ExternalResource>();
        for (Resource res : this.getModel().listResourcesWithProperty(SearchVocabulary.RELATED_KEYWORD, this)
                .filterKeep(Filters.EXTERNAL_RESOURCE_FILTER).toList()) {
            resources.add(factory.getExternalResource(res.getURI()));
        }
        return Collections.unmodifiableList(resources);
    }

    @Override
    public String getSource() {
        return this.source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return this.getLocalName();
    }
}
