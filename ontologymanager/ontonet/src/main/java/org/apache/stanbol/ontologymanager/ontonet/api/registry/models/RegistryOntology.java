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

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryOntologyNotLoadedException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem.Type;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A special registry item that denotes an ontology referenced by a library.<br/>
 * <br/>
 * Note that this is <b>not equivalent</b> to an {@link OWLOntology}, since a {@link RegistryOntology} can
 * exist regardless of the corresponding OWL ontology being loaded. For this reason, a registry ontology
 * responds to {@link #getIRI()} with is stated <i>physical location</i>, even if it were found to differ from
 * the ontology ID once the corresponding OWL ontology is loaded.<br/>
 * <br/>
 * Once the corresponding ontology has been loaded (e.g. by a call to
 * {@link Library#loadOntologies(OWLOntologyManager)}), the corresponding {@link OWLOntology} object is
 * available via calls to {@link #asOWLOntology()}.
 */
public interface RegistryOntology extends RegistryItem {

    /**
     * The type of this registry item is {@link Type#ONTOLOGY}.
     */
    final Type type = Type.ONTOLOGY;

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. If the ontology was not
     * loaded, a {@link RegistryOntologyNotLoadedException} will be thrown.<br/>
     * <br/>
     * Upon invocation, this method immediately fires a registry content request event on itself. Note,
     * however, that this method is in general not synchronized. Therefore, any listeners that react by
     * invoking a load method may or may not cause the content to be available to this method before it
     * returns.
     * 
     * @return the OWL ontology corresponding to this registry ontology.
     * @throws RegistryOntologyNotLoadedException
     *             if the ontology is not loaded.
     */
    OWLOntology asOWLOntology() throws RegistryOntologyNotLoadedException;

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. Note that the method
     * does not check whether the registry item ID matches the ontology ID or its physical location.
     * 
     * @param owl
     *            the OWL ontology corresponding to this registry ontology.
     */
    void setOWLOntology(OWLOntology owl);

}
