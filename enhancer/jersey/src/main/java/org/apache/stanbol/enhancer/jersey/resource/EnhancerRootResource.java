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
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.JSON_LD;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.addActiveChains;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.addActiveEngines;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.rdf.Enhancer;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/**
 * RESTful interface to browse the list of available engines and allow to call
 * them in a stateless, synchronous way.
 * <p>
 * If you need the content of the extractions to be stored on the server, use
 * the StoreRootResource API instead.
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/enhancer")
public final class EnhancerRootResource extends BaseStanbolResource {

    @Reference
    private EnhancementJobManager jobManager;
    @Reference
    private EnhancementEngineManager engineManager;
    @Reference
    private ChainManager chainManager;
    @Reference
    private ContentItemFactory ciFactory;
    @Reference
    private Serializer serializer;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private QueryEngine queryEngine;
    
    @Path("")
    public EnhancerResource get() {
        return new EnhancerResource(jobManager, engineManager, 
                chainManager, ciFactory, serializer, getLayoutConfiguration(),
                getUriInfo());
    }
            
          

    public class EnhancerResource extends GenericEnhancerUiResource {
        public EnhancerResource(
            EnhancementJobManager jobManager, 
            EnhancementEngineManager engineManager, 
            ChainManager chainManager, 
            ContentItemFactory ciFactory,
            Serializer serializer,
            LayoutConfiguration layoutConfiguration, 
            UriInfo uriInfo) {
            super(null, jobManager, engineManager, chainManager, ciFactory, 
                    serializer, layoutConfiguration, uriInfo);
        }

        @GET
        @Produces(value = {JSON_LD, APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON, RDF_XML, TURTLE, X_TURTLE})
        public Response getEngines(@Context HttpHeaders headers) {
            Graph graph = getEnhancerConfigGraph();
            ResponseBuilder res = Response.ok(graph);
            //addCORSOrigin(servletContext,res, headers);
            return res.build();
        }

        /**
         * Creates the RDF graph for the current Stanbol Enhancer configuration
         *
         * @return the graph with the configuration
         */
        private Graph getEnhancerConfigGraph() {
            String rootUrl = getUriInfo().getBaseUriBuilder().path(getRootUrl()).build().toString();
            IRI enhancerResource = new IRI(rootUrl + "enhancer");
            Graph graph = new SimpleGraph();
            graph.add(new TripleImpl(enhancerResource, RDF.type, Enhancer.ENHANCER));
            addActiveEngines(engineManager, graph, rootUrl);
            addActiveChains(chainManager, graph, rootUrl);
            return graph;
        }

        @GET
        @Path("/sparql")
        @Consumes(APPLICATION_FORM_URLENCODED)
        @Produces({TEXT_HTML + ";qs=2", "application/sparql-results+xml", "application/rdf+xml", APPLICATION_XML})
        public Object sparql(@QueryParam(value = "query") String sparqlQuery,
                @Context HttpHeaders headers) throws ParseException {
            if (sparqlQuery == null) {
                return Response.ok(new Viewable("sparql", EnhancerRootResource.this), TEXT_HTML).build();
            }
            final Query query = QueryParser.getInstance().parse(sparqlQuery);
            String mediaType = "application/sparql-results+xml";
            if (query instanceof DescribeQuery || query instanceof ConstructQuery) {
                mediaType = "application/rdf+xml";
            }
            ResponseBuilder responseBuilder;
            if (queryEngine != null) {
                Object result = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return queryEngine.execute(null, getEnhancerConfigGraph(), query);
                    }
                });
                responseBuilder = Response.ok(result, mediaType);
            } else {
                responseBuilder = Response.status(Status.SERVICE_UNAVAILABLE)
                        .entity("No SPRAQL query engine available");
            }
            //    addCORSOrigin(servletContext, responseBuilder, headers);
            return responseBuilder.build();
        }

        @POST
        @Path("/sparql")
        @Consumes(APPLICATION_FORM_URLENCODED)
        @Produces({"application/sparql-results+xml", "application/rdf+xml", APPLICATION_XML})
        public Object postSparql(@FormParam("query") String sparqlQuery,
                @Context HttpHeaders headers) throws ParseException {
            return sparql(sparqlQuery, headers);
        }
    }
}
