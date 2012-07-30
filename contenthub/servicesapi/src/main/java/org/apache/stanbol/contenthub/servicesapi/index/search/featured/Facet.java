/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.servicesapi.index.search.featured;

import java.util.Locale;
import java.util.Set;

/**
 * A {@link Facet} is an aspect by which the {@link ConstrainedDocumentSet} can be narrowed. Facets correspond
 * to properties of documents and they are used in the search operations in {@link FeaturedSearch} of
 * Contenthub. Facets are considered as equal if their default labels are the same. Default labels of facets
 * are obtained by providing <code>null</code> to {@link #getLabel(Locale)}.
 */
public interface Facet {

    /**
     * This methods return all possible values regarding this facet wrapped in a {@link Set} of
     * {@link Constraint}s. Constraints are used to filter search results based on certain values of facets.
     * 
     * @return a {@link Set} of constraints that reduce the document to a non-empty set
     */
    Set<Constraint> getConstraints();

    /**
     * Returns a label for this {@link Facet} based on the given {@link Locale} preference.
     * 
     * @param locale
     *            the desired {@link Locale} or <code>null</code> if no preference.
     * @return a label for this facet
     */
    String getLabel(Locale locale);

}