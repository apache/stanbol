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

package org.apache.stanbol.contenthub.helper.enhancementlistener.tcmanager;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.event.AddEvent;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.stanbol.contenthub.core.utils.EntityHubClient;
import org.apache.stanbol.contenthub.core.utils.indexing.EnhancementLARQ;
import org.apache.stanbol.contenthub.core.utils.sparql.QueryGenerator;
import org.apache.stanbol.contenthub.servicesapi.enhancements.vocabulary.EnhancementGraphVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
public class EnhancementListener {
    private static final Logger logger = LoggerFactory.getLogger(EnhancementListener.class);

    private final GraphListener listener;
    private MGraph mGraph;
    private boolean active = false;

    public EnhancementListener(TcManager tcManager, String entityHubURI) {
        UriRef graphRef = new UriRef(EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
        boolean graphExists = tcManager.listMGraphs().contains(graphRef);
        // FIXME To allow this component to work for non-existing graphs, the graph is created here.
        // It is better to create this graph in somewhere else
        if (!graphExists) {
            tcManager.createMGraph(graphRef);
        }

        EntityHubClient entityHubClient = EntityHubClient.getInstance(entityHubURI);
        this.mGraph = tcManager.getMGraph(graphRef);
        EnhancementLARQ.getInstance().createIndex(this.mGraph);
        this.listener = new Listener(tcManager, entityHubClient);
    }

    public void listen() {
        if (!this.active) {
            this.mGraph.addGraphListener(listener, new FilterTriple(null, null, null), 1000);
            logger.debug("Started listening graph {} ", this.mGraph.toString());
            this.active = true;
        }
    }

    public void unlisten() {
        if (this.active) {
            this.mGraph.removeGraphListener(this.listener);
            logger.debug("Stopped listening graph {}", this.mGraph.toString());
            this.active = false;
        }
    }

    private final class Listener implements GraphListener {

        private TcManager tcManager;
        private EntityHubClient entityHubClient;
        private Semaphore sem = new Semaphore(1);

        public Listener(TcManager tcManager, EntityHubClient entityHubClient) {
            this.tcManager = tcManager;
            this.entityHubClient = entityHubClient;
        }

        @Override
        public void graphChanged(List<GraphEvent> arg0) {

            SimpleMGraph newEnhancements = new SimpleMGraph();
            for (GraphEvent e : arg0) {
            	if(e instanceof AddEvent) {
            		EnhancementLARQ.getInstance().updateIndex(e.getTriple());
                    newEnhancements.add(e.getTriple());	
            	}
            }

            try {
                this.sem.acquire();
                SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(
                    QueryGenerator.getExternalPlacesQuery());
                ResultSet result = tcManager.executeSparqlQuery(query, newEnhancements);
                while (result.hasNext()) {
                    SolutionMapping sol = result.next();
                    String uri = sol.get("ref").toString();
                    if (uri != null) {
                        uri = uri.replaceAll("\\<|\\>", "");
                        logger.debug("Found resource {} on graph {}. Trying to submit to entity hub",
                            newEnhancements.toString(), uri);
                        try {
                            logger.debug(
                                "Enhancement listener is calling entityhubclient.symbollookup with uri: {}",
                                uri);
                            entityHubClient.symbolLookup(uri, true);
                            logger.debug("Added symbol {} to entity hub");
                        } catch (Exception e) {
                            logger.debug("Error at submitting {} to entity hub at {}", uri,
                                entityHubClient.getEntityhubEndpoint());
                            logger.error("Error from entity hub", e);
                        }
                    } else {
                        logger.info("Found ehancement result without any URIs on graph {}",
                            newEnhancements.toString());
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Error at acquiring lock", e);
            } catch (ParseException e) {
                // Should never reach here
                logger.error("Settlement Query can not be parsed", e);
            } finally {
                // release acquired semaphore
                this.sem.release();
            }
        }
    }
}
