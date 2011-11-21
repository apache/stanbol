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
package org.apache.stanbol.ontologymanager.ontonet.impl.session;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session.State;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionIDGenerator;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Calls to <code>getSessionListeners()</code> return a {@link Set} of listeners.
 * 
 * TODO: implement storage (using persistence layer).
 * 
 * @author alexdma
 * 
 */
// @Component(immediate = true, metatype = true)
// @Service(SessionManager.class)
public class SessionManagerImpl implements SessionManager {

    private Map<String,Session> sessionsByID;

    protected Set<SessionListener> listeners;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected SessionIDGenerator idgen;

    protected ScopeRegistry scopeRegistry;

    private IRI namespace;

    private OntologyProvider<?> ontologyProvider;

    public SessionManagerImpl(IRI baseIri, ScopeRegistry scopeRegistry, OntologyProvider<?> ontologyProvider) {
        this.namespace = baseIri;
        this.ontologyProvider = ontologyProvider;
        idgen = new TimestampedSessionIDGenerator(baseIri);
        listeners = new HashSet<SessionListener>();
        sessionsByID = new HashMap<String,Session>();
        this.scopeRegistry = scopeRegistry;
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearSessionListeners() {
        listeners.clear();
    }

    @Override
    public Session createSession() {
        Set<String> exclude = getRegisteredSessionIDs();
        Session session = null;
        while (session == null)
            try {
                session = createSession(idgen.createSessionID(exclude));
            } catch (DuplicateSessionIDException e) {
                exclude.add(e.getDuplicateID());
                continue;
            }
        return session;
    }

    @Override
    public synchronized Session createSession(String sessionID) throws DuplicateSessionIDException {
        if (sessionsByID.containsKey(sessionID)) throw new DuplicateSessionIDException(sessionID.toString());
        TcProvider tcp = null;
        if (ontologyProvider.getStore() instanceof TcProvider) tcp = (TcProvider) ontologyProvider.getStore();
        else throw new UnsupportedOperationException(
                "Session manager does not support ontology providers based on "
                        + ontologyProvider.getStore().getClass() + ", only on " + TcProvider.class);
        Session session = new SessionImpl(sessionID, namespace, tcp);
        addSession(session);
        fireSessionCreated(session);
        return session;
    }

    @Override
    public synchronized void destroySession(String sessionID) {
        try {
            Session ses = sessionsByID.get(sessionID);
            if (ses == null) log.warn(
                "Tried to destroy nonexisting session {} . Could it have been previously destroyed?",
                sessionID);
            else {
                ses.close();
                if (ses instanceof SessionImpl) ((SessionImpl) ses).state = State.ZOMBIE;
                // Make session no longer referenceable
                removeSession(ses);
                fireSessionDestroyed(ses);
            }
        } catch (NonReferenceableSessionException e) {
            log.warn("Tried to kick a dead horse on session " + sessionID
                     + " which was already in a zombie state.", e);
        }
    }

    @Override
    public Session getSession(String sessionID) {
        return sessionsByID.get(sessionID);
    }

    @Override
    public Set<String> getRegisteredSessionIDs() {
        return sessionsByID.keySet();
    }

    protected void fireSessionCreated(Session session) {
        SessionEvent e;
        try {
            e = new SessionEvent(session, OperationType.CREATE);
            for (SessionListener l : listeners)
                l.sessionChanged(e);
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error(
                "KReS :: Exception occurred while attempting to fire session creation event for session "
                        + session.getID(), e1);
            return;
        }

    }

    protected void fireSessionDestroyed(Session session) {
        SessionEvent e;
        try {
            e = new SessionEvent(session, OperationType.KILL);
            for (SessionListener l : listeners)
                l.sessionChanged(e);
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error(
                "KReS :: Exception occurred while attempting to fire session destruction event for session "
                        + session.getID(), e1);
            return;
        }

    }

    protected synchronized void addSession(Session session) {
        sessionsByID.put(session.getID(), session);
    }

    protected synchronized void removeSession(Session session) {
        String id = session.getID();
        Session s2 = sessionsByID.get(id);
        if (session == s2) sessionsByID.remove(id);
    }

    @Override
    public Collection<SessionListener> getSessionListeners() {
        return listeners;
    }

    @Override
    public Set<SessionOntologySpace> getSessionSpaces(String sessionID) throws NonReferenceableSessionException {
        Set<SessionOntologySpace> result = new HashSet<SessionOntologySpace>();
        // Brute force search
        for (OntologyScope scope : scopeRegistry.getRegisteredScopes()) {
            SessionOntologySpace space = scope.getSessionSpace(sessionID);
            if (space != null) result.add(space);
        }
        return result;
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void storeSession(String sessionID, OutputStream out) throws NonReferenceableSessionException,
                                                                OWLOntologyStorageException {
        /*
         * For each gession space in the session save all the ontologies contained in the space.
         */
        for (SessionOntologySpace so : getSessionSpaces(sessionID)) {
            for (OWLOntology owlOntology : so.getOntologies(true)) {

                // store.store(owlOntology);

            }
        }

    }

    @Override
    public String getSessionNamespace() {
        return namespace.toString();
    }

    @Override
    public void setSessionNamespace(String namespace) {
        this.namespace = IRI.create(namespace);
    }

}
