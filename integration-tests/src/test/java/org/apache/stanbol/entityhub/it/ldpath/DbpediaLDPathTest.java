package org.apache.stanbol.entityhub.it.ldpath;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.Response.Status;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.stanbol.entityhub.it.ReferencedSiteTest;
import org.apache.stanbol.entityhub.it.SitesManagerTest;
import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.apache.stanbol.entityhub.test.query.FieldQueryTestCase;
import org.apache.stanbol.entityhub.test.query.FindQueryTestCase;
import org.apache.stanbol.entityhub.test.query.QueryTestBase;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.slf4j.LoggerFactory;
/**
 * Tests for the "/query" (Field Query) and "/find" (label search) 
 * implementation of the Entityhub.<p>
 * All the tests defined by this class assume the default data set for 
 * dbpedia as provided by the 
 * <code>org.apache.stanbol.data.sites.dbpedia.default</code> bundle. <p>
 * This test cases are used to test both the ReferencedSiteManager and 
 * the ReferencedSite. This is also the reason why having this abstract super
 * class defining the tests.
 * @see ReferencedSiteTest
 * @see SitesManagerTest
 * @author Rupert Westenthaler
 *
 */
public class DbpediaLDPathTest extends EntityhubTestBase {

    
    public DbpediaLDPathTest() {
        super(Collections.singleton("dbpedia"));
    }
    @Test
    public void testNoContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testEmptyContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;",
                "context","")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;",
                "context",null)
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testNoLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testEmptyLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath",null)
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath","")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testIllegalLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //missing semicolon
                "ldpath","name = rdfs:label[@en] :: xsd:string")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //unknown namespace prefix
                "ldpath","name = dct:subject :: xsd:anyURI;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //unknown dataType prefix
                "ldpath","name = rdfs:label[@en] :: xsd:String;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testMultipleContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "context","http://dbpedia.org/resource/London",
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentContains(
            "\"@literal\": \"Paris\"",
            "\"@literal\": \"London\"");
    }
    @Test
    public void testUnknownContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/ThisEntityDoesNotExist_ForSure_49283",
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentContains("{","}");
    }
    @Test
    public void testLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath","@prefix dct : <http://purl.org/dc/terms/subject/> ;" +
                    "@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;" +
                    "name = rdfs:label[@en] :: xsd:string;" +
                    "labels = rdfs:label :: xsd:string;" +
                    "comment = rdfs:comment[@en] :: xsd:string;" +
                    "categories = dct:subject :: xsd:anyURI;" +
                    "homepage = foaf:homepage :: xsd:anyURI;" +
                    "location = fn:concat(\"[\",geo:lat,\",\",geo:long,\"]\") :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentType("application/json")
        .assertContentContains(
            "\"@subject\": \"http://dbpedia.org/resource/Paris\"",
            "\"comment\": {",
            "Paris is the capital and largest city in France",
            "\"homepage\": [",
            "http://www.paris.fr",
            "\"labels\": [",
            "\"@literal\": \"Pariisi\",",
            "\"@literal\": \"巴黎\",",
            "\"location\": \"[48.856667,2.350833]\",",
            "\"name\": {",
            "\"@literal\": \"Paris\","
            );
    }
    
}
