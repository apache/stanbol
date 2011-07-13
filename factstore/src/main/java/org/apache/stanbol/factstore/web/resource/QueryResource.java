package org.apache.stanbol.factstore.web.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdParser;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.model.Query;
import org.apache.stanbol.factstore.model.FactResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("factstore/query")
public class QueryResource extends BaseStanbolResource {

	private static Logger logger = LoggerFactory.getLogger(QueryResource.class);

	private final FactStore factStore;

	public QueryResource(@Context ServletContext context) {
		this.factStore = ContextHelper.getServiceFromContext(FactStore.class,
				context);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(@QueryParam("q") String q) {
        logger.info("Query for fact: {}", q);
		
		if (this.factStore == null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
					"The FactStore is not configured properly").build();
		}

		if (q == null || q.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No query sent.")
					.build();
		}

		JsonLd jsonLdQuery = null;
		try {
			jsonLdQuery = JsonLdParser.parse(q);
		} catch (Exception e) {
			logger.info("Could not parse query", e);
			return Response.status(Status.BAD_REQUEST).entity(
					"Could not parse query: " + e.getMessage()).build();
		}

		Query query = null;
		try {
			query = Query.toQueryFromJsonLd(jsonLdQuery);
		} catch (Exception e) {
			logger.info("Could not extract Query from JSON-LD", e);
			return Response.status(Status.BAD_REQUEST).entity(
					"Could not extract FactStore query from JSON-LD: "
							+ e.getMessage()).build();
		}

		FactResultSet rs = null;
		try {
			rs = this.factStore.query(query);
		} catch (Exception e) {
			logger.info("Error while performing the query.", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
					"Error while performing the query. " + e.getMessage()).build();
		}

		if (rs != null) {
			return Response.ok(rs.toJSON()).build();
		} else {
			return Response.ok().build();
		}
	}
}
