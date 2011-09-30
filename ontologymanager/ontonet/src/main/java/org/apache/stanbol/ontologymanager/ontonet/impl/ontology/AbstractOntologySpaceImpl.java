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
package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.MissingOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of an ontology space. While it still leaves it up to developers to decide what
 * locking policies to adopt for subclasses (in the <code>setUp()</code> method), it provides default
 * implementations of all other interface methods.<br>
 * <br>
 * NOTE: By default, an ontology space is NOT write-locked. Developers need to set the <code>locked</code>
 * variable to true to make the space read-only.
 */
public abstract class AbstractOntologySpaceImpl implements OntologySpace {

    protected String _id = null;

    private Set<OntologySpaceListener> listeners = new HashSet<OntologySpaceListener>();

    /**
     * Indicates whether this ontology space is marked as read-only. Default value is false.
     */
    protected volatile boolean locked = false;

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The identifier of the ontologies directly managed by this space (i.e. that were directly added to this
     * space, hence not including those just pulled in via import statements).
     */
    protected Map<IRI,OWLOntology> managedOntologies;

    protected IRI namespace = null;

    /**
     * Each ontology space comes with its OWL ontology manager. By default, it is not available to the outside
     * world, unless subclasses implement methods to return it.
     */
    protected OWLOntologyManager ontologyManager;

    protected boolean silent = false;

    protected ClerezzaOntologyStorage storage;

    protected SpaceType type;

    protected AbstractOntologySpaceImpl(String spaceID,
                                        IRI namespace,
                                        SpaceType type,
                                        ClerezzaOntologyStorage storage) {
        this(spaceID, namespace, type, storage, OWLManager.createOWLOntologyManager());
    }

    /**
     * Creates a new ontology space with the supplied ontology manager as the default manager for this space.
     * 
     * @param spaceID
     *            the IRI that will uniquely identify this space.
     * @param ontologyManager
     *            the default ontology manager for this space.
     */
    protected AbstractOntologySpaceImpl(String spaceID,
                                        IRI namespace,
                                        SpaceType type,
                                        ClerezzaOntologyStorage storage,
                                        OWLOntologyManager ontologyManager) {
        setID(spaceID);
        setNamespace(namespace);
        this.type = type;
        this.storage = storage;
        if (ontologyManager != null) this.ontologyManager = ontologyManager;
        else this.ontologyManager = OWLManager.createOWLOntologyManager();

        this.managedOntologies = new HashMap<IRI,OWLOntology>();
    }

    @Override
    public synchronized void addOntology(OntologyInputSource ontologySource) throws UnmodifiableOntologySpaceException {
        if (locked) throw new UnmodifiableOntologySpaceException(this);
        log.debug("Trying to add ontology {} to space {}",
            ontologySource != null ? ontologySource.getRootOntology() : "<NULL>", getNamespace() + getID());
        // Avoid adding the space top ontology itself.
        if (ontologySource != null && ontologySource.hasRootOntology()) {
            OWLOntology o = ontologySource.getRootOntology();
            if (!o.isAnonymous() && getID().equals(o.getOntologyID().getOntologyIRI())) throw new IllegalArgumentException(
                    "Cannot add a space's own ontology to itself.");
            else performAdd(ontologySource);
            // Remember that this method also fires the event
        }
    }

    @Override
    public void addOntologySpaceListener(OntologySpaceListener listener) {
        listeners.add(listener);
    }

    @Override
    public OWLOntology asOWLOntology() {
        OWLOntology root;
        IRI iri = IRI.create(namespace + _id);
        try {
            root = ontologyManager.createOntology(iri);
        } catch (OWLOntologyAlreadyExistsException e) {
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
            List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
            OWLDataFactory df = ontologyManager.getOWLDataFactory();
            for (OWLOntology o : getOntologies(false)) {
                if (o == null) continue;

                String base = URIUtils.upOne(IRI.create(namespace + getID())) + "/";

                IRI ontologyIri;

                if (o.isAnonymous()) try {
                    ontologyIri = ontologyManager.getOntologyDocumentIRI(o);
                } catch (Exception ex) {
                    ontologyIri = o.getOWLOntologyManager().getOntologyDocumentIRI(o);
                }
                else {
                    ontologyIri = o.getOntologyID().getDefaultDocumentIRI();
                }

                IRI physIRI = IRI.create(base + ontologyIri);

                changes.add(new AddImport(root, df.getOWLImportsDeclaration(physIRI)));
            }
            ontologyManager.applyChanges(changes);
        }
        // return rootOntology;
        return root;
    }

    @Override
    public void clearOntologySpaceListeners() {
        listeners.clear();
    }

    @Override
    public boolean containsOntology(IRI ontologyIri) {
        return ontologyManager.contains(ontologyIri);
    }

    /**
     * Notifies all ontology space listeners that an ontology has been added to this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was added to this space.
     */
    protected void fireOntologyAdded(IRI ontologyIri) {
        for (OntologySpaceListener listener : listeners)
            listener.onOntologyAdded(IRI.create(namespace + _id), ontologyIri);
    }

    /**
     * Notifies all ontology space listeners that an ontology has been removed from this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was removed from this space.
     */
    protected void fireOntologyRemoved(IRI ontologyIri) {
        for (OntologySpaceListener listener : listeners)
            listener.onOntologyRemoved(IRI.create(namespace + _id), ontologyIri);
    }

    @Override
    public String getID() {
        return _id;
    }

    @Override
    public IRI getNamespace() {
        return this.namespace;
    }

    @Override
    public synchronized Set<OWLOntology> getOntologies(boolean withClosure) {
        return withClosure ? ontologyManager.getOntologies() : new HashSet<OWLOntology>(managedOntologies.values());
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        log.debug("Requesting ontology {} from space {}", ontologyIri, getNamespace() + getID());
        OWLOntology o = managedOntologies.get(ontologyIri);
//        Iterator<OWLOntology> it = managedOntologies.iterator();
//        while (it.hasNext() && o == null) {
//            OWLOntology temp = it.next();
//            if (!temp.isAnonymous() && ontologyIri.equals(temp.getOntologyID().getOntologyIRI())) o = temp;
//        }
//        if (o == null) o = ontologyManager.getOntology(ontologyIri);
        return o;
    }

    @Override
    public Collection<OntologySpaceListener> getOntologyScopeListeners() {
        return listeners;
    }

    @Override
    public boolean hasOntology(IRI ontologyIri) {
        return this.getOntology(ontologyIri) != null;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public boolean isSilentMissingOntologyHandling() {
        return silent;
    }

    private void performAdd(OntologyInputSource ontSrc) {

        OWLOntology ontology = ontSrc.getRootOntology();

        // Should not modify the child ontology in any way.
        // TODO implement transaction control.
        // See to it that the ontology is copied to this manager.
        OWLOntology newOnt = reload(ontology, ontologyManager, true, false);
        managedOntologies.put(OWLUtils.getIdentifyingIRI(newOnt),newOnt);

        try {
            // Store the top ontology
            if (!(this instanceof SessionOntologySpace)) {
                if (storage == null) log.warn(
                    "No ontology storage found. Ontology {} will be stored in-memory only.", ontology);
                else {
                    // storage = new ClerezzaOntologyStorage(tcManager, wtcProvider)
                    storage.store(ontology);
                }
            }
            // ONManager.get().getOntologyStore().load(rootOntology.getOntologyID().getOntologyIRI());
        } catch (Exception ex) {
            log.warn("An error occurred while storing ontology " + ontology
                     + " . Ontology management will be volatile.", ex);
        }

        fireOntologyAdded(OWLUtils.getIdentifyingIRI(ontology));

    }

    /**
     * Utility method to copy an ontology (or ontology network) across ontology managers without having it go
     * online to reload it.
     * 
     * @param ontology
     *            the ontology to copy from its own manager to the supplied one.
     * @param mgr
     *            the ontology manager where the ontology will be copied to. If null, the space's default
     *            ontology manager will be used.
     * @param withClosure
     *            if true, the whole imports closure will be reloaded, otherwise only the root ontology will
     *            be reloaded. However, import statements and other non-logical axioms will still be copied.
     * @param merge
     *            if true, all the axioms from the ontology closure will be merged in the new ontology maned
     *            after <code>ontology</code>. This parameter has virtually no effect if
     *            <code>withClosure</code> is set to false.
     */
    protected OWLOntology reload(OWLOntology ontology,
                                 OWLOntologyManager mgr,
                                 boolean withClosure,
                                 boolean merge) {
        if (ontology == null) throw new IllegalArgumentException("ontology cannot be null");
        if (ontology.getOWLOntologyManager() == ontologyManager) {
            log.warn("Ontology {} is already managed by the supplied OWLOntologyManager. Skipping copy.",
                ontology);
            return ontology;
        }

        OWLOntology root = null;

        IRI location = ontology.getOWLOntologyManager().getOntologyDocumentIRI(ontology);
        IRI idd = OWLUtils.getIdentifyingIRI(ontology);
        if (mgr == null) mgr = ontologyManager;
        Set<OWLOntology> closure = withClosure ? ontology.getOWLOntologyManager().getImportsClosure(ontology)
                : Collections.singleton(ontology);
        mgr.removeOntology(ontology);
        if (merge) try {
            root = mgr.createOntology(idd, closure);
            mgr.setOntologyDocumentIRI(root, location);
            return root;
        } catch (OWLOntologyCreationException e1) {
            log.error("Unexpected exception caught while copying ontology " + ontology.getOntologyID()
                      + " across managers", e1);
        }
        else {

            for (OWLOntology o : closure) {
                IRI id2 = OWLUtils.getIdentifyingIRI(o);
                // OWLOntologyID id = o.getOntologyID();
                if (mgr.contains(id2)) {
                    mgr.removeOntology(mgr.getOntology(id2));
                }
                try {
                    OWLOntology o1 = mgr.createOntology(id2, Collections.singleton(o));
                    mgr.setOntologyDocumentIRI(o1, location);
                    if (idd.equals(id2)) root = o1;
                } catch (OWLOntologyAlreadyExistsException e) {
                    if (o.getOWLOntologyManager() != mgr) {
                        mgr.removeOntology(o);
                        try {
                            OWLOntology o1 = mgr.createOntology(id2, Collections.singleton(o));
                            mgr.setOntologyDocumentIRI(o1, location);
                            if (idd.equals(id2)) root = o1;
                        } catch (OWLOntologyCreationException e1) {
                            log.error(
                                "Unexpected exception caught while copying ontology "
                                        + ontology.getOntologyID() + " across managers", e1);
                        }
                    }

                } catch (OWLOntologyCreationException e) {
                    log.warn("Failed to re-create ontology {} for ontology space {} . Continuing...", id2,
                        getID());
                }
            }
            return root;
        }
        return root;
    }

    /**
     * TODO 1 : optimize addition/removal <br>
     * TODO 2 : set import statements
     */
    @Override
    public synchronized void removeOntology(OntologyInputSource src) throws OntologySpaceModificationException {
        if (locked) throw new UnmodifiableOntologySpaceException(this);

        if (src != null && src.hasRootOntology()) {
            OWLOntology o = src.getRootOntology();
            if (!o.isAnonymous() && getID().equals(o.getOntologyID().getOntologyIRI())) throw new IllegalArgumentException(
                    "Cannot remove a space's own ontology form.");
        }

        // TODO : find a way to remove anonymous ontologies.
        OWLOntology o = src.getRootOntology();
        IRI logicalID = null, physicalIRI = null;
        try {
            logicalID = o.getOntologyID().getOntologyIRI();
            physicalIRI = src.getPhysicalIRI();
            if (physicalIRI == null) if (isSilentMissingOntologyHandling()) return;
            else throw new MissingOntologyException(this, null);
            if (logicalID == null) logicalID = physicalIRI;
        } catch (RuntimeException ex) {
            if (isSilentMissingOntologyHandling()) return;
            else throw new MissingOntologyException(this, null);
        }

        try {
            ontologyManager.removeOntology(o);
            fireOntologyRemoved(logicalID);
        } catch (RuntimeException ex) {
            throw new OntologySpaceModificationException(this, ex);
        }

    }

    @Override
    public void removeOntologySpaceListener(OntologySpaceListener listener) {
        listeners.remove(listener);
    }

    /**
     * 
     * @param id
     *            The ontology space identifier. This implementation only allows non-null and non-empty
     *            alphanumeric sequences, case-sensitive and preferably separated by a single slash character,
     *            with optional dashes or underscores.
     */
    protected void setID(String id) {
        if (id == null) throw new IllegalArgumentException("Space ID cannot be null.");
        id = id.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Space ID cannot be empty.");
        if (id.matches("[\\w-]+")) log.warn(
            "Space ID {} is a single alphanumeric sequence, with no separating slash."
                    + " This is legal but strongly discouraged. Please consider using"
                    + " space IDs of the form [scope_id]/[space_type], e.g. Users/core .", id);
        else if (!id.matches("[\\w-]+/[\\w-]+")) throw new IllegalArgumentException(
                "Illegal space ID " + id + " - Must be an alphanumeric sequence, (preferably two, "
                        + " slash-separated), with optional underscores or dashes.");
        this._id = id;
    }

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
    public void setSilentMissingOntologyHandling(boolean silent) {
        this.silent = silent;
    }

}
