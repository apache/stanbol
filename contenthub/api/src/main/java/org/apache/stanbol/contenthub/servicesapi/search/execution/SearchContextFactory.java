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

/**
 * The factory interface to be used in the manipulation of a {@link SearchContext}.
 * 
 * @author cihan
 * 
 */
public interface SearchContextFactory {

    /**
     * Sets the {@link SearchContext} context of this factory. This factory manipulates the
     * {@link SearchContext} given with this function.
     * 
     * @param searchContext
     *            The {@link SearchContext} of this factory.
     */
    void setSearchContext(SearchContext searchContext);

    /**
     * Creates a {@link QueryKeyword} inside the {@link SearchContext} with the given keyword. The created
     * {@link QueryKeyword} is put into the {@link SearchContext} and returned.
     * 
     * @param keyword
     *            The keyword string.
     * @return The created {@link QueryKeyword}.
     */
    QueryKeyword createQueryKeyword(String keyword);

    /**
     * Creates a {@link Keyword} inside the {@link SearchContext} with the given keyword, score and
     * {@link QueryKeyword}. Each {@link Keyword} is associated with a {@link QueryKeyword}. That is, this
     * {@link Keyword} will be a related keyword of the given {@link QueryKeyword}. The created
     * {@link Keyword} is put into the {@link SearchContext} and returned.
     * 
     * @param keyword
     *            The keyword string.
     * @param score
     *            The score.
     * @param queryKeyword
     *            The {@link QueryKeyword} to be associated with.
     * @return The created {@link Keyword}.
     */
    Keyword createKeyword(String keyword, double score, QueryKeyword queryKeyword, String keywordSource);

    /**
     * Creates a {@link ClassResource} inside the {@link SearchContext} with the given classURI, weight, score
     * and {@link Keyword}. The created {@link ClassResource} is put into the {@link SearchContext} and
     * returned.
     * 
     * @param classURI
     *            The URI of the {@link ClassResource}
     * @param weight
     *            The weight.
     * @param score
     *            The score.
     * @param relatedKeyword
     *            The {@link Keyword} which will be added to this {@link ClassResource} as related.
     * @return The created {@link ClassResource}.
     */
    ClassResource createClassResource(String classURI, double weight, double score, Keyword relatedKeyword);

    /**
     * Creates an {@link IndividualResource} inside the {@link SearchContext} with the given classURI, weight,
     * score and {@link Keyword}. The created {@link IndividualResource} is put into the {@link SearchContext}
     * and returned.
     * 
     * @param individualURI
     *            The URI of the {@link IndividualResource}
     * @param weight
     *            The weight.
     * @param score
     *            The score.
     * @param relatedKeyword
     *            The {@link Keyword} which will be added to this {@link IndividualResource} as related.
     * @return The created {@link IndividualResource}.
     */
    IndividualResource createIndividualResource(String individualURI,
                                                double weight,
                                                double score,
                                                Keyword relatedKeyword);

    /**
     * Creates a {@link DocumentResource} with the given parameters. The created {@link DocumentResource} is
     * put into the {@link SearchContext} and returned.
     * 
     * @param documentURI
     *            The URI of the {@link DocumentResource}.
     * @param weight
     *            The weight.
     * @param score
     *            The score.
     * @param relatedKeyword
     *            The {@link Keyword} which will be added to this {@link DocumentResource} as related.
     * @param selectionText
     *            The actual text of the document.
     * @return The created {@link DocumentResource}.
     */
    DocumentResource createDocumentResource(String documentURI,
                                            double weight,
                                            double score,
                                            Keyword relatedKeyword,
                                            String selectionText,
                                            String documentTitle);

    /**
     * Creates an {@link ExternalResource} with the given parameters. The created {@link ExternalResource} is
     * put into the {@link SearchContext} and returned.
     * 
     * @param reference
     *            Dereferenceable URI of the external entity.
     * @param weight
     *            The weight.
     * @param score
     *            The score.
     * @param relatedKeyword
     * @return The {@link Keyword} which will be added to this {@link ExternalResource} as related.
     */
    ExternalResource createExternalResource(String reference,
                                            double weight,
                                            double score,
                                            Keyword relatedKeyword);

}
