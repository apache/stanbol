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

import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.semanticweb.owlapi.model.IRI;

/**
 * An ontology library references one or more ontologies.
 * 
 * @author alexdma
 */
public interface Library extends RegistryItem {

    /**
     * The type of this registry item is {@link Type#LIBRARY}.
     */
    final Type type = Type.LIBRARY;

    /**
     * Returns the OWL ontology manager that this library is using as a cache of its ontologies.
     * 
     * @return the ontology manager that is used as a cache.
     */
    OntologyProvider<?> getCache();

    /**
     * Returns the OWL ontologies that have been loaded in this library, if any, otherwise an exception is
     * thrown.<br/>
     * <br/>
     * Upon invocation, this method immediately fires a registry content request event on itself. Note,
     * however, that this method is in general not synchronized. Therefore, any listeners that react by
     * invoking a load method may or may not cause the content to be available to this method before it
     * returns.
     * 
     * @return the set of loaded OWL ontologies.
     * @throws RegistryContentException
     *             if the requested ontologies have not been loaded.
     */
    <O> Set<O> getOntologies(Class<O> returnType) throws RegistryContentException;

    <O> O getOntology(IRI id, Class<O> returnType) throws RegistryContentException;

    /**
     * Determines if the contents of this library have been loaded and are up-to-date.
     * 
     * @return true if the contents are loaded and up-to-date, false otherwise.
     */
    boolean isLoaded();

    /**
     * Causes all the ontologies referenced by this library to be loaded, so that when
     * {@link RegistryOntology#getRawOntology(IRI)} is invoked on one of its children, it will return the
     * corresponding OWL ontology, if a valid one was parsed from its location.
     * 
     * @param mgr
     *            the OWL ontology manager to use for loading the ontologies in the library. It must not be
     *            null, lest an {@link IllegalArgumentException} be thrown.
     */
    void loadOntologies(OntologyProvider<?> cache);

    /**
     * Sets the OWL ontology manager that this library will use as a cache of its ontologies. If null, if will
     * create its own.
     * 
     * @param cache
     *            the ontology manager to be used as a cache.
     */
    void setCache(OntologyProvider<?> cache);

}
