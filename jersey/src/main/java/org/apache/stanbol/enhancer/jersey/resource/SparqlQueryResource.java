package org.apache.stanbol.enhancer.jersey.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.SparqlQueryEngine.SparqlQueryEngineException;

import com.sun.jersey.api.view.Viewable;


import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

/**
 * Implementation of a SPARQL endpoint as defined by the W3C:
 *
 * http://www.w3.org/TR/rdf-sparql-protocol/
 *
 * (not 100% compliant yet, please report bugs/missing features in the issue
 * tracker).
 *
 * If the "query" parameter is not present, then fallback to display and HTML
 * view with an ajax-ified form to test the SPARQL endpoint from the browser.
 */
@Path("/sparql")
public class SparqlQueryResource extends NavigationMixin {

    protected Store store;

    protected TcManager tcManager;

    public SparqlQueryResource(@Context ServletContext servletContext) {
        tcManager = (TcManager) servletContext.getAttribute(TcManager.class.getName());
        store = (Store) servletContext.getAttribute(Store.class.getName());
    }

    @GET
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces( { TEXT_HTML + ";qs=2",
            "application/sparql-results+xml", "application/rdf+xml",
            APPLICATION_XML })
    public Object sparql(@QueryParam(value = "query") String sparqlQuery,
            @Deprecated @QueryParam(value = "q") String q)
            throws SparqlQueryEngineException, ParseException {
        if (q != null) {
            // compat with old REST API that was not respecting the SPARQL RDF
            // protocol
            sparqlQuery = q;
        }
        if (sparqlQuery == null) {
            return Response.ok(new Viewable("index", this), TEXT_HTML).build();
        }
        Query query = QueryParser.getInstance().parse(sparqlQuery);
        String mediaType = "application/sparql-results+xml";
        if (query instanceof DescribeQuery || query instanceof ConstructQuery) {
            mediaType = "application/rdf+xml";
        }
        Object result = tcManager.executeSparqlQuery(query,
                store.getEnhancementGraph());
        return Response.ok(result, mediaType).build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces( { "application/sparql-results+xml", "application/rdf+xml",
            APPLICATION_XML })
    public Object postSparql(@FormParam("query") String sparqlQuery)
            throws SparqlQueryEngineException, ParseException {
        return sparql(sparqlQuery, null);
    }

}
