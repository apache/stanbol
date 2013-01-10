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

import static org.junit.Assert.*;

import java.util.Set;

import org.apache.stanbol.commons.jsonld.JsonLd;
import org.apache.stanbol.commons.jsonld.JsonLdIRI;
import org.apache.stanbol.commons.jsonld.JsonLdResource;
import org.junit.Test;

public class FactTest {

    @Test
    public void testFactFromJsonLd() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        jsonLd.addNamespacePrefix("http://upb.de/persons/", "upb");
        
        JsonLdResource factResource = new JsonLdResource();
        factResource.setProfile("iks:employeeOf");
        factResource.putProperty("person", new JsonLdIRI("upb:bnagel"));
        factResource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(factResource);
        
        Fact fact = Fact.factFromJsonLd(jsonLd);
        assertEquals("http://iks-project.eu/ont/employeeOf", fact.getFactSchemaURN());
        assertEquals(2, fact.getRoles().size());
        
        boolean rolePersonOK = false;
        boolean roleOrgaOK = false;
        for (String role : fact.getRoles()) {
            if (role.equals("person")) {
                assertEquals("http://upb.de/persons/bnagel", fact.getValueOfRole(role));
                rolePersonOK = true;
            }
            if (role.equals("organization")) {
                assertEquals("http://uni-paderborn.de", fact.getValueOfRole(role));
                roleOrgaOK = true;
            }
        }
        assertTrue(rolePersonOK);
        assertTrue(roleOrgaOK);
    }

    @Test
    public void testFactFromJsonLdMultiFacts() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        jsonLd.addNamespacePrefix("http://upb.de/persons/", "upb");
        
        JsonLdResource fact1Resource = new JsonLdResource();
        fact1Resource.setSubject("fact1");
        fact1Resource.setProfile("iks:employeeOf");
        fact1Resource.putProperty("person", new JsonLdIRI("upb:bnagel"));
        fact1Resource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(fact1Resource);

        JsonLdResource fact2Resource = new JsonLdResource();
        fact2Resource.setSubject("fact2");
        fact2Resource.setProfile("iks:employeeOf");
        fact2Resource.putProperty("person", new JsonLdIRI("upb:fchrist"));
        fact2Resource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(fact2Resource);
        
        Fact fact = Fact.factFromJsonLd(jsonLd);
        assertNull(fact);
    }
    
    @Test
    public void testFactsFromJsonLdMultiFacts() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        jsonLd.addNamespacePrefix("http://upb.de/persons/", "upb");
        
        JsonLdResource fact1Resource = new JsonLdResource();
        fact1Resource.setSubject("fact1");
        fact1Resource.setProfile("iks:employeeOf");
        fact1Resource.putProperty("person", new JsonLdIRI("upb:bnagel"));
        fact1Resource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(fact1Resource);

        JsonLdResource fact2Resource = new JsonLdResource();
        fact2Resource.setSubject("fact2");
        fact2Resource.setProfile("iks:employeeOf");
        fact2Resource.putProperty("person", new JsonLdIRI("upb:fchrist"));
        fact2Resource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(fact2Resource);
        
        Set<Fact> facts = Fact.factsFromJsonLd(jsonLd);
        assertNotNull(facts);
        assertEquals(2, facts.size());
        for (Fact fact : facts) {
            assertEquals("http://iks-project.eu/ont/employeeOf", fact.getFactSchemaURN());
            assertEquals(2, fact.getRoles().size());
            
            boolean rolePersonOK = false;
            boolean roleOrgaOK = false;
            for (String role : fact.getRoles()) {
                if (role.equals("person")) {
                    assertTrue(fact.getValueOfRole(role).startsWith("http://upb.de/persons/"));
                    rolePersonOK = true;
                }
                if (role.equals("organization")) {
                    assertEquals("http://uni-paderborn.de", fact.getValueOfRole(role));
                    roleOrgaOK = true;
                }
            }
            assertTrue(rolePersonOK);
            assertTrue(roleOrgaOK);
        }
    }
    
    @Test
    public void testFactsFromJsonLdMultiFactsMultiSchema() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        jsonLd.addNamespacePrefix("http://upb.de/persons/", "upb");
        
        JsonLdResource fact1Resource = new JsonLdResource();
        fact1Resource.setSubject("fact1");
        fact1Resource.setProfile("iks:employeeOf");
        fact1Resource.putProperty("person", new JsonLdIRI("upb:bnagel"));
        fact1Resource.putProperty("organization", new JsonLdIRI("http://uni-paderborn.de"));
        jsonLd.put(fact1Resource);

        JsonLdResource fact2Resource = new JsonLdResource();
        fact2Resource.setSubject("fact2");
        fact2Resource.setProfile("iks:friendOf");
        fact2Resource.putProperty("person", new JsonLdIRI("upb:bnagel"));
        fact2Resource.putProperty("friend", new JsonLdIRI("upb:fchrist"));
        jsonLd.put(fact2Resource);
        
        Set<Fact> facts = Fact.factsFromJsonLd(jsonLd);
        assertNotNull(facts);
        assertEquals(2, facts.size());
        boolean fact1OK = false;
        boolean fact2OK = false;
        for (Fact fact : facts) {
            if (fact.getFactSchemaURN().equals("http://iks-project.eu/ont/employeeOf")) {
                assertEquals("http://iks-project.eu/ont/employeeOf", fact.getFactSchemaURN());
                assertEquals(2, fact.getRoles().size());
                
                boolean rolePersonOK = false;
                boolean roleOrgaOK = false;
                for (String role : fact.getRoles()) {
                    if (role.equals("person")) {
                        assertEquals("http://upb.de/persons/bnagel", fact.getValueOfRole(role));
                        rolePersonOK = true;
                    }
                    if (role.equals("organization")) {
                        assertEquals("http://uni-paderborn.de", fact.getValueOfRole(role));
                        roleOrgaOK = true;
                    }
                }
                assertTrue(rolePersonOK);
                assertTrue(roleOrgaOK);
                fact1OK = true;
            }
            if (fact.getFactSchemaURN().equals("http://iks-project.eu/ont/friendOf")) {
                assertEquals("http://iks-project.eu/ont/friendOf", fact.getFactSchemaURN());
                assertEquals(2, fact.getRoles().size());
                
                boolean rolePersonOK = false;
                boolean roleFriendOK = false;
                for (String role : fact.getRoles()) {
                    if (role.equals("person")) {
                        assertEquals("http://upb.de/persons/bnagel", fact.getValueOfRole(role));
                        rolePersonOK = true;
                    }
                    if (role.equals("friend")) {
                        assertEquals("http://upb.de/persons/fchrist", fact.getValueOfRole(role));
                        roleFriendOK = true;
                    }
                }
                assertTrue(rolePersonOK);
                assertTrue(roleFriendOK);
                fact2OK = true;
            }

        }
        assertTrue(fact1OK);
        assertTrue(fact2OK);
    }
}
