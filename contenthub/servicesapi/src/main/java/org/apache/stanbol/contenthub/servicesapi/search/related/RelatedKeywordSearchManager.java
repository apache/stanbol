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
package org.apache.stanbol.contenthub.servicesapi.search.related;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;

/**
 * This manager interface provides methods for searching related keywords for a query term. These methods
 * returns the related keyword results encapsulated in {@link SearchResult} objects.<br>
 * <br>
 * Three types of sources are offered for related keywords which are <i>Wordnet</i>, <i>a specified
 * ontology</i> and <i>referenced sites</i> managed by Stanbol Entityhub. Currently, this interface provides
 * separate services for each type of source, but please note that in the future a more elegant approach may
 * replace this one.
 * 
 * @author anil.sinaci
 * 
 */
public interface RelatedKeywordSearchManager {

    /**
     * Queries all the {@link RelatedKeywordSearch} instances with the given <code>keyword</code> and
     * aggregates the results.
     * 
     * @param keyword
     *            Keyword for which related keywords will be obtained
     * @return a {@link SearchResult} instance which encapsulates the related keyword {@link Map}. This map
     *         would have a single key which is the given <code>keyword</code>. The value corresponding to the
     *         key is another map. Its keys represent the different related keyword sources e.g Wordnet,
     *         dbpedia, etc. Values of inner map contain {@link List} of {@link RelatedKeyword}s obtained from
     *         that certain source.
     * @throws SearchException
     */
    SearchResult getRelatedKeywordsFromAllSources(String keyword) throws SearchException;

    /**
     * Queries all the {@link RelatedKeywordSearch} instances with the given <code>keyword</code> and
     * aggregates the results. It takes URI of an ontology which is passed to related keyword searchers which
     * process the ontology.
     * 
     * @param keyword
     *            Keyword for which related keywords will be obtained
     * @param ontologyURI
     *            URI of an ontology to be searched for related keywords
     * @return a {@link SearchResult} instance which encapsulates the related keyword {@link Map}. This map
     *         would have a single key which is the given <code>keyword</code>. The value corresponding to the
     *         key is another map. Its keys represent the different related keyword sources e.g Wordnet,
     *         dbpedia, etc. Values of inner map contain {@link List} of {@link RelatedKeyword}s obtained from
     *         a certain source.
     * @throws SearchException
     */
    SearchResult getRelatedKeywordsFromAllSources(String keyword, String ontologyURI) throws SearchException;

    /**
     * Searches related keywords from the ontology specified by <code>ontologyURI</code> for the given
     * <code>keyword</code>.
     * 
     * @param keyword
     *            Keyword for which related keywords will be obtained
     * @param ontologyURI
     *            URI of an ontology to be searched for related keywords
     * @return a {@link SearchResult} instance which encapsulates the related keyword {@link Map}. This map
     *         would have a single key which is the given <code>keyword</code>. The value corresponding to the
     *         key is another map. It also has a single key which indicates the "Ontology" source. Value
     *         corresponding to this key contains {@link List} of {@link RelatedKeyword}s obtained from the
     *         ontology.
     * @throws SearchException
     */
    SearchResult getRelatedKeywordsFromOntology(String keyword, String ontologyURI) throws SearchException;

    /**
     * Searches related keywords in the the referenced sites managed by Stanbol Entityhub for the given
     * <code>keyword</code>.
     * 
     * @param keyword
     *            Keyword for which related keywords will be obtained
     * @return a {@link SearchResult} instance which encapsulates the related keyword {@link Map}. This map
     *         would have a single key which is the given <code>keyword</code>. The value corresponding to the
     *         key is another map. Its keys represent the different related keyword sources i.e referenced
     *         sites. Values of inner map contain {@link List} of {@link RelatedKeyword}s obtained from a
     *         certain source.
     * @throws SearchException
     */
    SearchResult getRelatedKeywordsFromReferencedSites(String keyword) throws SearchException;
}
