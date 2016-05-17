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
package org.apache.stanbol.enhancer.engines.entitylinking.engine;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CREATOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateEntityAnnotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.TestSearcherImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.SimpleLabelTokenizer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: convert this to an integration test!
 * @author Rupert Westenthaler
 */
public class EntityLinkingEngineTest {
    
    private final static Logger log = LoggerFactory.getLogger(EntityLinkingEngineTest.class);

    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String TEST_TEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";
    
    /**
     * changed oder af given and family name
     */
    public static final String TEST_TEXT_WO = "Dr. Marshall Patrick (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";

    private static AnalysedText TEST_ANALYSED_TEXT;
    private static AnalysedText TEST_ANALYSED_TEXT_WO;
    
//    public static final String TEST_TEXT2 = "A CBS televised debate between Australia's " +
//    		"candidates for Prime Minister in the upcoming US election has been rescheduled " +
//    		"and shortend, to avoid a clash with popular cookery sow MasterChef.";
    
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    private static final String TEST_REFERENCED_SITE_NAME = "dummRefSiteName";
    
    private static Value<PhraseTag> NOUN_PHRASE = Value.value(new PhraseTag("NP",LexicalCategory.Noun),1d);
    
    static TestSearcherImpl searcher;
    
    public static final IRI NAME = new IRI(NamespaceEnum.rdfs+"label");
    public static final IRI TYPE = new IRI(NamespaceEnum.rdf+"type");
    public static final IRI REDIRECT = new IRI(NamespaceEnum.rdfs+"seeAlso");

    @BeforeClass
    public static void setUpServices() throws IOException {
        searcher = new TestSearcherImpl(TEST_REFERENCED_SITE_NAME,NAME,new SimpleLabelTokenizer());
        //add some terms to the searcher
        Graph graph = new IndexedGraph();
        IRI uri = new IRI("urn:test:PatrickMarshall");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Patrick Marshall")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_PERSON));
        searcher.addEntity(new Entity(uri, graph));
        
        uri = new IRI("urn:test:Geologist");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Geologist")));
        graph.add(new TripleImpl(uri, TYPE, new IRI(NamespaceEnum.skos+"Concept")));
        graph.add(new TripleImpl(uri, REDIRECT, new IRI("urn:test:redirect:Geologist")));
        searcher.addEntity(new Entity(uri, graph));
        //a redirect
        uri = new IRI("urn:test:redirect:Geologist");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Geologe (redirect)")));
        graph.add(new TripleImpl(uri, TYPE, new IRI(NamespaceEnum.skos+"Concept")));
        searcher.addEntity(new Entity(uri, graph));

        uri = new IRI("urn:test:NewZealand");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("New Zealand")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_PLACE));
        searcher.addEntity(new Entity(uri, graph));

        uri = new IRI("urn:test:UniversityOfOtago");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("University of Otago")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_ORGANISATION));
        searcher.addEntity(new Entity(uri, graph));
        
        uri = new IRI("urn:test:University");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("University")));
        graph.add(new TripleImpl(uri, TYPE, new IRI(NamespaceEnum.skos+"Concept")));
        searcher.addEntity(new Entity(uri, graph));

        uri = new IRI("urn:test:Otago");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Otago")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_PLACE));
        searcher.addEntity(new Entity(uri, graph));
        //add a 2nd Otago (Place and University
        uri = new IRI("urn:test:Otago_Texas");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Otago (Texas)")));
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("Otago")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_PLACE));
        searcher.addEntity(new Entity(uri, graph));

        uri = new IRI("urn:test:UniversityOfOtago_Texas");
        graph.add(new TripleImpl(uri, NAME, new PlainLiteralImpl("University of Otago (Texas)")));
        graph.add(new TripleImpl(uri, TYPE, OntologicalClasses.DBPEDIA_ORGANISATION));
        searcher.addEntity(new Entity(uri, graph));
        
        TEST_ANALYSED_TEXT = AnalysedTextFactory.getDefaultInstance().createAnalysedText(
            ciFactory.createBlob(new StringSource(TEST_TEXT)));
        TEST_ANALYSED_TEXT_WO = AnalysedTextFactory.getDefaultInstance().createAnalysedText(
                ciFactory.createBlob(new StringSource(TEST_TEXT_WO)));
        initAnalyzedText(TEST_ANALYSED_TEXT);
        TEST_ANALYSED_TEXT.addChunk(0, "Dr. Patrick Marshall".length()).addAnnotation(PHRASE_ANNOTATION, NOUN_PHRASE);
        TEST_ANALYSED_TEXT.addToken(4, 11).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
        TEST_ANALYSED_TEXT.addToken(12, 20).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
        initAnalyzedText(TEST_ANALYSED_TEXT_WO);
        TEST_ANALYSED_TEXT_WO.addChunk(0, "Dr. Marshall Patrick".length()).addAnnotation(PHRASE_ANNOTATION, NOUN_PHRASE);
        TEST_ANALYSED_TEXT_WO.addToken(4, 12).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
        TEST_ANALYSED_TEXT_WO.addToken(13, 20).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
    }

    /**
     * @param nounPhrase
     */
    private static void initAnalyzedText(AnalysedText at) {
        at.addSentence(0, TEST_ANALYSED_TEXT.getEnd());
        at.addChunk(TEST_TEXT.indexOf("New Zealand"), TEST_TEXT.indexOf("New Zealand")+"New Zealand".length())
        .addAnnotation(PHRASE_ANNOTATION, NOUN_PHRASE);
        at.addChunk(TEST_TEXT.indexOf("geologist"), TEST_TEXT.indexOf("geologist")+"geologist".length())
        .addAnnotation(PHRASE_ANNOTATION, NOUN_PHRASE);
        at.addChunk(TEST_TEXT.indexOf("the University of Otago"), 
            TEST_TEXT.length()-1).addAnnotation(PHRASE_ANNOTATION, NOUN_PHRASE);
        //add some tokens
        at.addToken(0, 2).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NE",Pos.Abbreviation),1d));
        at.addToken(2, 3).addAnnotation(POS_ANNOTATION, Value.value(new PosTag(".",Pos.Point),1d));
        int start = TEST_TEXT.indexOf("(1869 - November 1950)");
        at.addToken(start,start+1).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("(",Pos.OpenBracket),1d));
        at.addToken(start+1,start+5).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NUM",Pos.Numeral),1d));
        at.addToken(start+6,start+7).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("-",Pos.Hyphen),1d));
        at.addToken(start+8,start+16).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NE",Pos.CommonNoun),1d));
        at.addToken(start+17,start+21).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NUM",Pos.Numeral),1d));
        at.addToken(start+21,start+22).addAnnotation(POS_ANNOTATION, Value.value(new PosTag(")",Pos.CloseBracket),1d));
        
        at.addToken(start+23, start+26).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("O",LexicalCategory.Adjective)));
        at.addToken(start+27, start+28).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("A", LexicalCategory.Adposition)));
        
        start = TEST_TEXT.indexOf("geologist");
        at.addToken(start,start+9).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NE",Pos.CommonNoun),1d));

        at.addToken(start+10, start+13).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("O", LexicalCategory.Adjective)));
        at.addToken(start+14, start+19).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("V", LexicalCategory.Verb)));
        at.addToken(start+20, start+22).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("PP", LexicalCategory.PronounOrDeterminer)));

        start = TEST_TEXT.indexOf("New Zealand");
        at.addToken(start,start+3).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NE",Pos.CommonNoun),1d));
        at.addToken(start+4,start+11).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
        
        //add filler Tokens for "and worked at"
        at.addToken(start+12, start+15).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("O", LexicalCategory.Adjective)));
        at.addToken(start+16, start+22).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("V", LexicalCategory.Verb)));
        at.addToken(start+23, start+25).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("PP", LexicalCategory.PronounOrDeterminer)));
        
        start = TEST_TEXT.indexOf("the University of Otago");
        at.addToken(start,start+3).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("ART",Pos.Article),1d));
        at.addToken(start+4,start+14).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NE",Pos.CommonNoun),1d));
        at.addToken(start+15,start+17).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("OF",Pos.Preposition),1d));
        at.addToken(start+18,start+23).addAnnotation(POS_ANNOTATION, Value.value(new PosTag("NP",Pos.ProperNoun),1d));
        at.addToken(start+23,start+24).addAnnotation(POS_ANNOTATION, Value.value(new PosTag(".",Pos.Point),1d));
    }
    
    private LabelTokenizer labelTokenizer = new SimpleLabelTokenizer();


    @Before
    public void bindServices() throws IOException {
    }

    @After
    public void unbindServices() {
    }

    @AfterClass
    public static void shutdownServices() {
    }

    public static ContentItem getContentItem(final String id, final String text) throws IOException {
        return ciFactory.createContentItem(new IRI(id),new StringSource(text));
    }
    /**
     * This tests the EntityLinker functionality (if the expected Entities
     * are linked). In this case with the default configurations for
     * {@link LexicalCategory#Noun}.
     * @throws Exception
     */
    @Test
    public void testEntityLinkerWithNouns() throws Exception {
        LanguageProcessingConfig tpc = new LanguageProcessingConfig();
        tpc.setLinkedLexicalCategories(LanguageProcessingConfig.DEFAULT_LINKED_LEXICAL_CATEGORIES);
        tpc.setLinkedPos(Collections.EMPTY_SET);
        EntityLinkerConfig config = new EntityLinkerConfig();
        config.setMinFoundTokens(2);//this is assumed by this test
        config.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        EntityLinker linker = new EntityLinker(TEST_ANALYSED_TEXT,"en",
            tpc, searcher, config, labelTokenizer);
        linker.process();
        Map<String,List<String>> expectedResults = new HashMap<String,List<String>>();
        expectedResults.put("Patrick Marshall", new ArrayList<String>(
                Arrays.asList("urn:test:PatrickMarshall")));
        expectedResults.put("geologist", new ArrayList<String>(
                Arrays.asList("urn:test:redirect:Geologist"))); //the redirected entity
        expectedResults.put("New Zealand", new ArrayList<String>(
                Arrays.asList("urn:test:NewZealand")));
        expectedResults.put("University of Otago", new ArrayList<String>(
                Arrays.asList("urn:test:UniversityOfOtago","urn:test:UniversityOfOtago_Texas")));
        validateEntityLinkerResults(linker, expectedResults);
    }
    /**
     * This tests the EntityLinker functionality (if the expected Entities
     * are linked). In this case with the default configurations for
     * {@link LexicalCategory#Noun}.
     * @throws Exception
     */
    @Test
    public void testEntityLinkerWithWrongOrder() throws Exception {
        LanguageProcessingConfig tpc = new LanguageProcessingConfig();
        tpc.setLinkedLexicalCategories(LanguageProcessingConfig.DEFAULT_LINKED_LEXICAL_CATEGORIES);
        tpc.setLinkedPos(Collections.EMPTY_SET);
        tpc.setIgnoreChunksState(true); //to emulate pre STANBOL-1211
        EntityLinkerConfig config = new EntityLinkerConfig();
        config.setMinFoundTokens(2);//this is assumed by this test
        config.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        EntityLinker linker = new EntityLinker(TEST_ANALYSED_TEXT_WO,"en",
            tpc, searcher, config, labelTokenizer);
        linker.process();
        Map<String,List<String>> expectedResults = new HashMap<String,List<String>>();
        expectedResults.put("Marshall Patrick", new ArrayList<String>(
                Arrays.asList("urn:test:PatrickMarshall")));
        expectedResults.put("geologist", new ArrayList<String>(
                Arrays.asList("urn:test:redirect:Geologist"))); //the redirected entity
        expectedResults.put("New Zealand", new ArrayList<String>(
                Arrays.asList("urn:test:NewZealand")));
        expectedResults.put("University of Otago", new ArrayList<String>(
                Arrays.asList("urn:test:UniversityOfOtago","urn:test:UniversityOfOtago_Texas")));
        validateEntityLinkerResults(linker, expectedResults);
    }
    /**
     * This tests the EntityLinker functionality (if the expected Entities
     * are linked). In this case with the default configurations for
     * {@link Pos#ProperNoun}.
     * @throws Exception
     */
    @Test
    public void testEntityLinkerWithProperNouns() throws Exception {
        LanguageProcessingConfig tpc = new LanguageProcessingConfig();
        tpc.setLinkedLexicalCategories(Collections.EMPTY_SET);
        tpc.setLinkedPos(LanguageProcessingConfig.DEFAULT_LINKED_POS);
        EntityLinkerConfig config = new EntityLinkerConfig();
        config.setMinFoundTokens(2);//this is assumed by this test
        config.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        EntityLinker linker = new EntityLinker(TEST_ANALYSED_TEXT,"en",
            tpc, searcher, config, labelTokenizer);
        linker.process();
        Map<String,List<String>> expectedResults = new HashMap<String,List<String>>();
        expectedResults.put("Patrick Marshall", new ArrayList<String>(
                Arrays.asList("urn:test:PatrickMarshall")));
        //Geologist is a common noun and MUST NOT be found
        //expectedResults.put("geologist", new ArrayList<String>(
        //        Arrays.asList("urn:test:redirect:Geologist"))); //the redirected entity
        expectedResults.put("New Zealand", new ArrayList<String>(
                Arrays.asList("urn:test:NewZealand")));
        expectedResults.put("University of Otago", new ArrayList<String>(
                Arrays.asList("urn:test:UniversityOfOtago","urn:test:UniversityOfOtago_Texas")));
        validateEntityLinkerResults(linker, expectedResults);
    }
    private void validateEntityLinkerResults(EntityLinker linker, Map<String,List<String>> expectedResults) {
        log.info("---------------------");
        log.info("- Validating Results-");
        log.info("---------------------");
        for(LinkedEntity linkedEntity : linker.getLinkedEntities().values()){
            log.info("> LinkedEntity {}",linkedEntity);
            List<String> expectedSuggestions = expectedResults.remove(linkedEntity.getSelectedText());
            assertNotNull("LinkedEntity '"+linkedEntity.getSelectedText()+
                "' is not an expected Result (or was found twice)", expectedSuggestions);
            linkedEntity.getSuggestions().iterator();
            assertEquals("Number of suggestions "+linkedEntity.getSuggestions().size()+
                " != number of expected suggestions "+expectedSuggestions.size()+
                "for selection "+linkedEntity.getSelectedText() + "(Expected: " +
                expectedSuggestions +")", linkedEntity.getSuggestions().size(), 
                expectedSuggestions.size());
            double score = linkedEntity.getScore();
            for(int i=0;i<expectedSuggestions.size();i++){
                Suggestion suggestion = linkedEntity.getSuggestions().get(i);
                assertEquals("Expecced Suggestion at Rank "+i+" expected: "+
                    expectedSuggestions.get(i)+" suggestion: "+
                    suggestion.getEntity().getId(),
                    expectedSuggestions.get(i), 
                    suggestion.getEntity().getId());
                assertTrue("Score of suggestion "+i+"("+suggestion.getScore()+
                    " > as of the previous one ("+score+")",
                    score >= suggestion.getScore());
                score = suggestion.getScore();
            }
        }
        assertTrue("The expected Result(s) "+expectedResults+" wehre not found",
            expectedResults.isEmpty());
    }
    /**
     * This tests if the Enhancements created by the Engine confirm to the
     * rules defined for the Stanbol Enhancement Structure.
     * @throws IOException
     * @throws EngineException
     */
    @Test
    public void testEngine() throws IOException, EngineException {
        EntityLinkerConfig linkerConfig = new EntityLinkerConfig();
        linkerConfig.setRedirectProcessingMode(RedirectProcessingMode.FOLLOW);
        linkerConfig.setMinFoundTokens(2);//this is assumed by this test
        EntityLinkingEngine engine = new EntityLinkingEngine("dummy",
            searcher, new TextProcessingConfig(), 
            linkerConfig, labelTokenizer);
        ContentItem ci = ciFactory.createContentItem(new StringSource(TEST_TEXT));
        //tells the engine that this is an English text
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("en")));
        //and add the AnalysedText instance used for this test
        ci.addPart(AnalysedText.ANALYSED_TEXT_URI, TEST_ANALYSED_TEXT);
        //compute the enhancements
        engine.computeEnhancements(ci);
        //validate the enhancement results
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(DC_CREATOR,LiteralFactory.getInstance().createTypedLiteral(
            engine.getClass().getName()));
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        //validate create fise:TextAnnotations
        int numTextAnnotations = validateAllTextAnnotations(ci.getMetadata(), TEST_TEXT, expectedValues);
        assertEquals("Four fise:TextAnnotations are expected by this Test", 4, numTextAnnotations);
        //validate create fise:EntityAnnotations
        int numEntityAnnotations = validateAllEntityAnnotations(ci, expectedValues);
        assertEquals("Five fise:EntityAnnotations are expected by this Test", 5, numEntityAnnotations);
    }
    /**
     * Similar to {@link EnhancementStructureHelper#validateAllEntityAnnotations(org.apache.clerezza.commons.rdf.Graph, Map)}
     * but in addition checks fise:confidence [0..1] and entityhub:site properties
     * @param ci
     * @param expectedValues
     * @return
     */
    private static int validateAllEntityAnnotations(ContentItem ci, Map<IRI,RDFTerm> expectedValues){
        Iterator<Triple> entityAnnotationIterator = ci.getMetadata().filter(null,
                RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            IRI entityAnnotation = (IRI) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateEntityAnnotation(ci.getMetadata(), entityAnnotation, expectedValues);
            //validate also that the confidence is between [0..1]
            Iterator<Triple> confidenceIterator = ci.getMetadata().filter(entityAnnotation, ENHANCER_CONFIDENCE, null);
            //Confidence is now checked by the EnhancementStructureHelper (STANBOL-630)
//            assertTrue("Expected fise:confidence value is missing (entityAnnotation "
//                    +entityAnnotation+")",confidenceIterator.hasNext());
//            Double confidence = LiteralFactory.getInstance().createObject(Double.class,
//                (TypedLiteral)confidenceIterator.next().getObject());
//            assertTrue("fise:confidence MUST BE <= 1 (value= '"+confidence
//                    + "',entityAnnotation " +entityAnnotation+")",
//                    1.0 >= confidence.doubleValue());
//            assertTrue("fise:confidence MUST BE >= 0 (value= '"+confidence
//                    +"',entityAnnotation "+entityAnnotation+")",
//                    0.0 <= confidence.doubleValue());
            //Test the entityhub:site property (STANBOL-625)
            IRI ENTITYHUB_SITE = new IRI(NamespaceEnum.entityhub+"site");
            Iterator<Triple> entitySiteIterator = ci.getMetadata().filter(entityAnnotation, 
                ENTITYHUB_SITE, null);
            assertTrue("Expected entityhub:site value is missing (entityAnnotation "
                    +entityAnnotation+")",entitySiteIterator.hasNext());
            RDFTerm siteResource = entitySiteIterator.next().getObject();
            assertTrue("entityhub:site values MUST BE Literals", siteResource instanceof Literal);
            assertEquals("'"+TEST_REFERENCED_SITE_NAME+"' is expected as "
                + "entityhub:site value", TEST_REFERENCED_SITE_NAME, 
                ((Literal)siteResource).getLexicalForm());
            assertFalse("entityhub:site MUST HAVE only a single value", entitySiteIterator.hasNext());
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
        
    }
}
