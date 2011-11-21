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

import java.io.OutputStream;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Manages session objects via CRUD-like operations. A <code>SessionManager</code> maintains in-memory storage
 * of sessions, creates new ones and either destroys or stores existing ones persistently. All sessions are
 * managed via unique identifiers of the <code>org.semanticweb.owlapi.model.IRI</code> type.<br>
 * <br>
 * NOTE: implementations should be synchronized, or document whenever they are not.
 * 
 * @author alexdma
 * 
 */
public interface SessionManager extends SessionListenable {

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String SESSIONS_NS = "org.apache.stanbol.ontologymanager.session.ns";

    /**
     * Generates AND REGISTERS a new session and assigns a unique session ID generated internally.
     * 
     * @return the generated session
     */
    Session createSession();

    /**
     * Generates AND REGISTERS a new session and tries to assign it the supplied session ID. If a session with
     * that ID is already registered, the new session is <i>not</i> created and a
     * <code>DuplicateSessionIDException</code> is thrown.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     * @return the generated session
     * @throws DuplicateSessionIDException
     *             if a session with that sessionID is already registered
     */
    Session createSession(String sessionID) throws DuplicateSessionIDException;

    /**
     * Deletes the session identified by the supplied sessionID and releases its resources.
     * 
     * @param sessionID
     *            the IRI that uniquely identifies the session
     */
    void destroySession(String sessionID);

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

    String getSessionNamespace();

    /**
     * Returns the ontology space associated with this session.
     * 
     * @deprecated as session spaces are obsolete, so is this method.
     * 
     * @return the session space
     */
    Set<SessionOntologySpace> getSessionSpaces(String sessionID) throws NonReferenceableSessionException;

    void setSessionNamespace(String namespace);

    /**
     * Stores the session identified by <code>sessionID</code> using the output stream <code>out</code>.
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
