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
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.MissingOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of an ontology space. While it still leaves it up to developers to decide what
 * locking policies to adopt for subclasses (in the <code>setUp()</code> method), it provides default
 * implementations of all other interface methods.<br>
 * <br>
 * NOTE: By default, an ontology space is NOT write-locked. Developers need to set the <code>locked</code>
 * variable to true to make the space read-only.
 * 
 * 
 * @author alessandro
 * 
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
     * Each ontology space comes with its OWL ontology manager. By default, it is not available to the outside
     * world, unless subclasses implement methods to return it.
     */
    protected OWLOntologyManager ontologyManager;

    protected ClerezzaOntologyStorage storage;

    protected IRI parentID = null;

    // public static String SUFFIX = "";

    protected OWLOntology rootOntology = null;

    protected boolean silent = false;

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

        // this.parentID = parentID;
        // SUFFIX = type.getIRISuffix();

        // // FIXME: ensure that this is not null
        // OntologyScope parentScope = ONManager.get().getScopeRegistry()
        // .getScope(parentID);
        //
        // if (parentScope != null && parentScope instanceof OntologySpaceListener)
        // this.addOntologySpaceListener((OntologySpaceListener) parentScope);
        this.storage = storage;

        this._id = spaceID;
        if (ontologyManager != null) this.ontologyManager = ontologyManager;
        else this.ontologyManager = OWLManager.createOWLOntologyManager();

    }

    /**
     * TODO: manage import statements
     * 
     * TODO 2 : manage anonymous ontologies.
     */
    @Override
    public synchronized void addOntology(OntologyInputSource ontologySource) throws UnmodifiableOntologySpaceException {

        if (locked) throw new UnmodifiableOntologySpaceException(this);

        if (getTopOntology() == null) {
            // If no top ontology has been set, we must create one first.
            IRI rootIri = null;
            try {
                rootIri = IRI.create(StringUtils.stripIRITerminator(getID()) + "/root.owl");
                OntologyInputSource src = new RootOntologySource(ontologyManager.createOntology(rootIri),
                        null);
                // Don't bother about the ontology to be added right now.
                setTopOntology(src, false);
            } catch (OWLOntologyCreationException e) {
                log.error("KReS :: Exception caught when creating top ontology " + rootIri + " for space "
                          + this.getID() + ".", e);
                // No point in continuing if we can't even create the root...
                return;
            }
        }

        // Now add the new ontology.
        if (ontologySource != null && ontologySource.hasRootOntology()) {
            // Remember that this method also fires the event
            performAdd(ontologySource);
        }

    }

    @Override
    public void addOntologySpaceListener(OntologySpaceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearOntologySpaceListeners() {
        listeners.clear();
    }

    @Override
    public boolean containsOntology(IRI ontologyIri) {
        return ontologyManager.contains(ontologyIri);
    }

    protected void fireOntologyAdded(IRI ontologyIri) {
        for (OntologySpaceListener listener : listeners)
            listener.onOntologyAdded(this.getID(), ontologyIri);
    }

    protected void fireOntologyRemoved(IRI ontologyIri) {
        for (OntologySpaceListener listener : listeners)
            listener.onOntologyRemoved(this.getID(), ontologyIri);
    }

    @Override
    public IRI getID() {
        return _id;
    }

    @Override
    public synchronized Set<OWLOntology> getOntologies() {
        return ontologyManager.getOntologies();
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        return ontologyManager.getOntology(ontologyIri);
    }

    @Override
    public Collection<OntologySpaceListener> getOntologyScopeListeners() {
        return listeners;
    }

    @Override
    public OWLOntology getTopOntology() {
        return rootOntology;
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
        OWLOntologyID id = ontology.getOntologyID();

        // Should not modify the child ontology in any way.
        // TODO implement transaction control.
        OntologyUtils.appendOntology(new RootOntologySource(getTopOntology(), null), ontSrc, ontologyManager);

        // StringDocumentTarget tgt = new StringDocumentTarget();
        // try {
        // ontologyManager.saveOntology(ontology, new RDFXMLOntologyFormat(), tgt);
        // } catch (OWLOntologyStorageException e) {
        // log.error("KReS : [FATAL] Failed to store ontology " + id + " in memory.", e);
        // return;
        // }
        reload(ontology, ontologyManager, true, false);

        // try {
        // ontologyManager.removeOntology(ontology);
        //
        // // FIXME : this is not memory-efficient.
        // Set<OWLOntology> closure = ontology.getOWLOntologyManager().getImportsClosure(ontology);
        // ontologyManager.createOntology(ontology.getOntologyID().getOntologyIRI(), closure);
        // // FIXME : this on the other hand is neither time-efficient nor network-safe
        // // ontologyManager
        // // .loadOntologyFromOntologyDocument(new StringDocumentSource(
        // // tgt.toString()));
        // } catch (OWLOntologyAlreadyExistsException e) {
        // // Could happen if we supplied an ontology manager that already
        // // knows this ontology. Nothing to do then.
        // log.warn("KReS : [NONFATAL] Tried to copy ontology " + id + " to existing one.");
        // } catch (OWLOntologyCreationException e) {
        // log.error("Unexpected exception caught while copying ontology " + id + " across managers", e);
        // return;
        // }

        try {
            // Store the top ontology
            if (!(this instanceof SessionOntologySpace)) {
                if (storage == null) log
                        .error("KReS :: [NONFATAL] no ontology storage found. Ontology "
                               + ontology.getOntologyID() + " will be stored in-memory only.");
                else {
                    // storage = new ClerezzaOntologyStorage(tcManager, wtcProvider)
                    storage.store(ontology);
                }
            }
            // ONManager.get().getOntologyStore().load(rootOntology.getOntologyID().getOntologyIRI());
        } catch (Exception ex) {
            log.error("KReS :: [NONFATAL] An error occurred while storing ontology " + ontology
                      + " . Ontology management will be volatile!", ex);
        }
        fireOntologyAdded(id.getOntologyIRI());
    }

    /**
     * TODO 1 : optimize addition/removal <br>
     * TODO 2 : set import statements
     */
    @Override
    public synchronized void removeOntology(OntologyInputSource src) throws OntologySpaceModificationException {
        if (locked) throw new UnmodifiableOntologySpaceException(this);
        else {
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
            if (o.equals(getTopOntology()))
            // setTopOntology(null, false);
            throw new IrremovableOntologyException(this, logicalID);
            try {
                OWLImportsDeclaration imp = ontologyManager.getOWLDataFactory().getOWLImportsDeclaration(
                    physicalIRI);
                ontologyManager.applyChange(new RemoveImport(getTopOntology(), imp));
                ontologyManager.removeOntology(o);
                fireOntologyRemoved(logicalID);
            } catch (RuntimeException ex) {
                throw new OntologySpaceModificationException(this);
            }
        }
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
    protected void reload(OWLOntology ontology, OWLOntologyManager mgr, boolean withClosure, boolean merge) {
        if (mgr == null) mgr = ontologyManager;
        Set<OWLOntology> closure = withClosure ? ontology.getOWLOntologyManager().getImportsClosure(ontology)
                : Collections.singleton(ontology);
        mgr.removeOntology(ontology);
        if (merge) try {
            mgr.createOntology(ontology.getOntologyID().getOntologyIRI(), closure);
        } catch (OWLOntologyCreationException e1) {
            log.error("Unexpected exception caught while copying ontology " + ontology.getOntologyID()
                      + " across managers", e1);
        }
        else {
            for (OWLOntology o : closure) {
                OWLOntologyID id = o.getOntologyID();
                if (mgr.contains(id)) mgr.removeOntology(mgr.getOntology(id));
                try {
                    mgr.createOntology(id.getOntologyIRI(), Collections.singleton(o));
                } catch (OWLOntologyAlreadyExistsException e) {
                    if (o.getOWLOntologyManager() != mgr) {
                        mgr.removeOntology(o);
                        try {
                            mgr.createOntology(id.getOntologyIRI(), Collections.singleton(o));
                        } catch (OWLOntologyCreationException e1) {
                            log.error(
                                "Unexpected exception caught while copying ontology "
                                        + ontology.getOntologyID() + " across managers", e1);
                        }
                    }

                } catch (OWLOntologyCreationException e) {
                    log.warn("Failed to re-create ontology {} for ontology space {} . Continuing...", id,
                        getID());
                }
            }
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

    @Override
    public synchronized void setTopOntology(OntologyInputSource ontologySource) throws UnmodifiableOntologySpaceException {
        setTopOntology(ontologySource, true);
    }

    /**
     * TODO 1 : Attention: the ontology from <code>ontologySource</code> is NOT added to the ontology manager!
     */
    @Override
    public synchronized void setTopOntology(OntologyInputSource ontologySource, boolean createParent) throws UnmodifiableOntologySpaceException {

        // TODO : implement or avoid passing of OWLOntology objects across managers

        // Clear the ontology manager
        for (OWLOntology o : ontologyManager.getOntologies()) {
            ontologyManager.removeOntology(o);
            fireOntologyRemoved(o.getOntologyID().getOntologyIRI());
        }

        OWLOntologyID id = new OWLOntologyID(IRI.create(StringUtils.stripIRITerminator(_id) + "/root.owl"));
        OWLOntology ontology = null;
        if (ontologySource != null) {
            ontology = ontologySource.getRootOntology();
        }

        // Placeholder for the parent ontology (can be either the space root or the supplied ontology).
        OWLOntology oParent = null;

        // If set to create a parent ontology or this one is anonymous, create the parent
        if (createParent || ontology == null || ontology.isAnonymous()) {

            try {
                oParent = ontologyManager.createOntology(id);
            } catch (OWLOntologyAlreadyExistsException e) {
                // Should not happen since the manager was cleared, but anyway.
                oParent = ontologyManager.getOntology(id);
            } catch (OWLOntologyCreationException e) {
                log.error("KReS :: Failed to copy ontology " + ontology.getOntologyID()
                          + " across ontology managers.", e);
            }
        } else {
            // If we don't have to create a parent, set the original ontology to be the parent.
            oParent = ontology;
        }

        if (ontologySource != null) try {

            // Append the supplied ontology to the parent.
            oParent = OntologyUtils.appendOntology(new RootOntologySource(oParent, null), ontologySource,
                ontologyManager);

            // Save and reload it to make sure the whole import closure is
            // loaded in memory.
            StringDocumentTarget tgt = new StringDocumentTarget();
            ontologyManager.saveOntology(oParent, new RDFXMLOntologyFormat(), tgt);
            ontologyManager.removeOntology(oParent);
            ontologyManager.loadOntologyFromOntologyDocument(new StringDocumentSource(tgt.toString()));

            // // FIXME why doesnt this work?
            // // ontologyManager.removeOntology(oParent);
            // reload(oParent, ontologyManager, true, false);

        } catch (OWLOntologyAlreadyExistsException e) {
            log.warn("KReS : [NONFATAL] Tried to copy ontology " + id + " to existing one.", e);
        } catch (OWLOntologyCreationException e) {
            log.error("KReS : [FATAL] Failed to create ontology " + id, e);
        } catch (OWLOntologyStorageException e) {
            // Shouldn't be a problem to save it in memory as RDF/XML...
            log.error("KReS : [FATAL] In-memory store failed for ontology " + id, e);
        }

        // Assign the ontology and fire the corresponding event.
        rootOntology = oParent != null ? oParent : ontology;

        try {

            // Store the top ontology
            if (!(this instanceof SessionOntologySpace)) {
                if (storage == null) log.error("KReS :: [NONFATAL] no ontology storage found. Ontology "
                                               + rootOntology.getOntologyID()
                                               + " will be stored in-memory only.");
                else {
                    storage.store(rootOntology);
                }
            }
        } catch (Exception ex) {
            log.error("KReS :: [NONFATAL] An error occurred while storing root ontology " + rootOntology
                      + " . Ontology management will be volatile!", ex);
        }

        fireOntologyAdded(rootOntology.getOntologyID().getOntologyIRI());

    }

}
