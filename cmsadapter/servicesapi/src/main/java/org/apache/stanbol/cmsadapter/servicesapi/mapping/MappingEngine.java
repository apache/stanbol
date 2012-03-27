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
package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * {@link MappingEngine} is a wrapper that governs a mapping environment and able to start extraction process
 * on request.<br/>
 * Also provides two helper classer to processors that can reduce the work done by each processor. These
 * helper classes are:
 * <ol>
 * <li>
 * {@link DObjectAdapter}: Processor may want to access some properties that are not listed in plain CMS
 * models from package {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}. This adapter can wrap
 * plain CMS Objects into Decorated CMS Objects which can silently access a remote repository an fetch data
 * not provided by plain CMS Objects.</li>
 * <li>
 * {@link OntologyResourceHelper}: When processors are extract new triples, they need to create OWL classes,
 * individuals and properties. When these entity directly corresponds to a CMS node, type or property, need
 * for a generic mapper that maps OWL entities to CMS objects arise. Using {@link OntologyResourceHelper}
 * processors can create the OWL entities that corresponds any CMS object without having to keeping track of
 * the created classes</li>
 * </ol>
 * 
 * 
 * @author Suat
 * 
 */
public interface MappingEngine {
    /**
     * When extraction process includes {@link BridgeDefinitions} (i.e. registering a new bridge definition or
     * updating an existing one), this method is called. This function necessarily accesses a CMS repository.
     * 
     * @param conf
     *            Configuration that defines mapping/extraction environment.
     * @throws RepositoryAccessException
     * 
     * 
     */
    void mapCR(MappingConfiguration conf) throws RepositoryAccessException;

    /**
     * This method is called when a list of CMS objects are posted for lifting for the first time.
     * 
     * @param conf
     *            Configuration that defines mapping/extraction environment.
     */
    void createModel(MappingConfiguration conf);

    /**
     * This method is called when a list of previously submitted CMS objects are posted for updating.
     * 
     * @param conf
     *            Configuration that defines mapping/extraction environment.
     */
    void updateModel(MappingConfiguration conf);

    /**
     * This method is called when a list of previously submitted CMS objects are posted for removal. After
     * execution all processors are expected to delete previously generated triples by themselves from the
     * extracted ontology model.
     * 
     * @param conf
     *            Configuration that defines mapping/extraction environment.
     */
    void deleteModel(MappingConfiguration conf);

    /**
     * 
     * @return The URI of the ontology which will be generated in lifting process.
     */
    String getOntologyURI();

    /**
     * 
     * @return Ontology which is being generated in the lifting process.
     */
    OntModel getOntModel();

    /**
     * Getter for {@link DObjectAdapter} in this lifting context.
     * 
     */
    DObjectAdapter getDObjectAdapter();

    /**
     * Getter for {@link OntologyResourceHelper} in this lifting context.
     * 
     */
    OntologyResourceHelper getOntologyResourceHelper();

    /**
     * Getter for CMS Session in this lifting context.
     */
    Object getSession();

    /**
     * Getter for {@link BridgeDefinitions} (if any) in this lifting context.
     */
    BridgeDefinitions getBridgeDefinitions();

    /**
     * Getter for {@link RepositoryAccessManager} in this lifting context.
     */
    RepositoryAccessManager getRepositoryAccessManager();

    /**
     * Getter for {@link RepositoryAccess} in this lifting context.
     */
    RepositoryAccess getRepositoryAccess();

    /**
     * Getter for {@link NamingStrategy} in this lifting context.
     */
    NamingStrategy getNamingStrategy();
}
