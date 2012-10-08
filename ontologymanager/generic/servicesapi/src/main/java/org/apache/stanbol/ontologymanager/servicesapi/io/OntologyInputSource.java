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
package org.apache.stanbol.ontologymanager.servicesapi.io;

/**
 * An ontology input source provides a point for loading an ontology. Currently it provides two ways of
 * obtaining an ontology document:
 * 
 * <ol>
 * <li>From an OWLOntology.
 * <li>By dereferencing a physical IRI.
 * <li>By querying a triple store.
 * </ol>
 * 
 * Consumers that use an ontology input source will attempt to obtain a concrete representation of an ontology
 * in the above order. Implementations of this interface may try to dereference the IRI internally and just
 * provide the OWLOntology, or directly provide the physical IRI for other classes to dereference.
 * Implementations should allow multiple attempts at loading an ontology.
 * 
 * @author alexdma
 * 
 * @param <O>
 *            the root ontology object delivered by this input source.
 */
public interface OntologyInputSource<O> {

    /**
     * Returns a reference object that can be used for obtaining the supplied ontology. Depending on how the
     * ontology was obtained, the origin can be a physical URL, the ID of a database or graph in the local
     * storage, or something else.This method is supposed to return null if the ontology lives in-memory and
     * was not or is not going to be stored publicly.
     * 
     * @return a physical reference for this ontology source, or null if unknown.
     */
    Origin<?> getOrigin();

    /**
     * Returns the OWL Ontology that imports the whole ontology network addressed by this input source.
     * 
     * @return the ontology network root.
     */
    O getRootOntology();

    /**
     * Determines if a physical reference is known for this ontology source. Note that an anonymous ontology
     * may have been fetched from a physical location, just as a named ontology may have been stored in memory
     * and have no physical location.
     * 
     * @return true if a physical reference is known for this ontology source.
     */
    boolean hasOrigin();

    /**
     * Determines if a root ontology that imports the entire network is available.
     * 
     * @return true if a root ontology is available, false otherwise.
     */
    boolean hasRootOntology();

}
