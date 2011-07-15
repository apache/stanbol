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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;


/**
 * Manages KReS session objects via CRUD-like operations. A
 * <code>SessionManager</code> maintains in-memory storage of KReS sessions,
 * creates new ones and either destroys or stores existing ones persistently.
 * All KReS sessions are managed via unique identifiers of the
 * <code>org.semanticweb.owlapi.model.IRI</code> type.<br>
 * <br>
 * NOTE: implementations should be synchronized, or document whenever they are
 * not.
 * 
 * @author alessandro
 * 
 */
public interface SessionManager extends SessionListenable {

	Set<IRI> getRegisteredSessionIDs();

	/**
	 * Generates AND REGISTERS a new KReS session and assigns a unique session
	 * ID generated internally.
	 * 
	 * @return the generated KReS session
	 */
    Session createSession();

	/**
	 * Generates AND REGISTERS a new KReS session and tries to assign it the
	 * supplied session ID. If a session with that ID is already registered, the
	 * new session is <i>not</i> created and a
	 * <code>DuplicateSessionIDException</code> is thrown.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @return the generated KReS session
	 * @throws DuplicateSessionIDException
	 *             if a KReS session with that sessionID is already registered
	 */
    Session createSession(IRI sessionID)
			throws DuplicateSessionIDException;

	/**
	 * Deletes the KReS session identified by the supplied sessionID and
	 * releases its resources.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 */
    void destroySession(IRI sessionID);

	/**
	 * Retrieves the unique KReS session identified by <code>sessionID</code>.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @return the unique KReS session identified by <code>sessionID</code>
	 */
    Session getSession(IRI sessionID);

	/**
	 * Returns the ontology space associated with this session.
	 * 
	 * @return the session space
	 */
    Set<SessionOntologySpace> getSessionSpaces(IRI sessionID)
			throws NonReferenceableSessionException;

	/**
	 * Stores the KReS session identified by <code>sessionID</code> using the
	 * output stream <code>out</code>.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @param out
	 *            the output stream to store the session
	 * @throws OWLOntologyStorageException 
	 */
    void storeSession(IRI sessionID, OutputStream out)
			throws NonReferenceableSessionException, OWLOntologyStorageException;

}
