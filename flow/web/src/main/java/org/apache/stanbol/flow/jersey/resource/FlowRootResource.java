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
package org.apache.stanbol.flow.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.flow.jersey.utils.EnhancerUtils.addActiveChains;
import static org.apache.stanbol.flow.jersey.utils.EnhancerUtils.addActiveEngines;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.SparqlQueryEngine.SparqlQueryEngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.Enhancer;

import com.sun.jersey.api.view.Viewable;


/**
 * RESTful interface to browse the list of available engines and allow to call them in a stateless,
 * synchronous way.
 * <p>
 * If you need the content of the extractions to be stored on the server, use the StoreRootResource API
 * instead.
 */
@Path("/flow")
public final class FlowRootResource extends AbstractEnhancerUiResource {

    public FlowRootResource(@Context ServletContext context){
        super(null,context);
    }
    
    @GET
    @Produces(value={APPLICATION_JSON,N3,N_TRIPLE,RDF_JSON,RDF_XML,TURTLE,X_TURTLE})
    public Response getEngines(@Context HttpHeaders headers){
        MGraph graph = getEnhancerConfigGraph();
        ResponseBuilder res = Response.ok(graph);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    /**
     * Creates the RDF graph for the current Stanbol Enhancer configuration
     * @return the graph with the configuration
     */
    private MGraph getEnhancerConfigGraph() {
        String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
        UriRef enhancerResource = new UriRef(rootUrl+"enhancer");
        MGraph graph = new SimpleMGraph();
        graph.add(new TripleImpl(enhancerResource, RDF.type, Enhancer.ENHANCER));
        addActiveEngines(engineManager, graph, rootUrl);
        addActiveChains(chainManager, graph, rootUrl);
        return graph;
    }
    
    @POST
    @Path("{flowGraph}")
    @Consumes(WILDCARD)
    public Response enhanceFromData(byte[] data,
                                    @QueryParam(value = "uri") String uri,
                                    @Context HttpHeaders headers,
                                    @PathParam("flowGraph") final String flowGraph) throws EnhancementException, IOException {
        String format = TEXT_PLAIN;
        if (headers.getMediaType() != null) {
            format = headers.getMediaType().toString();
        }
        if (uri != null && uri.isEmpty()) {
            // let the store build an internal URI based on the content
            uri = null;
        }
        ContentItem ci = new InMemoryContentItem(uri, data, format);
        if (jobManager != null) {
        	/**
        	 * TODO : That's actually an hack for use the jobmanager.enhanceContent() interface.
        	 * That's not really a chain that we want to pass but an flowgraph...
        	 * May we have to differ from the jobmanager impl... but not too much could be cool...
        	 * At the end the flow graph can be able to call engines://, chain:// and store://  
        	 */
        	Chain ch = new Chain() {
				
				@Override
				public String getName() {
					return flowGraph;
				}
				
				@Override
				public Graph getExecutionPlan() throws ChainException {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public Set<String> getEngines() throws ChainException {
					// TODO Auto-generated method stub
					return null;
				}
			};
            //jobManager.enhanceContent(ci, getChain());
			jobManager.enhanceContent(ci, ch);
        }
        ResponseBuilder rb = Response.ok(ci);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) {
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    
    /*
    TODO :
    A method that return the list of endpoints of a context from this method camelContext.getEndpoints()
    optionaly manage differents context and allow definition of differents context. 
    }*/

}
