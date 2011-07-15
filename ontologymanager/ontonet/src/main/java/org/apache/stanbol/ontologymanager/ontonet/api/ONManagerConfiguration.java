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
import java.util.List;

/**
 * Provides the configuration needed for the {@link ONManager}. A configuration should only be handled
 * internally by the {@link ONManager} implementation.
 * 
 * @author alessandro
 * 
 */
public interface ONManagerConfiguration {

    /**
     * The key used to configure the path of the ontology network configuration.
     */
    String CONFIG_ONTOLOGY_PATH = "org.apache.stanbol.ontologymanager.ontonet.onconfig";

    /**
     * The key used to configure the ID of the ontology network manager.
     */
    String ID = "org.apache.stanbol.ontologymanager.ontonet.id";

    /**
     * The key used to configure the base namespace of the ontology network.
     */
    String ONTOLOGY_NETWORK_NS = "org.apache.stanbol.ontologymanager.ontonet.ns";

    /**
     * The key used to configure the paths of local ontologies.
     */
    String ONTOLOGY_PATHS = "org.apache.stanbol.ontologymanager.ontonet.ontologypaths";

    /**
     * Returns the ID of the ontology network manager.
     * 
     * @return the ID of the ontology network manager.
     */
    String getID();

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
     * Returns the paths of all the directories where the ontology network manager will try to locate
     * ontologies. These directories will be prioritaire if the engine is set to run in offline mode. This
     * list is ordered in that the higher-ordered directories generally override lower-ordered ones, that is,
     * any ontologies found in the directories belonging to the tail of this list will supersede any
     * ontologies with the same ID found in the directories belonging to its head.
     * 
     * @return an ordered list of directory paths for offline ontologies.
     */
    List<String> getOntologySourceDirectories();

}
