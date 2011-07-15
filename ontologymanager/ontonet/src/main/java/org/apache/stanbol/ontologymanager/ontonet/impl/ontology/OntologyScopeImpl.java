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
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeOntologyListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of an ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologyScopeImpl implements OntologyScope, OntologySpaceListener {

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
    protected IRI id = null;

    private Set<ScopeOntologyListener> listeners = new HashSet<ScopeOntologyListener>();

    /**
     * An ontology scope knows whether it's write-locked or not. Initially it is not.
     */
    protected boolean locked = false;

    /**
     * Maps session IDs to ontology space. A single scope has at most one space per session.
     */
    protected Map<IRI,SessionOntologySpace> sessionSpaces;

    public OntologyScopeImpl(IRI id, OntologySpaceFactory factory, OntologyInputSource coreRoot) {
        this(id, factory, coreRoot, null);
    }

    public OntologyScopeImpl(IRI id,
                             OntologySpaceFactory factory,
                             OntologyInputSource coreRoot,
                             OntologyInputSource customRoot) {
        if (id == null) throw new NullPointerException("Ontology scope must be identified by a non-null IRI.");

        this.id = id;
        this.coreSpace = factory.createCoreOntologySpace(id, coreRoot);
        this.coreSpace.addOntologySpaceListener(this);
        // let's just lock it. Once the core space is done it's done.
        this.coreSpace.setUp();
        // if (customRoot != null) {
        try {
            setCustomSpace(factory.createCustomOntologySpace(id, customRoot));
        } catch (UnmodifiableOntologySpaceException e) {
            // Can't happen unless the factory or space implementations are
            // really naughty.
            LoggerFactory
                    .getLogger(getClass())
                    .warn(
                        "KReS :: Ontology scope "
                                + id
                                + " was denied creation of its own custom space upon initialization! This should not happen.",
                        e);
        }
        this.customSpace.addOntologySpaceListener(this);
        // }
        sessionSpaces = new HashMap<IRI,SessionOntologySpace>();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope# addOntologyScopeListener
     * (eu.iksproject.kres.api.manager.ontology.ScopeOntologyListener)
     */
    @Override
    public void addOntologyScopeListener(ScopeOntologyListener listener) {
        listeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#addSessionSpace
     * (eu.iksproject.kres.api.manager.ontology.OntologySpace, org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public synchronized void addSessionSpace(OntologySpace sessionSpace, IRI sessionId) throws UnmodifiableOntologySpaceException {
        if (sessionSpace instanceof SessionOntologySpace) {
            sessionSpaces.put(sessionId, (SessionOntologySpace) sessionSpace);
            sessionSpace.addOntologySpaceListener(this);

            if (this.getCustomSpace() != null)
                ((SessionOntologySpace) sessionSpace).attachSpace(this.getCustomSpace(), true);
            else
                ((SessionOntologySpace) sessionSpace).attachSpace(this.getCoreSpace(), true);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope# clearOntologyScopeListeners()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getCoreSpace()
     */
    @Override
    public OntologySpace getCoreSpace() {
        return coreSpace;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getCustomSpace()
     */
    @Override
    public OntologySpace getCustomSpace() {
        return customSpace;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getID()
     */
    @Override
    public IRI getID() {
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope# getOntologyScopeListeners()
     */
    @Override
    public Collection<ScopeOntologyListener> getOntologyScopeListeners() {
        return listeners;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getSessionSpace
     * (org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public SessionOntologySpace getSessionSpace(IRI sessionID) {
        return sessionSpaces.get(sessionID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getSessionSpaces()
     */
    @Override
    public Set<OntologySpace> getSessionSpaces() {
        return new HashSet<OntologySpace>(sessionSpaces.values());
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceListener#onOntologyAdded
     * (org.semanticweb.owlapi.model.IRI, org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public void onOntologyAdded(IRI spaceId, IRI addedOntology) {
        // Propagate events to scope listeners
        fireOntologyAdded(addedOntology);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeeu.iksproject.kres.api.manager.ontology.OntologySpaceListener#
     * onOntologyRemoved(org.semanticweb.owlapi.model.IRI, org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public void onOntologyRemoved(IRI spaceId, IRI removedOntology) {
        // Propagate events to scope listeners
        fireOntologyRemoved(removedOntology);
    }

    @Override
    public void removeOntologyScopeListener(ScopeOntologyListener listener) {
        listeners.remove(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#setCustomSpace(
     * eu.iksproject.kres.api.manager.ontology.OntologySpace)
     */
    @Override
    public synchronized void setCustomSpace(OntologySpace customSpace) throws UnmodifiableOntologySpaceException {
        if (this.customSpace != null && this.customSpace.isLocked()) throw new UnmodifiableOntologySpaceException(
                getCustomSpace());
        else if (!(customSpace instanceof CustomOntologySpace)) throw new ClassCastException(
                "supplied object is not a CustomOntologySpace instance.");
        else {
            this.customSpace = (CustomOntologySpace) customSpace;
            this.customSpace.addOntologySpaceListener(this);
            this.customSpace.attachCoreSpace(this.coreSpace, true);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#setUp()
     */
    @Override
    public synchronized void setUp() {
        if (locked || (customSpace != null && !customSpace.isLocked())) return;
        this.coreSpace.addOntologySpaceListener(this);
        this.coreSpace.setUp();
        if (this.customSpace != null) {
            this.customSpace.addOntologySpaceListener(this);
            this.customSpace.setUp();
        }
        locked = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#tearDown()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getID().toString();
    }

    @Override
    public void synchronizeSpaces() {
        // TODO Auto-generated method stub

    }

}
