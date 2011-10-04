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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ExternalResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of {@link ExternalResource}.
 * 
 * @author cihan
 * 
 */
public class ExternalResourceImpl extends AbstractKeywordRelatedResource implements ExternalResource {

    ExternalResourceImpl(Node n,
                         EnhGraph g,
                         Double weight,
                         Double score,
                         String externalEntity,
                         Keyword related,
                         SearchContextFactoryImpl factory) {
        super(n, g, weight, score, related, factory);
        this.addProperty(RDF.type, SearchVocabulary.EXTERNAL_RESOURCE);
        this.addProperty(SearchVocabulary.EXTERNAL_ENTITY, this.getModel().createResource(externalEntity));

    }

    @Override
    public void addType(String type) {
        this.getPropertyResourceValue(SearchVocabulary.EXTERNAL_ENTITY).addProperty(RDF.type,
            this.getModel().createResource(type));

    }

    @Override
    public String getReference() {
        return this.getPropertyResourceValue(SearchVocabulary.EXTERNAL_ENTITY).getURI();
    }

    @Override
    public Set<String> getTypes() {
        Set<Statement> typeSet = this.getPropertyResourceValue(SearchVocabulary.EXTERNAL_ENTITY)
                .listProperties(RDF.type).toSet();
        Set<String> types = new HashSet<String>(typeSet.size());
        for (Statement stmt : typeSet) {
            types.add(stmt.getObject().asResource().getURI());
        }
        return types;
    }

    @Override
    public Set<DocumentResource> getRelatedDocuments() {
        Set<DocumentResource> docs = new HashSet<DocumentResource>();
        for (RDFNode res : this.listPropertyValues(SearchVocabulary.RELATED_DOCUMENT).toSet()) {
            DocumentResource docRes = factory.getDocumentResource(res.asResource().getURI());
            docs.add(docRes);
        }
        return Collections.unmodifiableSet(docs);
    }

    @Override
    public void addRelatedDocument(DocumentResource documentResource) {
        this.addProperty(SearchVocabulary.RELATED_DOCUMENT, factory.getDocumentResource(factory.documentName(
            documentResource.getDocumentURI(), documentResource.getRelatedKeywords().get(0))));

    }

    @Override
    public void addRelatedClass(ClassResource classResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRelatedIndividual(IndividualResource individualResource) {
        throw new UnsupportedOperationException();
    }

}
