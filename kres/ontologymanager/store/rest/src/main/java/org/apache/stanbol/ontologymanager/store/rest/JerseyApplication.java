package org.apache.stanbol.ontologymanager.store.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.ontologymanager.store.rest.resources.Ontologies;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyDatatypeProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyDump;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyIndividuals;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyObjectProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClass;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassDisjointClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassEquivalentClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassSuperClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypeProperty;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertyDomains;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertyRanges;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertySuperProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividual;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividualPropertyAssertions;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividualTypes;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectProperty;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertyDomains;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertyRanges;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertySuperProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularOntology;

public class JerseyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(Ontologies.class);
        classes.add(OntologyClasses.class);
        classes.add(OntologyDatatypeProperties.class);
        classes.add(OntologyObjectProperties.class);
        classes.add(OntologyIndividuals.class);
        classes.add(ParticularClass.class);
        classes.add(ParticularDatatypeProperty.class);
        classes.add(ParticularOntology.class);
        classes.add(ParticularObjectProperty.class);
        classes.add(ParticularIndividual.class);
        classes.add(OntologyDump.class);
        classes.add(ParticularClassSuperClasses.class);
        classes.add(ParticularClassDisjointClasses.class);
        classes.add(ParticularClassEquivalentClasses.class);
        classes.add(ParticularObjectPropertyRanges.class);
        classes.add(ParticularObjectPropertyDomains.class);
        classes.add(ParticularObjectPropertySuperProperties.class);
        classes.add(ParticularDatatypePropertyDomains.class);
        classes.add(ParticularDatatypePropertyRanges.class);
        classes.add(ParticularDatatypePropertySuperProperties.class);
        classes.add(ParticularIndividualTypes.class);
        classes.add(ParticularIndividualPropertyAssertions.class);

        classes.add(JAXBProvider.class);

        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        // view processors
        singletons.add(new FreemarkerViewProcessor());
        return singletons;
    }
}
