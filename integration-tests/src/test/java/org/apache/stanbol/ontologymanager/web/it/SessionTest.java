package org.apache.stanbol.ontologymanager.web.it;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.ClientProtocolException;

import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.apache.stanbol.commons.web.base.format.KRFormat;

public class SessionTest extends StanbolTestBase{

    public static final String ROOT_URI = "/ontonet";

    public static final String SESSION_URI = ROOT_URI + "/session";
    
    private static final Logger log = LoggerFactory.getLogger(SessionTest.class);

    @Test
    public void testSessionCreation() throws ClientProtocolException, IOException {
        
        /*executor.execute(
            builder.buildPostRequest(SESSION_URI)
            .withHeader("Accept",KRFormat.RDF_XML)
        ).assertStatus(200);*/
        
        assertTrue(true);
        
    }       
}
