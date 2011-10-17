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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeOntologyListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of an ontology scope.
 * 
 * @author alexdma
 * 
 */
public class OntologyScopeImpl implements OntologyScope, OntologyCollectorListener {

    /**
     * The core ontology space for this scope, always set as default.
     */
    protected CoreOntologySpace coreSpace;

    /**
     * The custom ontology space for this scope. This is optional, but cannot be set after the scope has been
     * setup.
     */
    protected CustomOntologySpace customSpace;

    /**
     * The unique identifier for this scope.
     */
    protected String id = null;

    private Set<ScopeOntologyListener> listeners = new HashSet<ScopeOntologyListener>();

    /**
     * An ontology scope knows whether it's write-locked or not. Initially it is not.
     */
    protected boolean locked = false;

    private Logger log = LoggerFactory.getLogger(getClass());

    protected IRI namespace = null;

    /**
     * Maps session IDs to ontology space. A single scope has at most one space per session.
     */
    protected Map<String,SessionOntologySpace> sessionSpaces;

    public OntologyScopeImpl(String id,
                             IRI namespace,
                             OntologySpaceFactory factory,
                             OntologyInputSource<?>... coreOntologies) {
        setID(id);
        setNamespace(namespace);

        this.coreSpace = factory.createCoreOntologySpace(id, coreOntologies);
        this.coreSpace.addListener(this);
        // let's just lock it. Once the core space is done it's done.
        this.coreSpace.setUp();
        // if (customRoot != null) {
        try {
            setCustomSpace(factory.createCustomOntologySpace(id/* , coreOntologies */));
        } catch (UnmodifiableOntologyCollectorException e) {
            // Can't happen unless the factory or space implementations are
            // really naughty.
            log.warn(
                "Ontology scope "
                        + id
                        + " was denied creation of its own custom space upon initialization! This should not happen.",
                e);
        }
        this.customSpace.addListener(this);
        // }
        sessionSpaces = new HashMap<String,SessionOntologySpace>();
    }

    @Override
    public void addOntologyScopeListener(ScopeOntologyListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void addSessionSpace(OntologySpace sessionSpace, String sessionId) throws UnmodifiableOntologyCollectorException {
        if (sessionSpace instanceof SessionOntologySpace) {
            sessionSpaces.put(sessionId, (SessionOntologySpace) sessionSpace);
            sessionSpace.addListener(this);

            if (this.getCustomSpace() != null) ((SessionOntologySpace) sessionSpace).attachSpace(
                this.getCustomSpace(), true);
            else ((SessionOntologySpace) sessionSpace).attachSpace(this.getCoreSpace(), true);

        }
    }

    @Override
    public OWLOntology asOWLOntology() {
        return this.asOWLOntology(false);
    }

    /**
     * FIXME not merging yet
     * 
     * @see OWLExportable#asOWLOntology(boolean)
     */
    @Override
    public OWLOntology asOWLOntology(boolean merge) {
        // Create an ontology manager on the fly. We don't really need a permanent one.
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = mgr.getOWLDataFactory();
        OWLOntology ont;
        try {
            // The root ontology ID is in the form [namespace][scopeId]
            ont = mgr.createOntology(IRI.create(getNamespace() + getID()));
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
            // Add the import statement for the custom space, if existing and not empty
            OntologySpace spc = getCustomSpace();
            if (spc != null && !spc.getOntologies(false).isEmpty()) {
                IRI spaceIri = IRI.create(getNamespace() + spc.getID());
                additions.add(new AddImport(ont, df.getOWLImportsDeclaration(spaceIri)));
            }
            // Add the import statement for the core space, if existing and not empty
            spc = getCoreSpace();
            if (spc != null && !spc.getOntologies(false).isEmpty()) {
                IRI spaceIri = IRI.create(getNamespace() + spc.getID());
                additions.add(new AddImport(ont, df.getOWLImportsDeclaration(spaceIri)));
            }
            mgr.applyChanges(additions);
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to generate an OWL form of scope " + getID(), e);
            ont = null;
        }
        return ont;
    }

    @Override
    public void clearOntologyScopeListeners() {
        listeners.clear();
    }

    protected void fireOntologyAdded(IRI ontologyIri) {
        for (ScopeOntologyListener listener : listeners)
            listener.onOntologyAdded(this.getID(), ontologyIri);
    }

    protected void fireOntologyRemoved(IRI ontologyIri) {
        for (ScopeOntologyListener listener : listeners)
            listener.onOntologyRemoved(this.getID(), ontologyIri);
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
    public String getID() {
        return id;
    }

    @Override
    public IRI getNamespace() {
        return this.namespace;
    }

    @Override
    public Collection<ScopeOntologyListener> getOntologyScopeListeners() {
        return listeners;
    }

    @Override
    public SessionOntologySpace getSessionSpace(String sessionID) {
        return sessionSpaces.get(sessionID);
    }

    @Override
    public Set<OntologySpace> getSessionSpaces() {
        return new HashSet<OntologySpace>(sessionSpaces.values());
    }

    @Override
    public void onOntologyAdded(IRI spaceId, IRI addedOntology) {
        // Propagate events to scope listeners
        fireOntologyAdded(addedOntology);
    }

    @Override
    public void onOntologyRemoved(IRI spaceId, IRI removedOntology) {
        // Propagate events to scope listeners
        fireOntologyRemoved(removedOntology);
    }

    @Override
    public void removeOntologyScopeListener(ScopeOntologyListener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void setCustomSpace(OntologySpace customSpace) throws UnmodifiableOntologyCollectorException {
        if (this.customSpace != null && this.customSpace.isLocked()) throw new UnmodifiableOntologyCollectorException(
                getCustomSpace());
        else if (!(customSpace instanceof CustomOntologySpace)) throw new ClassCastException(
                "supplied object is not a CustomOntologySpace instance.");
        else {
            this.customSpace = (CustomOntologySpace) customSpace;
            this.customSpace.addListener(this);
            this.customSpace.attachCoreSpace(this.coreSpace, true);
        }

    }

    protected void setID(String id) {
        if (id == null) throw new IllegalArgumentException("Scope ID cannot be null.");
        id = id.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Scope ID cannot be empty.");
        if (!id.matches("[\\w-]+")) throw new IllegalArgumentException(
                "Illegal scope ID " + id
                        + " - Must be an alphanumeric sequence, with optional underscores or dashes.");
        this.id = id;
    }

    /**
     * @param namespace
     *            The OntoNet namespace that will prefix the scope ID in Web references. This implementation
     *            only allows non-null and non-empty IRIs, with no query or fragment. Hash URIs are not
     *            allowed, slash URIs are preferred. If neither, a slash will be concatenated and a warning
     *            will be logged.
     * 
     * @see OntologyScope#setNamespace(IRI)
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
    public synchronized void setUp() {
        if (locked || (customSpace != null && !customSpace.isLocked())) return;
        this.coreSpace.addListener(this);
        this.coreSpace.setUp();
        if (this.customSpace != null) {
            this.customSpace.addListener(this);
            this.customSpace.setUp();
        }
        locked = true;
    }

    @Override
    public void synchronizeSpaces() {
        // TODO Auto-generated method stub

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
        return getNamespace() + getID();
    }

}
