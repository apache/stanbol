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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;

/**
 * An {@link OntologyInputSource} that gets ontologies from either a Clerezza {@link Graph} (or {@link MGraph}
 * ), or its identifier and an optionally supplied triple collection manager.
 * 
 * @author alexdma
 * 
 */
public class GraphSource extends AbstractClerezzaGraphInputSource {

    public GraphSource(TripleCollection graph) {
        if (graph instanceof Graph) bindRootOntology((Graph) graph);
        else if (graph instanceof MGraph) bindRootOntology(((MGraph) graph).getGraph());
        else throw new IllegalArgumentException("GraphSource supports only Graph and MGraph types. "
                                                + graph.getClass() + " is not supported.");
        bindPhysicalIri(null);
    }

    public GraphSource(UriRef graphId) {
        this(graphId, TcManager.getInstance());
    }

    /**
     * This constructor can be used to hijack ontologies using a physical IRI other than their default one.
     * 
     * @param rootOntology
     * @param phyicalIRI
     */
    public GraphSource(UriRef graphId, TcManager tcManager) {
        this(tcManager.getTriples(graphId));
        bindTriplesProvider(tcManager);
    }

    @Override
    public String toString() {
        return "GRAPH<" + rootOntology + ">";
    }

}
