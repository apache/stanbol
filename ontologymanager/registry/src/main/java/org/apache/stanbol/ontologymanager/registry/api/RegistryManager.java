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

import org.apache.stanbol.ontologymanager.registry.api.model.CachingPolicy;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An object responsible for the retrieval, loading and unloading of ontology registries and libraries. Also
 * works as an indexer for registry items.
 * 
 * @author alexdma
 */
public interface RegistryManager extends RegistryItemIndex {

    /**
     * The key used to configure the caching policy of the registry manager.
     */
    public String CACHING_POLICY = "org.apache.stanbol.ontologymanager.registry.cachingPolicy";

    /**
     * The key used to configure the ontology loading policy of the registry manager.
     */
    public String LAZY_LOADING = "org.apache.stanbol.ontologymanager.registry.laziness";

    /**
     * The key used to configure the retention policy for incomplete registries.
     */
    public String RETAIN_INCOMPLETE = "org.apache.stanbol.ontologymanager.registry.retainIncomplete";

    /**
     * The key used to configure the locations of the registries to be scanned by the registry manager.
     */
    public String REGISTRY_LOCATIONS = "org.apache.stanbol.ontologymanager.registry.locations";

    /**
     * Adds a registry to the set of ontology registries managed by this object.
     * 
     * @param registry
     *            the ontology registry to be added.
     */
    void addRegistry(Registry registry);

    /**
     * Clears the set of ontology registries managed by this object.
     */
    void clearRegistries();

    /**
     * Constructs the models of all the registry items discovered by scanning the supplied ontologies that are
     * supposed to denote registries. <br/>
     * <br/>
     * To denote a registry, an ontology must instantiate the metamodel at <a
     * href="http://www.ontologydesignpatterns.org/schemas/meta.owl"
     * >http://www.ontologydesignpatterns.org/schemas/meta.owl</a> <br/>
     * <br/>
     * Depending on implementations, this method may or may not be automatically invoked after a call to
     * methods that determine changes to the model.
     * 
     * @param registryDescriptors
     *            the source OWL ontologies that describe the registries. If any of these denotes an invalid
     *            registry, a {@link RegistryContentException} will be thrown. If it does not denote a
     *            registry at all, it will be skipped.
     * @return the ontology registries that are the parent items of the entire model.
     */
    Set<Registry> createModel(Set<OWLOntology> registryDescriptors);

    /**
     * Returns the ontology caching policy currently adopted by the registry manager.
     * 
     * @return the caching policy.
     */
    CachingPolicy getCachingPolicy();

    /**
     * Returns the offline configuration currently employed by this registry manager.
     * 
     * @return the offline configuration in use.
     */
    OfflineConfiguration getOfflineConfiguration();

    /**
     * Determines if the registry manager is set to load its resources only when a specific request for them
     * (e.g. by a call to {@link Library#getOntologies()} is issued).
     * 
     * @return true if set to load resources only upon request, false if set to load all resources eagerly
     *         when the model has been built.
     */
    boolean isLazyLoading();

    /**
     * Removes a registry from the set of ontology registries managed by this object.
     * 
     * @param registry
     *            the ontology registry to be removed.
     */
    void removeRegistry(IRI registryId);

    /**
     * Sets the resource loading policy of this registry manager. There is no guarantee that setting a policy
     * after a model has already been created will affect the existing model (i.e. unload all its ontologies
     * if true, load them if false), but it will affect any subsequent calls to {@link #createModel(Set)}.
     * 
     * @param lazy
     *            if true, the registry manager will be set to load resources only upon request, otherwise it
     *            will be set to load all resources eagerly when the model has been built.
     */
    void setLazyLoading(boolean lazy);

}
