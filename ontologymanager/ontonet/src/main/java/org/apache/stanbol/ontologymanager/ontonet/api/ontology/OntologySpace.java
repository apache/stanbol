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

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An ontology space identifies the set of OWL ontologies that should be "active" in a given context, e.g. for
 * a certain user session or a specific reasoning service. Each ontology space has an ID and a top ontology
 * that can be used as a shared resource for mutual exclusion and locking strategies.
 */
public interface OntologySpace {

    /**
     * Adds the given ontology to the ontology space.
     * 
     * @param ontology
     *            the ontology to be added
     * @throws UnmodifiableOntologySpaceException
     *             if the ontology space is read-only
     */
    void addOntology(OntologyInputSource ontologySource) throws UnmodifiableOntologySpaceException;

    void addOntologySpaceListener(OntologySpaceListener listener);

    /**
     * Returns the ontology that serves as a root module for this ontology space.
     * 
     * @return the OWL form of this ontology space.
     */
    OWLOntology asOWLOntology();

    void clearOntologySpaceListeners();

    boolean containsOntology(IRI ontologyIri);

    /**
     * Returns a Unique Resource Identifier (URI) that identifies this ontology space. For instance, this URI
     * could be the parent of (some/most of) the base URIs for the ontologies within this space.<br/>
     * <br/>
     * A possible way to construct these IDs is by concatenating <code>/{spacetype}</code> (e.g.
     * <code>/custom</code>) to the scope IRI. However, this is implementation-dependent.
     * 
     * @return the URI that identifies this ontology space
     */
    IRI getID();

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

    Collection<OntologySpaceListener> getOntologyScopeListeners();

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
     * Determines if it is no longer possible to modify this space until it is torn down.
     * 
     * @return true if this space is write-locked, false otherwise.
     */
    boolean isLocked();

    boolean isSilentMissingOntologyHandling();

    /**
     * Removes the given ontology from the ontology space, if the ontology is a direct child of the top
     * ontology. This means that the ontology must neither be the top ontology for this space, nor a subtree
     * of an imported ontology. This is a conservative measure to avoid using undefined entities in the space.
     * 
     * @param ontology
     *            the ontology to be removed
     * @throws UnmodifiableOntologySpaceException
     *             if the ontology space is read-only
     */
    void removeOntology(OntologyInputSource src) throws OntologySpaceModificationException;

    void removeOntologySpaceListener(OntologySpaceListener listener);

    void setSilentMissingOntologyHandling(boolean silent);

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
