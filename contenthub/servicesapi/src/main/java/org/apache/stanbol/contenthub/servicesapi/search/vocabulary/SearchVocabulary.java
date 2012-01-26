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

package org.apache.stanbol.contenthub.servicesapi.search.vocabulary;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * RDF Vocabulary for Apache Stanbol search component. Inspired by com.hp.hpl.jena.vocabulary.RDF class
 * {@link SearchContext} is implemented as a Jena graph. Any information of the search context is kept as a
 * triple inside the context graph. Accessing any information of the context (i.e. search results) is actually
 * executing a filter on the Jena graph, which is the context itself.
 * 
 * @author cihancimen
 * 
 */
public final class SearchVocabulary {

    private static final String URI = "http://stanbol.apache.org/search/";

    public static final String getUri() {
        return URI;
    }

    private static Resource resource(String local) {
        return ResourceFactory.createResource(URI + local);
    }

    private static Property property(String local) {
        return ResourceFactory.createProperty(URI, local);
    }

    /**
     * Represents a keyword directly entered in query string
     */
    public static final Resource QUERY_KEYWORD = resource("queryKeyword");

    /**
     * Represents a {@link Keyword} that is related to a {@link QueryKeyword}. Generally found by a
     * {@link SearchEngine}.
     */
    public static final Resource KEYWORD = resource("keyword");

    /**
     * Represents an OWL Class found by a {@link SearchEngine}.
     */
    public static final Resource CLASS_RESOURCE = resource("classResource");
    /**
     * Represents an OWL Individual found by a search engine.
     */
    public static final Resource INDIVIDUAL_RESOURCE = resource("individualResource");

    /**
     * Represents a Document in Enhancer Store, found by a {@link SearchEngine}.
     */
    public static final Resource DOCUMENT_RESOURCE = resource("documentResource");

    /**
     * Represents an external entity found by a {@link SearchEngine}
     */
    public static final Resource EXTERNAL_RESOURCE = resource("externalResource");

    /**
     * Represents the score of a resource.
     */
    public static final Property SCORE = property("score");

    /**
     * Represent the weight of a resource. Weight is used to update the score of the resource.
     */
    public static final Property WEIGHT = property("weight");

    /**
     * Represents the original string of a {@link Keyword}.
     */
    public static final Property KEYWORD_STRING = property("keywordString");

    /**
     * Represents relationship between a resource and a {@link Keyword}
     */
    public static final Property RELATED_KEYWORD = property("relatedKeyword");

    /**
     * Represents relationship between a resource and a {@link QueryKeyword}
     */
    public static final Property RELATED_QUERY_KEYWORD = property("relatedQueryKeyword");

    /**
     * Represents relationship between a resource and a {@link ClassResource}.
     */
    public static final Property RELATED_CLASS = property("relatedClass");

    /**
     * Represents relationship between a resource and a {@link IndividualResource}.
     */
    public static final Property RELATED_INDIVIDUAL = property("relatedIndividual");

    /**
     * Represents the URI of a {@link ClassResource}.
     */
    public static final Property CLASS_URI = property("classURI");

    /**
     * Represents the URI of an {@link IndividualResource}.
     */
    public static final Property INDIVIDUAL_URI = property("individualURI");

    /**
     * Represents the URI of an {@link ExternalResource}.
     */
    public static final Property DEREFENCEABLE_URI = property("dereferenceableURI");

    /**
     * Represents the special property which is used by Lucene while creating the index. At the beginning of a
     * search operation, user ontology is processed to add this special property to each class and individual
     * resource by using their local names.
     */
    public static final Property HAS_LOCAL_NAME = property("hasLocalName");

    /**
     * Represents relationship between a resource and a {@link DocumentResource}.
     */
    public static final Property RELATED_DOCUMENT = property("relatedDocument");

    /**
     * Represents the text of a {@link DocumentResource}.
     */
    public static final Property SELECTION_TEXT = property("selectionText");

    /**
     * Represents the title of a {@link DocumentResource}.
     */
    public static final Property DOCUMENT_TITLE = property("documentTitle");

    /**
     * Represents relationship between an {@link ExternalResource} and the entity which is externally
     * referenced.
     */
    public static final Property EXTERNAL_ENTITY = property("externalEntity");

    /**
     * Represents the path of a {@link DocumentResource}. This is the path which specifically identifies the
     * content item inside the content reposiroy.
     */
    public static final Property CONTENT_REPOSITORY_ITEM = property("contentRepositoryItem");

    /**
     * Special class to manage the conversion between {@link Resource} and {@link Node}.
     * 
     * @author cihan
     * 
     */
    // Nodes
    public static final class Nodes {
        private Nodes() {

        }

        /**
         * @see SearchVocabulary#QUERY_KEYWORD
         */
        public static final Node QUERY_KEYWORD = SearchVocabulary.QUERY_KEYWORD.asNode();

        /**
         * @see SearchVocabulary#KEYWORD
         */
        public static final Node KEYWORD = SearchVocabulary.KEYWORD.asNode();

        /**
         * @see SearchVocabulary#CLASS_RESOURCE
         */
        public static final Node CLASS_RESOURCE = SearchVocabulary.CLASS_RESOURCE.asNode();

        /**
         * @see SearchVocabulary#INDIVIDUAL_RESOURCE
         */
        public static final Node INDIVIDUAL_RESOURCE = SearchVocabulary.INDIVIDUAL_RESOURCE.asNode();

        /**
         * @see SearchVocabulary#DOCUMENT_RESOURCE
         */
        public static final Node DOCUMENT_RESOURCE = SearchVocabulary.DOCUMENT_RESOURCE.asNode();

        /**
         * @see SearchVocabulary#EXTERNAL_RESOURCE
         */
        public static final Node EXTERNAL_RESOURCE = SearchVocabulary.EXTERNAL_RESOURCE.asNode();

        /**
         * @see SearchVocabulary#KEYWORD_STRING
         */
        public static final Node KEYWORD_STRING = SearchVocabulary.KEYWORD_STRING.asNode();

        /**
         * @see SearchVocabulary#RELATED_CLASS
         */
        public static final Node RELATED_CLASS = SearchVocabulary.RELATED_CLASS.asNode();

        /**
         * @see SearchVocabulary#RELATED_INDIVIDUAL
         */
        public static final Node RELATED_INDIVIDUAL = SearchVocabulary.RELATED_INDIVIDUAL.asNode();

        /**
         * @see SearchVocabulary#INDIVIDUAL_URI
         */
        public static final Node INDIVIDUAL_URI = SearchVocabulary.INDIVIDUAL_URI.asNode();

        /**
         * @see SearchVocabulary#CLASS_URI
         */
        public static final Node CLASS_URI = SearchVocabulary.CLASS_URI.asNode();

        /**
         * @see SearchVocabulary#DEREFENCEABLE_URI
         */
        public static final Node DEREFERENCEABLE_URI = SearchVocabulary.DEREFENCEABLE_URI.asNode();

        /**
         * @see SearchVocabulary#RELATED_KEYWORD
         */
        public static final Node RELATED_KEYWORD = SearchVocabulary.RELATED_KEYWORD.asNode();

        /**
         * @see SearchVocabulary#RELATED_QUERY_KEYWORD
         */
        public static final Node RELATED_QUERY_KEYWORD = SearchVocabulary.RELATED_QUERY_KEYWORD.asNode();
    }

    private SearchVocabulary() {

    }
}
