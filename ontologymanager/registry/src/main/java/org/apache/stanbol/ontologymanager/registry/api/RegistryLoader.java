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
package org.apache.stanbol.ontologymanager.registry.api;

import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
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

    /**
     * Loads all the OWL ontologies referenced by <code>registryItem</code>.
     * 
     * @param registryItem
     *            the parent registry item.
     * @param manager
     *            the OWL ontology manager to use for loading (e.g. to avoid reloading ontologies).
     * @param recurse
     *            if true, load also ontologies that are indirectly referenced (e.g. if
     *            <code>registryItem</code> is a {@link Registry}).
     * @return
     * @throws OWLOntologyCreationException
     */
    Set<OWLOntology> gatherOntologies(RegistryItem registryItem, OWLOntologyManager manager, boolean recurse) throws OWLOntologyCreationException;

    /**
     * @deprecated obsolete. Refer to {@link Registry#getChild(IRI)} instead.
     */
    Library getLibrary(Registry reg, IRI libraryID);

    /**
     * @deprecated obsolete. Refer to {@link RegistryItem#getParent(IRI)} instead.
     */
    Object getParent(Object child);

    /**
     * @deprecated obsolete. Refer to {@link RegistryItem#hasChildren()} instead.
     */
    boolean hasChildren(Object parent);

    /**
     * @deprecated obsolete. Refer to {@link Registry#getChild(IRI)} instead.
     */
    boolean hasLibrary(Registry reg, IRI libraryID);

    /**
     * Only extracts the ontologies belonging to the library specified, if found in the registry at the
     * supplied location.
     * 
     * @param registryPhysicalRIRI
     * @param libraryID
     * @return
     * @deprecated This method does not what is supposed to do (ontology loading is selective, not model
     *             construction). Calls to this method should be replaced by the sequence:
     *             {@link RegistryManager#createModel(Set)} and {@link RegistryManager#getRegistry(IRI)}.
     */
    Registry loadLibrary(IRI registryPhysicalIRI, IRI libraryID);

    /**
     * 
     * @throws RegistryContentException
     * @deprecated obsolete
     */
    void loadLocations() throws RegistryContentException;

    /**
     * The ontology at <code>physicalIRI</code> may in turn include more than one library.
     * 
     * @param physicalIRI
     * @return
     * @deprecated Calls to this method should be replaced by the sequence:
     *             {@link RegistryManager#createModel(Set)} and {@link RegistryManager#getRegistry(IRI)}.
     */
    Registry loadRegistry(IRI registryPhysicalIRI, OWLOntologyManager mgr);
}
