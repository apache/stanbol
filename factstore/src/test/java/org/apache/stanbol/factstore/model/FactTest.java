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
                assertEquals(1, fact.getTypesOfRole(role).size());
                assertEquals("http://upb.de/persons/bnagel", fact.getTypesOfRole(role).get(0));
                rolePersonOK = true;
            }
            if (role.equals("organization")) {
                assertEquals(1, fact.getTypesOfRole(role).size());
                assertEquals("http://uni-paderborn.de", fact.getTypesOfRole(role).get(0));
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
                    assertEquals(1, fact.getTypesOfRole(role).size());
                    assertTrue(fact.getTypesOfRole(role).get(0).startsWith("http://upb.de/persons/"));
                    rolePersonOK = true;
                }
                if (role.equals("organization")) {
                    assertEquals(1, fact.getTypesOfRole(role).size());
                    assertEquals("http://uni-paderborn.de", fact.getTypesOfRole(role).get(0));
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
                        assertEquals(1, fact.getTypesOfRole(role).size());
                        assertEquals("http://upb.de/persons/bnagel", fact.getTypesOfRole(role).get(0));
                        rolePersonOK = true;
                    }
                    if (role.equals("organization")) {
                        assertEquals(1, fact.getTypesOfRole(role).size());
                        assertEquals("http://uni-paderborn.de", fact.getTypesOfRole(role).get(0));
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
                        assertEquals(1, fact.getTypesOfRole(role).size());
                        assertEquals("http://upb.de/persons/bnagel", fact.getTypesOfRole(role).get(0));
                        rolePersonOK = true;
                    }
                    if (role.equals("friend")) {
                        assertEquals(1, fact.getTypesOfRole(role).size());
                        assertEquals("http://upb.de/persons/fchrist", fact.getTypesOfRole(role).get(0));
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
