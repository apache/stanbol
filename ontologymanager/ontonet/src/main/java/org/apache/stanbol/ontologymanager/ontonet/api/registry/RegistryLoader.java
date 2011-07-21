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
package org.apache.stanbol.ontologymanager.ontonet.api.registry;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A registry loader is a toolkit for loading all ontologies indexed by an ontology registry, or those
 * referenced by one of the libraries within a registry.<br/>
 * <br/>
 * TODO will be dismissed along with its implementation in favor of the new registry management.
 */
public interface RegistryLoader {

    Set<OWLOntology> gatherOntologies(RegistryItem registryItem,
                                      OWLOntologyManager manager,
                                      boolean recurseRegistries) throws OWLOntologyCreationException;

    Library getLibrary(Registry reg, IRI libraryID);

    Object getParent(Object child);

    boolean hasChildren(Object parent);

    boolean hasLibrary(Registry reg, IRI libraryID);

    /**
     * Only extract the ontologies belonging to the library specified, if found in the registry at the
     * supplied location.
     * 
     * @param registryPhysicalRIRI
     * @param libraryID
     * @return
     */
    Registry loadLibraryEager(IRI registryPhysicalIRI, IRI libraryID);

    void loadLocations() throws RegistryContentException;

    /**
     * The ontology at <code>physicalIRI</code> may in turn include more than one library.
     * 
     * @param physicalIRI
     * @return
     */
    Registry loadRegistry(IRI registryPhysicalIRI, OWLOntologyManager mgr);
}
