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
package org.apache.stanbol.factstore.model;

import org.junit.Assert;
import org.junit.Test;


public class FactSchemaTest {

    @Test
    public void testToJsonLd() {
        FactSchema factSchema = new FactSchema();
        factSchema.setFactSchemaURN("http://this.isatest.de/test");
        factSchema.addRole("friend", "http://my.ontology.net/person");
        factSchema.addRole("person", "http://my.ontology.net/person");
        
        String expected = "{\n  \"@context\": {\n    \"@types\": {\n      \"friend\": \"http://my.ontology.net/person\",\n      \"person\": \"http://my.ontology.net/person\"\n    }\n  }\n}";
        String actual = factSchema.toJsonLdProfile().toString(2);
        Assert.assertEquals(expected, actual);
    }
    
    @SuppressWarnings("unused")
    private void toConsole(String actual) {
        System.out.println(actual);
        String s = actual;
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        System.out.println(s);
    }
}
