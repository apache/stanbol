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
package org.apache.stanbol.cmsadapter.core.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

/**
 * This class is a client for basic ontology level functionalities through Clerezza {@link TcManager}
 * 
 * @author suat
 * 
 */
public class TcManagerClient {
    private TcManager tcManager;

    public TcManagerClient(TcManager tcManager) {
        this.tcManager = tcManager;
    }

    /**
     * Checks the {@link MGraph} specified with <code>ontologyURI</code> exists
     * 
     * @param ontologyURI
     *            URI of the ontology
     * @return whether the {@link MGraph} specified exists
     */
    public boolean modelExists(String ontologyURI) {
        Set<UriRef> graphs = tcManager.listMGraphs();
        if (graphs.contains(new UriRef(ontologyURI))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the ontology specified with <code>ontologyURI</code> through {@link TcManager}. Obtained
     * {@link MGraph} is used to create a {@link JenaGraph}. If the graph does not exists, a new is created.
     * 
     * @param ontologyURI
     *            URI of the ontology
     * @return {@link Model} specified by <code>ontologyURI</code>
     */
    public Model getModel(String ontologyURI) {
        MGraph graph;
        if (modelExists(ontologyURI)) {
            graph = tcManager.getMGraph(new UriRef(ontologyURI));
        } else {
            graph = tcManager.createMGraph(new UriRef(ontologyURI));
        }
        JenaGraph jenaGraph = new JenaGraph(graph);
        Model model = ModelFactory.createModelForGraph(jenaGraph);
        return model;
    }

    /**
     * Stores the ontology specified by <code>ontology</code> through {@link TcManager}. If a graph specified
     * by <code>ontologyURI</code> already exists, it is deleted, then a new one is created.
     * 
     * @param ontology
     *            content of the ontology passed in a {@link Model} object
     * @param ontologyURI
     *            URI of the ontology
     */
    public void saveOntology(Model ontology, String ontologyURI) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFWriter rdfWriter = ontology.getWriter("RDF/XML");
        rdfWriter.setProperty("xmlbase", ontologyURI);
        rdfWriter.write(ontology, baos, ontologyURI);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        if (modelExists(ontologyURI)) {
            deleteModel(ontologyURI);
        }
        MGraph graph = tcManager.createMGraph(new UriRef(ontologyURI));
        JenaGraph jenaGraph = new JenaGraph(graph);
        Model model = ModelFactory.createModelForGraph(jenaGraph);
        model.read(bais, ontologyURI);

        if (model.supportsTransactions()) {
            model.commit();
        }
    }

    private void deleteModel(String ontologyURI) {
        tcManager.deleteTripleCollection(new UriRef(ontologyURI));
    }
}
