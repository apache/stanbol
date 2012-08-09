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
package org.apache.stanbol.ontologymanager.web.it;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeTest extends StanbolTestBase {

    private static final String _ROOT_URI = "/ontonet";

    private static final String URI_SCOPE_CLASS = "http://stanbol.apache.org/ontology/meta/ontonet#Scope";
    private static final String BASE_SCOPES_URI = _ROOT_URI + "/ontology";

    private static final Logger log = LoggerFactory.getLogger(ScopeTest.class);

    private String encodeURI(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            } else o.append(ch);
        }
        return o.toString();
    }

    private boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0) return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

    @Test
    public void testActive() throws Exception {
        RequestExecutor request;

        String tempActiveScopeUri = BASE_SCOPES_URI + "/" + getClass().getCanonicalName() + "-testActive-"
                                    + System.currentTimeMillis() + "-active";
        String tempInactiveScopeUri = BASE_SCOPES_URI + "/" + getClass().getCanonicalName() + "-testActive-"
                                      + System.currentTimeMillis() + "-inactive";

        // Scopes should not be there
        request = executor.execute(builder.buildGetRequest(tempActiveScopeUri).withHeader("Accept",
            KRFormat.TURTLE));
        request.assertStatus(404);
        log.info("Request: " + tempActiveScopeUri + " (should return 404) ... DONE");
        request = executor.execute(builder.buildGetRequest(tempInactiveScopeUri).withHeader("Accept",
            KRFormat.TURTLE));
        request.assertStatus(404);
        log.info("Request: " + tempInactiveScopeUri + " (should return 404) ... DONE");

        // Create scopes, only activate one
        executor.execute(builder.buildOtherRequest(new HttpPut(builder.buildUrl(tempActiveScopeUri
                                                                                + "?activate=true"))));
        log.info("PUT Request: " + tempActiveScopeUri + " ... DONE");
        executor.execute(builder.buildOtherRequest(new HttpPut(builder.buildUrl(tempInactiveScopeUri))));
        log.info("PUT Request: " + tempInactiveScopeUri + " ... DONE");

        // By default, we should only see the active scope
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.TURTLE))
                .assertStatus(200)
                .assertContentRegexp(false,
                    tempInactiveScopeUri + ">\\s+rdf:type\\s+<" + URI_SCOPE_CLASS + ">")
                .assertContentRegexp(true, tempActiveScopeUri + ">\\s+rdf:type\\s+<" + URI_SCOPE_CLASS + ">");
        log.info("Request: " + BASE_SCOPES_URI + " ... DONE");

        // Using with-inactive we should see both scopes
        executor.execute(
            builder.buildGetRequest(BASE_SCOPES_URI + "?with-inactive=true").withHeader("Accept",
                KRFormat.TURTLE))
                .assertStatus(200)
                .assertContentRegexp(true,
                    tempInactiveScopeUri + ">\\s+rdf:type\\s+<" + URI_SCOPE_CLASS + ">")
                .assertContentRegexp(true, tempActiveScopeUri + ">\\s+rdf:type\\s+<" + URI_SCOPE_CLASS + ">");
        log.info("Request: " + BASE_SCOPES_URI + " ... DONE");

        // Delete scopes
        executor.execute(builder.buildOtherRequest(new HttpDelete(builder.buildUrl(tempActiveScopeUri))));
        log.info("DELETE Request: " + tempActiveScopeUri + " ... DONE");
        executor.execute(builder.buildOtherRequest(new HttpDelete(builder.buildUrl(tempInactiveScopeUri))));
        log.info("DELETE Request: " + tempInactiveScopeUri + " ... DONE");

        // We won't test here if deletion succeeded.
    }

    @Test
    public void testCRUD() throws Exception {
        RequestExecutor request;

        // The needed Web resources to GET from.
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.TURTLE))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " ... DONE");

        String tempScopeUri = BASE_SCOPES_URI + "/" + getClass().getCanonicalName() + "-"
                              + System.currentTimeMillis();

        // Scope should not be there
        request = executor.execute(builder.buildGetRequest(tempScopeUri)
                .withHeader("Accept", KRFormat.TURTLE));
        request.assertStatus(404);
        log.info("Request: " + tempScopeUri + " (should return 404) ... DONE");

        // Create scope
        executor.execute(builder.buildOtherRequest(new HttpPut(builder.buildUrl(tempScopeUri))));
        log.info("PUT Request: " + tempScopeUri + " ... DONE");

        // Scope should be there now
        request = executor.execute(builder.buildGetRequest(tempScopeUri)
                .withHeader("Accept", KRFormat.TURTLE));
        request.assertStatus(200).assertContentContains(tempScopeUri);
        log.info("Request: " + tempScopeUri + " ... DONE");
        
        // TODO the U of CRUD

        // Delete scope
        executor.execute(builder.buildOtherRequest(new HttpDelete(builder.buildUrl(tempScopeUri))));
        log.info("DELETE Request: " + tempScopeUri + " ... DONE");

        // Scope should not be there
        request = executor.execute(builder.buildGetRequest(tempScopeUri)
                .withHeader("Accept", KRFormat.TURTLE));
        request.assertStatus(404);
        log.info("Request: " + tempScopeUri + " (should return 404) ... DONE");
    }

    @Test
    public void testLocking() throws Exception {
        // TODO first we need some offline content to POST
    }

    @Test
    public void testSupportedOWLFormats() throws Exception {
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.OWL_XML))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.OWL_XML + ")" + " ... DONE");
        executor.execute(
            builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.MANCHESTER_OWL))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.MANCHESTER_OWL + ")" + " ... DONE");
        executor.execute(
            builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.FUNCTIONAL_OWL))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.FUNCTIONAL_OWL + ")" + " ... DONE");
    }

    @Test
    public void testSupportedRDFFormats() throws Exception {
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.RDF_XML))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.RDF_XML + ")" + " ... DONE");
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.RDF_JSON))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.RDF_JSON + ")" + " ... DONE");
        executor.execute(builder.buildGetRequest(BASE_SCOPES_URI).withHeader("Accept", KRFormat.TURTLE))
                .assertStatus(200);
        log.info("Request: " + BASE_SCOPES_URI + " (Accept: " + KRFormat.TURTLE + ")" + " ... DONE");
    }

    private char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

}
