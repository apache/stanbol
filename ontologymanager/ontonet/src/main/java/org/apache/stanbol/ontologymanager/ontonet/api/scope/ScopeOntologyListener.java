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
package org.apache.stanbol.ontologymanager.ontonet.api.scope;

import org.semanticweb.owlapi.model.IRI;

/**
 * Implementations of this interface are able to react to modifications on the ontology network
 * infrastructure.
 * 
 * @author alexdma
 * 
 */
public interface ScopeOntologyListener {

    /**
     * Called whenever an ontology is set to be managed by a scope, space or session.
     * 
     * @param scopeId
     * @param addedOntology
     */
    void onOntologyAdded(String scopeId, IRI addedOntology);

    /**
     * Called whenever an ontology is set to no longer be managed by a scope, space or session. This method is
     * not called if that ontology was not being managed earlier.
     * 
     * @param scopeId
     * @param addedOntology
     */
    void onOntologyRemoved(String scopeId, IRI removedOntology);

}
