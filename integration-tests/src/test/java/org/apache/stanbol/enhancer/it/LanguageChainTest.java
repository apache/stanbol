package org.apache.stanbol.enhancer.it;

import org.junit.Test;

public class LanguageChainTest extends EnhancerTestBase {

    public LanguageChainTest() {
        super(getChainEndpoint("language"),
            "langid","LangIdEnhancementEngine");
    }
    
    
    @Test
    public void testSimpleEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withContent("This Stanbol chain does not detect detect famous cities " +
            		"such as Paris and people such as Bob Marley because it only" +
            		"includes Engines that detect the langauge of the parsed text!")
        )
        .assertStatus(200)
        .assertContentRegexp( // it MUST detect the language
                "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
                "http://purl.org/dc/terms/language.*en")
        .assertContentRegexp(false, //MUST NOT contain because NER is not in this chain
                "http://fise.iks-project.eu/ontology/entity-label.*Paris", //No entitylinking
                "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
                "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley");
        
    }
}
