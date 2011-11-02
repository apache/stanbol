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
package org.apache.stanbol.ontologymanager.registry.api.model;

import java.util.Map;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.RegistryOntologyNotLoadedException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * A special registry item that denotes an ontology referenced by a library.<br/>
 * <br/>
 * Note that this is <b>not equivalent</b> to an {@link OWLOntology}, since a {@link RegistryOntology} can
 * exist regardless of the corresponding OWL ontology being loaded. For this reason, a registry ontology
 * responds to {@link #getIRI()} with is stated <i>physical location</i>, even if it were found to differ from
 * the ontology ID once the corresponding OWL ontology is loaded.<br/>
 * <br/>
 * Once the corresponding ontology has been loaded (e.g. by a call to {@link Library#loadOntologies(OntologyProvider)}),
 * the corresponding {@link OWLOntology} object is available via calls to {@link #getRawOntology(IRI)}.
 * 
 * @author alexdma
 */
public interface RegistryOntology extends RegistryItem {

    /**
     * The type of this registry item is {@link Type#ONTOLOGY}.
     */
    final Type type = Type.ONTOLOGY;

    Map<IRI,String> getReferenceMap() throws RegistryOntologyNotLoadedException;

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. If the ontology was not
     * loaded, a {@link RegistryOntologyNotLoadedException} will be thrown.<br/>
     * <br/>
     * Upon invocation, this method immediately fires a registry content request event on itself. Note,
     * however, that this method is in general not synchronized. Therefore, any listeners that react by
     * invoking a load method may or may not cause the content to be available to this method before it
     * returns.
     * 
     * @param libraryID
     *            TODO
     * 
     * @return the OWL ontology corresponding to this registry ontology.
     * @throws RegistryOntologyNotLoadedException
     *             if the ontology is not loaded.
     */
    String getReference(IRI libraryID) throws RegistryOntologyNotLoadedException;

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. If the ontology was not
     * loaded, a {@link RegistryOntologyNotLoadedException} will be thrown.<br/>
     * <br/>
     * Upon invocation, this method immediately fires a registry content request event on itself. Note,
     * however, that this method is in general not synchronized. Therefore, any listeners that react by
     * invoking a load method may or may not cause the content to be available to this method before it
     * returns.
     * 
     * @deprecated this method will return null if the cache that stores the ontology content is not
     *             implemented as an OWLOntologyManager. Applications should either request the reference via
     *             a call to {@link #getReference(IRI)} and resolve it, or request the
     *             {@link OWLOntologyCreationException} from the library referencing this.
     * @param libraryID
     *            TODO
     * 
     * @return the OWL ontology corresponding to this registry ontology.
     * @throws RegistryOntologyNotLoadedException
     *             if the ontology is not loaded.
     */
    OWLOntology getRawOntology(IRI libraryID) throws RegistryOntologyNotLoadedException;

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. Note that the method
     * does not check whether the registry item ID matches the ontology ID or its physical location.
     * 
     * @param libraryID
     *            TODO
     * @param owl
     *            the OWL ontology corresponding to this registry ontology.
     */
    void setReference(IRI libraryID, String reference);

    /**
     * Returns the {@link OWLOntology} object corresponding to this registry ontology. Note that the method
     * does not check whether the registry item ID matches the ontology ID or its physical location.
     * 
     * @deprecated if the cache is not implemented in OWLAPI, this will not be set.
     * @param libraryID
     *            TODO
     * @param owl
     *            the OWL ontology corresponding to this registry ontology.
     */
    void setRawOntology(IRI libraryID, OWLOntology owl);

}
