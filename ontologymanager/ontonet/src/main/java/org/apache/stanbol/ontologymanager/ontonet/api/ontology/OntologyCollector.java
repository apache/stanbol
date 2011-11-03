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
package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Collection;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.NamedResource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * It is not literally an ontology <i>collection</i>, in that it only collects references to ontologies, not
 * the ontologies themselves. Unless implementations specify a different behaviour, removing ontologies from
 * the collector does not delete them from their persistence system.
 * 
 * @author alexdma
 * 
 */
public interface OntologyCollector extends NamedResource, OntologyInputSourceHandler {

    /**
     * Registers a new listener for changes in this ontology space. Has no effect if the same listener is
     * already registered with this ontology space.
     * 
     * @param listener
     *            the ontology space listener to be added.
     */
    void addListener(OntologyCollectorListener listener);

    /**
     * Adds the given ontology to the ontology space.
     * 
     * @param ontology
     *            the ontology to be added
     * @throws UnmodifiableOntologyCollectorException
     *             if the ontology space is read-only
     */
    void addOntology(OntologyInputSource<?> ontologySource) throws UnmodifiableOntologyCollectorException;

    /**
     * Removes all ontology space listeners registered with this space.
     */
    void clearListeners();

    /**
     * Returns all the listeners registered with this ontology space. Whether the collection reflects or not
     * the order in which they were registered depends on the implementation.
     * 
     * @return the registered ontology space listeners.
     */
    Collection<OntologyCollectorListener> getListeners();

    /**
     * The namespace can change dynamically (e.g. if the ontology network is relocated), so it is not part of
     * the scope identifier (although its concatenation with the identifier will still be unique).
     * 
     * @param namespace
     */
    IRI getNamespace();

    /**
     * Returns the ontologies managed by this ontology space.
     * 
     * @param withClosure
     *            if true, also the ontologies imported by those directly managed by this space will be
     *            included.
     * @return the set of ontologies in the ontology space
     */
    Set<OWLOntology> getOntologies(boolean withClosure);

    /**
     * Returns the ontology identified by the supplied <i>logical</i> IRI, if such an ontology has been loaded
     * in this space.<br>
     * <br>
     * Note that ontologies are not identified by physical IRI here. There's no need to ask KReS for
     * ontologies by physical IRI, use a browser or some other program instead!
     * 
     * @param ontologyIri
     *            the <i>logical</i> identifier of the ontology to query for.
     * 
     * @return the requested ontology, or null if no ontology with this ID has been loaded.
     */
    OWLOntology getOntology(IRI ontologyIri);

    /**
     * Determines if the ontology identified by the supplied <i>logical</i> IRI has been loaded in this space.<br>
     * <br>
     * Note that ontologies are not identified by physical IRI here. There's no need to ask KReS for
     * ontologies by physical IRI, use a browser or some other program instead!
     * 
     * @param ontologyIri
     *            the <i>logical</i> identifier of the ontology to query for.
     * 
     * @return true if an ontology with this ID has been loaded in this space.
     */
    boolean hasOntology(IRI ontologyIri);

    /**
     * Unregisters the supplied for changes in this ontology space. Has no effect if the same listener was not
     * registered with this ontology space.
     * 
     * @param listener
     *            the ontology space listener to be removed.
     */
    void removeListener(OntologyCollectorListener listener);

    /**
     * Removes the given ontology from the ontology space, if the ontology is a direct child of the top
     * ontology.<br/>
     * <u>Note</u> that this will NOT delete the ontology from the store! This method simply states that the
     * ontology is no longer managed by this space and its axioms will no longer appear when the space is
     * serialized as an ontology. To delete the ontology itself, please use the Ontology Manager Store.
     * 
     * @param ontologyIri
     *            the identifier of this ontology.
     */
    void removeOntology(IRI ontologyId) throws OntologyCollectorModificationException;

    /**
     * The namespace can be changed dynamically (e.g. if the ontology network is relocated).
     * 
     * @param namespace
     *            Must end with a slash character. If the IRI ends with a has, and
     *            {@link IllegalArgumentException} will be thrown. If it ends with neither, a slash will be
     *            added.
     */
    void setNamespace(IRI namespace);

    /**
     * Bootstraps the ontology space. In some cases (such as with core and custom spaces) this also implies
     * write-locking its ontologies.
     */
    void setUp();

    /**
     * Performs all required operations for disposing of an ontology space and releasing its resources (e.g.
     * removing the writelock).
     */
    void tearDown();
}
