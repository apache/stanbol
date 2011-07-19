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
package org.apache.stanbol.ontologymanager.ontonet.api.registry;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public interface RegistryItemFactory {

    /**
     * Implementations should recurse into factory method {@link #createRegistryOntology(OWLNamedIndividual)}
     * for creating the ontology entries and setting them as children.
     * 
     * @param ind
     * @return
     */
    Library createLibrary(OWLNamedIndividual ind, Set<OWLOntology> ontologies);

    /**
     * Implementations should recurse into factory method {@link #createRegistry(OWLNamedIndividual)} for
     * creating the referenced libraries and setting them as children.
     * 
     * @param ind
     * @return
     */
    Registry createRegistry(OWLNamedIndividual ind);

    RegistryOntology createRegistryOntology(OWLNamedIndividual ind);

}
