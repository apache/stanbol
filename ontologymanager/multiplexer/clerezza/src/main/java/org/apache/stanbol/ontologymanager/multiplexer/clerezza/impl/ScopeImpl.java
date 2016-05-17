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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of an ontology scope.
 * 
 * @author alexdma
 * 
 */
public class ScopeImpl implements Scope, OntologyCollectorListener {

    /**
     * The core ontology space for this scope, always set as default.
     */
    protected OntologySpace coreSpace;

    /**
     * The custom ontology space for this scope. This is optional, but cannot be set after the scope has been
     * setup.
     */
    protected OntologySpace customSpace;

    /**
     * The unique identifier for this scope.
     */
    protected String id = null;

    private Set<OntologyCollectorListener> listeners = new HashSet<OntologyCollectorListener>();

    /**
     * An ontology scope knows whether it's write-locked or not. Initially it is not.
     */
    protected volatile boolean locked = false;

    private Logger log = LoggerFactory.getLogger(getClass());

    protected org.semanticweb.owlapi.model.IRI namespace = null;

    public ScopeImpl(String id,
                     org.semanticweb.owlapi.model.IRI namespace,
                     OntologySpaceFactory factory,
                     OntologyInputSource<?>... coreOntologies) {
        setID(id);
        setDefaultNamespace(namespace);
        configureCoreSpace(factory);
        for (OntologyInputSource<?> src : coreOntologies)
            this.coreSpace.addOntology(src);
        // let's just lock it. Once the core space is done it's done.
        this.coreSpace.setUp();
        configureCustomSpace(factory);
    }

    @Override
    public void addOntologyCollectorListener(OntologyCollectorListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearOntologyCollectorListeners() {
        listeners.clear();
    }

    private void configureCoreSpace(OntologySpaceFactory factory) {
        this.coreSpace = factory.createCoreOntologySpace(id/* , coreOntologies */);
        this.coreSpace.addOntologyCollectorListener(this); // Set listener before adding core ontologies

    }

    private void configureCustomSpace(OntologySpaceFactory factory) {
        try {
            setCustomSpace(factory.createCustomOntologySpace(id));
        } catch (UnmodifiableOntologyCollectorException e) {
            // Cannot happen unless the factory or space implementations are really naughty.
            log.warn(
                "Ontology scope "
                        + id
                        + " was denied creation of its own custom space upon initialization! This should not happen.",
                e);
        }
        this.customSpace.addOntologyCollectorListener(this);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof Scope)) return false;
        Scope sc = (Scope) arg0;
//        return this.getID().equals(sc.getID()) && this.getDefaultNamespace().equals(sc.getDefaultNamespace())
//               && this.getCoreSpace().equals(sc.getCoreSpace())
//               && this.getCustomSpace().equals(sc.getCustomSpace());
if (!this.getID().equals(sc.getID())) return false;
if (!this.getDefaultNamespace().equals(sc.getDefaultNamespace())) return false;
if (!this.getCoreSpace().equals(sc.getCoreSpace())) return false;
if (!this.getCustomSpace().equals(sc.getCustomSpace())) return false;
        return true;

    }

    @Override
    public <O> O export(Class<O> returnType, boolean merge) {
        return export(returnType, merge, getDefaultNamespace());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O export(Class<O> returnType, boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        if (OWLOntology.class.isAssignableFrom(returnType)) {
            return (O) exportToOWLOntology(merge, universalPrefix);
        }
        if (Graph.class.isAssignableFrom(returnType)) {
            Graph root = exportToGraph(merge, universalPrefix);
            // A Clerezza graph has to be cast properly.
            if (returnType == ImmutableGraph.class) root = ((Graph) root).getImmutableGraph();
            else if (returnType == Graph.class) {}
            return (O) root;
        }
        throw new UnsupportedOperationException("Cannot export scope " + getID() + " to a " + returnType);
    }

    /**
     * Get a Clerezza {@link Graph} representation of the scope.
     * 
     * @param merge
     *            if true the core and custom spaces will be recursively merged with the scope graph,
     *            otherwise owl:imports statements will be added.
     * @return the RDF representation of the scope as a modifiable graph.
     */
    protected Graph exportToGraph(boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {

        // No need to store, give it a name, or anything.
        Graph root = new SimpleGraph();
        IRI iri = new IRI(universalPrefix + getID());

        if (root != null) {
            // Set the ontology ID
            root.add(new TripleImpl(iri, RDF.type, OWL.Ontology));

            if (merge) {

                ImmutableGraph custom, core;

                // Get the subjects of "bad" triples (those with subjects of type owl:Ontology).
                Iterator<Triple> it;
                Set<BlankNodeOrIRI> ontologies = new HashSet<BlankNodeOrIRI>();
                Set<RDFTerm> importTargets = new HashSet<RDFTerm>();
                custom = this.getCustomSpace().export(ImmutableGraph.class, merge);
                // root.addAll(space);
                it = custom.filter(null, RDF.type, OWL.Ontology);
                while (it.hasNext())
                    ontologies.add(it.next().getSubject());
                it = custom.filter(null, OWL.imports, null);
                while (it.hasNext())
                    importTargets.add(it.next().getObject());
                core = this.getCoreSpace().export(ImmutableGraph.class, merge);
                // root.addAll(space);
                it = core.filter(null, RDF.type, OWL.Ontology);
                while (it.hasNext())
                    ontologies.add(it.next().getSubject());
                it = core.filter(null, OWL.imports, null);
                while (it.hasNext())
                    importTargets.add(it.next().getObject());

                // Make sure the scope itself is not in the "bad" subjects.
                ontologies.remove(iri);

                for (BlankNodeOrIRI nl : ontologies)
                    log.debug("{} -related triples will not be added to {}", nl, iri);

                // Merge the two spaces, skipping the "bad" triples.
                log.debug("Merging custom space of {}.", getID());
                for (Triple t : custom)
                    if (!ontologies.contains(t.getSubject())) root.add(t);
                log.debug("Merging core space of {}.", getID());
                for (Triple t : core)
                    if (!ontologies.contains(t.getSubject())) root.add(t);

                /*
                 * Reinstate import statements, though. If imported ontologies were not merged earlier, we are
                 * not doing it now anyway.
                 */
                for (RDFTerm target : importTargets)
                    root.add(new TripleImpl(iri, OWL.imports, target));

            } else {
                IRI physIRI = new IRI(universalPrefix.toString() + this.getID() + "/"
                                            + SpaceType.CUSTOM.getIRISuffix());
                root.add(new TripleImpl(iri, OWL.imports, physIRI));
                physIRI = new IRI(universalPrefix.toString() + this.getID() + "/"
                                     + SpaceType.CORE.getIRISuffix());
                root.add(new TripleImpl(iri, OWL.imports, physIRI));
            }
        }
        return root;

    }

    /**
     * Get an OWL API {@link OWLOntology} representation of the scope.
     * 
     * @param merge
     *            if true the core and custom spaces will be recursively merged with the scope ontology,
     *            otherwise owl:imports statements will be added.
     * @return the OWL representation of the scope.
     */
    protected OWLOntology exportToOWLOntology(boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        // if (merge) throw new UnsupportedOperationException(
        // "Ontology merging only implemented for managed ontologies, not for collectors. "
        // + "Please set merge parameter to false.");
        // Create an ontology manager on the fly. We don't really need a permanent one.
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = mgr.getOWLDataFactory();
        OWLOntology ont = null;
        try {

            if (merge) {
                final Set<OWLOntology> set = new HashSet<OWLOntology>();

                log.debug("Merging custom space of {}.", getID());
                set.add(this.getCustomSpace().export(OWLOntology.class, merge));

                log.debug("Merging core space of {}.", getID());
                set.add(this.getCoreSpace().export(OWLOntology.class, merge));

                OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                    @Override
                    public Set<OWLOntology> getOntologies() {
                        return set;
                    }
                };
                OWLOntologyMerger merger = new OWLOntologyMerger(provider);
                try {
                    ont = merger.createMergedOntology(OWLManager.createOWLOntologyManager(),
                        org.semanticweb.owlapi.model.IRI.create(getDefaultNamespace() + getID()));
                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to merge imports for ontology.", e);
                    ont = null;
                }
            } else {
                // The root ontology ID is in the form [namespace][scopeId]
                ont = mgr.createOntology(org.semanticweb.owlapi.model.IRI.create(universalPrefix + getID()));
                List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
                // Add the import statement for the custom space, if existing and not empty
                OntologySpace spc = getCustomSpace();
                if (spc != null && spc.listManagedOntologies().size() > 0) {
                    org.semanticweb.owlapi.model.IRI spaceIri = org.semanticweb.owlapi.model.IRI.create(universalPrefix + spc.getID());
                    additions.add(new AddImport(ont, df.getOWLImportsDeclaration(spaceIri)));
                }
                // Add the import statement for the core space, if existing and not empty
                spc = getCoreSpace();
                if (spc != null && spc.listManagedOntologies().size() > 0) {
                    org.semanticweb.owlapi.model.IRI spaceIri = org.semanticweb.owlapi.model.IRI.create(universalPrefix + spc.getID());
                    additions.add(new AddImport(ont, df.getOWLImportsDeclaration(spaceIri)));
                }
                mgr.applyChanges(additions);
            }
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to generate an OWL form of scope " + getID(), e);
            ont = null;
        }
        return ont;
    }

    protected void fireOntologyAdded(OntologySpace space, OWLOntologyID addedOntology) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyAdded(space, addedOntology);
    }

    protected void fireOntologyRemoved(OntologySpace space, OWLOntologyID removedOntology) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyRemoved(space, removedOntology);
    }

    @Override
    public ConnectivityPolicy getConnectivityPolicy() {
        return ConnectivityPolicy.LOOSE;
    }

    @Override
    public OntologySpace getCoreSpace() {
        return coreSpace;
    }

    @Override
    public OntologySpace getCustomSpace() {
        return customSpace;
    }

    @Override
    public org.semanticweb.owlapi.model.IRI getDefaultNamespace() {
        return this.namespace;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public org.semanticweb.owlapi.model.IRI getNamespace() {
        return getDefaultNamespace();
    }

    @Override
    public Collection<OntologyCollectorListener> getOntologyCollectorListeners() {
        return listeners;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void onOntologyAdded(OntologyCollector collector, OWLOntologyID addedOntology) {
        // Propagate events to scope listeners
        if (collector instanceof OntologySpace) fireOntologyAdded((OntologySpace) collector, addedOntology);
    }

    @Override
    public void onOntologyRemoved(OntologyCollector collector, OWLOntologyID removedOntology) {
        // Propagate events to scope listeners
        if (collector instanceof OntologySpace) fireOntologyRemoved((OntologySpace) collector,
            removedOntology);
    }

    @Override
    public void removeOntologyCollectorListener(OntologyCollectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setConnectivityPolicy(ConnectivityPolicy policy) {
        throw new UnsupportedOperationException(
                "Cannot set connectivity policy on scopes. Ontology scopes only allow LOOSE connectivity policy (set by default).");
    }

    @Override
    public synchronized void setCustomSpace(OntologySpace customSpace) throws UnmodifiableOntologyCollectorException {
        if (this.customSpace != null && this.customSpace.isLocked()) throw new UnmodifiableOntologyCollectorException(
                getCustomSpace());
        this.customSpace = customSpace;
        this.customSpace.addOntologyCollectorListener(this);
    }

    /**
     * @param namespace
     *            The OntoNet namespace that will prefix the scope ID in Web references. This implementation
     *            only allows non-null and non-empty IRIs, with no query or fragment. Hash URIs are not
     *            allowed, slash URIs are preferred. If neither, a slash will be concatenated and a warning
     *            will be logged.
     */
    @Override
    public void setDefaultNamespace(org.semanticweb.owlapi.model.IRI namespace) {
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
            namespace = org.semanticweb.owlapi.model.IRI.create(namespace + "/");
        }
        this.namespace = namespace;
    }

    protected void setID(String id) {
        if (id == null) throw new IllegalArgumentException("Scope ID cannot be null.");
        id = id.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Scope ID cannot be empty.");
        if (!id.matches("[\\w-\\.]+")) throw new IllegalArgumentException(
                "Illegal scope ID " + id
                        + " - Must be an alphanumeric sequence, with optional underscores, dots or dashes.");
        this.id = id;
    }

    @Override
    public void setNamespace(org.semanticweb.owlapi.model.IRI namespace) {
        setDefaultNamespace(namespace);
    }

    @Override
    public synchronized void setUp() {
        if (locked || (customSpace != null && !customSpace.isLocked())) return;
        this.coreSpace.addOntologyCollectorListener(this);
        this.coreSpace.setUp();
        if (this.customSpace != null) {
            this.customSpace.addOntologyCollectorListener(this);
            this.customSpace.setUp();
        }
        locked = true;
    }

    @Override
    public synchronized void tearDown() {
        // this.coreSpace.addOntologySpaceListener(this);
        this.coreSpace.tearDown();
        if (this.customSpace != null) {
            // this.customSpace.addOntologySpaceListener(this);
            this.customSpace.tearDown();
        }
        locked = false;
    }

    @Override
    public String toString() {
        return getDefaultNamespace() + getID();
    }

}
