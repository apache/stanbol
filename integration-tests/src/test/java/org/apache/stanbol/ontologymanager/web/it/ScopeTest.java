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

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;

import org.apache.stanbol.commons.web.base.format.KRFormat;


public class ScopeTest extends StanbolTestBase{

    public static final String ROOT_URI = "/ontonet";

    public static final String ONTOLOGY_URI = ROOT_URI + "/ontology";

    public static final String ONT_FOAF_URI = "http://xmlns.com/foaf/spec/index.rdf";

    public static final String ONT_PIZZA_URI = "http://www.co-ode.org/ontologies/pizza/2007/02/12/pizza.owl";

    public static final String ONT_WINE_URI = "http://www.schemaweb.info/webservices/rest/GetRDFByID.aspx?id=62";

    public static final String REG_TEST_URI = "http://www.ontologydesignpatterns.org/registry/krestest.owl";

    public static final String SCOPE_BIZ_URI = ONTOLOGY_URI + "/" + "Biz";

    public static final String SCOPE_DRUNK_URI = ONTOLOGY_URI + "/" + "Drunk";

    public static final String SCOPE_USER_URI = ONTOLOGY_URI + "/" + "User";

    public static final String SCOPE1_URI = ONTOLOGY_URI + "/" + "Pippo%20Baudo";

    public static final String SCOPE2_URI = ONTOLOGY_URI + "/" + "TestScope2";

    private static final Logger log = LoggerFactory.getLogger(ScopeTest.class);

    @Test
    public void testGetScopes() throws ClientProtocolException, IOException {
        RequestExecutor request;

        // RDF_XML format
        request = executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        );

        request
        .assertStatus(200)
        .assertContentContains("<imports rdf:resource=\"http://www.ontologydesignpatterns.org/schemas/meta.owl\"/>");

        log.debug("Request: "+ONTOLOGY_URI+"\n"+request.getContent());

        // TURTLE format
        request = executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI)
            .withHeader("Accept",KRFormat.TURTLE)
        );

        request
        .assertStatus(200)
        .assertContentContains("owl:imports <http://www.ontologydesignpatterns.org/schemas/meta.owl>");
        
        log.debug("Request: "+ONTOLOGY_URI+"\n"+request.getContent());

    }

    @Test
    public void testRemoval() throws Exception {

        executor.execute(
            builder.buildOtherRequest( 
                new HttpPut(
                    builder.buildUrl(SCOPE_DRUNK_URI+"?corereg=" + encodeURI(REG_TEST_URI))))
        );

        executor.execute(
            builder.buildOtherRequest( 
                new HttpDelete(
                    builder.buildUrl(SCOPE_DRUNK_URI+"?ontology=" + encodeURI(ONT_WINE_URI))
                )
            )
        );
    }

    @Test
    public void testLocking() throws Exception {
        // Create a scope with a core ontology and a custom registry.
        executor.execute(
            builder.buildOtherRequest( 
                new HttpPut(
                    builder.buildUrl(SCOPE2_URI+"?customont="+encodeURI(ONT_PIZZA_URI)+"&corereg=" + encodeURI(REG_TEST_URI))
                )
            )
        );

        executor.execute(
            builder.buildPostRequest(SCOPE2_URI +
                "?location" + encodeURI(ONT_PIZZA_URI) +
            "&registry=false")
        );

        // get in RDF_XML format
        executor.execute(
            builder.buildGetRequest(SCOPE2_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200);

    }


    /**
     * Tests that the creation of active and inactive scopes is reflected in the RDF version of the scope set,
     * whether it is set to display all scopes or only the active ones.
     */
    @Test
    public void testActiveVsAll() throws Exception {

        // The needed Web resources to GET from.
        executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200);
        log.info("Request: "+ONTOLOGY_URI +" ... DONE");

        executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI,"with-inactive","true")
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200);
        log.info("Request: "+ONTOLOGY_URI+"?with-inactive=true"+" ... DONE");

        // Put a simple, inactive scope.
        executor.execute(
            builder.buildOtherRequest( 
                new HttpPut(
                    builder.buildUrl(SCOPE_USER_URI+"?coreont=" + encodeURI(ONT_FOAF_URI))))
        );
        log.info("Request: "+SCOPE_USER_URI+"?coreont=" + ONT_FOAF_URI+" ... DONE");

        // Check that it is in the list of all scopes.
        executor.execute(
            builder.buildGetRequest(SCOPE_USER_URI,"with-inactive","true")
        ).assertStatus(200).assertContentContains(SCOPE_USER_URI);

        // Check that it is not in the list of active scopes.
        executor.execute(
            builder.buildGetRequest(SCOPE_USER_URI)
        ).assertStatus(200).assertContentContains(SCOPE_USER_URI);

        // Now create a scope that is active on startup.
        executor.execute(
            builder.buildOtherRequest( 
                new HttpPut(
                    builder.buildUrl(SCOPE_BIZ_URI+"?activate=true&coreont=" + encodeURI(ONT_PIZZA_URI))))
        );
        log.info("Request: "+SCOPE_BIZ_URI+"?activate=true&coreont=" + ONT_PIZZA_URI+" ... DONE");

        // Check that it appears in both sets.
        executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200).assertContentContains(SCOPE_BIZ_URI);
        
        executor.execute(
            builder.buildGetRequest(ONTOLOGY_URI,"with-inactive","true")
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200).assertContentContains(SCOPE_BIZ_URI);

    }


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

    private char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0) return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

}
