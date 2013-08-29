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
package org.apache.stanbol.commons.solr.web.it;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;

public class DefaultDataDispatchFilterTest extends SolrDispatchFilterComponentTestBase {

    /**
     * the prefix of the managed solr server (part of the config for the launchers)
     */
    private static final String PREFIX = "/solr/default/";
    /**
     * the name of the default data index (part of the data.defaultdata bundle)
     */
    private static final String CORE_NAME = "dbpedia";
    
    
    public DefaultDataDispatchFilterTest() {
        super(PREFIX, CORE_NAME);
    }

    @Test
    public void testSimpleSolrSelect() throws ClientProtocolException, IOException{
        executor.execute(
            builder.buildPostRequest(
                getCorePath()+"select?q="+URLEncoder.encode("@en/rdfs\\:label/:Paris","UTF8"))
        )
        .assertStatus(200)
        .assertContentRegexp(
            "http://dbpedia.org/resource/Paris",
            "http://dbpedia.org/resource/Paris_Saint-Germain_F.C.",
            "http://dbpedia.org/resource/University_of_Paris",
            "http://dbpedia.org/resource/Paris_Opera",
            "http://dbpedia.org/resource/Paris_Hilton",
            "http://dbpedia.org/resource/Paris_M%C3%A9tro");
    }
}
