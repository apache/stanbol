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
package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;

/**
 * Interface used to search for Entities (e.g. as defined by a Controlled
 * Vocabulary) Different implementations of this interface allow to use 
 * different sources. Typically the {@link Entityhub} or a {@link ReferencedSite}
 * will be used as source, but in some cases one might also use in-memory
 * implementation.
 * @author Rupert Westenthaler
 */
public interface EntitySearcher {
    /**
     * Lookup Concepts for the parsed strings. Parameters follow the same
     * rules as  {@link TextConstraint#TextConstraint(List, String...)}
     * @param field the field used to search for values in the parsed languages
     * @param includeFields A set of fields that need to be included within the 
     * returned {@link Representation}. The parsed field needs also to be included
     * even if missing in this set. If <code>null</code> only the field needs
     * to be included. Other fields MAY also be included.
     * @param search the tokens to search for. MUST NOT be <code>null</code>
     * @param languages the languages to include in the search 
     * @return the Representations found for the specified query
     * @throws T An exception while searching for concepts
     */
    Collection<? extends Representation> lookup(String field,Set<String> includeFields,List<String> search,String...languages) throws IllegalStateException;
    /**
     * Lookup a concept of the taxonomy by the id.
     * @param id the id
     * @param includeFields A set of fields that need to be included within the 
     * returned {@link Representation}. Other fields MAY be also included.
     * @return the concept or <code>null</code> if not found
     */
    Representation get(String id,Set<String> includeFields) throws IllegalStateException;
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
}