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
package org.apache.stanbol.ontologymanager.registry.io;

import java.util.Set;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.impl.model.LibraryImpl;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.SetInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyImportUtils;
import org.apache.stanbol.ontologymanager.sources.owlapi.AbstractOWLOntologyInputSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology input source that loads all the ontologies in a given library and attaches them to a parent
 * ontology, either new or supplied by the developer. This input source can either accept an already built
 * {@link LibraryImpl} object, or parse a library OWL file from its logical URI.
 */
public class LibrarySource extends AbstractOWLOntologyInputSource implements SetInputSource {

    /**
     * Creates a new ontology manager that shares the same offline configuration as the registry manager.
     * 
     * @param registryManager
     * @return
     */
    private static OWLOntologyManager checkOntologyManager(RegistryManager registryManager) {
        OfflineConfiguration offline = registryManager.getOfflineConfiguration();
        if (offline == null) return OWLManager.createOWLOntologyManager();
        return OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations()
                .toArray(new IRI[0]));
    }

    private IRI libraryID;

    private Logger log = LoggerFactory.getLogger(getClass());

    private Set<OWLOntology> ontologies;

    /**
     * Creates a new ontology source from a library. The physical registry location is assumed to be the
     * parent URL of <code>libraryID</code>. <br/>
     * <br/>
     * Example : if <code>libraryID</code> is <tt>http://foo.bar.baz/registry#library</tt>, the registry
     * location will be <tt>http://foo.bar.baz/registry</tt>. Same goes for slash-URIs.
     * 
     * @param libraryID
     *            the identifier of the ontology library.
     * @param registryManager
     *            the registry manager that should contain the library data. Must not be null.
     * @throws OWLOntologyCreationException
     */
    public LibrarySource(IRI libraryID, RegistryManager registryManager) throws RegistryContentException,
                                                                        OWLOntologyCreationException {
        this(libraryID, registryManager, checkOntologyManager(registryManager));
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
     * @param registryManager
     *            the registry manager that should contain the library data. Must not be null.
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID,
                         RegistryManager registryManager,
                         OntologyInputSource<OWLOntology> parentSrc) throws RegistryContentException {
        this(libraryID, registryManager, checkOntologyManager(registryManager), parentSrc);
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
     * @param registryManager
     *            the registry manager that should contain the library data. Must not be null.
     * @param ontologyManager
     *            the ontology manager to be used for constructing the import tree. if null, a new one will be
     *            used.
     * @throws OWLOntologyCreationException
     */
    public LibrarySource(IRI libraryID, RegistryManager registryManager, OWLOntologyManager ontologyManager) throws RegistryContentException,
                                                                                                            OWLOntologyCreationException {
        this(libraryID, registryManager, ontologyManager, new RootOntologySource(OWLManager
                .createOWLOntologyManager().createOntology(libraryID
                /* IRI.create(libraryID.toString().replace("#", "%23")) */)));
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
     * @param registryManager
     *            the registry manager that should contain the library data. Must not be null.
     * @param ontologyManager
     *            the ontology manager to be used for constructing the import tree. if null, a new one will be
     *            used.
     * @param parentSrc
     *            the source of the ontology that will import all the ontologies in the registry. If null, a
     *            new blank ontology will be used.
     */
    public LibrarySource(IRI libraryID,
                         RegistryManager registryManager,
                         OWLOntologyManager ontologyManager,
                         OntologyInputSource<OWLOntology> parentSrc) throws RegistryContentException {
        if (registryManager == null) throw new IllegalArgumentException(
                "A null registry manager is not allowed");

        this.libraryID = libraryID;

        // The ontology that imports the whole network is created in-memory, therefore it has no physical IRI
        // unless it is borrowed from the supplied parent.
        bindPhysicalOrigin(parentSrc != null ? parentSrc.getOrigin() : null);

        Library lib = registryManager.getLibrary(libraryID);
        log.debug("Got library {}, expected {}", lib, libraryID);
        // If the manager is set to
        if (lib != null) {
            Set<OWLOntology> subtrees = lib.getOntologies(OWLOntology.class);
            this.ontologies = subtrees;

            for (OWLOntology o : subtrees)
                log.debug("\tGot ontology {}", o);

            // We always construct a new root now, even if there's just one subtree.

            // if (subtrees.size() == 1)
            // rootOntology = subtrees.iterator().next();
            // else
            try {
                OWLOntology parent;
                if (parentSrc != null) parent = OntologyImportUtils.buildImportTree(parentSrc, subtrees,
                    ontologyManager);
                else parent = OntologyImportUtils.buildImportTree(subtrees, ontologyManager);
                bindRootOntology(parent);
            } catch (OWLOntologyCreationException e) {
                log.error("Failed to build import tree for library source " + libraryID, e);
            }
        }

    }

    @Override
    public Set<OWLOntology> getOntologies() {
        return ontologies;
    }

    @Override
    public String toString() {
        return "LIBRARY<" + libraryID + ">";
    }

}
