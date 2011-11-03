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
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.LockableOntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyInputSourceHandler;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.owl.util.OWLUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic Clerezza-native implementation of an ontology collector.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractOntologyCollectorImpl implements LockableOntologyCollector,
        OntologyInputSourceHandler {

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
     */
    protected Set<IRI> managedOntologies;

    protected IRI namespace = null;

    protected Set<Class<?>> supportedTypes;

    private TcProvider tcProvider;

    public AbstractOntologyCollectorImpl(String id, IRI namespace, TcProvider tcProvider) {
        // Supports OWL API and Clerezza
        supportedTypes = new HashSet<Class<?>>();
        supportedTypes.add(OWLOntology.class);
        supportedTypes.add(TripleCollection.class);
        setID(id);
        setNamespace(namespace);
        this.tcProvider = tcProvider;
        this.managedOntologies = new HashSet<IRI>();
    }

    @Override
    public void addListener(OntologyCollectorListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void addOntology(OntologyInputSource<?> ontologySource) throws UnmodifiableOntologyCollectorException {
        if (locked) throw new UnmodifiableOntologyCollectorException(this);
        log.debug("Adding ontology {} to space {}", ontologySource != null ? ontologySource.getRootOntology()
                : "<NULL>", getNamespace() + getID());
        if (ontologySource == null || !ontologySource.hasRootOntology()) return; // No ontology to add

        Object o = ontologySource.getRootOntology();
        UriRef uri;
        if (o instanceof Graph) {
            uri = OWLUtils.guessOntologyIdentifier((Graph) o);
        } else if (o instanceof OWLOntology) {
            uri = new UriRef(OWLUtils.guessOntologyIdentifier((OWLOntology) o).toString());
        } else throw new UnsupportedOperationException("This ontology space implementation cannot handle "
                                                       + o.getClass() + " objects.");
        // create/get the graph and add the triples.
        MGraph mg;
        try {
            mg = tcProvider.createMGraph(uri);
        } catch (EntityAlreadyExistsException e) {
            mg = tcProvider.getMGraph(uri);
            mg.clear();
        }
        if (o instanceof TripleCollection) mg.addAll((TripleCollection) o);
        else if (o instanceof OWLOntology) {
            // FIXME there must be a better angle than using converters...
            mg.addAll(OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples((OWLOntology) o));
        }

        // add to index
        managedOntologies.add(IRI.create(uri.getUnicodeString()));

        // fire the event
        fireOntologyAdded(uri);
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
        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        for (IRI id : managedOntologies)
            ontologies.add(getOntology(id));
        return Collections.unmodifiableSet(ontologies);
    }

    @Override
    public OWLOntology getOntology(IRI ontologyIri) {
        if (!managedOntologies.contains(ontologyIri)) return null;
        TripleCollection g = tcProvider.getTriples(new UriRef(ontologyIri.toString()));
        return OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(g);
    }

    @Override
    public Set<Class<?>> getSupportedTypes() {
        return Collections.unmodifiableSet(supportedTypes);
    }

    public TcProvider getTcProvider() {
        return tcProvider;
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
