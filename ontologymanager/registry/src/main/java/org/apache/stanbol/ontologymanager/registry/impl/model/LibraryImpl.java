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
package org.apache.stanbol.ontologymanager.registry.impl.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.IllegalRegistryCycleException;
import org.apache.stanbol.ontologymanager.registry.api.LibraryContentNotLoadedException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryOntologyNotLoadedException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryOperation;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the ontology library model.
 */
public class LibraryImpl extends AbstractRegistryItem implements Library {

    private OntologyProvider<?> cache;

    private boolean loaded = false;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new instance of {@link LibraryImpl}.
     * 
     * @param iri
     *            the library identifier and possible physical location.
     * @param cache
     *            the {@link OWLOntologyManager} to be used for caching ontologies in-memory.
     */
    public LibraryImpl(IRI iri, OntologyProvider<?> cache) {
        super(iri);
        setCache(cache);
    }

    /**
     * Creates a new instance of {@link LibraryImpl}.
     * 
     * @param iri
     *            the library identifier and possible physical location.
     * @param name
     *            the short name of this library.
     * @param cache
     *            the {@link OWLOntologyManager} to be used for caching ontologies in-memory.
     */
    public LibraryImpl(IRI iri, String name, OntologyProvider<?> cache) {
        super(iri, name);
        setCache(cache);
    }

    @Override
    public void addChild(RegistryItem child) throws RegistryContentException {
        if (child instanceof Registry || child instanceof Library) throw new IllegalRegistryCycleException(
                this, child, RegistryOperation.ADD_CHILD);
        super.addChild(child);
    }

    @Override
    public void addParent(RegistryItem parent) throws RegistryContentException {
        if (parent instanceof RegistryOntology || parent instanceof Library) throw new IllegalRegistryCycleException(
                this, parent, RegistryOperation.ADD_PARENT);
        super.addParent(parent);
    }

    @Override
    public OntologyProvider<?> getCache() {
        return cache;
    }

    @Override
    public <O> Set<O> getOntologies(Class<O> returnType) throws RegistryContentException {
        /*
         * Note that this implementation is not synchronized. Listeners may indefinitely be notified before or
         * after the rest of this method is executed. If listeners call loadOntologies(), they could still get
         * a RegistryContentException, which however they can catch by calling loadOntologies() and
         * getOntologies() in sequence.
         */
        fireContentRequested(this);
        // If no listener has saved the day by loading the ontologies by now, an exception will be thrown.
        if (!loaded) throw new LibraryContentNotLoadedException(this);
        Set<O> ontologies = new HashSet<O>();
        for (RegistryItem child : getChildren()) {
            if (child instanceof RegistryOntology) {
                O o = getCache().getStoredOntology(child.getIRI(), returnType);
                // Should never be null if the library was loaded correctly (an error should have already been
                // thrown when loading it), but just in case.
                if (o != null) ontologies.add(o);
                else throw new RegistryOntologyNotLoadedException((RegistryOntology) child);
            }
        }
        return ontologies;
    }

    @Override
    public <O> O getOntology(IRI id, Class<O> returnType) throws RegistryContentException {
        /*
         * Note that this implementation is not synchronized. Listeners may indefinitely be notified before or
         * after the rest of this method is executed. If listeners call loadOntologies(), they could still get
         * a RegistryContentException, which however they can catch by calling loadOntologies() and
         * getOntologies() in sequence.
         */
        fireContentRequested(this);
        // If no listener has saved the day by loading the ontologies by now, an exception will be thrown.
        if (!loaded) throw new LibraryContentNotLoadedException(this);

        O ontology = null;

        RegistryItem child = getChild(id);
        if (child instanceof RegistryOntology) {
            ontology = getCache().getStoredOntology(child.getIRI(), returnType);
        }
        return ontology;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public synchronized void loadOntologies(OntologyProvider<?> loader) {
        if (loader == null) throw new IllegalArgumentException("A null loader is not allowed.");
        for (RegistryItem item : getChildren()) {
            if (item instanceof RegistryOntology) {
                RegistryOntology o = (RegistryOntology) item;
                IRI id = o.getIRI();
                try {
                    // No preferred key, we don't have a prefix here.
                    OWLOntologyID key = loader.loadInStore(id, null, false);
                    if (key == null || key.isAnonymous()) log.error(
                        "Empty storage key. Ontology {} was apparently not stored.", id);
                } catch (IOException ex) {
                    log.error("I/O error occurred loading {}", id);
                }
            }
        }
        loaded = true;
    }

    @Override
    public void removeChild(RegistryItem child) {
        super.removeChild(child);
        // Also unload the ontology version that comes from this library.
    }

    @Override
    public void setCache(OntologyProvider<?> cache) {
        if (cache == null) cache = new ClerezzaOntologyProvider(TcManager.getInstance(), null,
                Parser.getInstance());
        else {
            Object store = cache.getStore();
            if (!(store instanceof TcProvider || store instanceof OWLOntologyManager)) throw new IllegalArgumentException(
                    "Type "
                            + store.getClass()
                            + "is not supported. This ontology library implementation only supports caches based on either "
                            + TcProvider.class + " or " + OWLOntologyManager.class);
        }
        this.cache = cache;
    }

}
