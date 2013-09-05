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

import java.io.OutputStream;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.NamedArtifact;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Manages session objects via CRUD-like operations. A <code>SessionManager</code> maintains in-memory storage
 * of sessions, creates new ones and either destroys or stores existing ones persistently. All sessions are
 * managed via unique identifiers of the <code>org.semanticweb.owlapi.model.IRI</code> type.<br>
 * <br>
 * NOTE: implementations should either be synchronized, or document whenever they are not.
 * 
 * @author alexdma
 * 
 */
public interface SessionManager extends NamedArtifact, SessionListenable {

    /**
     * The key used to configure the connectivity policy.
     */
    String CONNECTIVITY_POLICY = "org.apache.stanbol.ontologymanager.ontonet.session_connectivity";

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String ID = "org.apache.stanbol.ontologymanager.ontonet.session_mgr_id";

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String MAX_ACTIVE_SESSIONS = "org.apache.stanbol.ontologymanager.ontonet.session_limit";

    /**
     * Generates <b>and registers</b> a new session and assigns a unique session ID generated internally. This
     * will not cause {@link DuplicateSessionIDException}s to be thrown.
     * 
     * @return the generated session
     */
    Session createSession() throws SessionLimitException;

    /**
     * Generates <b>and registers</b> a new session and tries to assign it the supplied session ID. If a
     * session with that ID is already registered, the new session is <i>not</i> created and a
     * <code>DuplicateSessionIDException</code> is thrown.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     * @return the generated session
     * @throws DuplicateSessionIDException
     *             if a session with that sessionID is already registered
     */
    Session createSession(String sessionID) throws DuplicateSessionIDException, SessionLimitException;

    /**
     * Deletes the session identified by the supplied sessionID and releases its resources.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     */
    void destroySession(String sessionID);

    /**
     * Gets the number of sessions that can be simultaneously active and managed by this session manager.
     * 
     * @return the session limit.
     */
    int getActiveSessionLimit();

    /**
     * Returns the set of strings that identify registered sessions, whatever their state.
     * 
     * @return the IDs of all registered sessions.
     */
    Set<String> getRegisteredSessionIDs();

    /**
     * Retrieves the unique session identified by <code>sessionID</code>.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     * @return the unique session identified by <code>sessionID</code>
     */
    Session getSession(String sessionID);

    /**
     * Sets the maximum allowed number of active sessions managed by this manager simultaneously. A negative
     * value denotes no limit.
     * 
     * Note that it is possible to set this value to zero, thus preventing Stanbol from creating session at
     * all.
     * 
     * @param limit
     */
    void setActiveSessionLimit(int limit);

    /**
     * Stores the session identified by <code>sessionID</code> using the output stream <code>out</code>.
     * 
     * @deprecated As of now, session contents are always stored. Deprecation will be removed if a new policy
     *             is implemented.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     * @param out
     *            the output stream to store the session
     * @throws OWLOntologyStorageException
     */
    void storeSession(String sessionID, OutputStream out) throws NonReferenceableSessionException,
                                                         OWLOntologyStorageException;

}
