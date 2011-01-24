package org.apache.stanbol.entityhub.site.linkedData.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;

import javax.ws.rs.core.UriBuilder;

public class SparqlEndpointUtils {

    public static final String SPARQL_RESULT_JSON = "application/sparql-results+json";
    public SparqlEndpointUtils() {/* Do not create instances of utility classes*/}

    /**
     * Sends an SPARQL Request to the accessUri. Please note that based on the
     * type of the SPARQL query different content are supported by the Site
     * @param accessUri the uri of the SPARQL endpoint
     * @param contentType the contentType of the returned RDF graph
     * @param query the SPARQL Construct query
     * @return the results as input stream
     * @throws IOException
     * @throws MalformedURLException
     */
    public static InputStream sendSparqlRequest(String accessUri, String query, String contentType) throws IOException, MalformedURLException {
        final URI dereferenceUri = UriBuilder.fromUri(accessUri)
            .queryParam("query", "{query}")
            .queryParam("format", "{format}")
            .build(query.toString(), contentType);
        final URLConnection con = dereferenceUri.toURL().openConnection();
        con.addRequestProperty("Accept", contentType);
        return con.getInputStream();
    }

}
