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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.semanticweb.owlapi.model.IRI;

/**
 * A system responsible for maintaining registry ontologies. Depending on the implementation, it can be
 * volatile or persistent, centralised or distributed.
 * 
 * @author alexdma
 * 
 * @param <S>
 *            the storage system actually used by this provider.
 */
public interface OntologyProvider<S> {

    /**
     * The key used to configure the prefix to be used for addressing ontologies stored by this provider.
     */
    public String GRAPH_PREFIX = "org.apache.stanbol.ontologymanager.ontonet.graphPrefix";

    /**
     * The key used to configure the default import resolution policy for this provider.
     */
    public String RESOLVE_IMPORTS = "org.apache.stanbol.ontologymanager.ontonet.resolveImports";

    Set<String> getOntologyReferences();

    /**
     * Returns the storage system used by this ontology provider.
     * 
     * @return the ontology store.
     */
    S getStore();

    /**
     * 
     * @param identifier
     * @param returnType
     *            The expected type for the returned ontology object. If null, the provider will arbitrarily
     *            select a supported return type. If the supplied type is not supported (i.e. not assignable
     *            to any type contained in the result of {@link #getSupportedReturnTypes()}) an
     *            {@link UnsupportedOperationException} will be thrown.
     * @return
     */
    Object getStoredOntology(String identifier, Class<?> returnType);

    /**
     * Returns an array containing the most specific types for ontology objects that this provider can manage
     * and return on a call to {@link #getStoredOntology(String, Class)}.
     * 
     * @return the supported ontology return types.
     */
    Class<?>[] getSupportedReturnTypes();

    String loadInStore(InputStream data, String formatIdentifier, boolean force) throws IOException,
                                                                                UnsupportedFormatException;

    String loadInStore(IRI location, String formatIdentifier, boolean force) throws IOException,
                                                                            UnsupportedFormatException;

}
