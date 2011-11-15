package org.apache.stanbol.reasoners.web.input.provider.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.owl.transformation.JenaToOwlConvert;
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

public class OntonetInputProvider implements ReasoningServiceInputProvider {

    private final Logger log = LoggerFactory.getLogger(UrlInputProvider.class);

    private String scopeId;
    private String sessionId;
    private ONManager onManager;

    public OntonetInputProvider(ONManager onManager, String scopeId) {
        this.onManager = onManager;
        this.scopeId = scopeId;
        this.sessionId = null;
    }

    public OntonetInputProvider(ONManager onManager, String scopeId, String sessionId) {
        this.onManager = onManager;
        this.scopeId = scopeId;
        this.sessionId = sessionId;
    }

    @Override
    public <T> Iterator<T> getInput(Class<T> type) throws IOException {
        // This ontology is already a merged version, no need to iterate over imported ones
        final OWLOntology o = getFromOntonet();
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

    private OWLOntology getFromOntonet() throws IOException {
        try {
            OntologyScope scope = null;
            synchronized (onManager) {
                scope = onManager.getScopeRegistry().getScope(this.scopeId);

            }
            if (scope == null) {
                log.error("Scope {} cannot be retrieved", this.scopeId);
                throw new IOException("Scope " + this.scopeId + " cannot be retrieved");
            }
            OntologySpace sessionSpace = null;
            if (this.sessionId != null) {
                IRI sessionIRI = IRI.create(this.sessionId);
                sessionSpace = scope.getSessionSpace(sessionIRI.toString());
            }
            OntologySpace coreSpace = scope.getCoreSpace();
            Set<OWLOntology> coreOntologies = coreSpace.getOntologies(true);
            log.info("Found {} ontologies in core space", coreOntologies.size());
            OntologySpace customSpace = scope.getCustomSpace();
            Set<OWLOntology> customOntologies = customSpace.getOntologies(true);
            log.info("Found {} ontologies in custom space", coreOntologies.size());

            Set<OWLOntology> sessionOntologies = new HashSet<OWLOntology>();
            log.info("Found {} ontologies in session space", coreOntologies.size());

            if (sessionSpace != null) {
                // FIXME
                // We collect all the ontologies in session (here we use
                // 'false')
                // The reason is that the set contains also an ontology which is
                // the
                // root of the session space, with buggy owl:import
                sessionOntologies.addAll(sessionSpace.getOntologies(false));
            }

            final Set<OWLOntology> set = new HashSet<OWLOntology>();
            set.addAll(coreOntologies);
            set.addAll(customOntologies);
            set.addAll(sessionOntologies);
            /**
             * Now we merge the ontologies
             */
            OWLOntologyMerger merger = new OWLOntologyMerger(new OWLOntologySetProvider() {
                @Override
                public Set<OWLOntology> getOntologies() {
                    return set;
                }
            });
            return merger.createMergedOntology(createOWLOntologyManager(),
                IRI.create("reasoners:input-" + System.currentTimeMillis()));
        } catch (OWLOntologyCreationException e) {
            log.error("The network for scope/session cannot be retrieved:", e);
            throw new IllegalArgumentException("The network for scope/session cannot be retrieved");
        }
    }

    @SuppressWarnings("deprecation")
    private OWLOntologyManager createOWLOntologyManager() {
        // We isolate here the creation of the temporary manager
        // TODO How to behave when resolving owl:imports?
        // We should set the manager to use a service to lookup for ontologies,
        // instead of trying on the web
        // directly
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
