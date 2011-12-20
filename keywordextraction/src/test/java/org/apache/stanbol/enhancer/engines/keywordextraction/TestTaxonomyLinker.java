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
package org.apache.stanbol.enhancer.engines.keywordextraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.TextAnalyzerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.ClasspathDataFileProvider;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.TestSearcherImpl;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinker;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LinkedEntity;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.Suggestion;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.OpenNlpAnalysedContentFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO: convert this to an integration test!
 * @author Rupert Westenthaler
 */
public class TestTaxonomyLinker {

    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String TEST_TEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";
    public static final String TEST_TEXT2 = "A CBS televised debate between Australia's " +
    		"candidates for Prime Minister in the upcoming US election has been rescheduled " +
    		"and shortend, to avoid a clash with popular cookery sow MasterChef.";
    
    static TestSearcherImpl searcher;
    static ValueFactory factory = InMemoryValueFactory.getInstance();
    private static OpenNLP openNLP;
    
    public static final String NAME = NamespaceEnum.rdfs+"label";
    public static final String TYPE = NamespaceEnum.rdf+"type";
    public static final String REDIRECT = NamespaceEnum.rdfs+"seeAlso";

    @BeforeClass
    public static void setUpServices() throws IOException {
        openNLP = new OpenNLP(new ClasspathDataFileProvider("DUMMY_SYMBOLIC_NAME"));
        searcher = new TestSearcherImpl(NAME,SimpleTokenizer.INSTANCE);
        //add some terms to the searcher
        Representation rep = factory.createRepresentation("urn:test:PatrickMarshall");
        rep.addNaturalText(NAME, "Patrick Marshall");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_PERSON.getUnicodeString());
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:Geologist");
        rep.addNaturalText(NAME, "Geologist");
        rep.addReference(TYPE, NamespaceEnum.skos+"Concept");
        rep.addReference(REDIRECT, "urn:test:redirect:Geologist");
        searcher.addEntity(rep);
        //a redirect
        rep = factory.createRepresentation("urn:test:redirect:Geologist");
        rep.addNaturalText(NAME, "Geologe (redirect)");
        rep.addReference(TYPE, NamespaceEnum.skos+"Concept");
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:NewZealand");
        rep.addNaturalText(NAME, "New Zealand");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_PLACE.getUnicodeString());
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:UniversityOfOtago");
        rep.addNaturalText(NAME, "University of Otago");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString());
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:University");
        rep.addNaturalText(NAME, "University");
        rep.addReference(TYPE, NamespaceEnum.skos+"Concept");
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:Otago");
        rep.addNaturalText(NAME, "Otago");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_PLACE.getUnicodeString());
        searcher.addEntity(rep);
    }

    @Before
    public void bindServices() throws IOException {
    }

    @After
    public void unbindServices() {
    }

    @AfterClass
    public static void shutdownServices() {
    }

    public static ContentItem getContentItem(final String id, final String text) {
        return new InMemoryContentItem(id, text, "text/plain");
    }

    @Test
    public void testTaxonomyLinker() throws Exception{
        OpenNlpAnalysedContentFactory acf = OpenNlpAnalysedContentFactory.getInstance(openNLP,
            new TextAnalyzerConfig());
        EntityLinkerConfig config = new EntityLinkerConfig();
        config.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        EntityLinker linker = new EntityLinker(
            acf.create(TEST_TEXT,"en"), searcher, config);
        linker.process();
        Map<String,List<String>> expectedResults = new HashMap<String,List<String>>();
        expectedResults.put("Patrick Marshall", new ArrayList<String>(
                Arrays.asList("urn:test:PatrickMarshall")));
        expectedResults.put("geologist", new ArrayList<String>(
                Arrays.asList("urn:test:redirect:Geologist"))); //the redirected entity
        expectedResults.put("New Zealand", new ArrayList<String>(
                Arrays.asList("urn:test:NewZealand")));
        expectedResults.put("University of Otago", new ArrayList<String>(
                Arrays.asList("urn:test:UniversityOfOtago")));
        for(LinkedEntity linkedEntity : linker.getLinkedEntities().values()){
            List<String> expectedSuggestions = expectedResults.remove(linkedEntity.getSelectedText());
            assertNotNull("LinkedEntity "+linkedEntity.getSelectedText()+
                "is not an expected Result (or was found twice)", expectedSuggestions);
            linkedEntity.getSuggestions().iterator();
            assertEquals("Number of suggestions "+linkedEntity.getSuggestions().size()+
                " != number of expected suggestions "+expectedSuggestions.size()+
                "for selection "+linkedEntity.getSelectedText(), 
                linkedEntity.getSuggestions().size(),
                expectedSuggestions.size());
            double score = linkedEntity.getScore();
            for(int i=0;i<expectedSuggestions.size();i++){
                Suggestion suggestion = linkedEntity.getSuggestions().get(i);
                assertEquals("Expecced Suggestion at Rank "+i+" expected: "+
                    expectedSuggestions.get(i)+" suggestion: "+
                    suggestion.getRepresentation().getId(),
                    expectedSuggestions.get(i), 
                    suggestion.getRepresentation().getId());
                assertTrue("Score of suggestion "+i+"("+suggestion.getScore()+
                    " > as of the previous one ("+score+")",
                    score >= suggestion.getScore());
                score = suggestion.getScore();
            }
        }
    }

}
