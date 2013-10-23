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

import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.collector.Lockable;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OWLExportable;

/**
 * An ontology collector that can be used by client applications to store volatile data, e.g. for the duration
 * of a service call. It has an aggressive severance policy and tries to delete as many managed ontologies as
 * possible when it goes down.<br>
 * <br>
 * Note that sessions are generally disjoint with HTTP sessions or the like, but can be used in conjunction
 * with them, or manipulated to mimic their behaviour.
 * 
 * @author alexdma
 * 
 */
public interface Session extends OntologyCollector, OWLExportable, Lockable, SessionListenable {

    static final String shortName = "session";

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

    /**
     * Instructs the session to reference the supplied ontology scope. This way, whenever session data are
     * processed, scope data will be considered as well.
     * 
     * @param scope
     *            the ontology scope to be referenced.
     */
    void attachScope(String scopeId);

    /**
     * Removes all references to ontology scopes, thus leaving the session data as standalone.
     */
    void clearScopes();

    /**
     * Closes this Session irreversibly. Most likely includes setting the state to ZOMBIE.
     */
    void close();

    /**
     * Instructs the session to no longer reference the supplied ontology scope. If a scope with the supplied
     * identifier was not attached, this method has no effect.
     * 
     * @param scope
     *            the identifer of the ontology scope to be detached.
     */
    void detachScope(String scopeId);

    /**
     * Gets the identifiers of the scopes currently attached to this session.
     * 
     * @return the attached scope identifiers
     */
    Set<String> getAttachedScopes();

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
    void open();

    /**
     * Sets the session as ACTIVE if <code>active</code> is true, INACTIVE otherwise. The state set is
     * returned, which should match the input state unless an error occurs.<br>
     * <br>
     * Should throw an exception if this session is in a ZOMBIE state.
     * 
     * @param active
     *            the desired activity state for this session
     * @return the resulting state of this KReS session
     */
    State setActive(boolean active);

}
