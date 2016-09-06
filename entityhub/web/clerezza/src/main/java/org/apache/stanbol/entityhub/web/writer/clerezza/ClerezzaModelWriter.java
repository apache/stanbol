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

package org.apache.stanbol.entityhub.web.writer.clerezza;

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TEXT_RDF_NT;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;


import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.fieldquery.FieldQueryToJsonUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
public class ClerezzaModelWriter implements ModelWriter {

    private final Logger log = LoggerFactory.getLogger(ClerezzaModelWriter.class);
    
    /**
     * {@link MediaType} instance for {@link SupportedFormat#TURTLE}
     */
    public static final MediaType TURTLE_TYPE = MediaType.valueOf(SupportedFormat.TURTLE);
    /**
     * {@link MediaType} instance for <code>application/ld+json</code>
     */
    public static final MediaType JSONLD_TYPE = MediaType.valueOf("application/ld+json");
    /**
     * {@link MediaType} instance for {@link SupportedFormat#N3}
     */
    public static final MediaType N3_TYPE = MediaType.valueOf(SupportedFormat.N3);
    /**
     * {@link MediaType} instance for {@link SupportedFormat#RDF_JSON}
     */
    public static final MediaType RDF_JSON_TYPE = MediaType.valueOf(SupportedFormat.RDF_JSON);
    /**
     * {@link MediaType} instance for {@link SupportedFormat#RDF_XML}
     */
    public static final MediaType RDF_XML_TYPE = MediaType.valueOf(SupportedFormat.RDF_XML);
    /**
     * {@link MediaType} instance for {@link SupportedFormat#X_TURTLE}
     */
    public static final MediaType X_TURTLE_TYPE = MediaType.valueOf(SupportedFormat.X_TURTLE);
    
    public static final MediaType N_TRIPLE_TYPE = MediaType.valueOf(SupportedFormat.N_TRIPLE);
    /**
     * Support for the deprecated <code>text/rdf+nt</code> media type
     */
    public static final MediaType TEXT_RDF_NT = MediaType.valueOf(SupportedFormat.TEXT_RDF_NT);
    /**
     * Read-only list of the supported RDF formats
     */
    public static final List<MediaType> SUPPORTED_RDF_TYPES = Collections.unmodifiableList(
        Arrays.asList(TURTLE_TYPE, JSONLD_TYPE, N3_TYPE, N_TRIPLE_TYPE, RDF_JSON_TYPE, RDF_XML_TYPE, X_TURTLE_TYPE, TEXT_RDF_NT));

    //some Concepts and Relations we use to represent Entities
    private final static IRI FOAF_DOCUMENT = new IRI(NamespaceEnum.foaf+"Document");
    private final static IRI FOAF_PRIMARY_TOPIC = new IRI(NamespaceEnum.foaf+"primaryTopic");
    private final static IRI FOAF_PRIMARY_TOPIC_OF = new IRI(NamespaceEnum.foaf+"isPrimaryTopicOf");
    private final static IRI SIGN_SITE = new IRI(RdfResourceEnum.site.getUri());
//    private final static IRI ENTITY_TYPE = new IRI(RdfResourceEnum.Entity.getUri());
    private final static RdfValueFactory valueFactory = RdfValueFactory.getInstance();
    /**
     * The URI used for the query result list (static for all responses)
     */
    private static final IRI QUERY_RESULT_LIST = new IRI(RdfResourceEnum.QueryResultSet.getUri());
    /**
     * The property used for all results
     */
    private static final IRI QUERY_RESULT = new IRI(RdfResourceEnum.queryResult.getUri());
    /**
     * The property used for the JSON serialised FieldQuery (STANBOL-298)
     */
    private static final IRI FIELD_QUERY = new IRI(RdfResourceEnum.query.getUri());

    /**
     * This Serializer only supports UTF-8
     */
    public static final String CHARSET = Charset.forName("UTF-8").toString();
    
    /**
     * The literal factory used (currently {@link LiteralFactory#getInstance()},
     * but we might use a custom one for Stanbol therefore it is better to
     * have it as a field 
     */
    static final LiteralFactory literalFactory = LiteralFactory.getInstance();

    /**
     * The Clerezza {@link Serializer} service
     */
    @Reference
    protected Serializer ser;

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService nsPrefixService; 
    
    @Override
    public Class<? extends Representation> getNativeType() {
        return RdfRepresentation.class;
    }

    @Override
    public List<MediaType> supportedMediaTypes() {
        return SUPPORTED_RDF_TYPES;
    }

    @Override
    public MediaType getBestMediaType(MediaType mediaType) {
        for(MediaType supported : SUPPORTED_RDF_TYPES){
            if(supported.isCompatible(mediaType)){
                return supported;
            }
        }
        return null;
    }

    @Override
    public void write(Representation rep, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        writeRdf(toRDF(rep), out, mediaType);
    }

    @Override
    public void write(Entity entity, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        writeRdf(toRDF(entity),out,mediaType);

    }

    @Override
    public void write(QueryResultList<?> result, OutputStream out, MediaType mediaType) throws WebApplicationException,
            IOException {
        Graph queryRdf = toRDF(result);
        //we need also to the JSON formatted FieldQuery as a literal to the
        //RDF data.
        FieldQuery query = result.getQuery();
        if(query != null){
            try {
                JSONObject fieldQueryJson = FieldQueryToJsonUtils.toJSON(query,
                    nsPrefixService);
                if(fieldQueryJson != null){
                    //add the triple with the fieldQuery
                    queryRdf.add(new TripleImpl(QUERY_RESULT_LIST, FIELD_QUERY, 
                        literalFactory.createTypedLiteral(fieldQueryJson.toString())));
                }
            } catch (JSONException e) {
                log.warn(String.format("Unable to serialize Fieldquery '%s' to JSON! "
                    + "Query response will not contain the serialized query.",
                    query),e);
            }
        }
        //now serialise the data
        writeRdf(queryRdf,out,mediaType);
    }
    
    /**
     * @param tc
     * @param out
     * @param mediaType
     */
    private void writeRdf(Graph tc, OutputStream out, MediaType mediaType) {
        String charset = mediaType.getParameters().get("charset");
        if(charset == null){
            charset = ModelWriter.DEFAULT_CHARSET;
        }
        if(!CHARSET.equalsIgnoreCase(charset)){
            log.warn("Unsupported Charset {} requested (will use {})!",charset,CHARSET);
        }
        ser.serialize(out, tc , new StringBuilder(mediaType.getType())
            .append('/').append(mediaType.getSubtype()).toString());
    }

    private Graph toRDF(Representation representation) {
        Graph graph = new IndexedGraph();
        addRDFTo(graph, representation);
        return graph;
    }

    private void addRDFTo(Graph graph, Representation representation) {
        graph.addAll(valueFactory.toRdfRepresentation(representation).getRdfGraph());
    }

    private Graph toRDF(Entity entity) {
        Graph graph = new IndexedGraph();
        addRDFTo(graph, entity);
        return graph;
    }

    private void addRDFTo(Graph graph, Entity entity) {
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
    private void addEntityTriplesToGraph(Graph graph, Entity sign) {
        IRI id = new IRI(sign.getId());
        IRI metaId = new IRI(sign.getMetadata().getId());
        //add the FOAF triples between metadata and content
        graph.add(new TripleImpl(id, FOAF_PRIMARY_TOPIC_OF, metaId));
        graph.add(new TripleImpl(metaId, FOAF_PRIMARY_TOPIC, metaId));
        graph.add(new TripleImpl(metaId, RDF.type, FOAF_DOCUMENT));
        //add the site to the metadata
        //TODO: this should be the HTTP URI and not the id of the referenced site
        Literal siteName = literalFactory.createTypedLiteral(sign.getSite());
        graph.add(new TripleImpl(metaId, SIGN_SITE, siteName));
        
    }
    
    private Graph toRDF(QueryResultList<?> resultList) {
        final Graph resultGraph;
        Class<?> type = resultList.getType();
        if (String.class.isAssignableFrom(type)) {
            resultGraph = new IndexedGraph(); //create a new ImmutableGraph
            for (Object result : resultList) {
                //add a triple to each reference in the result set
                resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, new IRI(result.toString())));
            }
        } else {
            //first determine the type of the resultList
            final boolean isSignType;
            if (Representation.class.isAssignableFrom(type)) {
                isSignType = false;
            } else if (Representation.class.isAssignableFrom(type)) {
                isSignType = true;
            } else {
                //incompatible type -> throw an Exception
                throw new IllegalArgumentException("Parsed type " + type + " is not supported");
            }
            //special treatment for RdfQueryResultList for increased performance
            if (resultList instanceof RdfQueryResultList) {
                resultGraph = ((RdfQueryResultList) resultList).getResultGraph();
                if (isSignType) { //if we build a ResultList for Signs, that we need to do more things
                    //first remove all triples representing results
                    Iterator<Triple> resultTripleIt = resultGraph.filter(QUERY_RESULT_LIST, QUERY_RESULT, null);
                    while (resultTripleIt.hasNext()) {
                        resultTripleIt.next();
                        resultTripleIt.remove();
                    }
                    //now add the Sign specific triples and add result triples
                    //to the Sign IDs
                    for (Object result : resultList) {
                        IRI signId = new IRI(((Entity) result).getId());
                        addEntityTriplesToGraph(resultGraph, (Entity) result);
                        resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, signId));
                    }
                }
            } else { //any other implementation of the QueryResultList interface
                resultGraph = new IndexedGraph(); //create a new graph
                if (Representation.class.isAssignableFrom(type)) {
                    for (Object result : resultList) {
                        IRI resultId;
                        if (!isSignType) {
                            addRDFTo(resultGraph, (Representation) result);
                            resultId = new IRI(((Representation) result).getId());
                        } else {
                            addRDFTo(resultGraph, (Entity) result);
                            resultId = new IRI(((Entity) result).getId());
                        }
                        //Note: In case of Representation this Triple points to
                        //      the representation. In case of Signs it points to
                        //      the sign.
                        resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, resultId));
                    }
                }
            }
        }
        return resultGraph;
    }
}
