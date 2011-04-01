package org.apache.stanbol.ontologymanager.store.clerezza;

import java.util.List;

import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GraphSynchronizer {
    Logger logger = LoggerFactory.getLogger(GraphSynchronizer.class);

    private ResourceManager resourceManager;
    private OntModel model;
    private String graphURI;

    public GraphSynchronizer(ResourceManager resourceManager, Model model, String graphURI) {
        this.resourceManager = resourceManager;
        this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        this.graphURI = graphURI;
    }

    public void synchronize() {
        logger.info("Synchronizing: " + graphURI);
        // Remove ontology so that every resource is unregistered
        resourceManager.registerOntology(graphURI);
        synchronizeClasses();
        synchronizeObjectProperties();
        synchronizeDataProperties();
        synchronizeIndividuals();

    }

    private void synchronizeIndividuals() {
        List<Individual> individuals = model.listIndividuals().toList();
        for (Individual individual : individuals) {
            String individualURI = individual.getURI();
            if (individualURI == null) continue;
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            if (ontologyURI == null) {
                logger.warn("Resource not found:" + individualURI);
                resourceManager.registerIndividual(graphURI, individualURI);
                logger.info("Resource registered:" + individualURI + "on ontology " + graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered: " + individualURI);
            } else {
                logger.warn("Resource found on another ontology (This case will be handled later)");
            }
        }
    }

    private void synchronizeDataProperties() {
        List<DatatypeProperty> dataProperties = model.listDatatypeProperties().toList();
        for (DatatypeProperty dataProperty : dataProperties) {
            String propertyURI = dataProperty.getURI();
            if (propertyURI == null) continue;
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            if (ontologyURI == null) {
                logger.warn("Resource not found:" + propertyURI);
                resourceManager.registerDatatypeProperty(graphURI, propertyURI);
                logger.info("Resource registered:" + propertyURI + "on ontology " + graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered: " + propertyURI);
            } else {
                logger.warn("Resource found on another ontology (This case will be handled later)");
            }
        }
    }

    private void synchronizeObjectProperties() {
        List<ObjectProperty> objectProperties = model.listObjectProperties().toList();
        for (ObjectProperty objectProperty : objectProperties) {
            String propertyURI = objectProperty.getURI();
            if (propertyURI == null) continue;
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            if (ontologyURI == null) {
                logger.warn("Resource not found:" + propertyURI);
                resourceManager.registerObjectProperty(graphURI, propertyURI);
                logger.info("Resource registered:" + propertyURI + "on ontology " + graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered: " + propertyURI);
            } else {
                logger.warn("Resource found on another ontology (This case will be handled later)");
            }
        }
    }

    private void synchronizeClasses() {
        List<OntClass> classes = model.listClasses().toList();
        for (OntClass klass : classes) {
            String classURI = klass.getURI();
            if (classURI == null) continue;
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            if (ontologyURI == null) {
                logger.warn("Resource not found:" + classURI);
                resourceManager.registerClass(graphURI, classURI);
                logger.info("Resource registered:" + classURI + "on ontology " + graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered: " + classURI);
            } else {
                logger.warn("Resource found on another ontology (This case will be handled later)");
            }
        }

    }

}
