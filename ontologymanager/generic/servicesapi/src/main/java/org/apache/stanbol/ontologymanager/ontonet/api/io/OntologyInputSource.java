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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

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
 * @deprecated Packages, class names etc. containing "ontonet" in any capitalization are being phased out.
 *             Please switch to {@link org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource}
 *             as soon as possible.
 * 
 * @see org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource
 * 
 * @author alexdma
 * 
 * @param <O>
 *            the root ontology object delivered by this input source.
 * 
 */
public interface OntologyInputSource<O> extends
        org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource<O> {

}
