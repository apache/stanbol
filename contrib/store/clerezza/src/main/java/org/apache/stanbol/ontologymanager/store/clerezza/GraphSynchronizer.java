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
package org.apache.stanbol.ontologymanager.store.clerezza;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
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
    private PersistenceStore store;
    private TcManager tcManager;
    private OntModel model;
    private String graphURI;

    public GraphSynchronizer(ResourceManager resourceManager,
                             PersistenceStore store,
                             TcManager tcManager,
                             Model model,
                             String graphURI) {
        this.resourceManager = resourceManager;
        this.store = store;
        this.tcManager = tcManager;
        this.model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        this.graphURI = graphURI;
    }

    public void synchronize() {
        logger.info("Synchronizing: {}", graphURI);
        // Remove ontology so that every resource is unregistered
        resourceManager.registerOntology(graphURI);
        synchronizeClasses();
        synchronizeObjectProperties();
        synchronizeDataProperties();
        synchronizeIndividuals();
        synchronizeImports();

    }

    private void synchronizeIndividuals() {
        List<Individual> individuals = model.listIndividuals().toList();
        for (Individual individual : individuals) {
            String individualURI = individual.getURI();
            if (individualURI == null) continue;
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            if (ontologyURI == null) {
                logger.warn("Resource not found:{} ", individualURI);
                resourceManager.registerIndividual(graphURI, individualURI);
                logger.info("Resource registered:{} on ontology {}", individualURI, graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered:{} ", individualURI);
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
                logger.warn("Resource not found:{} ", propertyURI);
                resourceManager.registerDatatypeProperty(graphURI, propertyURI);
                logger.info("Resource registered:{} on ontology {}", propertyURI, graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered:{} ", propertyURI);
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
                logger.warn("Resource not found: {}", propertyURI);
                resourceManager.registerObjectProperty(graphURI, propertyURI);
                logger.info("Resource registered:{} on ontology {}", propertyURI, graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered:{} ", propertyURI);
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
                logger.warn("Resource not found: {}", classURI);
                resourceManager.registerClass(graphURI, classURI);
                logger.info("Resource registered: {} on ontology {}", classURI, graphURI);
            } else if (graphURI.equals(ontologyURI)) {
                logger.debug("Resource already registered: {}", classURI);
            } else {
                logger.warn("Resource found on another ontology (This case will be handled later)");
            }
        }
    }

    private void synchronizeImports() {
        for (String uri : model.listImportedOntologyURIs()) {
            Set<UriRef> graphs = tcManager.listTripleCollections();
            UriRef current = new UriRef(uri);
            boolean found = false;
            for (UriRef currentGraph : graphs) {
                if (currentGraph.equals(current)) {
                    found = true;
                }
            }
            if (!found) {
                try {
                    store.saveOntology(new URL(uri), uri, "UTF-8");
                } catch (Exception e) {
                    logger.debug("Registered imported graph with uri {}", uri);
                }
            }
        }
    }

}
