
package org.apache.stanbol.ontologymanager.web.it;

import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Test;

/** 
 * Test the ontonet homepage and demonstrate the test classes.
 * 
 * @author alberto musetti
 */

public class HomepageTest extends StanbolTestBase {
    
    @Test
    public void testHomepageExamples() throws Exception {
        
        executor.execute(
                builder.buildGetRequest("/ontonet")
                .withHeader("Accept", "text/html")
        )
        .assertStatus(200)
        .assertContentType("text/html")
        .assertContentContains(
            "/static/home/style/stanbol.css", 
            "The RESTful Semantic Engine")
        .assertContentRegexp(
            "stylesheet.*stanbol.css",
            "<title.*[Ss]tanbol");
    }
}