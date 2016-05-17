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
package org.apache.stanbol.entityhub.model.clerezza;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
/**
 * Factory for creating instances of the RDF implementation of the Entityhub model
 * based on Clerezza.
 * TODO: Check if it makes sense to add a instance cache for {@link RdfReference}
 *       instances.
 * @author Rupert Westenthaler
 *
 */
public final class RdfValueFactory implements ValueFactory {

    private static RdfValueFactory instance;
    /**
     * TODO:Currently implements the singleton pattern. This might change in the
     * future if ValueFactoy becomes an own OSGI Service
     * @return
     */
    public static RdfValueFactory getInstance() {
        if(instance == null){
            instance = new RdfValueFactory();
        }
        return instance;
    }
    /**
     * If not <code>null</code> all {@link RdfRepresentation} created by this
     * instance will use this graph.
     */
    private Graph graph;
    private RdfValueFactory(){
        this(null);
    }
    /**
     * This allows to create an instance that uses the same graph for all
     * created {@link Representation}s. This allows to automatically add all
     * data added to {@link Representation} created by this Factory to this
     * graph. 
     * @param graph
     */
    public RdfValueFactory(Graph graph){
        super();
        this.graph = graph;
    }

    @Override
    public RdfReference createReference(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        } else if (value instanceof IRI) {
            return new RdfReference((IRI) value);
        } else {
            return new RdfReference(value.toString());
        }
    }

    @Override
    public RdfText createText(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL");
        } else if (value instanceof Literal) {
            return new RdfText((Literal) value);
        } else {
            return createText(value.toString(), null);
        }
    }

    @Override
    public RdfText createText(String text, String language) {
        return new RdfText(text, language);
    }

    @Override
    public RdfRepresentation createRepresentation(String id) {
        if (id == null){
           throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
        } else if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be empty!");
        } else {
            return createRdfRepresentation(new IRI(id), 
                graph == null ? new IndexedGraph() : graph);
        }
    }

    /**
     * {@link RdfRepresentation} specific create Method based on an existing
     * RDF ImmutableGraph.
     *
     * @param node The node of the node used for the representation. If this
     *     node is not part of the parsed graph, the resulting representation
     *     will be empty
     * @param graph the graph.
     * @return The representation based on the state of the parsed graph
     */
    public RdfRepresentation createRdfRepresentation(IRI node, Graph graph) {
        if (node == null) {
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
        }
        if(graph == null){
            throw new IllegalArgumentException("The parsed graph MUST NOT be NULL!");
        }
        return new RdfRepresentation(node, graph);
    }

    /**
     * Extracts the ImmutableGraph for {@link RdfRepresentation} or creates a {@link ImmutableGraph}
     * for all other implementations of {@link Representation}.
     *
     * @param representation the representation
     * @return the read only RDF ImmutableGraph.
     */
    public RdfRepresentation toRdfRepresentation(Representation representation) {
        if (representation instanceof RdfRepresentation) {
            return (RdfRepresentation) representation;
        } else {
            //create the Clerezza Represenation
            RdfRepresentation clerezzaRep = createRepresentation(representation.getId());
            //Copy all values field by field
            for (Iterator<String> fields = representation.getFieldNames(); fields.hasNext();) {
                String field = fields.next();
                for (Iterator<Object> fieldValues = representation.get(field); fieldValues.hasNext();) {
                    clerezzaRep.add(field, fieldValues.next());
                }
            }
            return clerezzaRep;
        }
    }

}
