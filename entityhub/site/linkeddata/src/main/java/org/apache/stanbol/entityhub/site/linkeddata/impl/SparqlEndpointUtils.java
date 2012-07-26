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
package org.apache.stanbol.entityhub.site.linkeddata.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SparqlEndpointUtils {
    private SparqlEndpointUtils() {/*
                                    * Do not create instances of utility classes
                                    */
    }

    private static final Logger log = LoggerFactory.getLogger(SparqlEndpointUtils.class);
    public static final String SPARQL_RESULT_JSON = "application/sparql-results+json";

    /**
     * Sends an SPARQL Request to the accessUri. Please note that based on the type of the SPARQL query
     * different content are supported by the Site
     * 
     * @param accessUri
     *            the uri of the SPARQL endpoint
     * @param contentType
     *            the contentType of the returned RDF graph
     * @param query
     *            the SPARQL Construct query
     * @return the results as input stream
     * @throws IOException
     * @throws MalformedURLException
     */
    public static InputStream sendSparqlRequest(String accessUri, String query, String contentType) 
            throws IOException, MalformedURLException {


        log.trace("Sending SPARQL request [accessUri :: {}][query :: {}][contentType :: {}].", 
            new Object[]{accessUri, query,contentType});

        final URI dereferenceUri = UriBuilder.fromUri(accessUri).queryParam("query", "{query}")
                .queryParam("format", "{format}").build(query, contentType);
        final URLConnection con = dereferenceUri.toURL().openConnection();
        con.addRequestProperty("Accept", contentType);
        try {
            return con.getInputStream();
        } catch (IOException e) {
            if (con instanceof HttpURLConnection) {
                // try to create a better Error Message
                InputStream reason = ((HttpURLConnection) con).getErrorStream();
                if (reason != null) {
                    String errorMessage = null;
                    try {
                        errorMessage = IOUtils.toString(reason);
                    } catch (IOException e1) {
                        // ignore ...
                    }
                    IOUtils.closeQuietly(reason);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        throw new IOException(((HttpURLConnection) con).getRequestMethod()
                                              + " with Content: \n" + errorMessage, e);
                    }
                }
            }
            // if still here re-throw the original exception
            throw e;
        }
    }
}
