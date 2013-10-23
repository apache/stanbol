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
package org.apache.stanbol.entityhub.it.reconcile;

import java.io.IOException;
import java.util.Collections;

import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.junit.Test;

public class DbpediaReconcileTest extends EntityhubTestBase {

    public static final String SIMPLE_QUERIES = "{\"q0\":{\"query\":\"Paris\",\"limit\":3},"
            + "\"q1\":{\"query\":\"London\",\"limit\":3}}";
    
    
    public DbpediaReconcileTest() {
        super(Collections.singleton("dbpedia"));
    }

    /**
     * Tests if adding a Standard Reconciliation services works.
     * @throws IOException
     */
    @Test
    public void testInitialization() throws IOException {
        String callback = "jsonp1361172630576";
        executor.execute(builder.buildGetRequest("/entityhub/site/dbpedia/reconcile", 
            "callback",callback)
            ) //callback("name":"{human readable name}")
            .assertStatus(200).assertContentContains(callback+"({\"name\":\"");
    }
    
    @Test
    public void testSimpleReconciliation() throws IOException {
        executor.execute(builder.buildGetRequest("/entityhub/site/dbpedia/reconcile", 
            "queries",SIMPLE_QUERIES)
            )
            .assertStatus(200).assertContentContains("\"q1\":{\"result\":",
                "\"q0\":{\"result\":",
                "\"id\":\"http:\\/\\/dbpedia.org\\/resource\\/Paris\"",
                "\"name\":\"Paris\"",
                "\"id\":\"http:\\/\\/dbpedia.org\\/resource\\/London\"",
                "\"name\":\"London\"",
                "\"type\":[\"",
                "\"http:\\/\\/dbpedia.org\\/ontology\\/Place\"");
    }

    
}
