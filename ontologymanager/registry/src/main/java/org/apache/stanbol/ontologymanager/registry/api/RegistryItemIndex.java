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
import org.semanticweb.owlapi.model.IRI;

/**
 * Objects that keep track of known libraries, registries and ontologies can implement this interface.
 * 
 * @author alexdma
 * 
 */
public interface RegistryItemIndex {

    /**
     * Returns all the libraries managed by this object.
     * 
     * @return the set of all managed libraries.
     */
    Set<Library> getLibraries();

    /**
     * Returns all the libraries that contain the ontology with the given identifier.
     * 
     * @param ontologyID
     *            the ontology identifier.
     * @return the set of libraries that contain the ontology.
     */
    Set<Library> getLibraries(IRI ontologyID);

    /**
     * Returns the library with the given identifier, if present.<br/>
     * <br/>
     * NOTE THAT IF THE REGISTRY ITEM EXIST BUT IS NOT A LIBRARY, THIS METHOD WILL RETURN NULL.
     * 
     * @param id
     *            the library identifier.
     * @return the library with the given identifier, or null if not present or not a library.
     */
    Library getLibrary(IRI id);

    /**
     * Returns all the registries managed by this object.
     * 
     * @return the set of all managed registries.
     */
    Set<Registry> getRegistries();

    /**
     * Returns all the registries that reference the library with the given identifier.
     * 
     * @return the set of all managed registries.
     */
    Set<Registry> getRegistries(IRI libraryID);

    /**
     * Returns the registry with the given identifier, if present.<br/>
     * <br/>
     * NOTE THAT IF THE REGISTRY ITEM EXIST BUT IS NOT A REGISTRY, THIS METHOD WILL RETURN NULL.
     * 
     * @param id
     *            the registry identifier.
     * @return the registry with the given identifier, or null if not present.
     */
    Registry getRegistry(IRI id);

}
