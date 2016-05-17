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
package org.apache.stanbol.enhancer.engines.keywordextraction.engine;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import opennlp.tools.tokenize.SimpleTokenizer;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.TextAnalyzerConfig;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.keywordextraction.engine.KeywordLinkingEngine;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.ClasspathDataFileProvider;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.TestSearcherImpl;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.OpenNlpAnalysedContentFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
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
public class KeywordLinkingEngineTest {
    
    private final static Logger log = LoggerFactory.getLogger(KeywordLinkingEngineTest.class);

    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String TEST_TEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";
    public static final String TEST_TEXT2 = "A CBS televised debate between Australia's " +
    		"candidates for Prime Minister in the upcoming US election has been rescheduled " +
    		"and shortend, to avoid a clash with popular cookery sow MasterChef.";
    
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    private static final String TEST_REFERENCED_SITE_NAME = "dummRefSiteName";
    
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
        //add a 2nd Otago (Place and University
        rep = factory.createRepresentation("urn:test:Otago_Texas");
        rep.addNaturalText(NAME, "Otago (Texas)");
        rep.addNaturalText(NAME, "Otago");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_PLACE.getUnicodeString());
        searcher.addEntity(rep);
        rep = factory.createRepresentation("urn:test:UniversityOfOtago_Texas");
        rep.addNaturalText(NAME, "University of Otago (Texas)");
        rep.addReference(TYPE, OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString());
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

    public static ContentItem getContentItem(final String id, final String text) throws IOException {
        return ciFactory.createContentItem(new IRI(id),new StringSource(text));
    }
    /**
     * This tests the EntityLinker functionality (if the expected Entities
     * are linked)
     * @throws Exception
     */
    @Test
    public void testTaxonomyLinker() throws Exception {
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
                Arrays.asList("urn:test:UniversityOfOtago","urn:test:UniversityOfOtago_Texas")));
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
        KeywordLinkingEngine engine = KeywordLinkingEngine.createInstance(openNLP, searcher, new TextAnalyzerConfig(), 
            linkerConfig);
        engine.referencedSiteName = TEST_REFERENCED_SITE_NAME;
        ContentItem ci = ciFactory.createContentItem(new StringSource(TEST_TEXT));
        //tells the engine that this is an English text
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("en")));
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
            IRI ENTITYHUB_SITE = new IRI(RdfResourceEnum.site.getUri());
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
