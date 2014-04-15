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
package org.apache.stanbol.ontologymanager.servicesapi.util;

import static org.apache.stanbol.ontologymanager.servicesapi.util.OntologyConstants.NS_ONM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * <p>
 * This is the helper class for parsing the ONM configuration ontology. The configuration ontology should
 * import the following:
 * </p>
 * <ul>
 * <li>http://ontologydesignpatterns.org/ont/iks/kres/onm.owl</li>
 * </ul>
 * 
 * <p>
 * and must use the following vocabs:
 * </p>
 * <ul>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#Scope : defines a scope</li>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#activateOnStart : activate the scope on startup</li>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#usesCoreOntology : relates a scope to an ontology to
 * be added in the core space</li>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#usesCoreLibrary : relates a scope to a library of
 * ontologies to be added in the core space</li>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#usesCustomOntology : relates a scope to an ontology to
 * be added in the custom space</li>
 * <li>http://kres.iks-project.eu/ontology/meta/onm.owl#usesCustomLibrary : relates scope to a library of
 * ontologies to be added in the custom space</li>
 * <li>
 * http://www.ontologydesignpatterns.org/cpont/codo/coddata.owl#OntologyLibrary : the class of a library</li>
 * <li>http://www.ontologydesignpatterns.org/schemas/meta.owl#hasOntology : to relate a library to an ontology
 * </li>
 * </ul>
 * 
 * @author alexdma
 * @author enridaga
 */
public final class OntologyNetworkConfigurationUtils {

    /**
     * Restrict instantiation
     */
    private OntologyNetworkConfigurationUtils() {}

    private static OWLDataFactory _df = OWLManager.getOWLDataFactory();

    private static final String[] EMPTY_IRI_ARRAY = new String[0];

    private static final OWLClass cScope = _df.getOWLClass(IRI.create(NS_ONM + "Scope"));

    private static final OWLClass cLibrary = _df.getOWLClass(IRI
            .create("http://www.ontologydesignpatterns.org/cpont/codo/coddata.owl#OntologyLibrary"));

    private static final OWLDataProperty activateOnStart = _df.getOWLDataProperty(IRI
            .create(NS_ONM + "activateOnStart"));

    private static final OWLObjectProperty usesCoreOntology = _df.getOWLObjectProperty(IRI
            .create(NS_ONM + "usesCoreOntology"));

    private static final OWLObjectProperty usesCoreLibrary = _df.getOWLObjectProperty(IRI
            .create(NS_ONM + "usesCoreLibrary"));

    private static final OWLObjectProperty usesCustomOntology = _df.getOWLObjectProperty(IRI
            .create(NS_ONM + "usesCustomOntology"));

    private static final OWLObjectProperty usesCustomLibrary = _df.getOWLObjectProperty(IRI
            .create(NS_ONM + "usesCustomLibrary"));

    private static final OWLObjectProperty libraryHasOntology = _df.getOWLObjectProperty(IRI
            .create(NS_ONM + "hasOntology"));

    /**
     * Get the list of scopes to activate on startup
     * 
     * @param config
     * @return
     */
    public static String[] getScopesToActivate(OWLOntology config) {

        Set<OWLIndividual> scopes = cScope.getIndividuals(config);
        List<String> result = new ArrayList<String>();
        boolean doActivate = false;
        for (OWLIndividual iScope : scopes) {
            Set<OWLLiteral> activate = iScope.getDataPropertyValues(activateOnStart, config);

            Iterator<OWLLiteral> it = activate.iterator();
            while (it.hasNext() && !doActivate) {
                OWLLiteral l = it.next();
                doActivate |= Boolean.parseBoolean(l.getLiteral());
            }

            if (iScope.isNamed() && doActivate) result.add(((OWLNamedIndividual) iScope).getIRI().toString());
        }

        return result.toArray(EMPTY_IRI_ARRAY);
    }

    /**
     * To get all the instances of Scope in this configuration
     * 
     * @param config
     * @return
     */
    public static String[] getScopes(OWLOntology config) {
        Set<OWLIndividual> scopes = cScope.getIndividuals(config);
        List<String> result = new ArrayList<String>();
        for (OWLIndividual iScope : scopes) {
            for (OWLClassExpression sce : iScope.getTypes(config)) {
                if (sce.containsConjunct(cScope)) {
                    if (iScope.isNamed()) {
                        result.add(((OWLNamedIndividual) iScope).getIRI().toString());
                    }
                }
            }
        }
        return result.toArray(EMPTY_IRI_ARRAY);
    }

    /**
     * Utility method to get all the values of an object property of a Scope
     * 
     * @param ontology
     * @param individualIRI
     * @param op
     * @return
     */
    private static String[] getScopeObjectPropertyValues(OWLOntology ontology,
                                                         String individualIRI,
                                                         OWLObjectProperty op) {
        Set<OWLIndividual> scopes = cScope.getIndividuals(ontology);
        List<String> result = new ArrayList<String>();

        OWLIndividual iiScope = null;

        // Optimised loop.
        for (OWLIndividual ind : scopes) {
            if (ind.isAnonymous()) continue;
            if (((OWLNamedIndividual) ind).getIRI().toString().equals(individualIRI)) {
                iiScope = ind;
                break;
            }
        }

        if (iiScope != null) {

        }

        for (OWLIndividual iScope : scopes) {
            if (iScope.isNamed()) {
                if (((OWLNamedIndividual) iScope).getIRI().toString().equals(individualIRI)) {
                    Set<OWLIndividual> values = iScope.getObjectPropertyValues(op, ontology);

                    Iterator<OWLIndividual> it = values.iterator();
                    while (it.hasNext()) {
                        OWLIndividual i = it.next();
                        if (i.isNamed()) result.add(((OWLNamedIndividual) i).getIRI().toString());
                    }
                }
            }
        }

        return result.toArray(EMPTY_IRI_ARRAY);
    }

    /**
     * Utility method to get all the values of a property from a Library subject
     * 
     * @param ontology
     * @param individualIRI
     * @param op
     * @return
     */
    private static String[] getLibraryObjectPropertyValues(OWLOntology ontology,
                                                           String individualIRI,
                                                           OWLObjectProperty op) {
        Set<OWLIndividual> scopes = cLibrary.getIndividuals(ontology);
        List<String> result = new ArrayList<String>();

        for (OWLIndividual iLibrary : scopes) {
            if (iLibrary.isNamed()) {
                if (((OWLNamedIndividual) iLibrary).getIRI().toString().equals(individualIRI)) {
                    Set<OWLIndividual> values = iLibrary.getObjectPropertyValues(op, ontology);

                    Iterator<OWLIndividual> it = values.iterator();
                    while (it.hasNext()) {
                        OWLIndividual i = it.next();
                        if (i.isNamed()) result.add(((OWLNamedIndividual) iLibrary).getIRI().toString());
                    }
                }
            }
        }

        return result.toArray(EMPTY_IRI_ARRAY);
    }

    /**
     * Returns all the IRIs to be loaded in the core space of the scope
     * 
     * @param config
     * @param scopeIRI
     * @return
     */
    public static String[] getCoreOntologies(OWLOntology config, String scopeIRI) {
        List<String> ontologies = new ArrayList<String>();
        ontologies.addAll(Arrays.asList(getScopeObjectPropertyValues(config, scopeIRI, usesCoreOntology)));

        for (String libraryID : getCoreLibraries(config, scopeIRI)) {
            ontologies.addAll(Arrays.asList(getLibraryObjectPropertyValues(config, libraryID,
                libraryHasOntology)));
        }
        return ontologies.toArray(new String[ontologies.size()]);
    }

    /**
     * Returns all the resources to be part of the Custom space
     * 
     * @param config
     * @param scopeIRI
     * @return
     */
    public static String[] getCustomOntologies(OWLOntology config, String scopeIRI) {
        List<String> ontologies = new ArrayList<String>();
        ontologies.addAll(Arrays.asList(getScopeObjectPropertyValues(config, scopeIRI, usesCustomOntology)));

        for (String libraryID : getCustomLibraries(config, scopeIRI)) {
            ontologies.addAll(Arrays.asList(getLibraryObjectPropertyValues(config, libraryID,
                libraryHasOntology)));
        }
        return ontologies.toArray(new String[ontologies.size()]);
    }

    private static String[] getCoreLibraries(OWLOntology config, String scopeIRI) {
        return getScopeObjectPropertyValues(config, scopeIRI, usesCoreLibrary);
    }

    private static String[] getCustomLibraries(OWLOntology config, String scopeIRI) {
        return getScopeObjectPropertyValues(config, scopeIRI, usesCustomLibrary);
    }

}
