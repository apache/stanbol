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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of the {@link Session} interface. A SessionImpl is initially inactive and creates
 * its own identifier.
 * 
 * @author alexdma
 * 
 */
public class SessionImpl implements Session {

    /**
     * A KReS session knows about its own ID.
     */
    protected String id = null;

    protected Set<SessionListener> listeners;

    /**
     * A KReS session knows about its own state.
     */
    State state = State.HALTED;

    /**
     * Utility constructor for enforcing a given IRI as a session ID. It will not throw duplication
     * exceptions, since a KReS session does not know about other sessions.
     * 
     * @param sessionID
     *            the IRI to be set as unique identifier for this session
     */
    public SessionImpl(String sessionID) {
        this.id = sessionID;
        listeners = new HashSet<SessionListener>();
    }

    public SessionImpl(String sessionID, State initialState) throws NonReferenceableSessionException {
        this(sessionID);
        if (initialState == State.ZOMBIE) throw new NonReferenceableSessionException();
        else setActive(initialState == State.ACTIVE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.SessionListenable#addSessionListener
     * (eu.iksproject.kres.api.manager.session.SessionListener)
     */
    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.kres.api.manager.session.SessionListenable# clearSessionListeners()
     */
    @Override
    public void clearSessionListeners() {
        listeners.clear();

    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.Session#close()
     */
    @Override
    public void close() throws NonReferenceableSessionException {
        // if (getSessionState() == State.ZOMBIE)
        // throw new NonReferenceableSessionException();
        // state = State.ZOMBIE;
        this.setActive(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.Session#getID()
     */
    @Override
    public String getID() {
        return id.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.SessionListenable#getSessionListeners ()
     */
    @Override
    public Collection<SessionListener> getSessionListeners() {
        return listeners;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.Session#getSessionState()
     */
    @Override
    public State getSessionState() {
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.Session#isActive()
     */
    @Override
    public boolean isActive() {
        return state == State.ACTIVE;
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.session.Session#setActive(boolean)
     */
    @Override
    public State setActive(boolean active) throws NonReferenceableSessionException {
        if (getSessionState() == State.ZOMBIE) throw new NonReferenceableSessionException();
        else state = active ? State.ACTIVE : State.HALTED;
        return getSessionState();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getID().toString();
    }

    protected void fireClosed() {
        SessionEvent e = null;
        try {
            e = new SessionEvent(this, OperationType.CLOSE);
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error("KReS :: Could not close session " + getID(), e1);
            return;
        }
        for (SessionListener l : listeners)
            l.sessionChanged(e);
    }

    @Override
    public void open() throws NonReferenceableSessionException {
        setActive(true);
    }

    @Override
    public OWLOntology asOWLOntology(boolean merge) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void attachScope(OntologyScope scope) {
        // TODO Auto-generated method stub

    }

    @Override
    public void detachScope(String scopeId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearScopes() {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<String> getAttachedScopes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addOntology(OntologyInputSource<?> ontologySource) throws UnmodifiableOntologyCollectorException {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<OWLOntology> getOntologies(boolean withClosure) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasOntology(IRI ontologyIri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeOntology(IRI ontologyId) throws OntologyCollectorModificationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void addListener(OntologyCollectorListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clearListeners() {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<OntologyCollectorListener> getListeners() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IRI getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeListener(OntologyCollectorListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setNamespace(IRI namespace) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setUp() {
        // TODO Auto-generated method stub

    }

    @Override
    public void tearDown() {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<Class<?>> getSupportedTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
