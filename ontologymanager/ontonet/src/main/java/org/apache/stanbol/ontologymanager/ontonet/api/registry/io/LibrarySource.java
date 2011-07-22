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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.io;

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.io.AbstractOntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryLoader;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.LibraryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.apache.stanbol.owl.util.URIUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology input source that loads all the ontologies in a given library and attaches them to a parent
 * ontology, either new or supplied by the developer. This input source can either accept an already built
 * {@link LibraryImpl} object, or parse a library OWL file from its logical URI.
 */
public class LibrarySource extends AbstractOntologyInputSource {

    private IRI libraryID;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     */
    public LibrarySource(IRI libraryID) {
        this(libraryID, URIUtils.upOne(libraryID));
    }

    /**
     * Creates a new ontology source from a library.
     * 
     * @param libraryID
     * @param registryLocation
     */
    public LibrarySource(IRI libraryID, IRI registryLocation) {
        this(libraryID, registryLocation, null);
    }

    /**
     * Creates a new ontology source from a library.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param registryLocation
     * @param ontologyManager
     * @param loader
     */
    public LibrarySource(IRI libraryID,
                         IRI registryLocation,
                         OWLOntologyManager ontologyManager,
                         RegistryLoader loader) {
        this(libraryID, registryLocation, ontologyManager, loader, null);
    }

    /**
     * Creates a new ontology source from a library.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param registryLocation
     * @param ontologyManager
     * @param loader
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID,
                         IRI registryLocation,
                         OWLOntologyManager ontologyManager,
                         RegistryLoader loader,
                         OntologyInputSource parentSrc) {
        this.libraryID = libraryID;

        // The ontology that imports the whole network is created in-memory, therefore it has no physical IRI.
        bindPhysicalIri(null);

        Set<OWLOntology> subtrees = new HashSet<OWLOntology>();
        Registry reg = loader.loadLibrary(registryLocation, libraryID);
        for (RegistryItem ri : reg.getChildren()) {
            if (ri.isLibrary()) try {
                Set<OWLOntology> adds = loader.gatherOntologies(ri, ontologyManager, true);
                subtrees.addAll(adds);
            } catch (OWLOntologyAlreadyExistsException e) {
                // Chettefreca
                continue;
            } catch (OWLOntologyCreationException e) {
                log.warn("Failed to load ontology library " + ri.getName() + ". Skipping.", e);
                // If we can't load this library at all, scrap it.
                // TODO : not entirely convinced of this step.
                continue;
            }
        }

        // We always construct a new root now, even if there's just one subtree.

        // Set<OWLOntology> subtrees = mgr.getOntologies();
        // if (subtrees.size() == 1)
        // rootOntology = subtrees.iterator().next();
        // else
        try {
            if (parentSrc != null) bindRootOntology(OntologyUtils.buildImportTree(parentSrc, subtrees,
                ontologyManager));
            else bindRootOntology(OntologyUtils.buildImportTree(subtrees, ontologyManager));
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to build import tree for registry source " + registryLocation, e);
        }
    }

    /**
     * Creates a new ontology source from a library.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param registryLocation
     * @param loader
     */
    public LibrarySource(IRI libraryID, IRI registryLocation, RegistryLoader loader) {
        this(libraryID, registryLocation, OWLManager.createOWLOntologyManager(), loader);
    }

    /**
     * Creates a new ontology source from a library.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param registryLocation
     * @param loader
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID,
                         IRI registryLocation,
                         RegistryLoader loader,
                         OntologyInputSource parentSrc) {
        this(libraryID, registryLocation, OWLManager.createOWLOntologyManager(), loader, parentSrc);
    }

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param ontologyManager
     * @param loader
     */
    public LibrarySource(IRI libraryID, OWLOntologyManager ontologyManager, RegistryLoader loader) {
        this(libraryID, URIUtils.upOne(libraryID), ontologyManager, loader);
    }

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param ontologyManager
     * @param loader
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID,
                         OWLOntologyManager ontologyManager,
                         RegistryLoader loader,
                         OntologyInputSource parentSrc) {
        this(libraryID, URIUtils.upOne(libraryID), ontologyManager, loader, parentSrc);
    }

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param loader
     */
    public LibrarySource(IRI libraryID, RegistryLoader loader) {
        this(libraryID, URIUtils.upOne(libraryID), loader);
    }

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param loader
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID, RegistryLoader loader, OntologyInputSource parentSrc) {
        this(libraryID, URIUtils.upOne(libraryID), OWLManager.createOWLOntologyManager(), loader, parentSrc);
    }

    @Override
    public String toString() {
        return "LIBRARY<" + libraryID + ">";
    }

}
