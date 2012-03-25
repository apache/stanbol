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
package org.apache.stanbol.ontologymanager.registry.api;

import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;

/**
 * Thrown whenever there is a request for the raw OWL version of a registry ontology which has not been loaded
 * yet (e.g. due to lazy loading policies). Developers who catch this exception may, for example, decide to
 * load the ontology.<br/>
 * <br/>
 * Note that this exception is independent from calls to
 * {@link RegistryContentListener#registryContentRequested(RegistryItem)}, although it can be expected to be
 * thrown thereafter.
 * 
 * @author alexdma
 */
public class RegistryOntologyNotLoadedException extends RegistryContentException {

    /**
     * 
     */
    private static final long serialVersionUID = 6336128674251128796L;

    private RegistryOntology ontology;

    /**
     * Creates a new instance of {@link RegistryOntologyNotLoadedException}.
     * 
     * @param library
     *            the ontology that caused the exception.
     */
    public RegistryOntologyNotLoadedException(RegistryOntology ontology) {
        super(ontology.getIRI().toString());
        this.ontology = ontology;
    }

    /**
     * Returns the requested ontology that is not loaded yet.
     * 
     * @return the ontology that caused the exception.
     */
    public RegistryOntology getOntology() {
        return ontology;
    }

}
