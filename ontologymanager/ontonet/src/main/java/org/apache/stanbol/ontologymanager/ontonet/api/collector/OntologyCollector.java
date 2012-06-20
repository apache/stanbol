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
package org.apache.stanbol.ontologymanager.ontonet.api.collector;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.NamedResource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSourceHandler;
import org.semanticweb.owlapi.model.IRI;

/**
 * It is not literally an ontology <i>collection</i>, in that it only collects references to ontologies, not
 * the ontologies themselves. Unless implementations specify a different behaviour, removing ontologies from
 * the collector does not delete them from their persistence system.
 * 
 * @author alexdma
 * 
 */
public interface OntologyCollector extends OntologyCollectorListenable, NamedResource,
        OntologyInputSourceHandler {

    /**
     * Adds the given ontology to the ontology space. If the supplied ontology is not already present in
     * storage and does not have an OWL version IRI of its own, this ontology collector will 'claim ownership'
     * of the ontology by setting its own logical ID as the version IRI of the new ontology.
     * 
     * @param ontology
     *            the ontology to be added
     * @return the key that can be used for accessing the stored ontology directly
     */
    String addOntology(OntologyInputSource<?,?> ontologySource);

    /**
     * Returns the ontologies managed by this ontology space. This is a shortcut method for iterating
     * {@link #getOntology(IRI, Class)} calls over {@link #listManagedOntologies()}.
     * 
     * @param withClosure
     *            if true, also the ontologies imported by those directly managed by this space will be
     *            included.
     * @return the set of ontologies in the ontology space
     */
    <O> Set<O> getManagedOntologies(Class<O> returnType, boolean withClosure);

    /**
     * TODO replace with Ontology IDs
     * 
     * @return
     */
    Set<IRI> listManagedOntologies();

    <O> O getOntology(IRI ontologyIri, Class<O> returnType);

    /**
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @param ontologyIri
     * @param returnType
     * @param merge
     * @return
     */
    <O> O getOntology(IRI ontologyIri, Class<O> returnType, boolean merge);

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
     * Removes the given ontology from the ontology space, if the ontology is a direct child of the top
     * ontology.<br/>
     * <u>Note</u> that this will NOT delete the ontology from the store! This method simply states that the
     * ontology is no longer managed by this space and its axioms will no longer appear when the space is
     * serialized as an ontology. To delete the ontology itself, please use the Ontology Manager Store.
     * 
     * @param ontologyIri
     *            the identifier of this ontology.
     */
    void removeOntology(IRI ontologyId);

    /**
     * Bootstraps the ontology space. In some cases (such as with core and custom spaces) this also implies
     * write-locking its ontologies.
     * 
     * XXX make it a protected, non-interface method ?
     */
    void setUp();

    /**
     * Performs all required operations for disposing of an ontology space and releasing its resources (e.g.
     * removing the writelock).
     * 
     * XXX make it a protected, non-interface method ?
     */
    void tearDown();
}
