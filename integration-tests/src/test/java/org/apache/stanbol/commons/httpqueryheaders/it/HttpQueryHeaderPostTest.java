package org.apache.stanbol.commons.httpqueryheaders.it;

import static org.apache.stanbol.enhancer.it.StatelessEngineTest.ACCEPT_FORMAT_TEST_DATA;

import org.apache.http.client.methods.HttpPost;
import org.apache.stanbol.enhancer.it.EnhancerTestBase;
import org.junit.Test;

public class HttpQueryHeaderPostTest extends EnhancerTestBase {
    
    @Test
    public void testSetAccept() throws Exception {
        for (int i = 0; i < ACCEPT_FORMAT_TEST_DATA.length; i += 3) {
            executor.execute(
                    builder.buildOtherRequest(new HttpPost(
                        builder.buildUrl("/engines", 
                            "header_Accept",ACCEPT_FORMAT_TEST_DATA[i])))
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(ACCEPT_FORMAT_TEST_DATA[i+1])
            .assertContentRegexp(ACCEPT_FORMAT_TEST_DATA[i+2]);
        }

    }
    @Test
    public void testOverrideAccept() throws Exception {
        for (int i = 0; i < ACCEPT_FORMAT_TEST_DATA.length; i += 3) {
            executor.execute(
                    builder.buildOtherRequest(new HttpPost(
                        builder.buildUrl("/engines", 
                            "header_Accept",ACCEPT_FORMAT_TEST_DATA[i])))
                    //use an other Accept header
                    .withHeader("Accept", ACCEPT_FORMAT_TEST_DATA[(i+3)%ACCEPT_FORMAT_TEST_DATA.length])
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(ACCEPT_FORMAT_TEST_DATA[i+1])
            .assertContentRegexp(ACCEPT_FORMAT_TEST_DATA[i+2]);
        }

    }
    @Test
    public void testRemoveAccept() throws Exception {
        executor.execute(
            builder.buildOtherRequest(new HttpPost(
                builder.buildUrl("/engines", 
                "header_Accept",""))) //override the parse Accept Header
            .withHeader("Accept","text/turtle") //set Accept to turtle (overridden) 
            .withContent("John Smith was born in London.")
        )
        .assertStatus(200)
        //check for JSON-LD (the default content type
        .assertContentType("application/json")
        .assertContentRegexp("\"entity-reference\": \"http://dbpedia.org/resource/London\",",
            "\"creator\": \"org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine\"",
            "\"creator\": \"org.apache.stanbol.enhancer.engines.entitytagging.impl.NamedEntityTaggingEngine\"");
    }
}
