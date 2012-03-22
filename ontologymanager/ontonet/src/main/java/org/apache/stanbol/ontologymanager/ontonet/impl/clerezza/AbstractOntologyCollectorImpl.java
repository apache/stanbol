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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.Lockable;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSourceHandler;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
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
import org.semanticweb.owlapi.model.SetOntologyID;
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

    @Override
    public Set<IRI> listManagedOntologies() {
        return managedOntologies;
    }

    protected String _id = null;

    /**
     * How many levels back to go in the namespace+id concatenation in order to write resolvable import
     * statements.
     */
    protected int backwardPathLength = 0;

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
        } else throw new UnsupportedOperationException(
                "This ontology collector implementation cannot handle " + o.getClass().getCanonicalName()
                        + " objects.");

        // Now for the actual storage. We pass the ontology object directly.
        String key = null;
        if (ontologyProvider.hasOntology(IRI.create(uri.getUnicodeString()))) if (o instanceof MGraph) claimOwnership((MGraph) o);
        else if (o instanceof OWLOntology) claimOwnership((OWLOntology) o);
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

    protected void claimOwnership(OWLOntology ontology) {
        log.info("Checking ownership of {} {}", OWLOntology.class.getSimpleName(), ontology.getOntologyID());
        OWLOntologyID id = ontology.getOntologyID();
        if (id.getOntologyIRI() != null) {
            IRI ontologyIRI = id.getOntologyIRI();
            IRI versionIri = id.getVersionIRI();
            if (versionIri == null) {
                log.info("    No OWL version IRI Found . Will set to own ID. ");
                versionIri = IRI.create(getNamespace() + getID());
                OWLOntologyID newId = new OWLOntologyID(ontologyIRI, versionIri);
                OWLOntologyChange change = new SetOntologyID(ontology, newId);
                ontology.getOWLOntologyManager().applyChange(change);
                log.info("    Set OWL version IRI : {} . ", versionIri);
            } else log.info("    Found OWL version IRI {} . Will not claim ownership. ", versionIri);
        }

    }

    protected void claimOwnership(MGraph ontology) {
        UriRef owl_viri = new UriRef("http://www.w3.org/2002/07/owl#versionIRI");
        UriRef ontologyId = null;
        UriRef versionIri = new UriRef(getNamespace() + getID());
        Iterator<Triple> it = ontology.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            NonLiteral r = it.next().getSubject();
            if (r instanceof UriRef) ontologyId = (UriRef) r;
        }
        log.info("Checking ownership of {} {}", MGraph.class.getSimpleName(), ontologyId != null ? ontologyId
                : "(anonymous)");
        if (ontologyId != null) {
            it = ontology.filter(ontologyId, owl_viri, OWL.Ontology);
            if (it.hasNext()) {
                versionIri = (UriRef) it.next().getObject();
                log.info("    Found OWL version IRI {} . Will not claim ownership. ", versionIri);
            } else {
                log.info("    No OWL version IRI Found . Will set to own ID. ");
                Triple t = new TripleImpl(ontologyId, owl_viri, versionIri);
                ontology.add(t);
                log.info("    Set OWL version IRI : {} . ", versionIri);
            }
        }
    }

    @Override
    public OWLOntology asOWLOntology(boolean merge) {
        return export(OWLOntology.class, merge);
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O export(Class<O> returnType, boolean merge) {
        if (OWLOntology.class.isAssignableFrom(returnType)) {
            return (O) exportToOWLOntology(merge);
        }
        if (TripleCollection.class.isAssignableFrom(returnType)) {
            TripleCollection root = exportToMGraph(merge);
            // A Clerezza graph has to be cast properly.
            if (returnType == Graph.class) root = ((MGraph) root).getGraph();
            else if (returnType == MGraph.class) {}
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
    protected MGraph exportToMGraph(boolean merge) {
        // if (merge) throw new UnsupportedOperationException(
        // "Merge not implemented yet for Clerezza triple collections.");

        long before = System.currentTimeMillis();

        // No need to store, give it a name, or anything.
        MGraph root = new SimpleMGraph();
        UriRef iri = new UriRef(namespace + _id);
        // Add the import declarations for directly managed ontologies.
        if (root != null) {
            // Set the ontology ID
            root.add(new TripleImpl(iri, RDF.type, OWL.Ontology));

            if (merge) {
                log.warn("Merging of Clerezza triple collections is only implemented one level down. Import statements will be preserved for further levels.");
                Iterator<Triple> it;
                Set<Resource> importTargets = new HashSet<Resource>();
                for (IRI ontologyIri : managedOntologies) {
                    Graph g = getOntology(ontologyIri, Graph.class, false);
                    root.addAll(g);

                    it = g.filter(null, OWL.imports, null);
                    while (it.hasNext())
                        importTargets.add(it.next().getObject());

                    it = g.filter(null, RDF.type, OWL.Ontology);
                    while (it.hasNext()) {
                        NonLiteral ontology = it.next().getSubject();
                        log.debug("Removing all triples related to {} from {}", ontology, iri);
                        Iterator<Triple> it2 = g.filter(ontology, null, null);
                        while (it2.hasNext())
                            root.remove(it2.next());
                    }

                    /*
                     * Reinstate import statements, though. If imported ontologies were not merged earlier, we
                     * are not doing it now anyway.
                     */
                    for (Resource target : importTargets)
                        root.add(new TripleImpl(iri, OWL.imports, target));
                }

            } else {

                String base = getNamespace() + getID();
                for (int i = 0; i < backwardPathLength; i++)
                    base = URIUtils.upOne(URI.create(base)).toString();
                base += "/";

                // The key set of managedOntologies contains the ontology IRIs, not their storage keys.
                for (IRI ontologyIri : managedOntologies) {
                    UriRef physIRI = new UriRef(base + ontologyIri);
                    root.add(new TripleImpl(iri, OWL.imports, physIRI));
                }
            }

            log.debug("Clerezza export of {} completed in {} ms.", getID(), System.currentTimeMillis()
                                                                            - before);
        }

        return root;
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
    protected OWLOntology exportToOWLOntology(boolean merge) {

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

                String base = getNamespace() + getID();
                for (int i = 0; i < backwardPathLength; i++)
                    base = URIUtils.upOne(URI.create(base)).toString();
                base += "/";

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
    public IRI getDocumentIRI() {
        return IRI.create(getNamespace() + getID());
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
    public <O> Set<O> getManagedOntologies(Class<O> returnType, boolean withClosure) {
        if (withClosure) log.warn("Closure support not implemented yet. Will merge instead.");
        Set<O> ontologies = new HashSet<O>();
        for (IRI id : managedOntologies)
            // FIXME temporary fix is to merge instead of including closure
            ontologies.add(getOntology(id, returnType, withClosure));
        return Collections.unmodifiableSet(ontologies);
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
        return getManagedOntologies(OWLOntology.class, withClosure);
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        return getOntology(ontologyIri, false);
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri, boolean merge) {
        return getOntology(ontologyIri, OWLOntology.class, merge);
    }

    @Override
    public <O> O getOntology(IRI ontologyIri, Class<O> returnType) {
        return getOntology(ontologyIri, returnType, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O getOntology(IRI ontologyIri, Class<O> returnType, boolean merge) {
        if (OWLOntology.class.isAssignableFrom(returnType)) {
            return (O) getOntologyAsOWLOntology(ontologyIri, merge);
        }
        if (TripleCollection.class.isAssignableFrom(returnType)) {
            TripleCollection root = getOntologyAsMGraph(ontologyIri, merge);
            // A Clerezza graph has to be cast properly.
            if (returnType == Graph.class) root = ((MGraph) root).getGraph();
            else if (returnType == MGraph.class) {}
            // We don't know of other TripleCollection subclasses: just try to cast the MGraph.
            return (O) root;
        }
        throw new UnsupportedOperationException("Cannot export ontology collector " + getID() + " to a "
                                                + returnType);
    }

    protected MGraph getOntologyAsMGraph(IRI ontologyIri, boolean merge) {
        if (merge) throw new UnsupportedOperationException(
                "Merge not implemented yet for Clerezza triple collections.");
        /*
         * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
         * going upOne() add "session" or "ontology" if needed). But only do this if we keep considering
         * imported ontologies as *not* managed.
         */
        // if (!merge) {
        MGraph o = new SimpleMGraph(ontologyProvider.getStoredOntology(ontologyIri, MGraph.class, merge));

        // Now rewrite import statements

        // Scan import statements for each owl:Ontology instance (hopefully one).
        IRI ns = getNamespace();
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
                String s = ((UriRef) (t.getObject())).getUnicodeString();
                // FIXME note the different import targets in the OWLOntology and TripleColllection objects!
                // s = s.substring(s.indexOf("::") + 2, s.length());
                boolean managed = managedOntologies.contains(IRI.create(s));
                UriRef target = new UriRef((managed ? ns + "/" + tid + "/" : URIUtils.upOne(ns) + "/") + s);
                o.remove(t);
                newImports.add(new TripleImpl(t.getSubject(), OWL.imports, target));
            }
        }

        for (Triple t : newImports)
            o.add(t);
        // }
        // TODO else if (merge)

        return o;
    }

    protected OWLOntology getOntologyAsOWLOntology(IRI ontologyIri, boolean merge) {
        // if (merge) throw new UnsupportedOperationException("Merge not implemented yet for OWLOntology.");

        // Remove the check below. It might be an unmanaged dependency (TODO remove from collector and
        // reintroduce check?).
        // if (!hasOntology(ontologyIri)) return null;
        OWLOntology o;
        o = (OWLOntology) ontologyProvider.getStoredOntology(ontologyIri, OWLOntology.class, merge);

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
                o = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), ontologyIri);
            } catch (OWLOntologyCreationException e) {
                log.error("Failed to merge imports for ontology " + ontologyIri, e);
                // do not reassign the root ontology
            }
        } else {
            // Rewrite import statements
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();

            /*
             * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
             * going upOne() add "session" or "ontology" if needed). But only do this if we keep considering
             * imported ontologies as *not* managed.
             */
            for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
                changes.add(new RemoveImport(o, oldImp));
                String s = oldImp.getIRI().toString();
                s = s.substring(s.indexOf("::") + 2, s.length());
                boolean managed = managedOntologies.contains(oldImp.getIRI());
                // For space, always go up at least one
                IRI ns = getNamespace();

                String tid = getID();
                if (backwardPathLength > 0) tid = tid.split("/")[0];

                IRI target = IRI.create((managed ? ns + "/" + tid + "/" : URIUtils.upOne(ns) + "/") + s);
                changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
            }
            o.getOWLOntologyManager().applyChanges(changes);
        }

        return o;
    }

    @Override
    public int getOntologyCount() {
        return getOntologyCount(false);
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
            log.warn("Namespace {} does not end with a slash ('/') character. It be added automatically.",
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
