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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNamedEntityExtractionEnhancementEngine extends Assert {

    public static final String SINGLE_SENTENCE = "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the University of Otago.";
    
    public static final String SINGLE_SENTENCE_WITH_CONTROL_CHARS = "Dr Patrick Marshall (1869 - November 1950) was a" 
    		+ " \u0014geologist\u0015 who lived in New\tZealand and worked at the University\nof Otago.";

    public static final String MULTI_SENTENCES = "The life of Patrick Marshall\n\n"
            + "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the"
            + " University of Otago. This is another unrelated sentence"
            + " without any name.\n"
            + "A new paragraph is being written. This paragraph has two sentences.";

    
    public static final String EHEALTH = "Whereas activation of the HIV-1 enhancer following T-cell " 
            + "stimulation is mediated largely through binding of the transcription factor NF-kappa "
            + "B to two adjacent kappa B sites in the HIV-1 long terminal repeat, activation of the "
            + "HIV-2 enhancer in monocytes and T cells is dependent on four cis-acting elements : a "
            + "single kappa B site, two purine-rich binding sites , PuB1 and PuB2 , and a pets site .";
    
    private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private NEREngineCore nerEngine;
    
    public static final String FAKE_BUNDLE_SYMBOLIC_NAME = "FAKE_BUNDLE_SYMBOLIC_NAME";
    public static OpenNLP openNLP;
    
    @BeforeClass
    public static void initDataFileProvicer(){
        DataFileProvider dataFileProvider = new ClasspathDataFileProvider(FAKE_BUNDLE_SYMBOLIC_NAME);
        openNLP = new OpenNLP(dataFileProvider);
    }
    
    @Before
    public void setUpServices() throws IOException {
        nerEngine = new NEREngineCore(openNLP,
            new NEREngineConfig()){};
    }

    public static ContentItem wrapAsContentItem(final String id,
            final String text, String language) throws IOException {
    	ContentItem ci =  ciFactory.createContentItem(new IRI(id),new StringSource(text));
    	if(language != null){
    	    ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl(language)));
    	}
    	return ci;
    }

    @Test
    public void testPersonNamesExtraction() {
        Collection<String> names = nerEngine.extractPersonNames(SINGLE_SENTENCE);
        assertEquals(1, names.size());
        assertTrue(names.contains("Patrick Marshall"));
    }

    @Test
    public void testPersonNameOccurrencesExtraction() {
        Map<String, List<NameOccurrence>> nameOccurrences = nerEngine.extractPersonNameOccurrences(MULTI_SENTENCES);
        assertEquals(1, nameOccurrences.size());

        List<NameOccurrence> pmOccurrences = nameOccurrences.get("Patrick Marshall");
        assertNotNull(pmOccurrences);
        assertEquals(2, pmOccurrences.size());

        NameOccurrence firstOccurrence = pmOccurrences.get(0);
        assertEquals("Patrick Marshall", firstOccurrence.name);
        assertEquals(12, firstOccurrence.start.intValue());
        assertEquals(28, firstOccurrence.end.intValue());
        assertEquals(0.998, firstOccurrence.confidence, 0.05);

        NameOccurrence secondOccurrence = pmOccurrences.get(1);
        assertEquals("Patrick Marshall", secondOccurrence.name);
        assertEquals(33, secondOccurrence.start.intValue());
        assertEquals(49, secondOccurrence.end.intValue());
        assertEquals(0.997, secondOccurrence.confidence, 0.05);
    }

    @Test
    public void testPersonNameOccurrencesExtractionWithControlChars() {
        Map<String, List<NameOccurrence>> nameOccurrences = nerEngine.extractPersonNameOccurrences(SINGLE_SENTENCE_WITH_CONTROL_CHARS);
        assertEquals(1, nameOccurrences.size());

        List<NameOccurrence> pmOccurrences = nameOccurrences.get("Patrick Marshall");
        assertNotNull(pmOccurrences);
        assertEquals(1, pmOccurrences.size());

        NameOccurrence firstOccurrence = pmOccurrences.get(0);
        assertEquals("Patrick Marshall", firstOccurrence.name);
        assertFalse(firstOccurrence.context.contains("\u0014"));
        assertTrue(firstOccurrence.context.contains("\t"));
        assertTrue(firstOccurrence.context.contains("\n"));
    }
    
    @Test
    public void testLocationNamesExtraction() {
        Collection<String> names = nerEngine.extractLocationNames(SINGLE_SENTENCE);
        assertEquals(1, names.size());
        assertTrue(names.contains("New Zealand"));
    }

    @Test
    public void testComputeEnhancements()
            throws EngineException, IOException {
        ContentItem ci = wrapAsContentItem("urn:test:content-item:single:sentence", SINGLE_SENTENCE,"en");
        nerEngine.computeEnhancements(ci);
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(nerEngine.getClass().getName()));
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        Graph g = ci.getMetadata();
        int textAnnotationCount = validateAllTextAnnotations(g,SINGLE_SENTENCE,expectedValues);
        assertEquals(3, textAnnotationCount);
    }
    @Test
    public void testCustomModel() throws EngineException, IOException {
        ContentItem ci = wrapAsContentItem("urn:test:content-item:single:sentence", EHEALTH,"en");
        //this test does not use default models
        nerEngine.config.getDefaultModelTypes().clear(); 
        //but instead a custom model provided by the test data
        nerEngine.config.addCustomNameFinderModel("en", "bionlp2004-DNA-en.bin");
        nerEngine.config.setMappedType("DNA", new IRI("http://www.bootstrep.eu/ontology/GRO#DNA"));
        nerEngine.computeEnhancements(ci);
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(nerEngine.getClass().getName()));
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        //and dc:type values MUST be the URI set as mapped type
        expectedValues.put(Properties.DC_TYPE, new IRI("http://www.bootstrep.eu/ontology/GRO#DNA"));
        Graph g = ci.getMetadata();
        int textAnnotationCount = validateAllTextAnnotations(g,EHEALTH,expectedValues);
        assertEquals(7, textAnnotationCount);
    }
    

}