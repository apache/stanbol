package org.apache.stanbol.commons.jsonld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class JsonLdProfileParserTest {

    @Test
    public void testParseProfile() throws Exception {
        String jsonldInput = "{\"@context\":{\"iks\":\"http://iks-project.eu/ont/\",\"#types\":{\"person\":\"iks:person\",\"organization\":\"iks:organization\"}}}";
        
        JsonLdProfile profile = JsonLdProfileParser.parseProfile(jsonldInput);
        
        String actual = profile.toString();
        String expected = "{\"@context\":{\"iks\":\"http:\\/\\/iks-project.eu\\/ont\\/\",\"#types\":{\"organization\":\"iks:organization\",\"person\":\"iks:person\"}}}";
        assertEquals(expected, actual);
        assertNotNull(profile);
    }
    
    @Test
    public void testParseProfileMultiType() throws Exception {
        String jsonldInput = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}";
        
        JsonLdProfile profile = JsonLdProfileParser.parseProfile(jsonldInput);
        
        String expected = "{\"@context\":{\"#types\":{\"organization\":\"http:\\/\\/iks-project.eu\\/ont\\/organization\",\"person\":[\"http:\\/\\/iks-project.eu\\/ont\\/person\",\"http:\\/\\/www.schema.org\\/Person\"]}}}";
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
