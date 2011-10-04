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

package org.apache.stanbol.contenthub.servicesapi.search.execution;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.Search;
import org.apache.stanbol.contenthub.servicesapi.search.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.larq.IndexLARQ;

/**
 * SearchContext provides any information needed by any {@link SearchEngine}. This is the context of a search
 * operation. User ontology (if supplied), search results, relations among the keywords can be accessed within
 * {@link SearchContext}. As the search engines operates, the results are stored inside the same context. That
 * is, {@link SearchContext} is created at the beginning of a search operation, filled by each
 * {@link SearchEngine}. In the end, the results of a search operation can be found in the
 * {@link SearchContext}.
 * 
 * @author anil.sinaci
 * @author cihan
 * 
 */
public interface SearchContext extends SearchResult {

    /**
     * Returns a list of {@link QueryKeyword}s. Each {@link QueryKeyword} is the {@link String} supplied
     * through the {@link Search#search(String[])} method.
     * 
     * @return A list of {@link QueryKeyword}.
     */
    List<QueryKeyword> getQueryKeyWords();

    /**
     * Returns the {@link SearchContextFactory} associated with this {@link SearchContextFactory}. The factory
     * is used to manipulate the resources in the {@link SearchContext}, i.e. adding new resources.
     * 
     * @return The {@link SearchContextFactory} associated with this {@link SearchContextFactory}.
     */
    SearchContextFactory getFactory();

    /**
     * If the search operation uses an ontologu (if an ontology is provided to the search operation), this
     * function returns it.
     * 
     * @return The {@link OntModel} representing the ontology used in the semantic search.
     */
    OntModel getSearchModel();

    /**
     * Returns the list of {@link SearchEngine}s executed within this search operation. The list contains the
     * String serializations of the {@link SearchEngine} objects. Default toString() methods are used to get
     * the String serializations.
     * 
     * @return A list of {@link SearchEngine}s.
     */
    List<String> getAllowedEngines();

    /**
     * Retrieves the map of constraints (facets) to be applied during the semantic search operation. Each
     * constraint limits the search results according to the given list of values for that constraint. For
     * example, if the constraints map includes a mapping like "lang":"tr","en", search results will only
     * contain documents which include "lang" fields having "tr" or "en" values.
     * 
     * @return A map holding the constraints (facets).
     */
    Map<String,List<Object>> getConstraints();

    /**
     * If there is an ontology used in the search operation, a Lucene index is built on top of it for keyword
     * based searchs on the ontology.
     * 
     * @return The Lucene index built on top of the ontology.
     */
    IndexLARQ getIndex();

}
