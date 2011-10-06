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
package org.apache.stanbol.commons.jsonld;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class JsonLdProfileTest {

    @Test
    public void testDefineProfile() {
        JsonLdProfile profile = new JsonLdProfile();
        profile.setUseCuries(true);
        profile.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        
        profile.addType("person", "iks:person");
        profile.addType("organization", "iks:organization");
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testDefineProfileNoNS() {
        JsonLdProfile profile = new JsonLdProfile();
        
        profile.addType("person", "http://iks-project.eu/ont/person");
        profile.addType("organization", "http://iks-project.eu/ont/organization");
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":\"http://iks-project.eu/ont/person\"}}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testDefineProfileNoNSMultiTypes() {
        JsonLdProfile profile = new JsonLdProfile();
        
        profile.addType("person", "http://iks-project.eu/ont/person");
        profile.addType("person", "http://www.schema.org/Person");
        profile.addType("organization", "http://iks-project.eu/ont/organization");
        
        String actual = profile.toString(0);
        String expected = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}";
        assertEquals(expected, actual);
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
