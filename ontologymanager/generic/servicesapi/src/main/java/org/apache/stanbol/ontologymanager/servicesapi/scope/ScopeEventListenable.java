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
package org.apache.stanbol.ontologymanager.servicesapi.scope;

import java.util.Collection;

/**
 * Implementations of this interface are able to fire events related to the modification of an ontology scope,
 * not necessarily including its ontologies.<br/>
 * <br/>
 * This interface adds support for CRUD operations on scope event listeners.
 * 
 */
public interface ScopeEventListenable {

    /**
     * Registers a listener to scope-related events fired by this object.
     * 
     * @param listener
     *            the listener to be registered.
     */
    void addScopeEventListener(ScopeEventListener listener);

    /**
     * Unregisters all the scope event listeners registered with this object.
     */
    void clearScopeEventListeners();

    /**
     * Gets all the scope event listeners registered with this object.
     * 
     * @return the registered scope event listeners.
     */
    Collection<ScopeEventListener> getScopeEventListeners();

    /**
     * Unregisters a listener to scope-related events fired by this object. Has no effect if the supplied
     * listener was not registered with this object in the first place.
     * 
     * @param listener
     *            the listener to be unregistered.
     */
    void removeScopeEventListener(ScopeEventListener listener);

}
