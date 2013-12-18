package org.apache.stanbol.enhancer.nlp.json.valuetype;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.coref.CorefFeature;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CorefFeatureSupportTest extends ValueTypeSupportTest {
	
	private static final String sentenceText1 = "Obama visited China.";
	private static final String sentenceText2 = " He met with the Chinese prime-minister.";
	private static final String text = sentenceText1 + sentenceText2;
	
	private static final String jsonCorefCheckObama = "{\n"
	    + "    \"type\" : \"Token\",\n"
	    + "    \"start\" : 0,\n"
	    + "    \"end\" : 5,\n"
	    + "    \"stanbol.enhancer.nlp.coref\" : {\n"
	    + "      \"isRepresentative\" : true,\n"
	    + "      \"mentions\" : [ {\n"
	    + "        \"type\" : \"Token\",\n"
	    + "        \"start\" : 21,\n"
	    + "        \"end\" : 23\n"
	    + "      } ],\n"
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.coref.CorefFeature\"\n"
	    + "    }\n"
		+ "  }";
	
	private static final String jsonCorefCheckHe = "{\n"
	    + "    \"type\" : \"Token\",\n"
	    + "    \"start\" : 21,\n"
	    + "    \"end\" : 23,\n"
	    + "    \"stanbol.enhancer.nlp.coref\" : {\n"
	    + "      \"isRepresentative\" : false,\n"
	    + "      \"mentions\" : [ {\n"
	    + "        \"type\" : \"Token\",\n"
	    + "        \"start\" : 0,\n"
	    + "        \"end\" : 5\n"
	    + "      } ],\n"
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.coref.CorefFeature\"\n"
	    + "    }\n"
	    + "  }";
	
	@BeforeClass
    public static void setup() throws IOException {
		setupAnalysedText(text);
		
		initCorefAnnotations();
	}
	
	@Test
    public void testSerializationAndParse() throws IOException {
		String serialized = getSerializedString();
		
		Assert.assertTrue(serialized.contains(jsonCorefCheckObama));
		Assert.assertTrue(serialized.contains(jsonCorefCheckHe));
		
		AnalysedText parsedAt = getParsedAnalysedText(serialized);
		assertAnalysedTextEquality(parsedAt);
	}
	
	private static void initCorefAnnotations() {
		Sentence sentence1 = at.addSentence(0, sentenceText1.indexOf(".") + 1);
        Token obama = sentence1.addToken(0, "Obama".length());
        
        Sentence sentence2 = at.addSentence(sentenceText1.indexOf(".") + 2, sentenceText2.indexOf(".") + 1);
        int heStartIdx = sentence2.getSpan().toString().indexOf("He");
        Token he = sentence2.addToken(heStartIdx, heStartIdx + "He".length());
        
        Set<Span> obamaMentions = new HashSet<Span>();
        obamaMentions.add(he);
        obama.addAnnotation(NlpAnnotations.COREF_ANNOTATION, 
        	Value.value(new CorefFeature(true, Collections.unmodifiableSet(obamaMentions))));
        
        Set<Span> heMentions = new HashSet<Span>();
        heMentions.add(obama);
        he.addAnnotation(NlpAnnotations.COREF_ANNOTATION, 
        	Value.value(new CorefFeature(false, Collections.unmodifiableSet(heMentions))));
	}
}
