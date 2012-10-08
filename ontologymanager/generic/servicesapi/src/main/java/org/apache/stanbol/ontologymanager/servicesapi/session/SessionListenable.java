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
package org.apache.stanbol.ontologymanager.servicesapi.session;

import java.util.Collection;

/**
 * An object that can fire or propagate changes in {@link Session} objects.
 * 
 * @author alexdma
 * 
 */
public interface SessionListenable {

    /**
     * Adds the given SessionListener to the pool of registered listeners.
     * 
     * @param listener
     *            the session listener to be added
     */
    void addSessionListener(SessionListener listener);

    /**
     * Clears the pool of registered session listeners.
     */
    void clearSessionListeners();

    /**
     * Returns all the registered session listeners. It is up to developers to decide whether implementations
     * should return sets (unordered but without redundancy), lists (e.g. in the order they wer registered but
     * potentially redundant) or other data structures that implement {@link Collection}.
     * 
     * @return a collection of registered session listeners.
     */
    Collection<SessionListener> getSessionListeners();

    /**
     * Removes the given SessionListener from the pool of active listeners.
     * 
     * @param listener
     *            the session listener to be removed
     */
    void removeSessionListener(SessionListener listener);

}
