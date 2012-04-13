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
package org.apache.stanbol.contenthub.servicesapi.search.featured;

import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;

/**
 * A set of documents that can be narrowed by applying {@link Constraint}s that are grouped by {@link Facet}s.
 * Instances of this class are immutable, narrowing and broadening return new instances.
 */
public interface ConstrainedDocumentList {
    /**
     * Returns the documents contained in this {@link ConstrainedDocumentList}. There is no defined order of
     * the list, but implementations should keep the order stable as too allow stateless pagination.
     * 
     * Implementations may populate the list just when the respective elements are accessed and implement
     * size() to access optimized backend functionality. Clients must thus take into account the possibility
     * that the list changes while they are using it. For example the size returned by List.size() may not
     * match the actual number of elements when iterating throw it at a later point in time. The iterate() as
     * well as the subList(int,int) method are safe.
     * 
     * @return the documents included in this {@link ConstrainedDocumentList}
     * @throws SearchException
     */
    List<UriRef> getDocuments() throws SearchException;

    /**
     * This method returns the {@link Set} of {@link Constraint}s which have been used to obtain the documents
     * included in this set.
     * 
     * @return the constrains that apply to this ConstrainedDocumentSet (might be empty)
     */
    Set<Constraint> getConstraints();

    /**
     * This method returns all possible {@link Facet}s together with all their possible {@link Constraint}s
     * that can be used to filter documents included in this set.
     * 
     * @return the {@link Facet}s by which this {@link ConstrainedDocumentList} can be restricted.
     */
    Set<Facet> getFacets();

    /**
     * Note that the new set need not to be computed when this method is called, the matching document might
     * be computed when needed. So implimentations can provided efficient way to allow a client to call
     * 
     * <code>narrow(additionalConstraint).getDocuments().size()</code>
     * 
     * Creates a new {@link ConstrainedDocumentList} with the new set of {@link Constraint} which is formed by
     * adding the specified <code>constraint</code> the constraint set returned by {@link #getConstraints()}.
     * 
     * @param constraint
     *            the additional {@link Constraint} to apply
     * @return the restricted {@link ConstrainedDocumentList} by applying the given additional
     *         <code>constraint</code>
     * @throws SearchException
     */
    ConstrainedDocumentList narrow(Constraint constraint) throws SearchException;

    /**
     * Creates a new {@link ConstrainedDocumentList} with the new set of {@link Constraint}s which is formed by
     * removing the specified <code>constraint</code> from the constraint set returned by
     * {@link #getConstraints()}.
     * 
     * @param constraint
     *            the {@link Constraint} which must be member of the set returned by {@link #getConstraints()}
     * @return the broadened {@link ConstrainedDocumentList} by removing the given <code>constraint</code>.
     * @throws SearchException
     */
    ConstrainedDocumentList broaden(Constraint constraint) throws SearchException;

}
