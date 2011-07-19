package org.apache.stanbol.ontologymanager.ontonet.impl.registry;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryItemFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryLibraryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryOntologyImpl;
import org.apache.stanbol.ontologymanager.ontonet.xd.vocabulary.CODOVocabulary;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
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
    public Library createLibrary(OWLNamedIndividual ind, Set<OWLOntology> ontologies) {
        if (!ind.getTypes(ontologies).contains(cRegistryLibrary)) throw new IllegalArgumentException(
                "Will not create a library from an individual not stated to be of type "
                        + REGISTRY_LIBRARY_ID + " in the supplied ontologies.");
        Library l = null;
        try {
            l = new RegistryLibraryImpl(ind.getIRI().getFragment(), ind.getIRI().toURI().toURL());
            // recurse into its children
            for (OWLOntology o : ontologies) {
                for (OWLAxiom ax : ind.getReferencingAxioms(o, true)) {
                    if (ax.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)
                        && (isOntologyOf.equals(((OWLObjectPropertyAssertionAxiom) ax).getProperty()) || isPartOf
                                .equals(((OWLObjectPropertyAssertionAxiom) ax).getProperty()))) {

                    }
                }
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return l;
    }

    @Override
    public Registry createRegistry(OWLNamedIndividual ind) {
        try {
            return new RegistryImpl(ind.getIRI().getFragment(), ind.getIRI().toURI().toURL());
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
