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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.Lockable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyInputSourceHandler;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.owl.util.OWLUtils;
import org.apache.stanbol.owl.util.URIUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic Clerezza-native implementation of an ontology collector.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractOntologyCollectorImpl implements OntologyCollector, Lockable,
        OntologyInputSourceHandler, OWLExportable {

    protected String _id = null;

    private Set<OntologyCollectorListener> listeners = new HashSet<OntologyCollectorListener>();

    /**
     * Indicates whether this ontology space is marked as read-only. Default value is false.
     */
    protected volatile boolean locked = false;

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The identifier of the ontologies directly managed by this collector (i.e. that were directly added to
     * this space, hence not including those just pulled in via import statements).
     * 
     * TODO make it a set again and have the ontology provider manage the mapping?
     */
    protected Set<IRI> managedOntologies;

    protected IRI namespace = null;

    protected OntologyProvider<?> ontologyProvider;

    protected Set<Class<?>> supportedTypes;

    public AbstractOntologyCollectorImpl(String id, IRI namespace, OntologyProvider<?> ontologyProvider) {
        // Supports OWL API and Clerezza
        supportedTypes = new HashSet<Class<?>>();
        supportedTypes.add(OWLOntology.class);
        supportedTypes.add(TripleCollection.class);
        setID(id);
        setNamespace(namespace);
        this.ontologyProvider = ontologyProvider;
        this.managedOntologies = new HashSet<IRI>();
    }

    @Override
    public void addListener(OntologyCollectorListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized String addOntology(OntologyInputSource<?,?> ontologySource) throws UnmodifiableOntologyCollectorException {

        long before = System.currentTimeMillis();

        if (locked) throw new UnmodifiableOntologyCollectorException(this);

        log.debug("Adding ontology {} to space {}", ontologySource != null ? ontologySource : "<NULL>",
            getNamespace() + getID());
        if (ontologySource == null || !ontologySource.hasRootOntology()) // No ontology to add
        throw new IllegalArgumentException(
                "Ontology source cannot be null and must provide an ontology object.");

        Object o = ontologySource.getRootOntology();
        UriRef uri;
        /*
         * Note for the developer: make sure the call to guessOntologyIdentifier() is only performed once
         * during all the storage process, otherwise multiple calls could return different results for
         * anonymous ontologies.
         */
        if (o instanceof TripleCollection) {
            uri = OWLUtils.guessOntologyIdentifier((TripleCollection) o);
        } else if (o instanceof OWLOntology) {
            uri = new UriRef(OWLUtils.guessOntologyIdentifier((OWLOntology) o).toString());
        } else throw new UnsupportedOperationException("This ontology space implementation cannot handle "
                                                       + o.getClass() + " objects.");

        // Now for the actual storage. We pass the ontology object directly.
        String key = null;
        key = ontologyProvider.loadInStore(o, uri.getUnicodeString(), false);
        /*
         * Actually we are not interested in knowing the key here (ontology collectors are not concerned with
         * them), but knowing it is non-null and non-empty indicates the operation was successful.
         */
        if (key != null && !key.isEmpty()) {
            // add to index
            managedOntologies.add(IRI.create(uri.getUnicodeString()));
            // Note that imported ontologies are not considered as managed! TODO should we change this?
            log.debug("Add ontology completed in {} ms.", (System.currentTimeMillis() - before));
            // fire the event
            fireOntologyAdded(uri);
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O export(Class<O> returnType, boolean merge) {
        if (OWLOntology.class.isAssignableFrom(returnType)) return (O) exportToOWLOntology(merge);
        if (TripleCollection.class.isAssignableFrom(returnType)) {
            if (merge) throw new UnsupportedOperationException(
                    "Merge not implemented yet for Clerezza triple collections.");
            // No need to store, give it a name, or anything.
            TripleCollection root = new SimpleMGraph();
            UriRef iri = new UriRef(namespace + _id);
            // Add the import declarations for directly managed ontologies.
            if (root != null) {
                String base = URIUtils.upOne(IRI.create(namespace + getID())) + "/";
                // The key set of managedOntologies contains the ontology IRIs, not their storage keys.
                for (IRI ontologyIri : managedOntologies) {
                    UriRef physIRI = new UriRef(base + ontologyIri);
                    root.add(new TripleImpl(iri, OWL.imports, physIRI));
                }
            }
            return (O) root;
        }
        throw new UnsupportedOperationException("Cannot export to " + returnType);
    }

    @Override
    public OWLOntology asOWLOntology(boolean merge) {
        return export(OWLOntology.class, merge);
    }

    private OWLOntology exportToOWLOntology(boolean merge) {

        long before = System.currentTimeMillis();

        // Create a new ontology
        OWLOntology root;
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        IRI iri = IRI.create(namespace + _id);
        try {
            root = ontologyManager.createOntology(iri);
        } catch (OWLOntologyAlreadyExistsException e) {
            // It should be impossible, but just in case.
            ontologyManager.removeOntology(ontologyManager.getOntology(iri));
            try {
                root = ontologyManager.createOntology(iri);
            } catch (OWLOntologyAlreadyExistsException e1) {
                root = ontologyManager.getOntology(iri);
            } catch (OWLOntologyCreationException e1) {
                log.error("Failed to assemble root ontology for scope " + iri, e);
                root = null;
            }
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to assemble root ontology for scope " + _id, e);
            root = null;
        }

        // Add the import declarations for directly managed ontologies.
        if (root != null) {

            if (merge) {
                final Set<OWLOntology> set = new HashSet<OWLOntology>();
                log.debug("Merging {} with its imports.", root);
                set.add(root);

                for (IRI ontologyIri : managedOntologies) {
                    log.debug("Merging {} with {}.", ontologyIri, root);
                    set.add(getOntology(ontologyIri, true));
                }

                OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                    @Override
                    public Set<OWLOntology> getOntologies() {
                        return set;
                    }
                };
                OWLOntologyMerger merger = new OWLOntologyMerger(provider);
                try {
                    root = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), iri);
                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to merge imports for ontology " + iri, e);
                    root = null;
                }

            } else {
                // Add the import declarations for directly managed ontologies.
                List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
                OWLDataFactory df = ontologyManager.getOWLDataFactory();

                String base = URIUtils.upOne(IRI.create(namespace + getID())) + "/";

                // The key set of managedOntologies contains the ontology IRIs, not their storage keys.
                for (IRI ontologyIri : managedOntologies) {
                    IRI physIRI = IRI.create(base + ontologyIri);
                    changes.add(new AddImport(root, df.getOWLImportsDeclaration(physIRI)));
                }

                ontologyManager.applyChanges(changes);
            }

        }
        log.debug("OWL export of {} completed in {} ms.", getID(), System.currentTimeMillis() - before);

        return root;
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * Notifies all ontology space listeners that an ontology has been added to this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was added to this space.
     */
    protected void fireOntologyAdded(IRI ontologyIri) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyAdded(this.getID(), ontologyIri);
    }

    /**
     * Notifies all ontology space listeners that an ontology has been added to this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was added to this space.
     */
    protected void fireOntologyAdded(UriRef ontologyIri) {
        fireOntologyAdded(IRI.create(ontologyIri.getUnicodeString()));
    }

    /**
     * Notifies all ontology space listeners that an ontology has been removed from this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was removed from this space.
     */
    protected void fireOntologyRemoved(IRI ontologyIri) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyRemoved(this.getID(), ontologyIri);
    }

    protected void fireOntologyRemoved(UriRef ontologyIri) {
        fireOntologyRemoved(IRI.create(ontologyIri.getUnicodeString()));
    }

    @Override
    public String getID() {
        return _id;
    }

    @Override
    public Collection<OntologyCollectorListener> getListeners() {
        return listeners;
    }

    @Override
    public IRI getNamespace() {
        return namespace;
    }

    /**
     * FIXME not including closure yet.
     * 
     * @see OntologySpace#getOntologies(boolean)
     */
    @Override
    public Set<OWLOntology> getOntologies(boolean withClosure) {
        // if (withClosure) throw new UnsupportedOperationException(
        // "Closure support not implemented efficiently yet. Please call getOntologies(false) and compute the closure union for the OWLOntology objects in the set.");
        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        for (IRI id : managedOntologies) {
            // FIXME temporary fix is to merge instead of including closure
            ontologies.add(getOntology(id, withClosure));
        }
        return Collections.unmodifiableSet(ontologies);
    }

    @Override
    public int getOntologyCount(boolean withClosure) {
        if (withClosure) throw new UnsupportedOperationException(
                "Closure support not implemented efficiently yet. Please call getOntologyCount(false).");
        return managedOntologies.size();
    }

    @Override
    public Set<Class<?>> getSupportedOntologyTypes() {
        return Collections.unmodifiableSet(supportedTypes);
    }

    @Override
    public boolean hasOntology(IRI ontologyIri) {
        return managedOntologies.contains(ontologyIri);
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void removeListener(OntologyCollectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void removeOntology(IRI ontologyId) throws OntologyCollectorModificationException {
        if (locked) throw new UnmodifiableOntologyCollectorException(this);
        try {
            managedOntologies.remove(ontologyId);
            fireOntologyRemoved(ontologyId);
        } catch (RuntimeException ex) {
            throw new OntologyCollectorModificationException(this, ex);
        }
    }

    protected abstract void setID(String id);

    /**
     * @param namespace
     *            The OntoNet namespace that will prefix the space ID in Web references. This implementation
     *            only allows non-null and non-empty IRIs, with no query or fragment. Hash URIs are not
     *            allowed, slash URIs are preferred. If neither, a slash will be concatenated and a warning
     *            will be logged.
     * 
     * @see OntologySpace#setNamespace(IRI)
     */
    @Override
    public void setNamespace(IRI namespace) {
        if (namespace == null) throw new IllegalArgumentException("Namespace cannot be null.");
        if (namespace.toURI().getQuery() != null) throw new IllegalArgumentException(
                "URI Query is not allowed in OntoNet namespaces.");
        if (namespace.toURI().getFragment() != null) throw new IllegalArgumentException(
                "URI Fragment is not allowed in OntoNet namespaces.");
        if (namespace.toString().endsWith("#")) throw new IllegalArgumentException(
                "OntoNet namespaces must not end with a hash ('#') character.");
        if (!namespace.toString().endsWith("/")) {
            log.warn("Namespace {} does not end with slash character ('/'). It will be added automatically.",
                namespace);
            namespace = IRI.create(namespace + "/");
        }
        this.namespace = namespace;
    }

    @Override
    public void setUp() {
        this.locked = true;
    }

    @Override
    public void tearDown() {
        this.locked = false;
    }

}
