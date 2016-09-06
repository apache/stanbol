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
	
	private static final String jsonCorefCheckObama = "{" + LINE_SEPARATOR
	    + "    \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "    \"start\" : 0," + LINE_SEPARATOR
	    + "    \"end\" : 5," + LINE_SEPARATOR
	    + "    \"stanbol.enhancer.nlp.coref\" : {" + LINE_SEPARATOR
	    + "      \"isRepresentative\" : true," + LINE_SEPARATOR
	    + "      \"mentions\" : [ {" + LINE_SEPARATOR
	    + "        \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "        \"start\" : 21," + LINE_SEPARATOR
	    + "        \"end\" : 23" + LINE_SEPARATOR
	    + "      } ]," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.coref.CorefFeature\"" + LINE_SEPARATOR
	    + "    }" + LINE_SEPARATOR
		+ "  }";
	
	private static final String jsonCorefCheckHe = "{" + LINE_SEPARATOR
	    + "    \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "    \"start\" : 21," + LINE_SEPARATOR
	    + "    \"end\" : 23," + LINE_SEPARATOR
	    + "    \"stanbol.enhancer.nlp.coref\" : {" + LINE_SEPARATOR
	    + "      \"isRepresentative\" : false," + LINE_SEPARATOR
	    + "      \"mentions\" : [ {" + LINE_SEPARATOR
	    + "        \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "        \"start\" : 0," + LINE_SEPARATOR
	    + "        \"end\" : 5" + LINE_SEPARATOR
	    + "      } ]," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.coref.CorefFeature\"" + LINE_SEPARATOR
	    + "    }" + LINE_SEPARATOR
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
        int heStartIdx = sentence2.getSpan().indexOf("He");
        Token he = sentence2.addToken(heStartIdx, heStartIdx + "He".length());
        
        Set<Span> obamaMentions = new HashSet<Span>();
        obamaMentions.add(he);
        obama.addAnnotation(NlpAnnotations.COREF_ANNOTATION, 
        	Value.value(new CorefFeature(true, obamaMentions)));
        
        Set<Span> heMentions = new HashSet<Span>();
        heMentions.add(obama);
        he.addAnnotation(NlpAnnotations.COREF_ANNOTATION, 
        	Value.value(new CorefFeature(false, heMentions)));
	}
}
