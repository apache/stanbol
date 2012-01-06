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
package org.apache.stanbol.entityhub.it;

import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS;
import static org.apache.stanbol.entityhub.it.DbpediaDefaultdataConstants.DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertEntity;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertRepresentation;

import java.io.IOException;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.it.query.DbpediaQueryTest;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Tests of the ReferencedSiteManager RESTful service of the Entityhub based on the
 * DBpedia.org default data set.<p>
 * Note that the tests for the query interfaces are defined by 
 * {@link DbpediaQueryTest} because they are shared with
 * {@link SitesManagerTest}.<p>
 * TODO: We would need at least a second ReferencedSite to be up and running to
 * really test the ReferencedSiteManager! However in that case one would need
 * to be carefully about breaking the assumptions within the 
 * {@link DbpediaQueryTest} with results from other ReferencedSites. 
 * 
 * @author Rupert Westenthaler
 *
 */
public final class SitesManagerTest extends DbpediaQueryTest {

    public static final String SITES_MANAGER_PATH = "/entityhub/sites";

    /**
     * Executes the {@link DbpediaQueryTest} on the Entityhub Sites Manager
     * service (/entityhub/sites)
     */
    public SitesManagerTest() {
        super(SITES_MANAGER_PATH, null);
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
            builder.buildGetRequest(SITES_MANAGER_PATH+"/entity",
                "id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(200);
        //do not check for the site of the entity, because this might change
        JSONObject jEntity = assertEntity(re.getContent(), id, null);
        assertRepresentation(jEntity.getJSONObject("representation"), 
            DBPEDIA_DEFAULTDATA_REQUIRED_FIELDS, 
            DBPEDIA_DEFAULTDATA_OPTIONAL_FIELDS);
    }

}
