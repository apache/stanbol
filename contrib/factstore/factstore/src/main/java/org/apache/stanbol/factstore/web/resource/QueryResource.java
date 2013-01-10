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
package org.apache.stanbol.factstore.web.resource;

import static javax.ws.rs.HttpMethod.POST;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdParser;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.CorsHelper;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.FactResultSet;
import org.apache.stanbol.factstore.model.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("factstore/query")
public class QueryResource extends BaseFactStoreResource {

	private static Logger logger = LoggerFactory.getLogger(QueryResource.class);

    private final FactStore factStore;

    public QueryResource(@Context ServletContext context) {
        this.factStore = ContextHelper.getServiceFromContext(FactStore.class, context);
    }

    @OPTIONS
    public Response handleCorsPreflightQuery(@Context HttpHeaders requestHeaders) {
        ResponseBuilder res = Response.ok();
        CorsHelper.enableCORS(servletContext, res, requestHeaders, POST);
        return res.build();
    }
    
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(String queryString, @Context HttpHeaders requestHeaders) {
        logger.info("Query for fact: {}", queryString);
		
		if (this.factStore == null) {
		    ResponseBuilder rb = Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                    "The FactStore is not configured properly");
		    CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}

		if (queryString == null || queryString.isEmpty()) {
		    ResponseBuilder rb = Response.status(Status.BAD_REQUEST).entity("No query sent.");
		    CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}

		JsonLd jsonLdQuery = null;
		try {
			jsonLdQuery = JsonLdParser.parse(queryString);
		} catch (Exception e) {
			logger.info("Could not parse query", e);
			ResponseBuilder rb = Response.status(Status.BAD_REQUEST).entity(
                "Could not parse query: " + e.getMessage());
			CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}

		Query query = null;
		try {
			query = Query.toQueryFromJsonLd(jsonLdQuery);
		} catch (Exception e) {
			logger.info("Could not extract Query from JSON-LD", e);
            ResponseBuilder rb = Response.status(Status.BAD_REQUEST).entity(
                "Could not extract FactStore query from JSON-LD: " + e.getMessage());
            CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}

		FactResultSet rs = null;
		try {
			rs = this.factStore.query(query);
		} catch (Exception e) {
			logger.info("Error while performing the query.", e);
            ResponseBuilder rb = Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                "Error while performing the query. " + e.getMessage());
			CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}

		if (rs != null) {
		    ResponseBuilder rb = Response.ok(rs.toJSON());
		    CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		} else {
		    ResponseBuilder rb = Response.ok();
		    CorsHelper.addCORSOrigin(servletContext, rb, requestHeaders);
			return rb.build();
		}
	}
}
