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
package org.apache.stanbol.ontologymanager.ontonet.api;

import java.io.File;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.session.SessionManagerImpl;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An Ontology Network Manager holds all references and tools for creating, modifying and deleting the logical
 * realms that store Web Ontologies, as well as offer facilities for handling the ontologies contained
 * therein.
 * 
 * @author alexdma, anuzzolese
 * 
 */
public interface ONManager {

    /**
     * The key used to configure the path of the ontology network configuration.
     */
    String CONFIG_ONTOLOGY_PATH = "org.apache.stanbol.ontologymanager.ontonet.onconfig";

    /**
     * The key used to configure the ID of the ontology network manager.
     */
    String ID = "org.apache.stanbol.ontologymanager.ontonet.id";

    /**
     * The key used to configure the simple identifier of the scope registry (which should also be
     * concatenated with the base namespace to obtain the registry's HTTP endpoint URI).
     */
    String ID_SCOPE_REGISTRY = "org.apache.stanbol.ontologymanager.ontonet.scopeRegistry.id";

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String ONTOLOGY_NETWORK_NS = "org.apache.stanbol.ontologymanager.ontonet.ns";

    /**
     * Returns the offline configuration set for this ontology network manager, if any.
     * 
     * @return the offline configuration, or null if none was set.
     */
    OfflineConfiguration getOfflineConfiguration();

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
     * @return the base namespace of the Stanbol ontology network.
     */
    String getOntologyNetworkNamespace();

    /**
     * Returns the ontology scope factory that was created along with the manager context.
     * 
     * @return the default ontology scope factory
     */
    OntologyScopeFactory getOntologyScopeFactory();

    /**
     * Returns the ontology space factory that was created along with the manager context.
     * 
     * @return the default ontology space factory.
     */
    OntologySpaceFactory getOntologySpaceFactory();

    /**
     * Returns an OWL Ontology Manager that is never cleared of its ontologies, so it can be used for caching
     * ontologies without having to reload them using other managers. It is sufficient to catch
     * {@link OWLOntologyAlreadyExistsException}s and obtain the ontology with that same ID from this manager.
     * 
     * @deprecated the ONManager will soon stop providing a cache manager, as it will gradually be replaced by
     *             {@link OntologyProvider}. Implementations that need to use an OWLOntologyManager which
     *             avoids reloading stored ontologies can either call {@link OntologyProvider#getStore()} on
     *             an {@link OWLOntologyManager}-based implementation, or create a new one by calling
     *             {@link OWLOntologyManagerFactory#createOWLOntologyManager(IRI[])} or OWL API methods.
     * @return the OWL Ontology Manager used for caching ontologies.
     */
    OWLOntologyManager getOwlCacheManager();

    /**
     * Returns the unique ontology scope registry for this context.
     * 
     * @return the ontology scope registry.
     */
    ScopeRegistry getScopeRegistry();

    /**
     * Returns the unique session manager for this context.
     * 
     * @deprecated {@link SessionManager} is now a standalone component and should be accessed independently
     *             from the ONManager (e.g. by instantiating a new {@link SessionManagerImpl} or by
     *             referencing {@link SessionManager} in OSGi components).
     * 
     * @return the session manager.
     */
    SessionManager getSessionManager();

    /**
     * Returns the list of IRIs that identify scopes that should be activated on startup, <i>if they
     * exist</i>.
     * 
     * @return the list of scope IDs to activate.
     */
    String[] getUrisToActivate();
}
