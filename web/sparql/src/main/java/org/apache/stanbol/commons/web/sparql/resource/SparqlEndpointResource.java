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
package org.apache.stanbol.commons.web.sparql.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.enhancer.servicesapi.SparqlQueryEngine.SparqlQueryEngineException;

import com.sun.jersey.api.view.Viewable;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

/**
 * Implementation of a SPARQL endpoint as defined by the W3C:
 * 
 * http://www.w3.org/TR/rdf-sparql-protocol/
 * 
 * (Might not be 100% compliant yet, please report bugs/missing features in the issue tracker).
 * 
 * If the "query" parameter is not present, then fallback to display and HTML view with an ajax-ified form to
 * test the SPARQL endpoint from the browser.
 */
@Path("/sparql")
public class SparqlEndpointResource extends BaseStanbolResource {

    protected Store store;

    protected TcManager tcManager;

    public SparqlEndpointResource(@Context ServletContext ctx) {
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, ctx);
        store = ContextHelper.getServiceFromContext(Store.class, ctx);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @GET
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({TEXT_HTML + ";qs=2", "application/sparql-results+xml", "application/rdf+xml", APPLICATION_XML})
    public Response sparql(@QueryParam(value = "query") String sparqlQuery, @Context HttpHeaders headers) throws SparqlQueryEngineException,
                                                                                                         ParseException {
        if (sparqlQuery == null) {
            return Response.ok(new Viewable("index", this), TEXT_HTML).build();
        }
        Query query = QueryParser.getInstance().parse(sparqlQuery);
        String mediaType = "application/sparql-results+xml";
        if (query instanceof DescribeQuery || query instanceof ConstructQuery) {
            mediaType = "application/rdf+xml";
        }
        // TODO: remove dependency on the "store" service and make it possible to select the default graph
        // instead
        Object result = tcManager.executeSparqlQuery(query, store.getEnhancementGraph());
        ResponseBuilder rb = Response.ok(result, mediaType);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({"application/sparql-results+xml", "application/rdf+xml", APPLICATION_XML})
    public Response postSparql(@FormParam("query") String sparqlQuery, @Context HttpHeaders headers) throws SparqlQueryEngineException,
                                                                                                    ParseException {
        return sparql(sparqlQuery, headers);
    }

}
