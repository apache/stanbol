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

import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNamedEntityExtractionEnhancementEngine extends Assert {

    public static final String SINGLE_SENTENCE = "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the University of Otago.";
    
    public static final String SINGLE_SENTENCE_WITH_CONTROL_CHARS = "Dr Patrick Marshall (1869 - November 1950) was a" 
    		+ " \u0014geologist\u0015 who lived in New Zealand and worked at the University of Otago.";

    public static final String MULTI_SENTENCES = "The life of Patrick Marshall\n\n"
            + "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the"
            + " University of Otago. This is another unrelated sentence"
            + " without any name.\n"
            + "A new paragraph is being written. This paragraph has two sentences.";

    private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    static NEREngineCore nerEngine;
    
    public static final String FAKE_BUNDLE_SYMBOLIC_NAME = "FAKE_BUNDLE_SYMBOLIC_NAME";

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUpServices() throws IOException {
        nerEngine = new NEREngineCore(new ClasspathDataFileProvider(FAKE_BUNDLE_SYMBOLIC_NAME),
            "en",Collections.EMPTY_SET);
    }

    public static ContentItem wrapAsContentItem(final String id,
            final String text) throws IOException {
    	return ciFactory.createContentItem(new UriRef(id),new StringSource(text));
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
        ContentItem ci = wrapAsContentItem("my doc id", SINGLE_SENTENCE);
        nerEngine.computeEnhancements(ci);
        Map<UriRef,Resource> expectedValues = new HashMap<UriRef,Resource>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(nerEngine.getClass().getName()));
        MGraph g = ci.getMetadata();
        int textAnnotationCount = validateAllTextAnnotations(g,SINGLE_SENTENCE,expectedValues);
        assertEquals(3, textAnnotationCount);
    }

}