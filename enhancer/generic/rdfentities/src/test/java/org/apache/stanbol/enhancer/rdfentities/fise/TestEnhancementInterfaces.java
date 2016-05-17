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
package org.apache.stanbol.enhancer.rdfentities.fise;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItem;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.rdfentities.RdfEntityFactory;
import org.apache.stanbol.enhancer.rdfentities.fise.EntityAnnotation;
import org.apache.stanbol.enhancer.rdfentities.fise.TextAnnotation;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.junit.Test;


/**
 * Tests if the FISE enhancement structure RDF facades
 *
 * @author Rupert Westenthaler
 */
public class TestEnhancementInterfaces {

    public static final String SINGLE_SENTENCE = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";
    protected static final ContentSource SINGLE_SENTENCE_SOURCE = new StringSource(SINGLE_SENTENCE);
    public static final IRI TEST_ENHANCEMENT_ENGINE_URI = new IRI("urn:test:dummyEnhancementEngine");
    private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    public static ContentItem createContentItem(ContentSource cs) throws IOException {
    	return ciFactory.createContentItem(cs);
    }

    @Test
    public void testEnhancementInterfaces() throws Exception {
        ContentItem ci = createContentItem(SINGLE_SENTENCE_SOURCE);
        IRI ciUri = new IRI(ci.getUri().getUnicodeString());
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        long start = System.currentTimeMillis();
        //create an Text Annotation representing an extracted Person
        TextAnnotation personAnnotation = factory.getProxy(
                createEnhancementURI(), TextAnnotation.class);
        personAnnotation.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        personAnnotation.setCreated(new Date());
        personAnnotation.setExtractedFrom(ciUri);
        personAnnotation.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/text#Person"));
        personAnnotation.setConfidence(0.8);
        personAnnotation.setSelectedText("Patrick Marshall");
        personAnnotation.setStart(SINGLE_SENTENCE.indexOf(personAnnotation.getSelectedText()));
        personAnnotation.setEnd(personAnnotation.getStart()+personAnnotation.getSelectedText().length());
        personAnnotation.setSelectionContext(SINGLE_SENTENCE);

        //create an Text Annotation representing an extracted Location
        TextAnnotation locationAnnotation = factory.getProxy(
                createEnhancementURI(),    TextAnnotation.class);
        locationAnnotation.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        locationAnnotation.setCreated(new Date());
        locationAnnotation.setExtractedFrom(ciUri);
        locationAnnotation.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/text#Location"));
        locationAnnotation.setConfidence(0.78);
        locationAnnotation.setSelectedText("New Zealand");
        locationAnnotation.setStart(SINGLE_SENTENCE.indexOf(locationAnnotation.getSelectedText()));
        locationAnnotation.setEnd(locationAnnotation.getStart()+locationAnnotation.getSelectedText().length());
        locationAnnotation.setSelectionContext(SINGLE_SENTENCE);

        //create an Text Annotation representing an extracted Organisation
        TextAnnotation orgAnnotation = factory.getProxy(
                createEnhancementURI(),    TextAnnotation.class);
        orgAnnotation.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        orgAnnotation.setCreated(new Date());
        orgAnnotation.setExtractedFrom(ciUri);
        orgAnnotation.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/text#Organisation"));
        orgAnnotation.setConfidence(0.78);
        orgAnnotation.setSelectedText("University of Otago");
        orgAnnotation.setStart(SINGLE_SENTENCE.indexOf(orgAnnotation.getSelectedText()));
        orgAnnotation.setEnd(orgAnnotation.getStart()+orgAnnotation.getSelectedText().length());
        orgAnnotation.setSelectionContext(SINGLE_SENTENCE);

        // create an Entity Annotation for the person TextAnnotation
        EntityAnnotation patrickMarshall = factory.getProxy(
                createEnhancementURI(), EntityAnnotation.class);
        patrickMarshall.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        patrickMarshall.setCreated(new Date());
        patrickMarshall.setExtractedFrom(ciUri);
        patrickMarshall.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/entity#Entity"));
        patrickMarshall.setConfidence(0.56);
        patrickMarshall.getRelations().add(personAnnotation);
        patrickMarshall.setEntityLabel("Patrick Marshall");
        patrickMarshall.setEntityReference(new IRI("http://rdf.freebase.com/rdf/en/patrick_marshall"));
        patrickMarshall.getEntityTypes().addAll(Arrays.asList(
                        new IRI("http://rdf.freebase.com/ns/people.person"),
                        new IRI("http://rdf.freebase.com/ns/common.topic"),
                        new IRI("http://rdf.freebase.com/ns/education.academic")));
        // and an other for New Zealand
        EntityAnnotation newZealand = factory.getProxy(
                createEnhancementURI(), EntityAnnotation.class);
        newZealand.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        newZealand.setCreated(new Date());
        newZealand.setExtractedFrom(ciUri);
        newZealand.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/entity#Entity"));
        newZealand.setConfidence(0.98);
        newZealand.getRelations().add(locationAnnotation);
        newZealand.setEntityLabel("New Zealand");
        newZealand.setEntityReference(new IRI("http://rdf.freebase.com/rdf/en/new_zealand"));
        newZealand.getEntityTypes().addAll(Arrays.asList(
                new IRI("http://rdf.freebase.com/ns/location.location"),
                new IRI("http://rdf.freebase.com/ns/common.topic"),
                new IRI("http://rdf.freebase.com/ns/location.country")));

        // and an other option for New Zealand
        EntityAnnotation airNewZealand = factory.getProxy(
                createEnhancementURI(), EntityAnnotation.class);
        airNewZealand.setCreator(TEST_ENHANCEMENT_ENGINE_URI);
        airNewZealand.setCreated(new Date());
        airNewZealand.setExtractedFrom(ciUri);
        airNewZealand.getDcType().add(new IRI("http://www.example.org/cv/annotatation-types/entity#Entity"));
        airNewZealand.setConfidence(0.36);
        airNewZealand.getRelations().add(locationAnnotation);
        airNewZealand.setEntityLabel("New Zealand");
        airNewZealand.setEntityReference(new IRI("http://rdf.freebase.com/rdf/en/air_new_zealand"));
        airNewZealand.getEntityTypes().addAll(Arrays.asList(
                new IRI("http://rdf.freebase.com/ns/business.sponsor"),
                new IRI("http://rdf.freebase.com/ns/common.topic"),
                new IRI("http://rdf.freebase.com/ns/travel.transport_operator"),
                new IRI("http://rdf.freebase.com/ns/aviation.airline"),
                new IRI("http://rdf.freebase.com/ns/aviation.aircraft_owner"),
                new IRI("http://rdf.freebase.com/ns/business.employer"),
                new IRI("http://rdf.freebase.com/ns/freebase.apps.hosts.com.appspot.acre.juggle.juggle"),
                new IRI("http://rdf.freebase.com/ns/business.company")));
        System.out.println("creation time "+(System.currentTimeMillis()-start)+"ms");

        //now test the enhancement
        int numberOfTextAnnotations = checkAllTextAnnotations(ci.getMetadata());
        assertEquals(3, numberOfTextAnnotations);

        int numberOfEntityAnnotations = checkAllEntityAnnotations(ci.getMetadata());
        assertEquals(3, numberOfEntityAnnotations);
    }

    private static IRI createEnhancementURI() {
        //TODO: add some Utility to create Instances to the RdfEntityFactory
        //      this should create a new URI by some default Algorithm
        return new IRI("urn:enhancement-" + EnhancementEngineHelper.randomUUID());
    }

    /*
     * -----------------------------------------------------------------------
     * Helper Methods to check Text and EntityAnnotations
     * -----------------------------------------------------------------------
     */

    private int checkAllEntityAnnotations(Graph g) {
        Iterator<Triple> entityAnnotationIterator = g.filter(null,
                RDF_TYPE, TechnicalClasses.ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            IRI entityAnnotation = (IRI) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkEntityAnnotation(g, entityAnnotation);
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
    }

    private int checkAllTextAnnotations(Graph g) {
        Iterator<Triple> textAnnotationIterator = g.filter(null,
                RDF_TYPE, ENHANCER_TEXTANNOTATION);
        // test if a textAnnotation is present
        assertTrue("Expecting non-empty textAnnotationIterator", textAnnotationIterator.hasNext());
        int textAnnotationCount = 0;
        while (textAnnotationIterator.hasNext()) {
            IRI textAnnotation = (IRI) textAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkTextAnnotation(g, textAnnotation);
            textAnnotationCount++;
        }
        return textAnnotationCount;
    }

    /**
     * Checks if a text annotation is valid.
     */
    private void checkTextAnnotation(Graph g, IRI textAnnotation) {
        Iterator<Triple> selectedTextIterator = g.filter(textAnnotation,
                ENHANCER_SELECTED_TEXT, null);
        // check if the selected text is added
        assertTrue(selectedTextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        RDFTerm object = selectedTextIterator.next().getObject();
        assertTrue(object instanceof Literal);
        assertTrue(SINGLE_SENTENCE.contains(((Literal) object).getLexicalForm()));
        // test if context is added
        Iterator<Triple> selectionContextIterator = g.filter(textAnnotation,
                ENHANCER_SELECTION_CONTEXT, null);
        assertTrue(selectionContextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        object = selectionContextIterator.next().getObject();
        assertTrue(object instanceof Literal);
        assertTrue(SINGLE_SENTENCE.contains(((Literal) object).getLexicalForm()));
    }

    /**
     * Checks if an entity annotation is valid.
     */
    private void checkEntityAnnotation(Graph g, IRI entityAnnotation) {
        Iterator<Triple> relationToTextAnnotationIterator = g.filter(
                entityAnnotation, DC_RELATION, null);
        // check if the relation to the text annotation is set
        assertTrue(relationToTextAnnotationIterator.hasNext());
        while (relationToTextAnnotationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            IRI referredTextAnnotation = (IRI) relationToTextAnnotationIterator.next().getObject();
            assertTrue(g.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_TEXTANNOTATION).hasNext());
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = g.filter(entityAnnotation,
                ENHANCER_ENTITY_REFERENCE, null);
        assertTrue(entityReferenceIterator.hasNext());
        // test if the reference is an URI
        assertTrue(entityReferenceIterator.next().getObject() instanceof IRI);
        // test if there is only one entity referred
        assertFalse(entityReferenceIterator.hasNext());

        // finally test if the entity label is set
        Iterator<Triple> entityLabelIterator = g.filter(entityAnnotation,
                ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
    }

}
