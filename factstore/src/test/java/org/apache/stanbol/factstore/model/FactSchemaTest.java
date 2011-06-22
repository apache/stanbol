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
        
        String expected = "{\n  \"@context\": {\n    \"#types\": {\n      \"friend\": \"http:\\/\\/my.ontology.net\\/person\",\n      \"person\": \"http:\\/\\/my.ontology.net\\/person\"\n    }\n  }\n}";
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
