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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.ontologymanager.core.scope.ScopeManagerImpl;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionEvent;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionListener;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of the {@link Session} interface. A SessionImpl is initially inactive and creates
 * its own identifier.
 * 
 * @author alexdma
 * 
 */
public class SessionImpl extends AbstractOntologyCollectorImpl implements Session {

    protected Set<String> attachedScopes;

    protected Set<SessionListener> listeners;

    /**
     * A session knows its own state.
     */
    public State state = State.HALTED;

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
        attachedScopes = new HashSet<String>();
        listeners = new HashSet<SessionListener>();
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void attachScope(String scopeId) {
        attachedScopes.add(scopeId);
        fireScopeAppended(scopeId);
    }

    private void attachScopeImportsClerezza(TripleCollection target, IRI prefix) {
        UriRef iri = new UriRef(prefix + _id);
        String scopePrefix = prefix.toString();
        scopePrefix = scopePrefix.substring(0, scopePrefix.lastIndexOf("/" + shortName + "/")) + "/ontology/";
        for (String scopeID : attachedScopes) {
            UriRef physIRI = new UriRef(scopePrefix + scopeID);
            target.add(new TripleImpl(iri, OWL.imports, physIRI));
        }
    }

    private void attachScopeImportsOwlApi(OWLOntology target, IRI prefix) {
        if (!attachedScopes.isEmpty()) {
            String scopePrefix = prefix.toString();
            scopePrefix = scopePrefix.substring(0, scopePrefix.lastIndexOf("/" + shortName + "/"))
                          + "/ontology/";
            List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
            OWLOntologyManager ontologyManager = target.getOWLOntologyManager();
            OWLDataFactory df = ontologyManager.getOWLDataFactory();
            // Add import declarations for attached scopes.
            for (String scopeID : attachedScopes) {
                IRI physIRI = IRI.create(scopePrefix + scopeID);
                changes.add(new AddImport(target, df.getOWLImportsDeclaration(physIRI)));
            }
            // Commit
            ontologyManager.applyChanges(changes);
        }
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
        if (!attachedScopes.contains(scopeId)) return;
        attachedScopes.remove(scopeId);
        fireScopeDetached(scopeId);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof Session)) return false;
        if (this == arg0) return true;
        log.warn(
            "{} only implements weak equality, i.e. managed ontologies are only checked by public key, not by content.",
            getClass());
        Session coll = (Session) arg0;
        return super.equals(arg0) && this.getAttachedScopes().equals(coll.getAttachedScopes())
               && this.getAttachedScopes().equals(coll.getAttachedScopes())
               && this.getSessionState().equals(coll.getSessionState());
    }

    @Override
    protected MGraph exportToMGraph(boolean merge, IRI universalPrefix) {
        MGraph mg = super.exportToMGraph(merge, universalPrefix);
        attachScopeImportsClerezza(mg, universalPrefix);
        return mg;
    }

    /**
     * TODO support merging for attached scopes as well?
     */
    @Override
    protected OWLOntology exportToOWLOntology(boolean merge, IRI universalPrefix) {
        OWLOntology o = super.exportToOWLOntology(merge, universalPrefix);

        IRI iri = o.getOntologyID().getOntologyIRI();

        if (merge) { // Re-merge
            ScopeManager onm = ScopeManagerImpl.get(); // FIXME try to avoid this.
            final Set<OWLOntology> set = new HashSet<OWLOntology>();
            set.add(o);
            for (String scopeID : attachedScopes) {
                log.debug(" ... Merging with attached scope {}.", scopeID);

                Scope sc = onm.getScope(scopeID);
                if (sc != null)

                set.add(sc.export(OWLOntology.class, merge));

                for (OWLOntologyID ontologyId : managedOntologies) {
                    set.add(getOntology(ontologyId, OWLOntology.class, true));
                }

                OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                    @Override
                    public Set<OWLOntology> getOntologies() {
                        return set;
                    }
                };
                OWLOntologyMerger merger = new OWLOntologyMerger(provider);
                try {
                    o = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), iri);
                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to merge imports for ontology " + iri, e);
                    o = null;
                }

            }

        } else

        attachScopeImportsOwlApi(o, universalPrefix);
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

    protected void fireScopeAppended(String scopeId) {
        for (SessionListener l : listeners)
            l.scopeAppended(this, scopeId);
    }

    protected void fireScopeDetached(String scopeId) {
        for (SessionListener l : listeners)
            l.scopeDetached(this, scopeId);
    }

    @Override
    public Set<String> getAttachedScopes() {
        return attachedScopes;
    }

    @Override
    protected MGraph getOntologyAsMGraph(OWLOntologyID ontologyId, boolean merge, IRI universalPrefix) {
        MGraph o = super.getOntologyAsMGraph(ontologyId, merge, universalPrefix);
        switch (getConnectivityPolicy()) {
            case LOOSE:
                break;
            case TIGHT:
                attachScopeImportsClerezza(o, universalPrefix);
                break;
            default:
                break;
        }
        return o;
    }

    @Override
    protected OWLOntology getOntologyAsOWLOntology(OWLOntologyID ontologyId,
                                                   boolean merge,
                                                   IRI universalPrefix) {
        OWLOntology o = super.getOntologyAsOWLOntology(ontologyId, merge, universalPrefix);
        switch (getConnectivityPolicy()) {
            case LOOSE:
                break;
            case TIGHT:
                attachScopeImportsOwlApi(o, universalPrefix);
                break;
            default:
                break;
        }
        return o;
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
        return getID();
    }

}
