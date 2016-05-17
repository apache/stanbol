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
package org.apache.stanbol.enhancer.engines.entitylinking;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;

/**
 * Interface used to search for Entities (e.g. as defined by a Controlled
 * Vocabulary) Different implementations of this interface allow to use 
 * different sources.<p>
 * <b>NOTE:</b> Implementations that support entity rankings SHOULD provide an
 * own {@link Entity} implementation that overrides the default 
 * {@link Entity#getEntityRanking()} implementation.
 * @author Rupert Westenthaler
 */
public interface EntitySearcher {
    /**
     * Lookup Entities for the parsed parameters.
     * @param field the field used to search for values in the parsed languages
     * @param selectedFields A set of fields that need to be included within the 
     * returned {@link Representation}. The parsed field needs also to be included
     * even if missing in this set. If <code>null</code> only the field needs
     * to be included. Other fields MAY also be included.
     * @param search the tokens to search for. MUST NOT be <code>null</code>
     * @param languages the languages to include in the search 
     * @param limit The maximum number of resutls of <code>null</code> to use the default
     * @param offset The offset of the first requested search result
     * @return the Entities found for the specified query containing information for
     * all selected fields
     * @throws EntitySearcherException An exception while searching for concepts
     * @throws IllegalArgumentException if the parsed field is <code>null</code>;
     * the list with the search terms is <code>null</code> or empty;
     */
    Collection<? extends Entity> lookup(IRI field, Set<IRI> selectedFields, 
        List<String> search, String[] languages, Integer limit, Integer offset) 
                throws EntitySearcherException;
    /**
     * Lookup an Entity of the linked vocabulary by the id.
     * @param id the id
     * @param selectedFields A set of fields that need to be included within the 
     * returned {@link Representation}. Other fields MAY be also included.
     * @param languages the list of languages for {@link PlainLiteral}s that
     * should be included in the returned Entity
     * @return the concept or <code>null</code> if not found
     * @throws EntitySearcherException on any error while dereferencing the
     * Entity with the parsed Id
     * @throws IllegalArgumentException if the parsed id is <code>null</code>
     */
    Entity get(IRI id,Set<IRI> selectedFields, String...languages) throws EntitySearcherException;
    /**
     * Returns <code>true</code> if this EntitySearcher can operate without
     * dependencies to remote services. This is important because Stanbol can
     * be forced to run in offline-mode.
     * @return the state
     */
    boolean supportsOfflineMode();
    
    /**
     * The maximum number of {@link Representation}s returned for {@link #lookup(String, Set, List, String...)}
     * queries
     * @return the Number or <code>null</code> if not known
     */
    Integer getLimit();
    
    /**
     * Information in this map are added to each
     * <code>fise:EntityAnnotation</code> linking to
     * an entity returned by this EntitySearcher.   
     * @return the predicate[1..1] -> predicate[1..*] tuples added to any 
     * 'fise:EntityAnnotation'.
     */
    Map<IRI,Collection<RDFTerm>> getOriginInformation();
}