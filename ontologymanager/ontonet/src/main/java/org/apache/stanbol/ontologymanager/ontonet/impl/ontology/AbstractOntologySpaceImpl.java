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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

    protected IRI _id = null;

    private Set<OntologySpaceListener> listeners = new HashSet<OntologySpaceListener>();

    /**
     * Indicates whether this ontology space is marked as read-only. Default value is false.
     */
    protected boolean locked = false;

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The identifier of the ontologies directly managed by this space (i.e. that were directly added to this
     * space, hence not including those just pulled in via import statements).
     */
    protected Set<OWLOntology> managedOntologies;

    /**
     * Each ontology space comes with its OWL ontology manager. By default, it is not available to the outside
     * world, unless subclasses implement methods to return it.
     */
    protected OWLOntologyManager ontologyManager;

    protected IRI parentID = null;

    protected boolean silent = false;

    protected ClerezzaOntologyStorage storage;

    protected SpaceType type;

    protected AbstractOntologySpaceImpl(IRI spaceID, SpaceType type, ClerezzaOntologyStorage storage) {
        this(spaceID, type, storage, OWLManager.createOWLOntologyManager());
    }

    /**
     * Creates a new ontology space with the supplied ontology manager as the default manager for this space.
     * 
     * @param spaceID
     *            the IRI that will uniquely identify this space.
     * @param parentID
     *            IRI of the parent scope (TODO: get rid of it).
     * @param ontologyManager
     *            the default ontology manager for this space.
     */
    protected AbstractOntologySpaceImpl(IRI spaceID,
                                        SpaceType type,
                                        ClerezzaOntologyStorage storage,
                                        OWLOntologyManager ontologyManager) {
        this._id = spaceID;
        this.type = type;
        this.storage = storage;
        if (ontologyManager != null) this.ontologyManager = ontologyManager;
        else this.ontologyManager = OWLManager.createOWLOntologyManager();

        this.managedOntologies = new HashSet<OWLOntology>();
    }

    @Override
    public synchronized void addOntology(OntologyInputSource ontologySource) throws UnmodifiableOntologySpaceException {
        if (locked) throw new UnmodifiableOntologySpaceException(this);
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
        try {
            root = ontologyManager.createOntology(_id);
        } catch (OWLOntologyAlreadyExistsException e) {
            ontologyManager.removeOntology(ontologyManager.getOntology(_id));
            try {
                root = ontologyManager.createOntology(_id);
            } catch (OWLOntologyAlreadyExistsException e1) {
                root = ontologyManager.getOntology(_id);
            } catch (OWLOntologyCreationException e1) {
                log.error("Failed to assemble root ontology for scope " + _id, e);
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

                String base = URIUtils.upOne(getID()) + "/";

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
            listener.onOntologyAdded(_id, ontologyIri);
    }

    /**
     * Notifies all ontology space listeners that an ontology has been removed from this space.
     * 
     * @param ontologyIri
     *            the identifier of the ontology that was removed from this space.
     */
    protected void fireOntologyRemoved(IRI ontologyIri) {
        for (OntologySpaceListener listener : listeners)
            listener.onOntologyRemoved(_id, ontologyIri);
    }

    @Override
    public IRI getID() {
        return _id;
    }

    @Override
    public synchronized Set<OWLOntology> getOntologies(boolean withClosure) {
        return withClosure ? ontologyManager.getOntologies() : managedOntologies;
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        OWLOntology o = null;
        Iterator<OWLOntology> it = managedOntologies.iterator();
        while (it.hasNext() && o == null) {
            OWLOntology temp = it.next();
            if (!temp.isAnonymous() && ontologyIri.equals(temp.getOntologyID().getOntologyIRI())) o = temp;
        }
        if (o == null) o = ontologyManager.getOntology(ontologyIri);
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
        managedOntologies.add(newOnt);

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

    @Override
    public void setSilentMissingOntologyHandling(boolean silent) {
        this.silent = silent;
    }

}
