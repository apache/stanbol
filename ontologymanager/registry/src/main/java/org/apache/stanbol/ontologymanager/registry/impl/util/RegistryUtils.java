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
package org.apache.stanbol.ontologymanager.registry.impl.util;

import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem.Type;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.registry.xd.vocabulary.CODOVocabulary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistryUtils {

    /**
     * Restrict instantiation
     */
    private RegistryUtils() {}

    private static final OWLClass cRegistryLibrary, cOntology;

    private static final OWLObjectProperty hasPart, hasOntology, isPartOf, isOntologyOf;

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(RegistryUtils.class);

    static {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        cOntology = factory.getOWLClass(IRI.create(CODOVocabulary.CODK_Ontology));
        cRegistryLibrary = factory.getOWLClass(IRI.create(CODOVocabulary.CODD_OntologyLibrary));
        isPartOf = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.PARTOF_IsPartOf));
        isOntologyOf = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.ODPM_IsOntologyOf));
        hasPart = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.PARTOF_HasPart));
        hasOntology = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.ODPM_HasOntology));
    }

    /**
     * Utility method to recurse into registry items.
     * 
     * TODO: move this to main?
     * 
     * @param item
     * @param ontologyId
     * @return
     */
    public static boolean containsOntologyRecursive(RegistryItem item, IRI ontologyId) {

        boolean result = false;
        if (item instanceof RegistryOntology) {
            // An Ontology MUST have a non-null URI.
            try {
                IRI iri = item.getIRI();
                result |= iri.equals(ontologyId);
            } catch (Exception e) {
                return false;
            }
        } else if (item instanceof Library || item instanceof Registry)
        // Inspect children
        for (RegistryItem child : item.getChildren()) {
            result |= containsOntologyRecursive(child, ontologyId);
            if (result) break;
        }
        return result;

    }

    @Deprecated
    public static Type getType(final OWLIndividual ind, Set<OWLOntology> ontologies) {

        // 0 is for library, 1 is for ontology (more in the future?)
        final int[] pointsFor = new int[] {0, 0};
        final int[] pointsAgainst = new int[] {0, 0};

        OWLAxiomVisitor v = new OWLAxiomVisitorAdapter() {

            @Override
            public void visit(OWLClassAssertionAxiom axiom) {
                if (ind.equals(axiom.getIndividual())) {
                    OWLClassExpression type = axiom.getClassExpression();
                    if (cRegistryLibrary.equals(type)) {
                        pointsFor[0]++;
                        pointsAgainst[1]++;
                    } else if (cOntology.equals(type)) {
                        pointsFor[1]++;
                        pointsAgainst[0]++;
                    }
                }
            }

            @Override
            public void visit(OWLObjectPropertyAssertionAxiom axiom) {
                OWLObjectPropertyExpression prop = axiom.getProperty();
                if (ind.equals(axiom.getSubject())) {

                    if (hasOntology.equals(prop)) {
                        pointsFor[0]++;
                        pointsAgainst[1]++;
                    } else if (isOntologyOf.equals(prop)) {
                        pointsFor[1]++;
                        pointsAgainst[0]++;
                    }

                } else if (ind.equals(axiom.getObject())) {
                    if (isOntologyOf.equals(prop)) {
                        pointsFor[0]++;
                        pointsAgainst[1]++;
                    } else if (hasOntology.equals(prop)) {
                        pointsFor[1]++;
                        pointsAgainst[0]++;
                    }
                }
            }

        };

        // TODO use this strategy in the single pass algorithm for constructing the model.
        for (OWLOntology o : ontologies)
            for (OWLAxiom ax : o.getAxioms())
                ax.accept(v);

        if (pointsFor[0] > 0 && pointsAgainst[0] == 0) return Type.LIBRARY;
        if (pointsFor[1] > 0 && pointsAgainst[1] == 0) return Type.ONTOLOGY;
        // Cannot determine registries, since they have no associated individual.
        return null;

    }

}
