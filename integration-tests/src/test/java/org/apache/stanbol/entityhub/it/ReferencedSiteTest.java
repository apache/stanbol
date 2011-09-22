package org.apache.stanbol.entityhub.it;

import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS;
import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS;
import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_SITE_ID;
import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_SITE_PATH;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertEntity;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertRepresentation;

import java.io.IOException;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.it.query.DbpediaQueryTest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Tests of the ReferencedSite RESTful service of the Entityhub based on the
 * DBpedia.org default data set.<p>
 * Note that the tests for the query interfaces are defined by 
 * {@link DbpediaQueryTest} because they are shared with
 * {@link SitesManagerTest}.
 * @author Rupert Westenthaler
 *
 */
public final class ReferencedSiteTest extends DbpediaQueryTest {

    /**
     * Executes the {@link DbpediaQueryTest} on the 'dbpedia' referenced
     * site (assuming the default dataset
     */
    public ReferencedSiteTest() {
        super(DBPEDIA_SITE_PATH, DBPEDIA_SITE_ID);
    }
    @Override
    protected String getDefaultFindQueryField() {
        return RDFS_LABEL;
    }
    /**
     * Tests retrieval of Entities
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testRetrievel() throws IOException, JSONException {
        String id = "http://dbpedia.org/resource/Paris";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest(DBPEDIA_SITE_PATH+"/entity",
                "id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(200);
        JSONObject jEntity = assertEntity(re.getContent(), id, DBPEDIA_SITE_ID);
        assertRepresentation(jEntity.getJSONObject("representation"), 
            DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS, 
            DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS);

    }

}
