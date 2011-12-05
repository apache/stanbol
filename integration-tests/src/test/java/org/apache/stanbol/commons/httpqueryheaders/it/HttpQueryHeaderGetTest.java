package org.apache.stanbol.commons.httpqueryheaders.it;

import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_SITE_ID;
import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_SITE_PATH;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertEntity;

import java.io.IOException;
import java.util.Collections;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants;
import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

/**
 * Test overriding of Accept headers by using the Entityhub dbpedia.org
 * referenced site
 * @author Rupert Westenthaler
 *
 */
public class HttpQueryHeaderGetTest extends EntityhubTestBase {


    public HttpQueryHeaderGetTest() {
        super(Collections.singleton(DbpediaDefaultdataConstants.DBPEDIA_SITE_ID));
    }
    /**
     * Validates that parsing rdf+xml normally as Accept header and that
     * JSON is the default if no header is parsed. 
     * NOTE: This does not actually test the http query header, but checks 
     * the assumptions behind all the following tests.
     * @throws IOException
     * @throws JSONException 
     */
    @Test
    public void testGetAccept() throws IOException, JSONException {
        //first a normal request with application/rdf+xml
        String id = "http://dbpedia.org/resource/Paris";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id)
            .withHeader("Accept", "application/rdf+xml"));
        re.assertStatus(200);
        re.assertContentType("application/rdf+xml");
        re.assertContentContains(
            "<rdf:Description rdf:about=\"http://dbpedia.org/resource/Paris\">",
            "<rdfs:label xml:lang=\"en\">Paris</rdfs:label>");
        //now test the default Accept
        re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id));
        re.assertContentType("application/json");
        re.assertStatus(200);
        assertEntity(re.getContent(), id, DBPEDIA_SITE_ID);
    }
    @Test 
    public void testSetAccept() throws IOException {
        //first a normal request with application/rdf+xml
        String id = "http://dbpedia.org/resource/Paris";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id,
                "header_Accept","text/rdf+nt")); //parse the rdf+nt format as query parameter
        re.assertStatus(200);
        re.assertContentType("text/rdf+nt");
        re.assertContentContains(
            "<http://dbpedia.org/resource/Paris> " +
            "<http://www.w3.org/2000/01/rdf-schema#label> " +
            "\"Paris\"@en .");
    }
    @Test
    public void testOverrideAccept() throws IOException {
        //first a normal request with application/rdf+xml
        String id = "http://dbpedia.org/resource/Paris";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id,
                "header_Accept","text/rdf+nt") //parse the rdf+nt format as query parameter
            .withHeader("Accept", "application/rdf+xml")); //MUST override the rdf+xml
        re.assertStatus(200);
        re.assertContentType("text/rdf+nt");
        re.assertContentContains(
            "<http://dbpedia.org/resource/Paris> " +
            "<http://www.w3.org/2000/01/rdf-schema#label> " +
            "\"Paris\"@en .");
    }
    @Test
    public void testRemovalOfAccept() throws IOException, JSONException {
        //now test the removal of headers
        //first a normal request with application/rdf+xml
        String id = "http://dbpedia.org/resource/Paris";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id,
                "header_Accept","") //empty value to remove
            .withHeader("Accept", "application/rdf+xml")); //MUST override the rdf+xml
        re.assertStatus(200);
        //The default format (JSON) is expected
        assertEntity(re.getContent(), id, DBPEDIA_SITE_ID);
    }
}
