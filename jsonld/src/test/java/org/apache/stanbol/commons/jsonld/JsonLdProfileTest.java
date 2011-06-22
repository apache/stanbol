package org.apache.stanbol.commons.jsonld;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class JsonLdProfileTest {

    @Test
    public void testDefineProfile() {
        JsonLdProfile profile = new JsonLdProfile();
        profile.addNamespacePrefix("http://iks-project.eu/ont/", "iks");
        
        profile.addType("person", "iks:person");
        profile.addType("organization", "iks:organization");
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testDefineProfileNoNS() {
        JsonLdProfile profile = new JsonLdProfile();
        
        profile.addType("person", "http://iks-project.eu/ont/person");
        profile.addType("organization", "http://iks-project.eu/ont/organization");
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":\"http:\\/\\/iks-project.eu\\/ont\\/person\"}}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testDefineProfileNoNSMultiTypes() {
        JsonLdProfile profile = new JsonLdProfile();
        
        profile.addType("person", "http://iks-project.eu/ont/person");
        profile.addType("person", "http://www.schema.org/Person");
        profile.addType("organization", "http://iks-project.eu/ont/organization");
        
        String actual = profile.toString(0);
        String expected = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}";
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
