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

/**
 * Related keyword searcher for a given query keyword.
 * 
 * @author suat
 * 
 */
public interface RelatedKeywordSearch {

    /**
     * This method searches for related keywords for the given <code>keyword</code>.
     * 
     * @param keyword
     *            The query keyword for which related keywords will be obtained.
     * @return a {@link Map} containing the related keywords. Keys of this map represents sources/categories
     *         of the related keywords. Values of the map keeps {@link List} of {@link RelatedKeyword}s.
     * @throws SearchException
     */
    Map<String,List<RelatedKeyword>> search(String keyword) throws SearchException;

    /**
     * This method searches for related keywords for the given <code>keyword</code>. It also takes URI of an
     * ontology which will be used as a related keyword source while searching through ontology resources.
     * 
     * @param keyword
     *            The query keyword for which related keywords will be obtained.
     * @param ontologyURI
     *            URI of the ontology in which related keyword will be searched
     * @return a {@link Map} containing the related keywords. Keys of this map represents sources/categories
     *         of the related keywords. Values of the map keeps {@link List} of {@link RelatedKeyword}s.
     * @throws SearchException
     */
    Map<String,List<RelatedKeyword>> search(String keyword, String ontologyURI) throws SearchException;
}
