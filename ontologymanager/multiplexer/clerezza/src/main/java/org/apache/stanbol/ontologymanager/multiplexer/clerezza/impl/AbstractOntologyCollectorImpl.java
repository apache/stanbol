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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Literal;
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
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.ontologymanager.servicesapi.collector.Lockable;
import org.apache.stanbol.ontologymanager.servicesapi.collector.MissingOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSourceHandler;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.RemoveImport;
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

    /**
     * How many levels back to go in the namespace+id concatenation in order to write resolvable import
     * statements.
     */
    protected int backwardPathLength = 0;

    protected ConnectivityPolicy connectivityPolicy;

    private Set<OntologyCollectorListener> listeners = new HashSet<OntologyCollectorListener>();

    /**
     * Indicates whether this ontology space is marked as read-only. Default value is false.
     */
    protected volatile boolean locked = false;

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The identifier of the ontologies directly managed by this collector (i.e. that were directly added to
     * this space, hence not including those just pulled in via import statements).<br>
     * <br>
     * XXX depending on whether we want to support multiple versionIRIs in the same collector, we may want to
     * turn this one into a set of {@link OWLOntologyID}.
     */
    protected Set<OWLOntologyID> managedOntologies;

    protected org.semanticweb.owlapi.model.IRI namespace = null;

    protected OntologyProvider<?> ontologyProvider;

    protected Set<Class<?>> supportedTypes;

    public AbstractOntologyCollectorImpl(String id, org.semanticweb.owlapi.model.IRI namespace, OntologyProvider<?> ontologyProvider) {
        // Supports OWL API and Clerezza
        supportedTypes = new HashSet<Class<?>>();
        supportedTypes.add(OWLOntology.class);
        supportedTypes.add(Graph.class);
        setID(id);
        setDefaultNamespace(namespace);
        this.ontologyProvider = ontologyProvider;
        this.managedOntologies = new HashSet<OWLOntologyID>();
    }

    @Override
    public synchronized OWLOntologyID addOntology(OntologyInputSource<?> ontologySource) throws UnmodifiableOntologyCollectorException {

        // Check for error conditions.
        if (locked) throw new UnmodifiableOntologyCollectorException(this);
        if (ontologySource == null) throw new IllegalArgumentException("Ontology source cannot be null.");

        log.debug("Adding ontology to collector {}", getID());
        OWLOntologyID key = null;

        if (ontologySource.hasRootOntology()) {
            long before = System.currentTimeMillis();
            Object o = ontologySource.getRootOntology();
            // // FIXME restore ownership management, but maybe not by directly setting the versionIRI
            // if (ontologyProvider.hasOntology(id.getOntologyIRI())) if (o instanceof Graph)
            // claimOwnership((Graph) o);
            // else if (o instanceof OWLOntology) claimOwnership((OWLOntology) o);

            // Check the origin anyhow, as it may be useful for setting aliases with physical locations etc.
            if (ontologySource.hasOrigin()) key = ontologyProvider.loadInStore(o, false,
                ontologySource.getOrigin());
            else key = ontologyProvider.loadInStore(o, false);
            if (key != null) {
                managedOntologies.add(key);
                // Note that imported ontologies are not considered as managed! TODO should we change this?
                log.info("Add ontology completed in {} ms.", (System.currentTimeMillis() - before));
                // Fire the event
                fireOntologyAdded(key);
            }
        } else if (ontologySource.hasOrigin()) {
            // Just the origin : see if it is satisfiable
            log.debug("Checking origin satisfiability...");
            Origin<?> origin = ontologySource.getOrigin();
            Object ref = origin.getReference();
            log.debug("Origin wraps a {}", ref.getClass().getCanonicalName());
            if (ref instanceof org.semanticweb.owlapi.model.IRI) try {
                log.debug("Deferring addition to physical IRI {} (if available).", ref);
                key = addOntology(new RootOntologySource((org.semanticweb.owlapi.model.IRI) ref));
            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException(e);
            }
            else if (ref instanceof IRI) {
                log.debug("Deferring addition to stored Clerezza graph {} (if available).", ref);
                key = addOntology(new GraphSource((IRI) ref));
            } else if (ref instanceof OWLOntologyID) {
                OWLOntologyID idref = (OWLOntologyID) ref;
                log.debug("Deferring addition to stored ontology with public key {} (if available).", ref);
                if (!ontologyProvider.hasOntology(idref)) throw new MissingOntologyException(this, idref);
                key = idref;
                if (managedOntologies.add(idref)) fireOntologyAdded(idref);
            } else throw new IllegalArgumentException("Invalid origin " + origin);
        } else throw new IllegalArgumentException(
                "Ontology source must provide either an ontology object, or a way to reference one (i.e. an origin).");
        log.info("Public key : {}", key);
        return key;
    }

    @Override
    public void addOntologyCollectorListener(OntologyCollectorListener listener) {
        listeners.add(listener);
    }

    protected void claimOwnership(OWLOntologyID publicKey) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void clearOntologyCollectorListeners() {
        listeners.clear();
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof OntologyCollector)) return false;
        if (this == arg0) return true;
        log.warn(
            "{} only implements weak equality, i.e. managed ontologies are only checked by public key, not by content.",
            getClass());
        OntologyCollector coll = (OntologyCollector) arg0;
        return this.getID().equals(coll.getID())
               && this.getDefaultNamespace().equals(coll.getDefaultNamespace())
               && this.listManagedOntologies().equals(coll.listManagedOntologies())
               && this.getSupportedOntologyTypes().equals(coll.getSupportedOntologyTypes());
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
        throw new UnsupportedOperationException("Cannot export ontology collector " + getID() + " to a "
                                                + returnType);
    }

    /**
     * This method has no conversion calls, to it can be invoked by subclasses that wish to modify it
     * afterwards.
     * 
     * @param merge
     * @return
     */
    protected Graph exportToGraph(boolean merge, org.semanticweb.owlapi.model.IRI prefix) {
        // if (merge) throw new UnsupportedOperationException(
        // "Merge not implemented yet for Clerezza triple collections.");

        long before = System.currentTimeMillis();

        // No need to store, give it a name, or anything.
        Graph root = new SimpleGraph();
        IRI iri = new IRI(prefix + _id);
        // Add the import declarations for directly managed ontologies.
        if (root != null) {
            // Set the ontology ID
            root.add(new TripleImpl(iri, RDF.type, OWL.Ontology));

            if (merge) {
                log.warn("Merging of Clerezza triple collections is only implemented one level down. Import statements will be preserved for further levels.");
                Iterator<Triple> it;
                Set<RDFTerm> importTargets = new HashSet<RDFTerm>();
                for (OWLOntologyID ontologyId : managedOntologies) {
                    ImmutableGraph g = getOntology(ontologyId, ImmutableGraph.class, false);
                    root.addAll(g);

                    it = g.filter(null, OWL.imports, null);
                    while (it.hasNext()) {
                        org.semanticweb.owlapi.model.IRI tgt;
                        RDFTerm r = it.next().getObject();
                        try {
                            if (r instanceof IRI) tgt = org.semanticweb.owlapi.model.IRI.create(((IRI) r).getUnicodeString());
                            else if (r instanceof Literal) tgt = org.semanticweb.owlapi.model.IRI.create(((Literal) r).getLexicalForm());
                            else tgt = org.semanticweb.owlapi.model.IRI.create(r.toString());
                            tgt = URIUtils.sanitize(tgt);
                            importTargets.add(new IRI(tgt.toString()));
                        } catch (Exception ex) {
                            log.error("FAILED to obtain import target from resource {}", r);
                            continue;
                        }

                    }

                    it = g.filter(null, RDF.type, OWL.Ontology);
                    while (it.hasNext()) {
                        BlankNodeOrIRI ontology = it.next().getSubject();
                        log.debug("Removing all triples related to {} from {}", ontology, iri);
                        Iterator<Triple> it2 = g.filter(ontology, null, null);
                        while (it2.hasNext())
                            root.remove(it2.next());
                    }

                    /*
                     * Reinstate import statements, though. If imported ontologies were not merged earlier, we
                     * are not doing it now anyway.
                     */
                    for (RDFTerm target : importTargets)
                        root.add(new TripleImpl(iri, OWL.imports, target));
                }

            } else {

                String base = prefix + getID();
                for (int i = 0; i < backwardPathLength; i++)
                    base = URIUtils.upOne(URI.create(base)).toString();
                base += "/";

                // The key set of managedOntologies contains the ontology IRIs, not their storage keys.
                for (OWLOntologyID ontologyId : managedOntologies) {
                    org.semanticweb.owlapi.model.IRI physIRI =
                    // ontologyId.getVersionIRI() == null ? URIUtils.sanitize(IRI
                    // .create(base + ontologyId.getOntologyIRI())) : URIUtils.sanitize(IRI
                    // .create(base + ontologyId.getVersionIRI()));
                    org.semanticweb.owlapi.model.IRI.create(base + OntologyUtils.encode(ontologyId));
                    root.add(new TripleImpl(iri, OWL.imports, new IRI(physIRI.toString())));
                }
            }

            log.debug("Clerezza export of {} completed in {} ms.", getID(), System.currentTimeMillis()
                                                                            - before);
        }

        return root;
    }

    private Graph getMergedTc() {
        Graph result = new SimpleGraph(); // Takes less memory than the Indexed one

        for (OWLOntologyID key : listManagedOntologies()) {
            // TODO when implemented, switch to true.
            Graph managed = getOntology(key, Graph.class, false);
            Set<RDFTerm> exclusions = new HashSet<RDFTerm>();
            Iterator<Triple> it = managed.filter(null, RDF.type, OWL.Ontology);
            while (it.hasNext())
                exclusions.add(it.next().getSubject());
            for (Triple t : managed)
                if (!exclusions.contains(t.getSubject())) result.add(t);
        }

        // TODO Purge property usage

        return result;
    }

    /**
     * This method has no conversion calls, to it can be invoked by subclasses that wish to modify it
     * afterwards.
     * 
     * FIXME not merging yet FIXME not including imported ontologies unless they are merged *before* storage.
     * 
     * @param merge
     * @return
     */
    protected OWLOntology exportToOWLOntology(boolean merge, org.semanticweb.owlapi.model.IRI prefix) {

        long before = System.currentTimeMillis();

        // Create a new ontology
        OWLOntology root;
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        org.semanticweb.owlapi.model.IRI iri = org.semanticweb.owlapi.model.IRI.create(prefix + _id);
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

                for (OWLOntologyID ontologyId : managedOntologies) {
                    log.debug("Merging {} with {}.", ontologyId, root);
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
                    root = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), iri);

                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to merge imports for ontology " + iri, e);
                    root = null;
                }

            } else {
                // Add the import declarations for directly managed ontologies.
                List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
                OWLDataFactory df = ontologyManager.getOWLDataFactory();

                String base = prefix + getID();
                for (int i = 0; i < backwardPathLength; i++)
                    base = URIUtils.upOne(URI.create(base)).toString();
                base += "/";

                // The key set of managedOntologies contains the ontology IRIs, not their storage keys.
                for (OWLOntologyID ontologyId : managedOntologies) {
                    // XXX some day the versionIRI will be the only physical reference for the ontology
                    org.semanticweb.owlapi.model.IRI physIRI = org.semanticweb.owlapi.model.IRI.create(base + OntologyUtils.encode(ontologyId));
                    changes.add(new AddImport(root, df.getOWLImportsDeclaration(physIRI)));
                }
                ontologyManager.applyChanges(changes);
            }

        }
        log.debug("OWL export of {} completed in {} ms.", getID(), System.currentTimeMillis() - before);

        return root;
    }

    /**
     * Notifies all ontology space listeners that an ontology has been added to this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was added to this space.
     */
    protected void fireOntologyAdded(OWLOntologyID ontologyId) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyAdded(this, ontologyId);
    }

    /**
     * Notifies all ontology space listeners that an ontology has been removed from this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was removed from this space.
     */
    protected void fireOntologyRemoved(OWLOntologyID ontologyId) {
        for (OntologyCollectorListener listener : listeners)
            listener.onOntologyRemoved(this, ontologyId);
    }

    @Override
    public ConnectivityPolicy getConnectivityPolicy() {
        return this.connectivityPolicy;
    }

    @Override
    public org.semanticweb.owlapi.model.IRI getDefaultNamespace() {
        return this.namespace;
    }

    @Override
    public String getID() {
        return _id;
    }

    @Override
    public <O> Set<O> getManagedOntologies(Class<O> returnType, boolean withClosure) {
        if (withClosure) log.warn("Closure support not implemented yet. Will merge instead.");
        Set<O> ontologies = new HashSet<O>();
        for (OWLOntologyID id : managedOntologies)
            // FIXME temporary fix is to merge instead of including closure
            ontologies.add(getOntology(id, returnType, withClosure));
        return Collections.unmodifiableSet(ontologies);
    }

    @Override
    public org.semanticweb.owlapi.model.IRI getNamespace() {
        return getDefaultNamespace();
    }

    @Override
    public <O> O getOntology(org.semanticweb.owlapi.model.IRI ontologyIri, Class<O> returnType) {
        return getOntology(new OWLOntologyID(ontologyIri), returnType);
    }

    @Override
    public <O> O getOntology(org.semanticweb.owlapi.model.IRI ontologyIri, Class<O> returnType, boolean merge) {
        return getOntology(new OWLOntologyID(ontologyIri), returnType, merge);
    }

    @Override
    public <O> O getOntology(org.semanticweb.owlapi.model.IRI ontologyIri, Class<O> returnType, boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        return getOntology(new OWLOntologyID(ontologyIri), returnType, merge, universalPrefix);
    }

    @Override
    public <O> O getOntology(org.semanticweb.owlapi.model.IRI ontologyIri, Class<O> returnType, org.semanticweb.owlapi.model.IRI universalPrefix) {
        return getOntology(new OWLOntologyID(ontologyIri), returnType, universalPrefix);
    }

    @Override
    public <O> O getOntology(OWLOntologyID ontologyId, Class<O> returnType) {
        return getOntology(ontologyId, returnType, false);
    }

    @Override
    public <O> O getOntology(OWLOntologyID ontologyId, Class<O> returnType, boolean merge) {
        return getOntology(ontologyId, returnType, merge, getDefaultNamespace());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O getOntology(OWLOntologyID ontologyId, Class<O> returnType, boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        if (OWLOntology.class.isAssignableFrom(returnType)) return (O) getOntologyAsOWLOntology(ontologyId,
            merge, universalPrefix);
        if (Graph.class.isAssignableFrom(returnType)) {
            Graph root = getOntologyAsGraph(ontologyId, merge, universalPrefix);
            // A Clerezza graph has to be cast properly.
            if (returnType == ImmutableGraph.class) root = ((Graph) root).getImmutableGraph();
            else if (returnType == Graph.class) {}
            // We don't know of other Graph subclasses: just try to cast the Graph.
            return (O) root;
        }
        throw new UnsupportedOperationException("Cannot export ontology collector " + getID() + " to a "
                                                + returnType);
    }

    @Override
    public <O> O getOntology(OWLOntologyID ontologyId, Class<O> returnType, org.semanticweb.owlapi.model.IRI universalPrefix) {
        return getOntology(ontologyId, returnType, false, universalPrefix);
    }

    protected Graph getOntologyAsGraph(OWLOntologyID ontologyId, boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        if (merge) throw new UnsupportedOperationException(
                "Merge not implemented yet for Clerezza triple collections.");
        /*
         * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
         * going upOne() add "session" or "ontology" if needed). But only do this if we keep considering
         * imported ontologies as *not* managed.
         */
        // if (!merge) { // TODO
        Graph o = new IndexedGraph(ontologyProvider.getStoredOntology(ontologyId, Graph.class, merge));

        // Now rewrite import statements

        // Scan import statements for each owl:Ontology instance (hopefully one).
        String tid = getID();
        // Bit of a hack : since ontology spaces are named like {scopeid}/{core|custom}, in that particular
        // case we go back to {scopeid}, whereas for sessions we maintain their original id.
        if (backwardPathLength > 0) tid = tid.split("/")[0];

        Iterator<Triple> it;
        List<Triple> newImports = new LinkedList<Triple>();
        synchronized (o) {
            it = o.filter(null, OWL.imports, null);
            // We use this list to avoid concurrent modification exceptions.
            List<Triple> replaceUs = new LinkedList<Triple>();
            while (it.hasNext())
                replaceUs.add(it.next());

            for (Triple t : replaceUs) {
                String s = ((IRI) (t.getObject())).getUnicodeString();
                // FIXME note the different import targets in the OWLOntology and TripleColllection objects!
                // s = s.substring(s.indexOf("::") + 2, s.length());
                boolean managed = managedOntologies.contains(org.semanticweb.owlapi.model.IRI.create(s));
                IRI target = new IRI((managed ? universalPrefix + "/" + tid + "/"
                        : URIUtils.upOne(universalPrefix) + "/")
                                           + s);
                o.remove(t);
                newImports.add(new TripleImpl(t.getSubject(), OWL.imports, target));
            }
        }

        for (Triple t : newImports)
            o.add(t);

        // } // TODO else if (merge)

        return o;
    }

    protected OWLOntology getOntologyAsOWLOntology(OWLOntologyID ontologyId,
                                                   boolean merge,
                                                   org.semanticweb.owlapi.model.IRI universalPrefix) {
        // if (merge) throw new UnsupportedOperationException("Merge not implemented yet for OWLOntology.");

        // Remove the check below. It might be an unmanaged dependency (TODO remove from collector and
        // reintroduce check?).
        // if (!hasOntology(ontologyIri)) return null;
        OWLOntology o;
        o = ontologyProvider.getStoredOntology(ontologyId, OWLOntology.class, merge);

        if (merge) {
            final Set<OWLOntology> set = new HashSet<OWLOntology>();
            log.debug("Merging {} with its imports, if any.", o);
            set.add(o);
            // Actually, if the provider already performed the merge, this won't happen
            for (OWLOntology impo : o.getImportsClosure()) {
                log.debug("Imported ontology {} will be merged with {}.", impo, o);
                set.add(impo);
            }
            OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                @Override
                public Set<OWLOntology> getOntologies() {
                    return set;
                }
            };
            OWLOntologyMerger merger = new OWLOntologyMerger(provider);
            try {
                o = merger.createMergedOntology(OWLManager.createOWLOntologyManager(),
                    ontologyId.getOntologyIRI());
            } catch (OWLOntologyCreationException e) {
                log.error("Failed to merge imports for ontology " + ontologyId, e);
                // do not reassign the root ontology
            }
        } else {
            // Rewrite import statements
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = OWLManager.getOWLDataFactory();

            /*
             * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
             * going upOne() add "session" or "ontology" if needed). But only do this if we keep considering
             * imported ontologies as *not* managed.
             */
            for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
                changes.add(new RemoveImport(o, oldImp));
                String s = oldImp.getIRI().toString();
                // FIXME Ugly way to check, but we'll get through with it
                if (s.contains("::")) s = s.substring(s.indexOf("::") + 2, s.length());
                boolean managed = managedOntologies.contains(oldImp.getIRI());
                // For space, always go up at least one

                String tid = getID();
                if (backwardPathLength > 0) tid = tid.split("/")[0];

                org.semanticweb.owlapi.model.IRI target = org.semanticweb.owlapi.model.IRI.create((managed ? universalPrefix + "/" + tid + "/" : URIUtils
                        .upOne(universalPrefix) + "/")
                                        + s);
                changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
            }
            o.getOWLOntologyManager().applyChanges(changes);
        }

        return o;
    }

    @Override
    public Collection<OntologyCollectorListener> getOntologyCollectorListeners() {
        return listeners;
    }

    @Override
    public Set<Class<?>> getSupportedOntologyTypes() {
        return Collections.unmodifiableSet(supportedTypes);
    }

    @Override
    public boolean hasOntology(org.semanticweb.owlapi.model.IRI ontologyIri) {
        return hasOntology(new OWLOntologyID(ontologyIri));
    }

    @Override
    public boolean hasOntology(OWLOntologyID ontologyId) {
        Set<OWLOntologyID> aliases = ontologyProvider.listAliases(ontologyId);
        if (managedOntologies.contains(ontologyId)) return true;
        for (OWLOntologyID alias : aliases)
            if (managedOntologies.contains(alias)) return true;
        return false;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public Set<OWLOntologyID> listManagedOntologies() {
        return new TreeSet<OWLOntologyID>(managedOntologies);
    }

    @Override
    public void removeOntology(org.semanticweb.owlapi.model.IRI ontologyId) throws OntologyCollectorModificationException {
        removeOntology(new OWLOntologyID(ontologyId));
    }

    @Override
    public void removeOntology(OWLOntologyID publicKey) throws OntologyCollectorModificationException {
        if (publicKey == null) throw new IllegalArgumentException(
                "Cannot remove an ontology by providing a null public key.");
        if (publicKey.getOntologyIRI() == null) throw new IllegalArgumentException(
                "Cannot remove an ontology whose public key has a null ontology IRI.");
        if (locked) throw new UnmodifiableOntologyCollectorException(this);

        Set<OWLOntologyID> aliases = ontologyProvider.listAliases(publicKey);
        aliases.add(publicKey);
        boolean removed = false;
        for (OWLOntologyID alias : aliases)
            removed |= managedOntologies.remove(alias);
        // Don't fire if the ontology wasn't there in the first place.
        if (removed) fireOntologyRemoved(publicKey);
        else throw new MissingOntologyException(this, publicKey);
    }

    @Override
    public void removeOntologyCollectorListener(OntologyCollectorListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setConnectivityPolicy(ConnectivityPolicy policy) {
        this.connectivityPolicy = policy;
    }

    /**
     * @param namespace
     *            The OntoNet namespace that will prefix the space ID in Web references. This implementation
     *            only allows non-null and non-empty IRIs, with no query or fragment. Hash URIs are not
     *            allowed, slash URIs are preferred. If neither, a slash will be concatenated and a warning
     *            will be logged.
     */
    @Override
    public void setDefaultNamespace(org.semanticweb.owlapi.model.IRI namespace) {
        if (namespace == null) throw new IllegalArgumentException(
                "Stanbol ontology namespace cannot be null.");
        if (namespace.toURI().getQuery() != null) throw new IllegalArgumentException(
                "URI Query is not allowed in Stanbol ontology namespaces.");
        if (namespace.toURI().getFragment() != null) throw new IllegalArgumentException(
                "URI Fragment is not allowed in Stanbol ontology namespaces.");
        if (namespace.toString().endsWith("#")) throw new IllegalArgumentException(
                "Stanbol ontology namespaces must not end with a hash ('#') character.");
        if (!namespace.toString().endsWith("/")) {
            log.warn("Namespace {} does not end with a slash ('/') character. It be added automatically.",
                namespace);
            namespace = org.semanticweb.owlapi.model.IRI.create(namespace + "/");
        }
        this.namespace = namespace;
    }

    protected abstract void setID(String id);

    @Override
    public void setNamespace(org.semanticweb.owlapi.model.IRI namespace) {
        setDefaultNamespace(namespace);
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
