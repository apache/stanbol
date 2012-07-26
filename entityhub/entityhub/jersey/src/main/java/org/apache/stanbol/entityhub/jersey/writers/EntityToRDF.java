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
package org.apache.stanbol.entityhub.jersey.writers;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Encodes an Entity into an single RDF graph by<ul>
 * <li> adding the {@link Entity#getRepresentation()}
 * <li> adding the {@link Entity#getMetadata()}
 * <li> creating a foaf:primaryTopic link between the metadata and the representation
 * <li> creating a foaf:isPromaryTopic link between the representation and the metadata
 * <li> adding the foaf:Document type to the metadata.
 * </ul>
 * @author Rupert Westenthaler
 *
 */
final class EntityToRDF {
    private EntityToRDF() { /* do not create instances of utility classes */}

    private final static UriRef FOAF_DOCUMENT = FOAF.Document;
    private final static UriRef FOAF_PRIMARY_TOPIC = FOAF.primaryTopic;
    private final static UriRef FOAF_PRIMARY_TOPIC_OF = FOAF.isPrimaryTopicOf;
    private final static UriRef signSite = new UriRef(RdfResourceEnum.site.getUri());
    private final static UriRef ENTITY_TYPE = new UriRef(RdfResourceEnum.Entity.getUri());
    private final static RdfValueFactory valueFactory = RdfValueFactory.getInstance();
    /**
     * The literal factory used (currently {@link LiteralFactory#getInstance()},
     * but we might use a custom one for Stanbol therefore it is better to
     * have it as a field 
     */
    static final LiteralFactory literalFactory = LiteralFactory.getInstance();

    static MGraph toRDF(Representation representation) {
        MGraph graph = new IndexedMGraph();
        addRDFTo(graph, representation);
        return graph;
    }

    static void addRDFTo(MGraph graph, Representation representation) {
        graph.addAll(valueFactory.toRdfRepresentation(representation).getRdfGraph());
    }

    static TripleCollection toRDF(Entity entity) {
        MGraph graph = new IndexedMGraph();
        addRDFTo(graph, entity);
        return graph;
    }

    static void addRDFTo(MGraph graph, Entity entity) {
        addRDFTo(graph, entity.getRepresentation());
        addRDFTo(graph, entity.getMetadata());
        //now add some triples that represent the Sign
        addEntityTriplesToGraph(graph, entity);
    }


    /**
     * Adds the Triples that represent the Sign to the parsed graph. Note that
     * this method does not add triples for the representation. However it adds
     * the triple (sign,singRepresentation,representation)
     *
     * @param graph the graph to add the triples
     * @param sign the sign
     */
    static void addEntityTriplesToGraph(MGraph graph, Entity sign) {
        UriRef id = new UriRef(sign.getId());
        UriRef metaId = new UriRef(sign.getMetadata().getId());
        //add the FOAF triples between metadata and content
        graph.add(new TripleImpl(id, FOAF_PRIMARY_TOPIC_OF, metaId));
        graph.add(new TripleImpl(metaId, FOAF_PRIMARY_TOPIC, metaId));
        graph.add(new TripleImpl(metaId, RDF.type, FOAF_DOCUMENT));
        //add the site to the metadata
        //TODO: this should be the HTTP URI and not the id of the referenced site
        TypedLiteral siteName = literalFactory.createTypedLiteral(sign.getSite());
        graph.add(new TripleImpl(metaId, EntityToRDF.signSite, siteName));
        
    }

}
