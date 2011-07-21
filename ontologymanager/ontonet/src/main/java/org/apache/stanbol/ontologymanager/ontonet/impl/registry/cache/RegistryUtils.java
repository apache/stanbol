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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry.cache;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryItemFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem.Type;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.RegistryItemFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.xd.vocabulary.CODOVocabulary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryUtils {

    private static final OWLClass cRegistryLibrary, cOntology;

    private static final OWLObjectProperty hasPart, hasOntology, isPartOf, isOntologyOf;

    private static Logger log = LoggerFactory.getLogger(RegistryUtils.class);

    private static Map<IRI,RegistryItem> population = new TreeMap<IRI,RegistryItem>();

    private static RegistryItemFactory riFactory;

    static {
        riFactory = new RegistryItemFactoryImpl();
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
                IRI iri = IRI.create(item.getURL());
                result |= iri.equals(ontologyId);
            } catch (Exception e) {
                return false;
            }
        } else if (item instanceof Library || item instanceof Registry)
        // Inspect children
        for (RegistryItem child : ((RegistryItem) item).getChildren()) {
            result |= containsOntologyRecursive(child, ontologyId);
            if (result) break;
        }
        return result;

    }

    /**
     * Simulates a classifier.
     * 
     * @param ind
     * @param o
     * @return
     */
    public static Type getType(OWLIndividual ind, Set<OWLOntology> ontologies) {
        // TODO also use property values
        Set<OWLClassExpression> types = ind.getTypes(ontologies);
        if (types.contains(cOntology) && !types.contains(cRegistryLibrary))
            return Type.ONTOLOGY;
        if (!types.contains(cOntology) && types.contains(cRegistryLibrary))
            return Type.LIBRARY;
        return null;
    }
   
//    public static Library populateLibrary(OWLNamedIndividual ind, Set<OWLOntology> registries) throws RegistryContentException {
//        IRI id = ind.getIRI();
//        RegistryItem lib = null;
//        if (population.containsKey(id)) {
//            // We are not allowing multityping either.
//            lib = population.get(id);
//            if (!(lib instanceof Library)) throw new RegistryContentException(
//                    "Inconsistent multityping: for item " + id + " : {" + Library.class + ", "
//                            + lib.getClass() + "}");
//        } else {
//            lib = riFactory.createLibrary(ind.asOWLNamedIndividual());
//            try {
//                population.put(IRI.create(lib.getURL()), lib);
//            } catch (URISyntaxException e) {
//                log.error("Invalid identifier for library item " + lib, e);
//                return null;
//            }
//        }
//        // EXIT nodes.
//        Set<OWLIndividual> ronts = new HashSet<OWLIndividual>();
//        for (OWLOntology o : registries)
//            ronts.addAll(ind.getObjectPropertyValues(hasOntology, o));
//        for (OWLIndividual iont : ronts) {
//            if (iont.isNamed())
//                lib.addChild(populateOntology(iont.asOWLNamedIndividual(), registries));
//        }
//        return (Library) lib;
//    }
//    
//    public static RegistryOntology populateOntology(OWLNamedIndividual ind, Set<OWLOntology> registries) throws RegistryContentException {
//        IRI id = ind.getIRI();
//        RegistryItem ront = null;
//        if (population.containsKey(id)) {
//            // We are not allowing multityping either.
//            ront = population.get(id);
//            if (!(ront instanceof RegistryOntology)) throw new RegistryContentException(
//                    "Inconsistent multityping: for item " + id + " : {" + RegistryOntology.class + ", "
//                            + ront.getClass() + "}");
//        } else {
//            ront = riFactory.createRegistryOntology(ind);
//            try {
//                population.put(IRI.create(ront.getURL()), ront);
//            } catch (URISyntaxException e) {
//                log.error("Invalid identifier for library item " + ront, e);
//                return null;
//            }
//        }
//        // EXIT nodes.
//        Set<OWLIndividual> libs = new HashSet<OWLIndividual>();
//        for (OWLOntology o : registries)
//            libs.addAll(ind.getObjectPropertyValues(isOntologyOf, o));
//        for (OWLIndividual ilib : libs) {
//            if (ilib.isNamed())
//                ront.addContainer(populateLibrary(ilib.asOWLNamedIndividual(), registries));
//        }
//        return (RegistryOntology) ront;
//    }
//
//    public static Registry populateRegistry(OWLOntology registry) throws RegistryContentException {
//
//        Registry reg = riFactory.createRegistry(registry);
//        Set<OWLOntology> closure = registry.getOWLOntologyManager().getImportsClosure(registry);
//
//        // Just scan all individuals. Recurse in case the registry imports more registries.
//        for (OWLIndividual ind : registry.getIndividualsInSignature(true)) {
//            // We do not allow anonymous registry items.
//            if (ind.isAnonymous()) continue;
//            RegistryItem item = null;
//            // IRI id = ind.asOWLNamedIndividual().getIRI();
//            Type t = getType(ind, closure);
//            if (t==null) {
//                log.warn("Undetermined type for registry ontology individual {}",ind);
//                continue;
//            }
//            switch (getType(ind, closure)) {
//                case LIBRARY:
//                    // // Create the library and attach to parent and children
//                    item = populateLibrary(ind.asOWLNamedIndividual(), closure);
//                    reg.addChild(item);
//                    break;
//                case ONTOLOGY:
//                    // Create the ontology and attach to parent
//                    item = populateOntology(ind.asOWLNamedIndividual(), closure);
//                    // We don't know where to attach it to in this method.
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        population = new TreeMap<IRI,RegistryItem>();
//        return reg;
//    }

}
