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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class JsonLdProfileParserTest {

    @Test
    public void testParseProfile() throws Exception {
        String jsonldInput = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}";
        
        JsonLdProfile profile = JsonLdProfileParser.parseProfile(jsonldInput);
        profile.setUseCuries(true);
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"@types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}";
        assertEquals(expected, actual);
        assertNotNull(profile);
    }
    
    @Test
    public void testParseProfileMultiType() throws Exception {
        String jsonldInput = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}";
        
        JsonLdProfile profile = JsonLdProfileParser.parseProfile(jsonldInput);
        
        String expected = "{\"@context\":{\"@types\":{\"organization\":\"http://iks-project.eu/ont/organization\",\"person\":[\"http://iks-project.eu/ont/person\",\"http://www.schema.org/Person\"]}}}";
        String actual = profile.toString();
        assertEquals(expected, actual);
        assertNotNull(profile);
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
