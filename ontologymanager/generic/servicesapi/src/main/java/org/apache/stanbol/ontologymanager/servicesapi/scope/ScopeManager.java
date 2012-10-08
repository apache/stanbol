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
package org.apache.stanbol.ontologymanager.servicesapi.scope;

import java.io.File;

import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;

/**
 * A Scope Manager holds all references and tools for creating, modifying and deleting the logical realms that
 * store Web Ontologies, as well as offer facilities for handling the ontologies contained therein.<br>
 * <br>
 * Note that since this object is both a {@link ScopeRegistry} and an {@link ScopeFactory}, the call to
 * {@link ScopeRegistry#registerScope(OntologyScope)} or its overloads after
 * {@link ScopeFactory#createOntologyScope(String, OntologyInputSource...)} is unnecessary, as the ONManager
 * automatically registers newly created scopes.
 * 
 * @author alexdma, anuzzolese
 * 
 */
public interface ScopeManager extends ScopeFactory, ScopeRegistry {

    /**
     * The key used to configure the path of the ontology network configuration.
     */
    String CONFIG_ONTOLOGY_PATH = "org.apache.stanbol.ontologymanager.ontonet.onconfig";

    /**
     * The key used to configure the connectivity policy.
     */
    String CONNECTIVITY_POLICY = "org.apache.stanbol.ontologymanager.ontonet.connectivity";

    /**
     * The key used to configure the simple identifier of the scope registry (which should also be
     * concatenated with the base namespace to obtain the registry's HTTP endpoint URI).
     */
    String ID_SCOPE_REGISTRY = "org.apache.stanbol.ontologymanager.ontonet.scopeRegistry.id";

    /**
     * Returns the offline configuration set for this ontology network manager, if any.
     * 
     * @return the offline configuration, or null if none was set.
     */
    OfflineConfiguration getOfflineConfiguration();

    OntologySpaceFactory getOntologySpaceFactory();

    /**
     * Implementations should be able to create a {@link File} object from this path.
     * 
     * @return the local path of the ontology storing the ontology network configuration.
     */
    String getOntologyNetworkConfigurationPath();

    /**
     * Returns the base namespace to be used for the Stanbol ontology network (e.g. for the creation of new
     * scopes). For convenience, it is returned as a string so that it can be concatenated to form IRIs.
     * 
     * @deprecated please use {@link OfflineConfiguration#getDefaultOntologyNetworkNamespace()} to obtain the
     *             namespace
     * 
     * @return the base namespace of the Stanbol ontology network.
     */
    String getOntologyNetworkNamespace();

    /**
     * Returns the unique ontology scope registry for this context.
     * 
     * @deprecated This methods now returns the current object, which is also a {@link ScopeRegistry}.
     * @return the ontology scope registry.
     */
    ScopeRegistry getScopeRegistry();

    /**
     * Sets the IRI that will be the base namespace for all ontology scopes and collectors created by this
     * object.
     * 
     * @deprecated {@link ScopeManager} should set its namespace to be the same as
     *             {@link OfflineConfiguration#getDefaultOntologyNetworkNamespace()} whenever it changes on
     *             the object obtained by calling {@link #getOfflineConfiguration()}.
     * 
     * @param namespace
     *            the base namespace.
     */
    void setOntologyNetworkNamespace(String namespace);
}
