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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

import java.util.Collection;

/**
 * Informs listeners about changes in an ontology collector. Implementations of this interface should be able
 * to fire events related to the modification of ontologies within an ontology collector.
 * 
 * @author alexdma
 * 
 */
public interface OntologyCollectorListenable {

    /**
     * Registers a new {@link OntologyCollectorListener} with this object.
     * 
     * @param listener
     *            the listener to be registered.
     */
    void addOntologyCollectorListener(OntologyCollectorListener listener);

    /**
     * Unregisters every {@link OntologyCollectorListener} from this object.
     */
    void clearOntologyCollectorListeners();

    /**
     * Returns the list of {@link OntologyCollectorListener}s registered with this object.
     * 
     * @return the registered listeners.
     */
    Collection<OntologyCollectorListener> getOntologyCollectorListeners();

    /**
     * Unregisters {@link OntologyCollectorListener} from this object.
     * 
     * @param listener
     *            the listener to be unregistered.
     */
    void removeOntologyCollectorListener(OntologyCollectorListener listener);

}
