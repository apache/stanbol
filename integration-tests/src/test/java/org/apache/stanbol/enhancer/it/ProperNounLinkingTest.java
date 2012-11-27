package org.apache.stanbol.enhancer.it;

import org.junit.Test;

public class ProperNounLinkingTest extends EnhancerTestBase {

    
    public static final String TEST_TEXT = "The ProperNoun linking Chain can not "
            + "only detect famous cities such as London and people such as Bob "
            + "Marley but also books like the Theory of Relativity, events like "
            + "the French Revolution or the Paris Peace Conference and even "
            + "prices such as the Nobel Prize in Literature.";
    
    /**
     * 
     */
    public ProperNounLinkingTest() {
        super(getChainEndpoint("dbpedia-proper-noun"), 
            "langdetect"," LanguageDetectionEnhancementEngine",
            "opennlp-sentence"," OpenNlpSentenceDetectionEngine",
            "opennlp-token"," OpenNlpTokenizerEngine",
            "opennlp-pos","OpenNlpPosTaggingEngine",
            "opennlp-chunker","OpenNlpChunkingEngine",
            "dbpedia-proper-noun-extraction","EntityLinkingEngine");
    }
    
    
    @Test
    public void testSimpleEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withContent(TEST_TEXT)
        )
        .assertStatus(200)
        .assertContentRegexp( // it MUST detect the language
                "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
                "http://purl.org/dc/terms/language.*en",
                //and the entityLinkingEngine
                "http://purl.org/dc/terms/creator.*EntityLinkingEngine",
                //needs to suggest the following Entities
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/London",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Bob_Marley",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/French_Revolution",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Nobel_Prize_in_Literature",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Nobel_Prize",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_Peace_Conference,_1919",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Theory_of_relativity",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Theory",
                //for the following sections within the text
                "http://fise.iks-project.eu/ontology/selected-text.*Theory of Relativity",
                "http://fise.iks-project.eu/ontology/selected-text.*Nobel Prize in Literature",
                "http://fise.iks-project.eu/ontology/selected-text.*Paris Peace Conference",
                "http://fise.iks-project.eu/ontology/selected-text.*French Revolution");
    }

    
}
