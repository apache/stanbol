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
package org.apache.stanbol.cmsadapter.servicesapi.processor;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * This interface provides a uniform way to semantically lift node/object type definitions in content
 * management systems.
 * 
 * @author suat
 * 
 */
public interface TypeLifter {
    /**
     * This method takes all node/object type definitions in the repository and creates related ontological
     * resources namely classes, object and data properties. It creates a class for each object/node type and
     * object properties for property definitions having types PATH, REFERENCE, etc.; and data properties for
     * property definitions takes literal values.
     * 
     * @param mappingEngine
     *            is the {@link MappingEngine} instance of which acts as context for the implementations of
     *            this interface. It provides context variables such as Session to access repository or
     *            {@link OntologyResourceHelper} to create ontology resources.
     * @throws RepositoryAccessException
     */

    void liftNodeTypes(MappingEngine mappingEngine) throws RepositoryAccessException;

    /**
     * Takes a protocol type e.g JCR/CMIS and returns whether implementation of this interface is capable of
     * lifting type definitions through the specified protocol
     * 
     * @param type
     *            protocol type e.g JCR/CMIS
     * @return
     */
    boolean canLift(String type);
}
