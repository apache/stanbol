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

import java.util.Set;

import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;

/**
 * The interface to represent entities which resides in another place outside the Stanbol Contenthub.
 * {@link ExternalResource}s are created when an external entity is encountered within a {@link SearchEngine}.
 * 
 * @author cihan
 * 
 */
public interface ExternalResource extends KeywordRelated {

    /**
     * Retrieves the URI of this resource. This URI is an external reference, it should be dereferenceable.
     * Hence, returns the same value as {@link #getDereferenceableURI()}.
     * 
     * @return Dereferenceable URI of this resource.
     */
    String getReference();

    /**
     * If there exist any types associated with this {@link ExternalResource}, they are returned as a
     * {@link Set<String>}. Types are determined through <code>rdf:type</code> property for
     * {@link ExternalResource}s.
     * 
     * @return Set of types associated with this {@link ExternalResource}.
     */
    Set<String> getTypes();

    /**
     * Associates a new type with this {@link ExternalResource}.
     * 
     * @param type
     *            The type to be added.
     */
    void addType(String type);

    /**
     * If this {@link ExternalResource} causes any {@link DocumentResource} to be added to the
     * {@link SearchContext}, then they are related. This function returns the {@link DocumentResource}s,
     * which are added to the {@link SearchContext} by means of this {@link ExternalResource}.
     * 
     * @return A list of {@link DocumentResource}s.
     */
    Set<DocumentResource> getRelatedDocuments();

    /**
     * Associates a new document with this {@link ExternalResource}.
     * 
     * @param documentResources
     *            The {@link DocumentResource} representing the document to be associated with this
     *            {@link ExternalResource}.
     */
    void addRelatedDocument(DocumentResource documentResources);

    /**
     * Retrieves the URI of this resource. This URI is an external reference and the returned URI is
     * dereferenceable.
     * 
     * @return Dereferenceable URI of this resource.
     */
    String getDereferenceableURI();

}
