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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
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

    static NEREngineCore nerEngine;
    
    public static final String FAKE_BUNDLE_SYMBOLIC_NAME = "FAKE_BUNDLE_SYMBOLIC_NAME";

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUpServices() throws IOException {
        nerEngine = new NEREngineCore(new ClasspathDataFileProvider(FAKE_BUNDLE_SYMBOLIC_NAME),
            "en",Collections.EMPTY_SET);
    }

    public static ContentItem wrapAsContentItem(final String id,
            final String text) {
    	return new InMemoryContentItem(id, text, "text/plain");
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
            throws EngineException {
        ContentItem ci = wrapAsContentItem("my doc id", SINGLE_SENTENCE);
        nerEngine.computeEnhancements(ci);
        MGraph g = ci.getMetadata();
        int textAnnotationCount = checkAllTextAnnotations(g,SINGLE_SENTENCE);
        assertEquals(3, textAnnotationCount);

        //This Engine dose no longer create entityAnnotations
//        int entityAnnotationCount = checkAllEntityAnnotations(g);
//        assertEquals(2, entityAnnotationCount);
    }

    /*
     * -----------------------------------------------------------------------
     * Helper Methods to check Text and EntityAnnotations
     * -----------------------------------------------------------------------
     */

    private int checkAllTextAnnotations(MGraph g, String content) {
        Iterator<Triple> textAnnotationIterator = g.filter(null,
                RDF_TYPE, ENHANCER_TEXTANNOTATION);
        // test if a textAnnotation is present
        assertTrue(textAnnotationIterator.hasNext());
        int textAnnotationCount = 0;
        while (textAnnotationIterator.hasNext()) {
            UriRef textAnnotation = (UriRef) textAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkTextAnnotation(g, textAnnotation,content);
            textAnnotationCount++;
        }
        return textAnnotationCount;
    }

    /**
     * Checks if a text annotation is valid
     */
    private void checkTextAnnotation(MGraph g, UriRef textAnnotation, String content) {
        Iterator<Triple> selectedTextIterator = g.filter(textAnnotation,
                ENHANCER_SELECTED_TEXT, null);
        // check if the selected text is added
        assertTrue(selectedTextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        Resource object = selectedTextIterator.next().getObject();
        assertTrue(object instanceof Literal);
        Literal selectedText = (Literal)object;
        object = null;
        assertTrue(SINGLE_SENTENCE.contains(selectedText.getLexicalForm()));
        // test if context is added
        Iterator<Triple> selectionContextIterator = g.filter(textAnnotation,
                ENHANCER_SELECTION_CONTEXT, null);
        assertTrue(selectionContextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        object = selectionContextIterator.next().getObject();
        assertTrue(object instanceof Literal);
        assertTrue(SINGLE_SENTENCE.contains(((Literal) object).getLexicalForm()));
        object = null;
        //test start/end if present
        Iterator<Triple> startPosIterator = g.filter(textAnnotation,
                ENHANCER_START, null);
        Iterator<Triple> endPosIterator = g.filter(textAnnotation,
                ENHANCER_END, null);
        //start end is optional, but if start is present, that also end needs to be set
        if(startPosIterator.hasNext()){
            Resource resource = startPosIterator.next().getObject();
            //only a single start position is supported
            assertTrue(!startPosIterator.hasNext());
            assertTrue(resource instanceof TypedLiteral);
            TypedLiteral startPosLiteral = (TypedLiteral) resource;
            resource = null;
            int start = LiteralFactory.getInstance().createObject(Integer.class, startPosLiteral);
            startPosLiteral = null;
            //now get the end
            //end must be defined if start is present
            assertTrue(endPosIterator.hasNext());
            resource = endPosIterator.next().getObject();
            //only a single end position is supported
            assertTrue(!endPosIterator.hasNext());
            assertTrue(resource instanceof TypedLiteral);
            TypedLiteral endPosLiteral = (TypedLiteral) resource;
            resource = null;
            int end = LiteralFactory.getInstance().createObject(Integer.class, endPosLiteral);
            endPosLiteral = null;
            //check for equality of the selected text and the text on the selected position in the content
            //System.out.println("TA ["+start+"|"+end+"]"+selectedText.getLexicalForm()+"<->"+content.substring(start,end));
            assertEquals(content.substring(start, end), selectedText.getLexicalForm());
        } else {
            //if no start position is present, there must also be no end position defined
            assertTrue(!endPosIterator.hasNext());
        }
    }
}