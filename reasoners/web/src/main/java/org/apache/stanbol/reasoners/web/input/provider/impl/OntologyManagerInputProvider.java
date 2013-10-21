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
package org.apache.stanbol.reasoners.web.input.provider.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.commons.owl.transformation.JenaToOwlConvert;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * An input provider which binds the reasoners input to Ontonet
 * 
 * @author enridaga
 * 
 */
public class OntologyManagerInputProvider implements ReasoningServiceInputProvider {

    private final Logger log = LoggerFactory.getLogger(UrlInputProvider.class);

    private String scopeId;
    private String sessionId;
    private ScopeManager onManager;
    private SessionManager sessionManager;

    /**
     * Contructor, if the input is a Scope
     * 
     * @param onManager
     * @param scopeId
     */
    public OntologyManagerInputProvider(ScopeManager onManager, String scopeId) {
        this(onManager, null, scopeId, null);
    }

    /**
     * Constructor, if the input are a Scope and a Session
     * 
     * @param onManager
     * @param scopeId
     * @param sessionId
     */
    public OntologyManagerInputProvider(ScopeManager onManager,
                                        SessionManager sessionManager,
                                        String scopeId,
                                        String sessionId) {
        this.onManager = onManager;
        this.scopeId = scopeId;
        this.sessionManager = sessionManager;
        this.sessionId = sessionId;
    }

    @Override
    public <T> Iterator<T> getInput(Class<T> type) throws IOException {
        // This ontology is already a merged version, no need to iterate over imported ones
        final OWLOntology o = getFromOntoMgr();
        if (type.isAssignableFrom(OWLAxiom.class)) {
            final Iterator<OWLAxiom> iterator = o.getAxioms().iterator();
            return new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @SuppressWarnings("unchecked")
                @Override
                public T next() {
                    return (T) iterator.next();
                }

                @Override
                public void remove() {
                    // This iterator is read-only
                    throw new UnsupportedOperationException("Cannot remove statements from the iterator");
                }

            };
        } else if (type.isAssignableFrom(Statement.class)) {
            final OntModel input = new JenaToOwlConvert().ModelOwlToJenaConvert(o, "RDF/XML");
            final StmtIterator iterator = input.listStatements();
            return new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @SuppressWarnings("unchecked")
                @Override
                public T next() {
                    return (T) iterator.next();
                }

                @Override
                public void remove() {
                    // This iterator is read-only
                    throw new UnsupportedOperationException("Cannot remove statements from the iterator");
                }
            };
        } else {
            throw new UnsupportedOperationException("This provider does not adapt to the given type");
        }
    }

    @Override
    public <T> boolean adaptTo(Class<T> type) {
        if (type.isAssignableFrom(OWLAxiom.class) || type.isAssignableFrom(Statement.class)) return true;
        return false;
    }

    private OWLOntology getFromOntoMgr() throws IOException {
        try {
            Scope scope = null;
            synchronized (onManager) {
                scope = onManager.getScope(this.scopeId);
            }
            if (scope == null) {
                log.error("Scope {} cannot be retrieved", this.scopeId);
                throw new IOException("Scope " + this.scopeId + " cannot be retrieved");
            }
            Session session = null;
            if (sessionManager != null) synchronized (sessionManager) {
                session = sessionManager.getSession(sessionId);
            }
            if (session == null) log.warn("Session {} cannot be retrieved. Ignoring.", this.sessionId);
            final Set<OWLOntology> set = new HashSet<OWLOntology>();
            set.add(scope.export(OWLOntology.class, true));
            if (session != null) set.add(session.export(OWLOntology.class, true));
            if (set.size() == 1) return set.iterator().next();
            OWLOntologyMerger merger = new OWLOntologyMerger(new OWLOntologySetProvider() {
                @Override
                public Set<OWLOntology> getOntologies() {
                    return set;
                }
            });
            return merger.createMergedOntology(createOWLOntologyManager(),
                IRI.create("reasoners:input-" + System.currentTimeMillis()));
        } catch (OWLOntologyCreationException e) {
            String message = "The network for scope/session cannot be retrieved";
            log.error(message + ":", e);
            throw new IllegalArgumentException(message);
        }
    }

    @SuppressWarnings("deprecation")
    private OWLOntologyManager createOWLOntologyManager() {
        // We isolate here the creation of the temporary manager
        // TODO How to behave when resolving owl:imports?
        // We should set the manager to use a service to lookup for ontologies,
        // instead of trying on the web directly
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // FIXME Which is the other way of doing this?
        // Maybe -> OWLOntologyManagerProperties();
        manager.setSilentMissingImportsHandling(true);
        // Listening for missing imports
        manager.addMissingImportListener(new MissingImportListener() {
            @Override
            public void importMissing(MissingImportEvent arg0) {
                log.warn("Missing import {} ", arg0.getImportedOntologyURI());
            }
        });
        manager.addOntologyLoaderListener(new OWLOntologyLoaderListener() {

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
                log.info("Finished loading {} (imported: {})", arg0.getOntologyID(), arg0.isImported());
            }

            @Override
            public void startedLoadingOntology(LoadingStartedEvent arg0) {
                log.info("Started loading {} (imported: {}) ...", arg0.getOntologyID(), arg0.isImported());
                log.info(" ... from {}", arg0.getDocumentIRI().toString());
            }
        });
        return manager;
    }
}
