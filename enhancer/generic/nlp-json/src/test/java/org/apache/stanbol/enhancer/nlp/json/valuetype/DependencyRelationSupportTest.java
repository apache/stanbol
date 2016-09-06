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

import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DependencyRelationSupportTest extends ValueTypeSupportTest {

	private static final String text = "Obama visited China.";
	
	private static final String jsonCheckObama = "{" + LINE_SEPARATOR
	    + "    \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "    \"start\" : 0," + LINE_SEPARATOR
	    + "    \"end\" : 5," + LINE_SEPARATOR
	    + "    \"stanbol.enhancer.nlp.dependency\" : {" + LINE_SEPARATOR
	    + "      \"tag\" : \"nsubj\"," + LINE_SEPARATOR
	    + "      \"relationType\" : 33," + LINE_SEPARATOR
	    + "      \"isDependent\" : true," + LINE_SEPARATOR
	    + "      \"partnerType\" : \"Token\"," + LINE_SEPARATOR
	    + "      \"partnerStart\" : 6," + LINE_SEPARATOR
	    + "      \"partnerEnd\" : 13," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation\"" + LINE_SEPARATOR
	    + "    }" + LINE_SEPARATOR
	    + "  }";
	
	private static final String jsonCheckVisited = "{" + LINE_SEPARATOR
	    + "    \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "    \"start\" : 6," + LINE_SEPARATOR
	    + "    \"end\" : 13," + LINE_SEPARATOR
	    + "    \"stanbol.enhancer.nlp.dependency\" : [ {" + LINE_SEPARATOR
	    + "      \"tag\" : \"root\"," + LINE_SEPARATOR
	    + "      \"relationType\" : 57," + LINE_SEPARATOR
	    + "      \"isDependent\" : true," + LINE_SEPARATOR
	    + "      \"partnerType\" : \"ROOT\"," + LINE_SEPARATOR
	    + "      \"partnerStart\" : 0," + LINE_SEPARATOR
	    + "      \"partnerEnd\" : 0," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation\"" + LINE_SEPARATOR
	    + "    }, {" + LINE_SEPARATOR
	    + "      \"tag\" : \"nsubj\"," + LINE_SEPARATOR
	    + "      \"relationType\" : 33," + LINE_SEPARATOR
	    + "      \"isDependent\" : false," + LINE_SEPARATOR
	    + "      \"partnerType\" : \"Token\"," + LINE_SEPARATOR
	    + "      \"partnerStart\" : 0," + LINE_SEPARATOR
	    + "      \"partnerEnd\" : 5," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation\"" + LINE_SEPARATOR
	    + "    }, {" + LINE_SEPARATOR
	    + "      \"tag\" : \"dobj\"," + LINE_SEPARATOR
	    + "      \"relationType\" : 24," + LINE_SEPARATOR
	    + "      \"isDependent\" : false," + LINE_SEPARATOR
	    + "      \"partnerType\" : \"Token\"," + LINE_SEPARATOR
	    + "      \"partnerStart\" : 14," + LINE_SEPARATOR
	    + "      \"partnerEnd\" : 19," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation\"" + LINE_SEPARATOR
	    + "    } ]" + LINE_SEPARATOR
	    + "  }";
	
	private static final String jsonCheckChina = "{" + LINE_SEPARATOR
	    + "    \"type\" : \"Token\"," + LINE_SEPARATOR
	    + "    \"start\" : 14," + LINE_SEPARATOR
	    + "    \"end\" : 19," + LINE_SEPARATOR
	    + "    \"stanbol.enhancer.nlp.dependency\" : {" + LINE_SEPARATOR
	    + "      \"tag\" : \"dobj\"," + LINE_SEPARATOR
	    + "      \"relationType\" : 24," + LINE_SEPARATOR
	    + "      \"isDependent\" : true," + LINE_SEPARATOR
	    + "      \"partnerType\" : \"Token\"," + LINE_SEPARATOR
	    + "      \"partnerStart\" : 6," + LINE_SEPARATOR
	    + "      \"partnerEnd\" : 13," + LINE_SEPARATOR
	    + "      \"class\" : \"org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation\"" + LINE_SEPARATOR
	    + "    }" + LINE_SEPARATOR
	    + "  }";
	
	@BeforeClass
    public static void setup() throws IOException {
		setupAnalysedText(text);
		
		initDepTreeAnnotations();
	}
	
	@Test
    public void testSerializationAndParse() throws IOException {
		String serialized = getSerializedString();
		Assert.assertTrue(serialized.contains(jsonCheckObama));
		Assert.assertTrue(serialized.contains(jsonCheckVisited));
		Assert.assertTrue(serialized.contains(jsonCheckChina));
		
		AnalysedText parsedAt = getParsedAnalysedText(serialized);
		assertAnalysedTextEquality(parsedAt);
	}
	
	private static void initDepTreeAnnotations() {
		Sentence sentence = at.addSentence(0, text.indexOf(".") + 1);
        Token obama = sentence.addToken(0, "Obama".length());
        
        int visitedStartIdx = sentence.getSpan().indexOf("visited");
        Token visited = sentence.addToken(visitedStartIdx, visitedStartIdx + "visited".length());
        
        int chinaStartIdx = sentence.getSpan().indexOf("China");
        Token china = sentence.addToken(chinaStartIdx, chinaStartIdx + "China".length());
        
        GrammaticalRelationTag nSubjGrammRelTag = new GrammaticalRelationTag(
                "nsubj", GrammaticalRelation.NominalSubject);
        obama.addAnnotation(NlpAnnotations.DEPENDENCY_ANNOTATION, 
        	Value.value(new DependencyRelation(nSubjGrammRelTag, true, visited)));
        
        GrammaticalRelationTag rootGrammRelTag = new GrammaticalRelationTag(
                "root", GrammaticalRelation.Root);
        GrammaticalRelationTag dobjGrammRelTag = new GrammaticalRelationTag(
                "dobj", GrammaticalRelation.DirectObject);
        visited.addAnnotation(NlpAnnotations.DEPENDENCY_ANNOTATION, 
        	Value.value(new DependencyRelation(rootGrammRelTag, true, null)));
        visited.addAnnotation(NlpAnnotations.DEPENDENCY_ANNOTATION, 
            Value.value(new DependencyRelation(nSubjGrammRelTag, false, obama)));
        visited.addAnnotation(NlpAnnotations.DEPENDENCY_ANNOTATION, 
            Value.value(new DependencyRelation(dobjGrammRelTag, false, china)));
        
        china.addAnnotation(NlpAnnotations.DEPENDENCY_ANNOTATION, 
            Value.value(new DependencyRelation(dobjGrammRelTag, true, visited)));
	}
}
