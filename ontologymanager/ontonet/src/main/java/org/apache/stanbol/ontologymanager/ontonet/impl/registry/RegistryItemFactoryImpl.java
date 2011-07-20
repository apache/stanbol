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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryItemFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.LibraryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryOntologyImpl;
import org.apache.stanbol.ontologymanager.ontonet.xd.vocabulary.CODOVocabulary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

public class RegistryItemFactoryImpl implements RegistryItemFactory {

    public static final String REGISTRY_LIBRARY_ID = CODOVocabulary.CODD_OntologyLibrary;

    public static final String IS_PART_OF_ID = CODOVocabulary.PARTOF_IsPartOf;

    public static final String IS_ONTOLOGY_OF_ID = CODOVocabulary.ODPM_IsOntologyOf;

    private final OWLClass cRegistryLibrary;

    private final OWLObjectProperty isPartOf, isOntologyOf;

    public RegistryItemFactoryImpl() {
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        cRegistryLibrary = df.getOWLClass(IRI.create(REGISTRY_LIBRARY_ID));
        isPartOf = df.getOWLObjectProperty(IRI.create(IS_PART_OF_ID));
        isOntologyOf = df.getOWLObjectProperty(IRI.create(IS_ONTOLOGY_OF_ID));
    }

    @Override
    public Library createLibrary(OWLNamedIndividual ind) {
        // if (!ind.getTypes(ontologies).contains(cRegistryLibrary)) throw new IllegalArgumentException(
        // "Will not create a library from an individual not stated to be of type "
        // + REGISTRY_LIBRARY_ID + " in the supplied ontologies.");
        Library l = null;
        try {
            l = new LibraryImpl(ind.getIRI().getFragment(), ind.getIRI().toURI().toURL());
            // // recurse into its children
            // for (OWLOntology o : ontologies) {
            // for (OWLAxiom ax : ind.getReferencingAxioms(o, true)) {
            // if (ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)
            // && (isOntologyOf.equals(((OWLObjectPropertyAssertionAxiom) ax).getProperty()) || isPartOf
            // .equals(((OWLObjectPropertyAssertionAxiom) ax).getProperty()))) {
            //
            // }
            // }
            // }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return l;
    }

    @Override
    public Registry createRegistry(OWLOntology o) {
        try {
            return new RegistryImpl(o.getOntologyID().toString(), o.isAnonymous() ? null : o.getOntologyID()
                    .getOntologyIRI().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public RegistryOntology createRegistryOntology(OWLNamedIndividual ind) {
        try {
            return new RegistryOntologyImpl(ind.getIRI().getFragment(), ind.getIRI().toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
