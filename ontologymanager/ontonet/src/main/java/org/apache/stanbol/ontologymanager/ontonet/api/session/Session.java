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
package org.apache.stanbol.ontologymanager.ontonet.api.session;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.LockableOntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;

/**
 * Note that sessions are possibly disjoint with HTTP sessions or the like.
 * 
 * @author alexdma
 * 
 */
public interface Session extends LockableOntologyCollector, SessionListenable, OWLExportable {

    /**
     * The states a session can be in: ACTIVE (for running sessions), HALTED (for inactive sessions that may
     * later be activated, e.g. when a user logs in), ZOMBIE (inactive and bound for destruction, no longer
     * referenceable).
     * 
     * @author alexdma
     * 
     */
    enum State {
        /**
         * Running session
         */
        ACTIVE,
        /**
         * inactive sessions that may later be activated
         */
        HALTED,
        /**
         * Inactive and bound for destruction, no longer referenceable
         */
        ZOMBIE
    }

    void attachScope(OntologyScope scope);

    void clearScopes();

    /**
     * Closes this Session irreversibly. Most likely includes setting the state to ZOMBIE.
     */
    void close() throws NonReferenceableSessionException;

    void detachScope(String scopeId);

    Set<String> getAttachedScopes();

    /**
     * Returns the unique Internationalized Resource Identifier (IRI) that identifies this KReS session.<br>
     * <br>
     * NOTE: There is no set method for the session ID as it is assumed to be set in its constructor once and
     * for all.
     * 
     * @return the IRI that identifies this session
     */
    String getID();

    /**
     * Returns the current state of this KReS session.
     * 
     * @return the state of this session
     */
    State getSessionState();

    /**
     * Equivalent to <code>getState() == State.ACTIVE</code>.
     * 
     * @return true iff this session is in the ACTIVE state
     */
    boolean isActive();

    /**
     * Sets this session as active
     * 
     * @throws NonReferenceableSessionException
     */
    void open() throws NonReferenceableSessionException;

    /**
     * Sets the KReS session as ACTIVE if <code>active</code> is true, INACTIVE otherwise. The state set is
     * returned, which should match the input state unless an error occurs.<br>
     * <br>
     * Should throw an exception if this session is in a ZOMBIE state.
     * 
     * @param active
     *            the desired activity state for this session
     * @return the resulting state of this KReS session
     */
    State setActive(boolean active) throws NonReferenceableSessionException;

}
