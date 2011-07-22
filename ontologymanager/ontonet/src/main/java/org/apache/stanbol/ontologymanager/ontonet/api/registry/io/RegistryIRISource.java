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
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An input source that provides a single ontology that imports all the imported ontology libraries found in
 * the ontology registry obtained by dereferencing a supplied IRI.
 * 
 */
public class RegistryIRISource extends AbstractOntologyInputSource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected IRI registryIRI = null;

    /**
     * @param registryIRI
     * @param ontologyManager
     * @param loader
     */
    public RegistryIRISource(IRI registryIRI, OWLOntologyManager ontologyManager, RegistryLoader loader) {
        this(registryIRI, ontologyManager, loader, null);
    }

    /**
     * Creates a new ontology input source by providing a new root ontology that imports the entire network
     * addressed by the ontology registry at the supplied IRI.
     * 
     * @param registryIRI
     */
    public RegistryIRISource(IRI registryIRI,
                             OWLOntologyManager ontologyManager,
                             RegistryLoader loader,
                             OntologyInputSource parentSrc) {

        this.registryIRI = registryIRI;

        // The ontology that imports the whole network is created in-memory, therefore it has no physical IRI.
        bindPhysicalIri(null);

        Set<OWLOntology> subtrees = new HashSet<OWLOntology>();
        Registry reg = loader.loadRegistry(registryIRI, ontologyManager);
        // for (Registry reg : loader.loadRegistriesEager(registryIRI)) {
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
        // }
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
            log.error("Failed to build import tree for registry source " + registryIRI, e);
        }
    }

    public RegistryIRISource(IRI registryIRI, RegistryLoader loader) {
        this(registryIRI, OWLManager.createOWLOntologyManager(), loader, null);
    }

    public RegistryIRISource(IRI registryIRI, RegistryLoader loader, OntologyInputSource parentSrc) {
        this(registryIRI, OWLManager.createOWLOntologyManager(), loader, parentSrc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.ontonet.api.io.AbstractOntologyInputSource#toString()
     */
    @Override
    public String toString() {
        return "REGISTRY_IRI<" + registryIRI + ">";
    }

}
