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
package org.apache.stanbol.ontologymanager.servicesapi;

import java.util.List;

import org.semanticweb.owlapi.model.IRI;

/**
 * Provides the configuration needed by the ontology manager in order to locate offline ontologies and export
 * them to the Web.
 * 
 * @author alexdma
 * 
 */
public interface OfflineConfiguration {

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String DEFAULT_NS = "org.apache.stanbol.ontologymanager.ontonet.ns";

    /**
     * The key used to configure the paths of local ontologies.
     */
    String ONTOLOGY_PATHS = "org.apache.stanbol.ontologymanager.ontonet.ontologypaths";

    /**
     * Returns the namespace that will be used as the prefix for all named artifacts when exported to RDF/OWL,
     * unless overridden manually (e.g. by calling the RESTful API on another base IRI).
     * 
     * @return the default ontology network namespace
     */
    IRI getDefaultOntologyNetworkNamespace();

    /**
     * Returns the paths of all the directories where the ontology network manager will try to locate
     * ontologies. These directories will be prioritaire if the engine is set to run in offline mode. This
     * list is ordered in that the higher-ordered directories generally override lower-ordered ones, that is,
     * any ontologies found in the directories belonging to the tail of this list will supersede any
     * ontologies with the same ID found in the directories belonging to its head.
     * 
     * @return an ordered list of directory paths for offline ontologies.
     */
    List<IRI> getOntologySourceLocations();

}
