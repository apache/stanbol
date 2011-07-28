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

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.LibraryContentNotLoadedException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryOntologyNotLoadedException;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the ontology library model.
 */
public class LibraryImpl extends AbstractRegistryItem implements Library {

    private OWLOntologyManager cache;

    private boolean loaded = false;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new instance of {@link LibraryImpl}.
     * 
     * @param iri
     *            the library identifier and possible physical location.
     */
    public LibraryImpl(IRI iri) {
        this(iri, OWLManager.createOWLOntologyManager());
    }

    /**
     * Creates a new instance of {@link LibraryImpl}.
     * 
     * @param iri
     *            the library identifier and possible physical location.
     * @param cache
     *            the {@link OWLOntologyManager} to be used for caching ontologies in-memory.
     */
    public LibraryImpl(IRI iri, OWLOntologyManager cache) {
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
     */
    public LibraryImpl(IRI iri, String name) {
        this(iri, name, OWLManager.createOWLOntologyManager());
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
    public LibraryImpl(IRI iri, String name, OWLOntologyManager cache) {
        super(iri, name);
        setCache(cache);
    }

    @Override
    public OWLOntologyManager getCache() {
        return cache;
    }

    @Override
    public Set<OWLOntology> getOntologies() throws RegistryContentException {
        /*
         * Note that this implementation is not synchronized. Listeners may indefinitely be notified before or
         * after the rest of this method is executed. If listeners call loadOntologies(), they could still get
         * a RegistryContentException, which however they can catch by calling loadOntologies() and
         * getOntologies() in sequence.
         */
        fireContentRequested(this);
        // If no listener has saved the day by loading the ontologies by now, an exception will be thrown.
        if (!loaded) throw new LibraryContentNotLoadedException(this);
        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        for (RegistryItem child : getChildren()) {
            if (child instanceof RegistryOntology) {
                OWLOntology o = ((RegistryOntology) child).getRawOntology(this.getIRI());
                // Should never be null if the library was loaded correctly (an error should have already been
                // thrown when loading it), but just in case.
                if (o != null) ontologies.add(o);
                else throw new RegistryOntologyNotLoadedException((RegistryOntology) child);
            }
        }
        return ontologies;
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
    public synchronized void loadOntologies(OWLOntologyManager mgr) {
        if (mgr == null) throw new IllegalArgumentException("A null ontology manager is not allowed.");
        for (RegistryItem item : getChildren()) {
            if (item instanceof RegistryOntology) {
                RegistryOntology o = (RegistryOntology) item;
                IRI id = o.getIRI();
                try {
                    o.setRawOntology(getIRI(), mgr.loadOntology(id));
                } catch (OWLOntologyAlreadyExistsException e) {
                    o.setRawOntology(getIRI(), mgr.getOntology(e.getOntologyID()));
                } catch (OWLOntologyDocumentAlreadyExistsException e) {
                    o.setRawOntology(getIRI(), mgr.getOntology(e.getOntologyDocumentIRI()));
                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to load ontology " + id, e);
                }
            }
        }
        loaded = true;
    }

    @Override
    public void removeChild(RegistryItem child) {
        super.removeChild(child);
        // Also unload the ontology version that comes from this library.
        if (child instanceof RegistryOntology) ((RegistryOntology) child).setRawOntology(getIRI(), null);
    }

    @Override
    public void setCache(OWLOntologyManager cache) {
        // TODO use the ontology manager factory.
        if (cache == null) cache = OWLManager.createOWLOntologyManager();
        this.cache = cache;
    }

}
