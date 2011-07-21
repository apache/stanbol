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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public interface Library extends RegistryItem {

    final Type type = Type.LIBRARY;

    /**
     * Upon invocation, this method immediately fires a registry content request event on itself. Note,
     * however, that this method is in general not synchronized. Therefore, any listeners that react by
     * invoking a load method may or may not cause the content to be available to this method.
     * 
     * @return
     * @throws RegistryContentException
     */
    Set<OWLOntology> getOntologies() throws RegistryContentException;

    boolean isLoaded();

    void loadOntologies(OWLOntologyManager mgr);

}
