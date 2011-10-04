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

import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;

/**
 * The interface to represent a keyword and its related resources. Related resources are attached to the
 * {@link Keyword} as the {@link SearchEngine}s execute.
 * 
 * @author cihan
 * 
 */
public interface Keyword extends Scored {

    /**
     * Getter function to retrieve the keyword string.
     * 
     * @return The keyword {@link String}
     */
    String getKeyword();

    /**
     * If this keyword is added to the context by means of a {@link QueryKeyword}, this function return that
     * {@link QueryKeyword}.
     * 
     * @return The {@link QueryKeyword} which has caused this {@link Keyword} to exist.
     */
    QueryKeyword getRelatedQueryKeyword();

    /**
     * If this {@link Keyword} causes any {@link ClassResource} to be added to the {@link SearchContext}, then
     * they are related. This function returns the {@link ClassResource}s, which are added to the
     * {@link SearchContext} by means of this {@link Keyword}.
     * 
     * @return A list of {@link ClassResource}s.
     */
    List<ClassResource> getRelatedClassResources();

    /**
     * If this {@link Keyword} causes any {@link IndividualResource} to be added to the {@link SearchContext},
     * then they are related. This function returns the {@link IndividualResource}s, which are added to the
     * {@link SearchContext} by means of this {@link Keyword}.
     * 
     * @return A list of {@link IndividualResource}s.
     */
    List<IndividualResource> getRelatedIndividualResources();

    /**
     * If this {@link Keyword} causes any {@link DocumentResource} to be added to the {@link SearchContext},
     * then they are related. This function returns the {@link DocumentResource}s, which are added to the
     * {@link SearchContext} by means of this {@link Keyword}.
     * 
     * @return A list of {@link DocumentResource}s.
     */
    List<DocumentResource> getRelatedDocumentResources();

    /**
     * If this {@link Keyword} causes any {@link ExternalResource} to be added to the {@link SearchContext},
     * then they are related. This function returns the {@link ExternalResource}s, which are added to the
     * {@link SearchContext} by means of this {@link Keyword}.
     * 
     * @return A list of {@link ExternalResource}s.
     */
    List<ExternalResource> getRelatedExternalResources();

}
