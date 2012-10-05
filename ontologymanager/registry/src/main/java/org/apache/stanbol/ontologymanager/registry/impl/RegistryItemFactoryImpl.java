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
package org.apache.stanbol.ontologymanager.registry.impl;

import org.apache.stanbol.ontologymanager.registry.api.RegistryItemFactory;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.registry.impl.model.LibraryImpl;
import org.apache.stanbol.ontologymanager.registry.impl.model.RegistryImpl;
import org.apache.stanbol.ontologymanager.registry.impl.model.RegistryOntologyImpl;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Default implementation of a registry item factory.
 * 
 * @author alexdma
 */
public class RegistryItemFactoryImpl implements RegistryItemFactory {

    private OntologyProvider<?> cache;

    /**
     * Creates a new instance of {@link RegistryItemFactoryImpl}.
     */
    public RegistryItemFactoryImpl(OntologyProvider<?> provider) {
        this.cache = provider;
    }

    @Override
    public Library createLibrary(OWLNamedObject ind) {
        return new LibraryImpl(ind.getIRI(), ind.getIRI().getFragment(), cache);
    }

    @Override
    public Registry createRegistry(OWLOntology o) {
        return o.isAnonymous() ? null : new RegistryImpl(o.getOntologyID().getOntologyIRI(), o
                .getOntologyID().toString());
    }

    @Override
    public RegistryOntology createRegistryOntology(OWLNamedObject ind) {
        return new RegistryOntologyImpl(ind.getIRI(), ind.getIRI().getFragment());
    }

}
