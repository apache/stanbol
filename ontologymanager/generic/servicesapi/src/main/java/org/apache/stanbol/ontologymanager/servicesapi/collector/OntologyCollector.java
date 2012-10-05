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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.NamedArtifact;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSourceHandler;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * It is not literally an ontology <i>collection</i>, in that it only collects references to ontologies, not
 * the ontologies themselves. Unless implementations specify a different behaviour, removing ontologies from
 * the collector does not delete them from their persistence system.
 * 
 * @author alexdma
 * 
 */
public interface OntologyCollector extends OntologyCollectorListenable, NamedArtifact,
        OntologyInputSourceHandler {

    /**
     * Adds the given ontology to the ontology collector. If the supplied ontology is not already present in
     * storage and does not have an OWL version IRI of its own, this ontology collector will 'claim ownership'
     * of the ontology by setting its own logical ID as the version IRI of the new ontology.
     * 
     * @param ontology
     *            the ontology to be added
     * @return the key that can be used for accessing the stored ontology directly
     */
    OWLOntologyID addOntology(OntologyInputSource<?> ontology);

    /**
     * Returns the ontologies managed by this ontology space. This is a shortcut method for iterating
     * {@link #getOntology(OWLOntologyID, Class)} calls over {@link #listManagedOntologies()}.
     * 
     * @deprecated Please simulate this method manually by iterating
     *             {@link #getOntology(OWLOntologyID, Class)} calls over {@link #listManagedOntologies()}
     * 
     * @param withClosure
     *            if true, also the ontologies imported by those directly managed by this space will be
     *            included.
     * @return the set of ontologies in the ontology space
     */
    <O> Set<O> getManagedOntologies(Class<O> returnType, boolean withClosure);

    /**
     * @deprecated Please use {@link #getOntology(OWLOntologyID, Class)} instead.
     * @param ontologyIri
     * @param returnType
     * @return
     */
    <O> O getOntology(IRI ontologyIri, Class<O> returnType);

    /**
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @deprecated Please use {@link #getOntology(OWLOntologyID, Class, boolean)} instead.
     * @param ontologyIri
     * @param returnType
     * @param merge
     * @return
     */
    <O> O getOntology(IRI ontologyIri, Class<O> returnType, boolean merge);

    /**
     * @deprecated Please use {@link #getOntology(OWLOntologyID, Class, boolean, IRI)} instead.
     * @param ontologyIri
     * @param returnType
     * @param merge
     * @param universalPrefix
     * @return
     */
    <O> O getOntology(IRI ontologyIri, Class<O> returnType, boolean merge, IRI universalPrefix);

    /**
     * @deprecated Please use {@link #getOntology(OWLOntologyID, Class, IRI)} instead.
     * @param ontologyIri
     * @param returnType
     * @param universalPrefix
     * @return
     */
    <O> O getOntology(IRI ontologyIri, Class<O> returnType, IRI universalPrefix);

    /**
     * Returns the managed ontology with the specified public key as a Java object of the specified type, if
     * supported. If such an ontology exists but is not managed by this collector, it will not be returned.<br>
     * <br>
     * How the method should react if <code>publicKey</code> is invalid or no such ontology exists, or whether
     * the desired return type is not supported, is implementation-dependent. However, developers should at
     * least expect that the methods either returns <code>null</code> or throws a
     * {@link MissingOntologyException} if no such ontology is managed by this collector. Likewise, if
     * <code>returnType</code> is not supported, an {@link UnsupportedOperationException} could be expected to
     * be thrown.
     * 
     * @param publicKey
     *            the public identifier of the requested ontology.
     * @param returnType
     *            the desired type of the ontology object.
     * @return the ontology object.
     */
    <O> O getOntology(OWLOntologyID publicKey, Class<O> returnType);

    /**
     * Returns the managed ontology with the specified public key as a Java object of the specified type, if
     * supported. If such an ontology exists but is not managed by this collector, it will not be returned.<br>
     * <br>
     * How the method should react if <code>publicKey</code> is invalid or no such ontology exists, or whether
     * the desired return type is not supported, is implementation-dependent. However, developers should at
     * least expect that the methods either returns <code>null</code> or throws a
     * {@link MissingOntologyException} if no such ontology is managed by this collector. Likewise, if
     * <code>returnType</code> is not supported, an {@link UnsupportedOperationException} could be expected to
     * be thrown.<br>
     * <br>
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @param publicKey
     *            the public identifier of the requested ontology.
     * @param returnType
     *            the desired type of the ontology object.
     * @param merge
     *            if true, the requested ontology will be merged with its imports up to whatever supported
     *            level.
     * @return the ontology object.
     */
    <O> O getOntology(OWLOntologyID publicKey, Class<O> returnType, boolean merge);

    /**
     * Returns the managed ontology with the specified public key as a Java object of the specified type, if
     * supported. If such an ontology exists but is not managed by this collector, it will not be returned.<br>
     * <br>
     * How the method should react if <code>publicKey</code> is invalid or no such ontology exists, or whether
     * the desired return type is not supported, is implementation-dependent. However, developers should at
     * least expect that the methods either returns <code>null</code> or throws a
     * {@link MissingOntologyException} if no such ontology is managed by this collector. Likewise, if
     * <code>returnType</code> is not supported, an {@link UnsupportedOperationException} could be expected to
     * be thrown.<br>
     * <br>
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @param publicKey
     *            the public identifier of the requested ontology.
     * @param returnType
     *            the desired type of the ontology object.
     * @param merge
     *            if true, the requested ontology will be merged with its imports up to whatever supported
     *            level.
     * @param universalPrefix
     *            the IRI to be used as a prefix and/or namespace for all derived IRIs, e.g. OWL import
     *            statements and version IRIs.
     * @return the ontology object.
     */
    <O> O getOntology(OWLOntologyID publicKey, Class<O> returnType, boolean merge, IRI universalPrefix);

    /**
     * Returns the managed ontology with the specified public key as a Java object of the specified type, if
     * supported. If such an ontology exists but is not managed by this collector, it will not be returned.<br>
     * <br>
     * How the method should react if <code>publicKey</code> is invalid or no such ontology exists, or whether
     * the desired return type is not supported, is implementation-dependent. However, developers should at
     * least expect that the methods either returns <code>null</code> or throws a
     * {@link MissingOntologyException} if no such ontology is managed by this collector. Likewise, if
     * <code>returnType</code> is not supported, an {@link UnsupportedOperationException} could be expected to
     * be thrown.<br>
     * <br>
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @param publicKey
     *            the public identifier of the requested ontology.
     * @param returnType
     *            the desired type of the ontology object.
     * @param universalPrefix
     *            the IRI to be used as a prefix and/or namespace for all derived IRIs, e.g. OWL import
     *            statements and version IRIs.
     * @return the ontology object.
     */
    <O> O getOntology(OWLOntologyID publicKey, Class<O> returnType, IRI universalPrefix);

    /**
     * Determines if the ontology identified by the supplied public key is being managed by this collector.<br>
     * <br>
     * Note that the public key will match the ontology's logical ID only if it has one. Otherwise it can be
     * an {@link OWLOntologyID} that wraps either the physical URL or the identifier chosen by Stanbol as its
     * ontologyIRI.
     * 
     * @deprecated the usage of {@link IRI} to identify ontologies is reductive. Please create a new
     *             {@link OWLOntologyID#OWLOntologyID(IRI)} from this IRI and use
     *             {@link #hasOntology(OWLOntologyID)} with this new public key as a parameter.
     * 
     * @param publicKey
     *            the <i>logical</i> identifier of the ontology to query for.
     * 
     * @return true iff an ontology with this public key has been loaded in this collector.
     */
    boolean hasOntology(IRI ontologyId);

    /**
     * Determines if the ontology identified by the supplied public key is being managed by this collector.<br>
     * <br>
     * Note that the public key will match the ontology's logical ID only if it has one. Otherwise it can be
     * an {@link OWLOntologyID} that wraps either the physical URL or the identifier chosen by Stanbol as its
     * ontologyIRI.
     * 
     * @param publicKey
     *            the <i>logical</i> identifier of the ontology to query for.
     * 
     * @return true iff an ontology with this public key has been loaded in this collector.
     */
    boolean hasOntology(OWLOntologyID publicKey);

    /**
     * Gets the public key set of all the ontologies managed by this ontology collector.
     * 
     * @return the key set of managed ontologies.
     */
    Set<OWLOntologyID> listManagedOntologies();

    /**
     * Removes the given ontology from the ontology collector, if it was being managed. Otherwise, it should
     * throw a {@link MissingOntologyException}.<br/>
     * <u>Note</u> that this will NOT necessarily delete the ontology from the store! This method simply
     * states that the ontology is no longer managed by this collectors and its axioms will no longer appear
     * when the collector is serialized as an ontology. To make sure the ontology itself is deleted, please
     * use the {@link OntologyProvider}.
     * 
     * @deprecated the usage of {@link IRI} to identify ontologies is reductive. Please create a new
     *             {@link OWLOntologyID#OWLOntologyID(IRI)} from this IRI and use
     *             {@link #removeOntology(OWLOntologyID)} with this new public key as a parameter.
     * 
     * @param ontologyIri
     *            the identifier of this ontology.
     */
    void removeOntology(IRI ontologyId);

    /**
     * Removes the given ontology from the ontology collector, if it was being managed. Otherwise, it should
     * throw a {@link MissingOntologyException}.<br/>
     * <u>Note</u> that this will NOT necessarily delete the ontology from the store! This method simply
     * states that the ontology is no longer managed by this collectors and its axioms will no longer appear
     * when the collector is serialized as an ontology. To make sure the ontology itself is deleted, please
     * use the {@link OntologyProvider}.
     * 
     * @param ontologyIri
     *            the identifier of this ontology.
     */
    void removeOntology(OWLOntologyID ontologyId);

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
