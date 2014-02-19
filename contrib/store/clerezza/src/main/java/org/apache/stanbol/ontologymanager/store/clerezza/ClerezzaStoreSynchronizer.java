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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.apache.stanbol.ontologymanager.store.api.StoreSynchronizer;
import org.osgi.service.component.ComponentContext;
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
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Synchronizer for {@link ResourceManager}. Uses a {@link TcManager} to access graphs stored by Clerezza.
 * 
 * @author Cihan
 */
@Component(factory = "org.apache.stanbol.ontologymanager.store.StoreSynchronizerFactory")
@Service
public class ClerezzaStoreSynchronizer implements StoreSynchronizer {

    Logger logger = LoggerFactory.getLogger(ClerezzaStoreSynchronizer.class);

    @Property(name = "org.apache.stanbol.ontologymanager.store.ResourceManager")
    private ResourceManager resourceManager;

    @Property(name = "org.apache.stanbol.ontologymanager.store.PersistenceStore")
    private PersistenceStore store;

    @Reference
    private TcManager tcManager;

    private Map<UriRef,GraphListener> listeningMGraph = new HashMap<UriRef,GraphListener>();

    @Activate
    public void activate(final Map<?,?> properties) {

        this.resourceManager = (ResourceManager) properties.get(ResourceManager.class.getName());
        this.store = (PersistenceStore) properties.get(PersistenceStore.class.getName());

        // FIXME Is it necessary to listen Immutable Graphs

        // Add Listener to existing MGraphs
        for (final UriRef graphURI : tcManager.listMGraphs()) {
            MGraph graph = tcManager.getMGraph(graphURI);
            registerOntologyIfNotExist(graphURI.getUnicodeString());
            if (!listeningMGraph.containsKey(graphURI) && !graph.isEmpty()) {
                GraphListener listener = new SynchronizerGraphListener(this, graphURI.getUnicodeString());
                graph.addGraphListener(listener, new FilterTriple(null, null, null), 100);
                listeningMGraph.put(graphURI, listener);
                logger.info("Added listener to the mgraph {} : {} ", graphURI.toString(), listener);
            }
        }
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {

        // Synchronize before deactivating
        synchronizeAll(true);

        // Unregister GraphListeners

        for (UriRef graphUri : listeningMGraph.keySet()) {
            logger.info("Removing graph listener {}  on {} ", listeningMGraph.get(graphUri),
                graphUri.toString());
            tcManager.getMGraph(graphUri).removeGraphListener(listeningMGraph.get(graphUri));

        }

        listeningMGraph.clear();
    }

    @Override
    public void clear() {
        // Unregister GraphListeners

        for (UriRef graphUri : listeningMGraph.keySet()) {
            logger.info("Removing graph listener {} on ", listeningMGraph.get(graphUri), graphUri.toString());
            tcManager.getMGraph(graphUri).removeGraphListener(listeningMGraph.get(graphUri));
        }
        listeningMGraph.clear();
    }

    @Override
    public void synchronizeAll(boolean force) {
        synchronized (tcManager) {
            if (force) {
                for (UriRef graphURI : tcManager.listMGraphs()) {
                    String ontologyURI = resourceManager.getOntologyFullPath(graphURI.getUnicodeString());
                    if (ontologyURI == null) {
                        resourceManager.registerOntology(graphURI.getUnicodeString());
                    }
                    synchronizeGraph(graphURI.getUnicodeString());
                }
            } else {

                // Process Removed MGraphs
                List<UriRef> toBedeleted = new ArrayList<UriRef>();

                for (final UriRef graphURI : listeningMGraph.keySet()) {
                    try {
                        MGraph graph = tcManager.getMGraph(graphURI);
                        if (graph.isEmpty()) {
                            graph.removeGraphListener(listeningMGraph.get(graphURI));
                            toBedeleted.add(graphURI);
                        }
                    } catch (NoSuchEntityException e) {
                        toBedeleted.add(graphURI);
                    }
                }

                for (UriRef graphURI : toBedeleted) {
                    listeningMGraph.remove(graphURI);
                    resourceManager.removeOntology(graphURI.getUnicodeString());
                    logger.info("Stopped Listening MGraph: {}", graphURI.getUnicodeString());

                }

                // Process Added MGraphs
                for (final UriRef graphURI : tcManager.listMGraphs()) {
                    MGraph graph = tcManager.getMGraph(graphURI);
                    registerOntologyIfNotExist(graphURI.getUnicodeString());
                    if (!listeningMGraph.containsKey(graphURI) && !graph.isEmpty()) {
                        GraphListener listener = new SynchronizerGraphListener(this,
                                graphURI.getUnicodeString());
                        graph.addGraphListener(listener, new FilterTriple(null, null, null), 100);
                        listeningMGraph.put(graphURI, listener);
                        logger.info("Added listener to the mgraph  {} : {}", graphURI.toString(), listener);
                    }
                }
            }
        }
    }

    @Override
    public void synchronizeGraph(String graphURI) {
        LockableMGraph mgraph = tcManager.getMGraph(new UriRef(graphURI));
        Lock lock = mgraph.getLock().readLock();
        lock.lock();
        try {
            JenaGraph jgraph = new JenaGraph(mgraph);
            Model model = ModelFactory.createModelForGraph(jgraph);
            GraphSynchronizer es = new GraphSynchronizer(resourceManager, store, tcManager, model, graphURI);
            es.synchronize();
        } finally {
            lock.unlock();
        }
    }

    protected void synchronizeResourceOnGraph(String graphURI, List<String> resourceURIs) {
        LockableMGraph graph = tcManager.getMGraph(new UriRef(graphURI));
        Lock lock = graph.getLock().readLock();
        lock.lock();
        try {
            JenaGraph jgraph = new JenaGraph(graph);
            OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM,
                ModelFactory.createModelForGraph(jgraph));
            for (String resourceURI : resourceURIs) {
                OntClass klazz = ontology.getOntClass(resourceURI);
                ObjectProperty objectProp = ontology.getObjectProperty(resourceURI);
                DatatypeProperty datatypeProp = ontology.getDatatypeProperty(resourceURI);
                Individual individual = ontology.getIndividual(resourceURI);
                if (klazz != null) {
                    resourceManager.registerClass(graphURI, resourceURI);
                    logger.info("Added Class {}", resourceURI);
                } else if (objectProp != null) {
                    resourceManager.registerObjectProperty(graphURI, resourceURI);
                    logger.info("Added ObjectProperty {}", resourceURI);
                } else if (datatypeProp != null) {
                    resourceManager.registerDatatypeProperty(graphURI, resourceURI);
                    logger.info("Added DataProperty {}", resourceURI);
                } else if (individual != null) {
                    resourceManager.registerIndividual(graphURI, resourceURI);
                    logger.info("Added Individual {}", resourceURI);
                } else if (ontology.listStatements(null, OWL.imports,
                    ResourceFactory.createResource(resourceURI)).hasNext()) {
                    try {
                        store.saveOntology(new URL(resourceURI), resourceURI, "UTF-8");
                        logger.info("Added imported ontology: {}", resourceURI);
                    } catch (Exception e) {
                        logger.warn("Failed to import ontology: {}", resourceURI);
                    }
                }

                else {
                    // Not found, delete if the resource belongs to this graph
                    String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(resourceURI);
                    if (ontologyURI != null && ontologyURI.equals(graphURI)) {
                        resourceManager.removeResource(resourceURI);
                        logger.info("Removed Resource {}", resourceURI);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void registerOntologyIfNotExist(String graphURI) {
        String ontologyURI = resourceManager.getOntologyFullPath(graphURI);
        if (ontologyURI == null) {
            logger.info("Registering ontology: {}", graphURI);
            resourceManager.registerOntology(graphURI);
            synchronizeGraph(graphURI);
        }

    }

    protected void bindTcManager(TcManager tcManager) {
        this.tcManager = tcManager;
    }

    protected void unbindTcManager(TcManager tcManager) {
        synchronized (tcManager) {
            this.tcManager = null;
        }
    }

}
