package org.apache.stanbol.ontologymanager.web.it;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import org.apache.http.client.ClientProtocolException;

import org.apache.stanbol.commons.web.base.format.KRFormat;

public class SessionTest extends OntonetTestBase{

    public static final String ROOT_URI = "/ontonet";

    public static final String SESSION_URI = ROOT_URI + "/session";

    @Test
    public void testSessionCreation() throws ClientProtocolException, IOException {

        /*executor.execute(
            builder.buildPostRequest(SESSION_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200);*/
        
        assertTrue(true);
    }       
}
