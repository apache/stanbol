package org.apache.stanbol.contenthub.it;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.core.MediaType;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.sun.jersey.api.client.ClientResponse.Status;

public class SemanticIndexResourceTest extends StanbolTestBase {

    private static final String defaultPath = "/contenthub/index";

    @Test
    public void testSubmitProgram() throws ClientProtocolException, UnsupportedEncodingException, IOException {
        RequestExecutor test;

        // create an index
        String name = "contenthub_index_ldpath_test_program";
        String program = "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string; country = dbp-ont:country / rdfs:label :: xsd:string; ";
        test = executor.execute(builder.buildPostRequest(defaultPath + "/ldpath")
                .withFormContent("name", name, "description", "test_desc", "program", program)
                .withHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED));
        test.assertStatus(Status.OK.getStatusCode());
        String pid = test.getContent();

        // get the created index
        test = executor.execute(builder.buildGetRequest(defaultPath + "?name=" + name).withHeader("Accept",
            MediaType.APPLICATION_JSON));
        test.assertContentType(MediaType.APPLICATION_JSON);
        try {
            JSONObject content = new JSONObject(test.getContent());
            content.get(name);
        } catch (JSONException e) {
            assertTrue("Failed to get submitted index.", false);
        }

        // delete the created index
        String path = builder.buildUrl(defaultPath + "/ldpath/" + pid);
        test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
        test.assertStatus(Status.OK.getStatusCode());

        // try to get the removed index
        test = executor.execute(builder.buildGetRequest(defaultPath).withHeader("Accept",
            MediaType.APPLICATION_JSON));
        test.assertContentType(MediaType.APPLICATION_JSON);
        try {
            JSONObject content = new JSONObject(test.getContent());
            content.get(name);
            assertTrue("Failed to remove index properly", false);
        } catch (JSONException e) {
            // content.get(name) must throw an exception to pass test.
        }
    }
}