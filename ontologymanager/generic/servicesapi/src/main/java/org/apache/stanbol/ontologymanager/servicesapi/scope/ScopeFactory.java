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

import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;

public interface ScopeFactory extends ScopeEventListenable {

    /**
     * Creates and returns a new ontology scope with the core space ontologies obtained from
     * <code>coreSource</code> and the custom space not set.
     * 
     * @param scopeID
     *            the desired unique identifier for the ontology scope.
     * @param coreSource
     *            the input source that provides the top ontology for the core space.
     * @return the newly created ontology scope.
     * @throws DuplicateIDException
     *             if an ontology scope with the given identifier is already <i>registered</i>. The exception
     *             is not thrown if another scope with the same ID has been created but not registered.
     */
    Scope createOntologyScope(String scopeID, OntologyInputSource<?>... coreOntologies) throws DuplicateIDException;

}
