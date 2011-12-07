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

import org.apache.stanbol.contenthub.core.utils.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of {@link DocumentResource}.
 * 
 * @author cihan
 * 
 */
public class DocumentResourceImpl extends AbstractKeywordRelatedResource implements DocumentResource {

    DocumentResourceImpl(Node n,
                         EnhGraph g,
                         String DocumentURI,
                         Double weight,
                         Double score,
                         Keyword relatedKeyword,
                         SearchContextFactoryImpl factory) {
        super(n, g, weight, score, relatedKeyword, factory);
        this.addProperty(SearchVocabulary.RELATED_DOCUMENT, DocumentURI);
        this.addProperty(RDF.type, SearchVocabulary.DOCUMENT_RESOURCE);
    }

    @Override
    public String getDocumentURI() {
        return this.getPropertyValue(SearchVocabulary.RELATED_DOCUMENT).asLiteral().getLexicalForm();
    }

    public String getLocalId() {
        return ContentItemIDOrganizer.detachBaseURI(this.getDocumentURI());
    }

    @Override
    public String getRelatedText() {
        return this.getPropertyValue(SearchVocabulary.SELECTION_TEXT).asLiteral().getLexicalForm();
    }

    @Override
    public String getDocumentTitle() {
        return this.getPropertyValue(SearchVocabulary.DOCUMENT_TITLE).asLiteral().getLexicalForm();
    }
    /*
     * @Override public String getRelatedContentRepositoryItem() { return
     * this.getPropertyValue(SearchVocabulary.CONTENT_REPOSITORY_ITEM).asLiteral().getLexicalForm(); }
     */

    @Override
    public void setRelatedText(String selectionText) {
        this.removeAll(SearchVocabulary.SELECTION_TEXT).addProperty(SearchVocabulary.SELECTION_TEXT,
            selectionText);
    }

    @Override
    public void setDocumentTitle(String documentTitle) {
        this.removeAll(SearchVocabulary.DOCUMENT_TITLE).addProperty(SearchVocabulary.DOCUMENT_TITLE,
        	documentTitle);
    }
    
    @Override
    public void addRelatedClass(ClassResource classResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRelatedIndividual(IndividualResource individualResource) {
        throw new UnsupportedOperationException();
    }

    /*
     * @Override public void setRelatedContentRepositoryItem(String path) {
     * this.removeAll(SearchVocabulary.CONTENT_REPOSITORY_ITEM).addProperty(
     * SearchVocabulary.CONTENT_REPOSITORY_ITEM, path); }
     */

}
