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
 * The interface to represent an individual resource (instance of an ontology class), and its related
 * resources. Related resources are attached to the {@link IndividualResource} as the {@link SearchEngine}s
 * execute.
 * 
 * @author cihan
 * 
 */
public interface IndividualResource extends KeywordRelated {

    /**
     * Retrieves the {@link ClassResource}s which this {@link IndividualResource} is related to.
     * 
     * @return List of related Ontology Classes of this resource.
     */
    List<ClassResource> getRelatedClasses();

    /**
     * Retrieves the {@link IndividualResource}s which this {@link IndividualResource} is related to.
     * 
     * @return List of related Ontology Individuals (Class instances) of this resource.
     */
    List<IndividualResource> getRelatedIndividuals();

    /**
     * Adds a {@link ClassResource} as related to this {@link IndividualResource}.
     * 
     * @param classResource
     *            The {@link ClassResource} which is related to this {@link IndividualResource}.
     */
    void addRelatedClass(ClassResource classResource);

    /**
     * Adds an {@link IndividualResource} as related to this {@link IndividualResource}.
     * 
     * @param individualResource
     *            The {@link IndividualResource} which is related to this {@link IndividualResource}.
     */
    void addRelatedIndividual(IndividualResource individualResource);

    /**
     * Returns the URI of this ontology individual within the ontology it resides.
     * 
     * @return The URI of this {@link IndividualResource}.
     */
    String getIndividualURI();

    /**
     * Returns the dereferenceable URI of this ontology individual. Dereferenceable URI can be accessed
     * through HTTP and presents this {@link IndividualResource}.
     * 
     * @return The dereferenceable URI of this {@link IndividualResource}.
     */
    String getDereferenceableURI();
}
