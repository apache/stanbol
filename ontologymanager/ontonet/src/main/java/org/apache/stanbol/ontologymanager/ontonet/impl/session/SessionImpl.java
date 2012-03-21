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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.AbstractOntologyCollectorImpl;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of the {@link Session} interface. A SessionImpl is initially inactive and creates
 * its own identifier.
 * 
 * @author alexdma
 * 
 */
public class SessionImpl extends AbstractOntologyCollectorImpl implements Session {

    protected Map<String,OntologyScope> attachedScopes;

    protected Set<SessionListener> listeners;

    /**
     * A session knows its own state.
     */
    State state = State.HALTED;

    /**
     * Utility constructor for enforcing a given IRI as a session ID. It will not throw duplication
     * exceptions, since a KReS session does not know about other sessions.
     * 
     * @param sessionID
     *            the IRI to be set as unique identifier for this session
     */
    public SessionImpl(String sessionID, IRI namespace, OntologyProvider<?> ontologyProvider) {
        super(sessionID, namespace, ontologyProvider);
        backwardPathLength = 0;
        // setNamespace(namespace);
        attachedScopes = new HashMap<String,OntologyScope>();
        listeners = new HashSet<SessionListener>();
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void attachScope(OntologyScope scope) {
        attachedScopes.put(scope.getID(), scope);
    }

    @Override
    public void clearScopes() {
        attachedScopes.clear();
    }

    @Override
    public void clearSessionListeners() {
        listeners.clear();
    }

    @Override
    public void close() throws NonReferenceableSessionException {
        // if (getSessionState() == State.ZOMBIE)
        // throw new NonReferenceableSessionException();
        // state = State.ZOMBIE;
        this.setActive(false);
    }

    @Override
    public void detachScope(String scopeId) {
        attachedScopes.remove(scopeId);
    }

    @Override
    protected MGraph exportToMGraph(boolean merge) {
        MGraph mg = super.exportToMGraph(merge);
        // Add import declarations for attached scopes.
        for (String scopeID : attachedScopes.keySet()) {
            UriRef iri = new UriRef(namespace + _id);
            UriRef physIRI = new UriRef(attachedScopes.get(scopeID).getDocumentIRI().toString());
            mg.add(new TripleImpl(iri, OWL.imports, physIRI));
        }
        return mg;
    }

    /**
     * TODO support merging for attached scopes as well?
     */
    @Override
    protected OWLOntology exportToOWLOntology(boolean merge) {
        OWLOntology o = super.exportToOWLOntology(merge);
        if (!attachedScopes.isEmpty()) {
            List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
            OWLOntologyManager ontologyManager = o.getOWLOntologyManager();
            OWLDataFactory df = ontologyManager.getOWLDataFactory();
            // Add import declarations for attached scopes.
            for (String scopeID : attachedScopes.keySet()) {
                IRI physIRI = attachedScopes.get(scopeID).getDocumentIRI();
                changes.add(new AddImport(o, df.getOWLImportsDeclaration(physIRI)));
            }
            // Commit
            ontologyManager.applyChanges(changes);
        }
        return o;
    }

    protected void fireClosed() {
        SessionEvent e = null;
        try {
            e = new SessionEvent(this, OperationType.CLOSE);
        } catch (Exception e1) {
            LoggerFactory.getLogger(getClass()).error("Could not close session " + getID(), e1);
            return;
        }
        for (SessionListener l : listeners)
            l.sessionChanged(e);
    }

    @Override
    public Set<String> getAttachedScopes() {
        return attachedScopes.keySet();
    }

    @Override
    public Collection<SessionListener> getSessionListeners() {
        return listeners;
    }

    @Override
    public State getSessionState() {
        return state;
    }

    @Override
    public boolean isActive() {
        return state == State.ACTIVE;
    }

    @Override
    public void open() throws NonReferenceableSessionException {
        setActive(true);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public State setActive(boolean active) throws NonReferenceableSessionException {
        if (getSessionState() == State.ZOMBIE) throw new NonReferenceableSessionException();
        else state = active ? State.ACTIVE : State.HALTED;
        return getSessionState();
    }

    @Override
    protected void setID(String id) {
        // TODO check form of ID
        this._id = id;
    }

    @Override
    public String toString() {
        return getID().toString();
    }

}
