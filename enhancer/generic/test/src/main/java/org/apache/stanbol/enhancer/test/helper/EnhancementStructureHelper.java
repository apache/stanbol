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
package org.apache.stanbol.enhancer.test.helper;

import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TOPICANNOTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.CONFIDENCE_LEVEL_ENUM;
import org.junit.Assert;

public final class EnhancementStructureHelper {

   /**
    * Restrict instantiation
    */
    private EnhancementStructureHelper() {}

    private static final LiteralFactory lf = LiteralFactory.getInstance();
    
    /**
     * Validates all TextAnnotations contained in the parsed enhancement graph
     * @param enhancements the enhancement graph
     * @param content the enhanced content
     * @param expectedValues the expected values of all validated EntityAnnotations.
     * Properties are used as keys. Typical example would be fise:extracted-from
     * with the id of the ContentItem as value; dc-terms:creator with the
     * {@link Class#getName()} as value.
     * @return the number of found TextAnnotations
     */
    public static int validateAllTextAnnotations(Graph enhancements, String content, Map<IRI,RDFTerm> expectedValues) {
        return validateAllTextAnnotations(enhancements,content,expectedValues,false);
    }
    /**
     * Validates all TextAnnotations contained in the parsed enhancement graph.
     * If <code>validatePrefixSuffix</code> is
     * enabled the fise:selection-prefix and fise:selection-suffix (as defined by
     * <a href="https://issues.apache.org/jira/browse/STANBOL-987">STANBOL-987</a>
     * are enforced and validated. If disabled those properties are not enforced but still
     * validated when present.
     * @param enhancements the enhancement graph
     * @param content the enhanced content
     * @param expectedValues the expected values of all validated EntityAnnotations.
     * Properties are used as keys. Typical example would be fise:extracted-from
     * with the id of the ContentItem as value; dc-terms:creator with the
     * {@link Class#getName()} as value.
     * @param validatePrefixSuffix enforce the presence of fise:selection-prefix and 
     * fise:selection-suffix if fise:start and fise:end are set.
     * @return the number of found TextAnnotations
     */
    @SuppressWarnings("unchecked")
    public static int validateAllTextAnnotations(Graph enhancements, String content, Map<IRI,RDFTerm> expectedValues, boolean validatePrefixSuffix) {
        expectedValues = expectedValues == null ? Collections.EMPTY_MAP : expectedValues;
        Iterator<Triple> textAnnotationIterator = enhancements.filter(null,
                RDF_TYPE, ENHANCER_TEXTANNOTATION);
        // test if a textAnnotation is present
        //assertTrue(textAnnotationIterator.hasNext()); 
        //  -> this might be used to test that there are no TextAnnotations
        int textAnnotationCount = 0;
        while (textAnnotationIterator.hasNext()) {
            IRI textAnnotation = (IRI) textAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateTextAnnotation(enhancements, textAnnotation,content,expectedValues, validatePrefixSuffix);
            textAnnotationCount++;
        }
        return textAnnotationCount;
    }
        
    /**
     * Validates the parsed TextAnnotation with a fise:selected-text. This
     * method also validates rules defined by fise:Enhancement by calling
     * {@link #validateEnhancement(Graph, IRI, Map)}<p>
     * NOTE: this method MUST NOT be used to validate fise:TextAnnotations that
     * do NOT select a part of the text - meaning TextAnnotations about the
     * whole parsed content.
     * @param enhancements the enhancements graph containing the text annotation
     * @param textAnnotation the TextAnnotation to validate
     * @param content the enhanced content
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateTextAnnotation(Graph enhancements, IRI textAnnotation, String content, Map<IRI,RDFTerm> expectedValues) {
        validateTextAnnotation(enhancements,textAnnotation,content,expectedValues,false);
    }
    /**
     * Validates fise:TextAnnotations. If <code>validatePrefixSuffix</code> is
     * enabled the fise:selection-prefix and fise:selection-suffix (as defined by
     * <a href="https://issues.apache.org/jira/browse/STANBOL-987">STANBOL-987</a>
     * are enforced and validated. If disabled those properties are not enforced but still
     * validated when present.
     * @param enhancements the enhancements graph containing the text annotation
     * @param textAnnotation the TextAnnotation to validate
     * @param content the enhanced content
     * @param expectedValues expected values (properties for the values are used as keys)
     * @param validatePrefixSuffix enforce the presence of fise:selection-prefix and 
     * fise:selection-suffix if fise:start and fise:end are set.
     */
    public static void validateTextAnnotation(Graph enhancements, IRI textAnnotation, String content, Map<IRI,RDFTerm> expectedValues, boolean validatePrefixSuffix) {
        //validate the rdf:type
        Iterator<Triple> rdfTypeIterator = enhancements.filter(textAnnotation, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        assertTrue("Parsed Enhancement "+textAnnotation +" is missing the fise:TextAnnotation type ",
            rdfTypeIterator.hasNext());
        Iterator<Triple> selectedTextIterator = enhancements.filter(textAnnotation,
                ENHANCER_SELECTED_TEXT, null);
        // check if the selected text is added (or not)
        RDFTerm selectedTextResource;
        if(selectedTextIterator.hasNext()){
            // test if the selected text is part of the TEXT_TO_TEST
            selectedTextResource = selectedTextIterator.next().getObject();
            assertTrue("fise:selected-text MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                selectedTextResource instanceof Literal);
            Literal selectedText = (Literal)selectedTextResource;
            assertTrue("The parsed content MUST contain the fise:selected-text value '"
                +selectedText.getLexicalForm()+"' (uri: "+textAnnotation+")!",content.contains(selectedText.getLexicalForm()));
            Assert.assertFalse("fise:selected-text MUST be single valued (uri: "+textAnnotation+")",selectedTextIterator.hasNext());
        } else {
            selectedTextResource = null; //no selected text
        }
        //check against an expected value
        RDFTerm expectedSelectedText = expectedValues.get(ENHANCER_SELECTED_TEXT);
        if(expectedSelectedText != null){
            assertEquals("The fise:selected-text is not the expected value "+expectedSelectedText+" (uri: "+textAnnotation+")!",
                expectedSelectedText, selectedTextResource);
        }
        //check for fise:selection-head and fise:selection-tail (STANBOL-987)
        Iterator<Triple> selectionHeadIterator = enhancements.filter(textAnnotation, Properties.ENHANCER_SELECTION_HEAD, null);
        if(selectedTextResource != null){
            Assert.assertFalse("If fise:selected-text is present fise:selection-head MUST NOT be present",selectionHeadIterator.hasNext());
        }
        RDFTerm selectionHeadResource;
        if(selectionHeadIterator.hasNext()){
            // test if the selected text is part of the TEXT_TO_TEST
            selectionHeadResource = selectionHeadIterator.next().getObject();
            assertTrue("fise:selection-head MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                selectionHeadResource instanceof Literal);
            Literal selectionHeadText = (Literal)selectionHeadResource;
            assertTrue("The parsed content MUST contain the fise:selected-head value '"
                +selectionHeadText.getLexicalForm()+"' (uri: "+textAnnotation+")!",content.contains(selectionHeadText.getLexicalForm()));
            Assert.assertFalse("fise:selection-head MUST be single valued (uri: "+textAnnotation+")",selectionHeadIterator.hasNext());
        } else {
            selectionHeadResource = null;
        }
        
        Iterator<Triple> selectionTailIterator = enhancements.filter(textAnnotation, Properties.ENHANCER_SELECTION_TAIL, null);
        if(selectedTextResource != null){
            Assert.assertFalse("If fise:selected-text is present fise:selection-tail MUST NOT be present",selectionTailIterator.hasNext());
        }
        RDFTerm selectionTailResource;
        if(selectionTailIterator.hasNext()){
            // test if the selected text is part of the TEXT_TO_TEST
            selectionTailResource = selectionTailIterator.next().getObject();
            assertTrue("fise:selection-head MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                selectionTailResource instanceof Literal);
            Literal selectionTailText = (Literal)selectionTailResource;
            assertTrue("The parsed content MUST contain the fise:selected-tail value '"
                +selectionTailText.getLexicalForm()+"' (uri: "+textAnnotation+")!",content.contains(selectionTailText.getLexicalForm()));
            Assert.assertFalse("fise:selection-tail MUST be single valued (uri: "+textAnnotation+")",selectionTailIterator.hasNext());
        } else {
            selectionTailResource = null;
        }
        Assert.assertTrue("Both fise:selection-tail AND fise:selection-head MUST BE defined "
            +"(if one of them is present) (uri: "+textAnnotation+")",
            (selectionHeadResource != null && selectionTailResource != null) ||
            (selectionHeadResource == null && selectionTailResource == null));
        
        RDFTerm selectionContextResource;
        // test if context is added
        Iterator<Triple> selectionContextIterator = enhancements.filter(textAnnotation,
                ENHANCER_SELECTION_CONTEXT, null);
        if(selectionContextIterator.hasNext()) { //context is optional
            //selection context is not allowed without selected-text
            assertTrue("If fise:selection-context is present also fise:selected-text or fise:selection-head and fise:selection-tail MUST BE present (uri: "+textAnnotation+")",
                selectedTextResource != null || (selectionHeadResource != null && selectionTailResource != null));
            // test if the selected text is part of the TEXT_TO_TEST
            selectionContextResource = selectionContextIterator.next().getObject();
            assertTrue("The fise:selection-context MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                selectionContextResource instanceof Literal);
            //check that the content contains the context
            assertTrue("The fise:selection-context MUST BE contained in the Content | context= "+ selectionContextResource,
            content.contains(((Literal)selectionContextResource).getLexicalForm()));
            //check that the context contains the selected text
            if(selectedTextResource != null){
                assertTrue("The fise:selected-text value MUST BE containted within the fise:selection-context value",
                    ((Literal)selectionContextResource).getLexicalForm().contains(
                        ((Literal)selectedTextResource).getLexicalForm()));
            }
            if(selectionHeadResource != null){
                assertTrue("The fise:selection-head value MUST BE containted within the fise:selection-context value",
                    ((Literal)selectionContextResource).getLexicalForm().contains(
                        ((Literal)selectionHeadResource).getLexicalForm()));
            }
            if(selectionTailResource != null){
                assertTrue("The fise:selection-tail value MUST BE containted within the fise:selection-context value",
                    ((Literal)selectionContextResource).getLexicalForm().contains(
                        ((Literal)selectionTailResource).getLexicalForm()));
            }
        } else {
            assertNull("If no fise:selection-context is present also fise:selected-text MUST BE NOT present!", selectedTextResource);
            selectionContextResource = null;
        }
        RDFTerm expectedSelectionContext = expectedValues.get(ENHANCER_SELECTION_CONTEXT);
        if(expectedSelectionContext != null){
            assertEquals("The value of fise:selection-context has not the expected value "+expectedSelectionContext,
                expectedSelectionContext, selectionContextResource);
        }
        //test start/end if present
        Iterator<Triple> startPosIterator = enhancements.filter(textAnnotation,
                ENHANCER_START, null);
        Iterator<Triple> endPosIterator = enhancements.filter(textAnnotation,
                ENHANCER_END, null);
        //start end is optional, but if start is present, that also end needs to be set
        Literal startPosLiteral;
        Literal endPosLiteral;
        if(startPosIterator.hasNext()){
            //NOTE: TextAnnotations might be use to select whole sections of a text
            //      (e.g. see STANBOL-617) in those cases adding the text of the
            //      whole section is not feasible.
            //assertNotNull("If fise:start is present the fise:selection-context MUST also be present (uri: "+textAnnotation+")!",
            //    selectionContextResource);
            RDFTerm resource = startPosIterator.next().getObject();
            //only a single start position is supported
            assertFalse("fise:start MUST HAVE only a single value (uri: "+textAnnotation+")!",startPosIterator.hasNext());
            assertTrue("fise:start MUST be a typed Literal (uri: "+textAnnotation+")!",resource instanceof Literal);
            startPosLiteral = (Literal) resource;
            assertEquals("fise:start MUST use xsd:int as data type (uri: "+textAnnotation+")",XSD.int_, startPosLiteral.getDataType());
            resource = null;
            Integer start = LiteralFactory.getInstance().createObject(Integer.class, startPosLiteral);
            assertNotNull("Unable to parse Integer from TypedLiteral "+startPosLiteral,start);
            //now get the end
            //end must be defined if start is present
            assertTrue("If fise:start is present also fise:end MUST BE defined (uri: "+textAnnotation+")!",endPosIterator.hasNext());
            resource = endPosIterator.next().getObject();
            //only a single end position is supported
            assertFalse("fise:end MUST HAVE only a single value (uri: "+textAnnotation+")!",endPosIterator.hasNext());
            assertTrue("fise:end values MUST BE TypedLiterals (uri: "+textAnnotation+")",resource instanceof Literal);
            endPosLiteral = (Literal) resource;
            assertEquals("fise:end MUST use xsd:int as data type (uri: "+textAnnotation+")",XSD.int_, endPosLiteral.getDataType());
            resource = null;
            Integer end = LiteralFactory.getInstance().createObject(Integer.class, endPosLiteral);
            assertNotNull("Unable to parse Integer from TypedLiteral "+endPosLiteral,end);
            //check for equality of the selected text and the text on the selected position in the content
            //System.out.println("TA ["+start+"|"+end+"]"+selectedText.getLexicalForm()+"<->"+content.substring(start,end));
            if(selectedTextResource != null){
                assertEquals("the substring [fise:start,fise:end] does not correspond to "
                    + "the fise:selected-text value '"+((Literal)selectedTextResource).getLexicalForm()
                    + "' of this TextAnnotation!",content.substring(start, end), ((Literal)selectedTextResource).getLexicalForm());
            } // else no selected-text present ... unable to test this
        } else {
            assertNull("if fise:selected-text is present also fise:start AND fise:end MUST BE present!",selectedTextResource);
            assertNull("If fise:selection-context is present also fise:start AND fise:end MUST BE present!",selectionContextResource);
            assertFalse("if fise:end is presnet also fise:start AND fise:selection-context MUST BE present!",endPosIterator.hasNext());
            startPosLiteral = null;
            endPosLiteral = null;
        }
        RDFTerm expectedStartPos = expectedValues.get(ENHANCER_START);
        if(expectedStartPos != null){
            assertEquals("The fise:start value is not the expected "+expectedStartPos,
                expectedStartPos, startPosLiteral);
        }
        RDFTerm expectedEndPos = expectedValues.get(ENHANCER_END);
        if(expectedEndPos != null){
            assertEquals("The fise:end value is not the expected "+expectedEndPos,
                expectedEndPos, endPosLiteral);
        }

        //fise:selection-prefix and fise:selection-suffix (STANBOL-987)
        Literal prefixLiteral;
        Iterator<Triple> selectionPrefixIterator = enhancements.filter(textAnnotation,
            Properties.ENHANCER_SELECTION_PREFIX, null);
        if(startPosLiteral != null){
            // check if the selectionPrefix text is present
            assertTrue("fise:selection-prefix property is missing for fise:TextAnnotation "
                + textAnnotation, selectionPrefixIterator.hasNext() || 
                !validatePrefixSuffix); //to support old and new fise:TextAnnotation model
            // test if the selected text is part of the TEXT_TO_TEST
            if(selectionPrefixIterator.hasNext()){
                RDFTerm selectionPrefixResource = selectionPrefixIterator.next().getObject();
                assertTrue("fise:selection-prefix MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                    selectionPrefixResource instanceof Literal);
                prefixLiteral = (Literal)selectionPrefixResource;
                assertTrue("The parsed content MUST contain the fise:selection-prefix value '"
                        +prefixLiteral.getLexicalForm()+"' (uri: "+textAnnotation+")!",content.contains(prefixLiteral.getLexicalForm()));
                assertFalse("fise:selection-prefix MUST BE single valued (uri: "+textAnnotation+")!",
                    selectionPrefixIterator.hasNext());
            } else {
                prefixLiteral = null;
            }
        } else {
            prefixLiteral = null;
        }
        Literal suffixLiteral;
        Iterator<Triple> selectionSuffixIterator = enhancements.filter(textAnnotation,
            Properties.ENHANCER_SELECTION_SUFFIX, null);
        if(endPosLiteral != null){
            // check if the selectionPrefix text is present
            assertTrue("fise:selection-suffix property is missing for fise:TextAnnotation "
                + textAnnotation, selectionSuffixIterator.hasNext() || 
                !validatePrefixSuffix); //to support old and new fise:TextAnnotation model
            if(selectionSuffixIterator.hasNext()){
                // test if the selected text is part of the TEXT_TO_TEST
                RDFTerm selectionSuffixResource = selectionSuffixIterator.next().getObject();
                assertTrue("fise:selection-suffix MUST BE of type PlainLiteral (uri: "+textAnnotation+")",
                    selectionSuffixResource instanceof Literal);
                suffixLiteral = (Literal)selectionSuffixResource;
                assertTrue("The parsed content MUST contain the fise:selection-suffix value '"
                        +suffixLiteral.getLexicalForm()+"' (uri: "+textAnnotation+")!",content.contains(suffixLiteral.getLexicalForm()));
                assertFalse("fise:selection-suffix MUST BE single valued (uri: "+textAnnotation+")!",
                    selectionSuffixIterator.hasNext());
            } else {
                suffixLiteral = null;
            }
        } else {
            suffixLiteral = null;
        }
        Assert.assertTrue("Both fise:selection-prefix AND fise:selection-suffix need to be present "
            + "(if one of them is present) (uri: "+textAnnotation+")",
            (suffixLiteral != null && prefixLiteral != null) ||
            (suffixLiteral == null && prefixLiteral == null));
        if(prefixLiteral != null && selectedTextResource != null){
            String occurrence = prefixLiteral.getLexicalForm() + 
                    ((Literal)selectedTextResource).getLexicalForm() +
                    suffixLiteral.getLexicalForm();
            assertTrue("The parsed content MUST contain the concated value of fise:selection-prefix,"
                + "fise:selected-text and fise:selection-suffix (value: '"+occurrence
                + "' (uri: "+textAnnotation+")!",content.contains(occurrence));
        }
        if(prefixLiteral != null && selectionHeadResource != null){
            String occurrence = prefixLiteral.getLexicalForm() +
                    ((Literal)selectionHeadResource).getLexicalForm();
            assertTrue("The parsed content MUST contain the concated value of fise:selection-prefix,"
                    + "fise:selection-head (value: '"+occurrence
                    + "' (uri: "+textAnnotation+")!",content.contains(occurrence));
            occurrence = ((Literal)selectionTailResource).getLexicalForm() +
                    suffixLiteral.getLexicalForm();
            assertTrue("The parsed content MUST contain the concated value of fise:selection-tail "
                    + "and fise:selection-suffix (value: '"+occurrence
                    + "' (uri: "+textAnnotation+")!",content.contains(occurrence));
        }
        
        //validate fise:Enhancement specific rules
        validateEnhancement(enhancements, textAnnotation, expectedValues);
        
        //validate for special TextAnnotations
        validateLanguageAnnotations(enhancements,textAnnotation);
        validateNERAnnotations(enhancements,textAnnotation, selectedTextResource);
    }
    /**
     * Validates the correctness of fise:TextAnnotations that annotate the language 
     * of the text as defined by 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-613">STANBOL-613</a><p>
     * Called by {@link #validateTextAnnotation(Graph, IRI, String, Map)}
     * @param enhancements
     * @param textAnnotation
     */
    private static void validateLanguageAnnotations(Graph enhancements, IRI textAnnotation) {
        Iterator<Triple> dcLanguageIterator = enhancements.filter(textAnnotation, DC_LANGUAGE, null);
        if(dcLanguageIterator.hasNext()){ //a language annotation
            RDFTerm dcLanguageResource = dcLanguageIterator.next().getObject();
            assertTrue("The dc:language value MUST BE a PlainLiteral", dcLanguageResource instanceof Literal);
            assertTrue("The dc:language value '"+dcLanguageResource+"'MUST BE at least two chars long", 
                ((Literal)dcLanguageResource).getLexicalForm().length() >=2);
            assertFalse("TextAnnotations with the dc:language property MUST only have a single dc:language value (uri "
                    +textAnnotation+")",dcLanguageIterator.hasNext());

            Iterator<Triple> dcTypeIterator = enhancements.filter(textAnnotation, DC_TYPE, null);
            assertTrue("TextAnnotations with the dc:language property MUST use dc:type dc:LinguisticSystem (uri "
                +textAnnotation+")", dcTypeIterator.hasNext());
            assertEquals("TextAnnotations with the dc:language property MUST use dc:type dc:LinguisticSystem (uri "
                +textAnnotation+")", DCTERMS_LINGUISTIC_SYSTEM,dcTypeIterator.next().getObject());
            assertFalse("TextAnnotations with the dc:language property MUST only have a single dc:type value (uri "
                +textAnnotation+")",dcTypeIterator.hasNext());
            //assert that the created TextAnnotation is correctly returned by the
            //EnhancementEngineHelper methods
            List<BlankNodeOrIRI> languageAnnotation = EnhancementEngineHelper.getLanguageAnnotations(enhancements);
            assertTrue("Language annotation "+textAnnotation+" was not returned by "
                +"EnhancementEngineHelper.getLanguageAnnotations(..)!",languageAnnotation.contains(textAnnotation));
        } else { //no language annotation
            Iterator<Triple> dcTypeIterator = enhancements.filter(textAnnotation, DC_TYPE, null);
            while(dcTypeIterator.hasNext()){
                assertFalse("Only fise:TextAnnotations without a dc:language value MUST NOT use the "
                    + "dc:type value dc:LinguisticSystem (uri "+textAnnotation+")",
                    DCTERMS_LINGUISTIC_SYSTEM.equals(dcTypeIterator.next().getObject()));
            }
        }
        
    }
    /**
     * Validates that fise:TextAnnotations with the dc:type dbp-ont:Person,
     * dbp-ont:Organisation and dbp-ont:Place do have a
     * fise:selected-text value (this implicitly also checks that
     * fise:selection-context, fise:start and fise:end are defined!<p>
     * Called by {@link #validateTextAnnotation(Graph, IRI, String, Map)}
     * @param enhancements
     * @param textAnnotation
     * @param selectedTextResource the fise:selected-text value
     */
    private static void validateNERAnnotations(Graph enhancements, IRI textAnnotation, RDFTerm selectedTextResource) {
        Iterator<Triple> dcTypeIterator = enhancements.filter(textAnnotation, DC_TYPE, null);
        boolean isNERAnnotation = false;
        while(dcTypeIterator.hasNext() && !isNERAnnotation){
            RDFTerm dcTypeValue = dcTypeIterator.next().getObject();
            isNERAnnotation = DBPEDIA_PERSON.equals(dcTypeValue) ||
                    DBPEDIA_ORGANISATION.equals(dcTypeValue) ||
                    DBPEDIA_PLACE.equals(dcTypeValue);
        }
        if(isNERAnnotation){
            assertNotNull("fise:TextAnnotations with a dc:type of c:type dbp-ont:Person, "
                +"dbp-ont:Organisation or dbp-ont:Place MUST have a fise:selected-text value (uri "
                    +textAnnotation+")", selectedTextResource);
        }
    }
    /**
     * Validates all fise:EntityAnnotations contained by the parsed enhancements
     * graph.
     * @param enhancements the enhancement graph
     * @param expectedValues the expected values of all validated EntityAnnotations.
     * Properties are used as keys. Typical example would be fise:extracted-from
     * with the id of the ContentItem as value; dc-terms:creator with the
     * {@link Class#getName()} as value.
     * @return the number of found and validated EntityAnnotations.
     */
    @SuppressWarnings("unchecked")
    public static int validateAllEntityAnnotations(Graph enhancements,Map<IRI,RDFTerm> expectedValues) {
        expectedValues = expectedValues == null ? Collections.EMPTY_MAP : expectedValues;
        Iterator<Triple> entityAnnotationIterator = enhancements.filter(null,
                RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            IRI entityAnnotation = (IRI) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateEntityAnnotation(enhancements, entityAnnotation, 
                expectedValues);
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
    }

    /**
     * Checks if a fise:EntityAnnotation is valid. NOTE that this also validates
     * all fise:Enhancement related requirements by calling
     * {@link #validateEnhancement(Graph, IRI, Map)}
     * @param enhancements the enhancements graph
     * @param entityAnnotation the entity annotation to validate
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateEntityAnnotation(Graph enhancements, IRI entityAnnotation,Map<IRI,RDFTerm> expectedValues) {
        Iterator<Triple> relationToTextAnnotationIterator = enhancements.filter(
                entityAnnotation, DC_RELATION, null);
        // check if the relation to the text annotation is set
        assertTrue(relationToTextAnnotationIterator.hasNext());
        while (relationToTextAnnotationIterator.hasNext()) {
            // test if the referred annotations are text annotations or
            // the referenced annotations is a fise:EntityAnnotation AND also a
            // dc:requires link is defined (STANBOL-766)
            IRI referredTextAnnotation = (IRI) relationToTextAnnotationIterator.next().getObject();
            assertTrue("fise:EntityAnnotations MUST BE dc:related to a fise:TextAnnotation OR dc:requires and dc:related to the same fise:EntityAnnotation",
                enhancements.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_TEXTANNOTATION).hasNext() || (
                enhancements.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_ENTITYANNOTATION).hasNext() && 
                    enhancements.filter(entityAnnotation, Properties.DC_REQUIRES, referredTextAnnotation).hasNext()));
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = enhancements.filter(entityAnnotation,
                ENHANCER_ENTITY_REFERENCE, null);
        assertTrue("fise:entity-reference MUST BE present! (EntityAnnotation: '"
                +entityAnnotation+"')'",entityReferenceIterator.hasNext());
        RDFTerm expectedReferencedEntity = expectedValues.get(ENHANCER_ENTITY_REFERENCE);
        while(entityReferenceIterator.hasNext()){ //check possible multiple references
            RDFTerm entityReferenceResource = entityReferenceIterator.next().getObject();
            // test if the reference is an URI
            assertTrue("fise:entity-reference value MUST BE of URIs",entityReferenceResource instanceof IRI);
            if(expectedReferencedEntity != null && expectedReferencedEntity.equals(entityReferenceResource)){
                expectedReferencedEntity = null; //found
            }
        }
        assertNull("EntityAnnotation "+entityAnnotation+"fise:entity-reference has not the expected value "
                +expectedReferencedEntity+"!", expectedReferencedEntity);
        
        //test if the entity label is set
        Iterator<Triple> entityLabelIterator = enhancements.filter(entityAnnotation, ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
        RDFTerm expectedEntityLabel = expectedValues.get(ENHANCER_ENTITY_LABEL);
        while(entityLabelIterator.hasNext()){
            RDFTerm entityLabelResource =  entityLabelIterator.next().getObject();
            assertTrue("fise:entity-label values MUST BE PlainLiterals (EntityAnnotation: "+entityAnnotation+")!",
                entityLabelResource instanceof Literal);
            if(expectedEntityLabel != null && expectedEntityLabel.equals(entityLabelResource)){
                expectedEntityLabel = null;
            }
        }
        assertNull("The expected EntityLabel "+expectedEntityLabel+" was not found",
            expectedEntityLabel);
        
        //test the optional entity types
        Iterator<Triple> entityTypeIterator = enhancements.filter(entityAnnotation, Properties.ENHANCER_ENTITY_TYPE, null);
        RDFTerm expectedEntityType = expectedValues.get(Properties.ENHANCER_ENTITY_TYPE);
        if(entityTypeIterator.hasNext()){
            RDFTerm entityTypeResource = entityTypeIterator.next().getObject();
            assertTrue("fise:entity-type values MUST BE URIs",entityTypeResource instanceof IRI);
            if(expectedEntityType != null && expectedEntityType.equals(entityTypeResource)){
                expectedEntityType = null; //found
            }
        }
        assertNull("The expected fise:entity-type value "+expectedEntityType+" was not found!", expectedEntityType);
        
        //test all properties required by fise:Enhancement
        validateEnhancement(enhancements, entityAnnotation, expectedValues);
    }
    /**
     * Validates all fise:Enhancement related properties and values. NOTE that
     * this method is called by {@link #validateEntityAnnotation(Graph, IRI, Map)}
     * and {@link #validateTextAnnotation(Graph, IRI, String)}.
     * @param enhancements the enhancements graph
     * @param enhancement the fise:Enhancement to validate
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateEnhancement(Graph enhancements, IRI enhancement, Map<IRI,RDFTerm> expectedValues){
        //validate the rdf:type
        Iterator<Triple> rdfTypeIterator = enhancements.filter(enhancement, RDF_TYPE, ENHANCER_ENHANCEMENT);
        assertTrue("Parsed Enhancement "+enhancement +" is missing the fise:Enhancement type ",
            rdfTypeIterator.hasNext());
        //validate the creator
        Iterator<Triple> creatorIterator = enhancements.filter(enhancement, Properties.DC_CREATOR, null);
        assertTrue("Enhancements MUST HAVE a creator",creatorIterator.hasNext());
        RDFTerm creatorResource = creatorIterator.next().getObject();
        assertTrue("Creator MUST BE an TypedLiteral (found '"+creatorResource.getClass().getSimpleName()+"')!",
            creatorResource instanceof Literal || creatorResource instanceof IRI);
        if(creatorResource instanceof Literal){
            assertEquals("The dc:creator value MUST be of dataType xsd:string",
                XSD.string,((Literal)creatorResource).getDataType());
        }
        RDFTerm expectedCreator = expectedValues.get(Properties.DC_CREATOR);
        if(expectedCreator != null){
            assertEquals("Creator is not the expected value!",expectedCreator, creatorResource);
        }
        assertFalse("only a single creater MUST BE present for an Enhancement", creatorIterator.hasNext());
        //validate the optional contributor
        RDFTerm expectedContributor = expectedValues.get(DCTERMS.contributor);
        Iterator<Triple> contributorIterator = enhancements.filter(enhancement, DCTERMS.contributor, null);
        while(contributorIterator.hasNext()){
            RDFTerm contributorResource = contributorIterator.next().getObject();
            assertTrue("Creator MUST BE an TypedLiteral or an IRI (found '"+contributorResource.getClass().getSimpleName()+"')!",
                contributorResource instanceof Literal || contributorResource instanceof IRI);
            if(contributorResource instanceof Literal){
                assertEquals("The dc:contributor value MUST be of dataType xsd:string",
                    XSD.string,((Literal)contributorResource).getDataType());
            }
            if(expectedContributor != null && expectedContributor.equals(expectedContributor)){
                expectedContributor = null; //found
            }
        }
        assertNull("The expected contributor '"+expectedContributor+"'was not present in the Enhancement",
            expectedContributor);
        //validate creation date
        Iterator<Triple> createdIterator = enhancements.filter(enhancement, Properties.DC_CREATED, null);
        assertTrue("The creation date MUST BE present for an Enhancement", createdIterator.hasNext());
        RDFTerm createdResource = createdIterator.next().getObject();
        assertTrue("Creation date MUST be a typed Literal", createdResource instanceof Literal);
        assertTrue("Creation date MUST have the dataTyoe xsd:dateTime",
            XSD.dateTime.equals(((Literal)createdResource).getDataType()));
        Date creationDate = LiteralFactory.getInstance().createObject(Date.class, (Literal)createdResource);
        assertNotNull("Unable to convert "+createdResource+" to a Java Date object",creationDate);
        Date now = new Date();
        assertTrue("CreationDate MUST NOT be in the Future",now.after(creationDate) || now.equals(creationDate));
        assertFalse("Only a single createnDate MUST BE present", createdIterator.hasNext());
        //validate optional modification date if present
        Iterator<Triple> modDateIterator = enhancements.filter(enhancement, DCTERMS.modified, null);
        while(modDateIterator.hasNext()){
            RDFTerm modDateResurce = modDateIterator.next().getObject();
            assertTrue("Creation date MUST be a typed Literal", modDateResurce instanceof Literal);
            assertTrue("Creation date MUST have the dataTyoe xsd:dateTime",
                XSD.dateTime.equals(((Literal)modDateResurce).getDataType()));
            Date modDate = LiteralFactory.getInstance().createObject(Date.class, (Literal)modDateResurce);
            assertNotNull("Unable to convert "+modDateResurce+" to a Java Date object",modDate);
            assertTrue("CreationDate MUST NOT be in the Future",new Date().after(modDate));
        }
        //validate the fise:extracted-from
        Iterator<Triple> extractedIterator = enhancements.filter(enhancement, Properties.ENHANCER_EXTRACTED_FROM, null);
        assertTrue("The fise:extracted-from property MUST BE present for an Enhancement", extractedIterator.hasNext());
        RDFTerm extractedResource = extractedIterator.next().getObject();
        assertTrue("Creator MUST BE an IRI (found '"+extractedResource.getClass().getSimpleName()+"')!",
            extractedResource instanceof IRI);
        RDFTerm expectedExtractedFrom = expectedValues.get(Properties.ENHANCER_EXTRACTED_FROM);
        if(expectedExtractedFrom != null){
            assertEquals("fise:extracted-from has not the expected value!",expectedExtractedFrom, extractedResource);
        }
        assertFalse("only a single creater MUST BE present for an Enhancement", extractedIterator.hasNext());
        //validate that all dc:requires and dc:relation link to resources of type fise:Enhancement
        Iterator<Triple> relatedIterator = enhancements.filter(enhancement, Properties.DC_RELATION, null);
        while(relatedIterator.hasNext()){
            RDFTerm relatedResource = relatedIterator.next().getObject();
            assertTrue("dc:relation values MUST BE URIs", relatedResource instanceof IRI);
            Iterator<Triple> relatedTypes = enhancements.filter((IRI)relatedResource, RDF_TYPE, TechnicalClasses.ENHANCER_ENHANCEMENT);
            assertTrue("dc:relation Resources MUST BE of rdf:type fise:Enhancement",relatedTypes.hasNext());
        }
        Iterator<Triple> requiresIterator = enhancements.filter(enhancement, Properties.DC_REQUIRES, null);
        while(requiresIterator.hasNext()){
            RDFTerm requiredResource = requiresIterator.next().getObject();
            assertTrue("dc:requires values MUST BE URIs", requiredResource instanceof IRI);
            Iterator<Triple> relatedTypes = enhancements.filter((IRI)requiredResource, RDF_TYPE, TechnicalClasses.ENHANCER_ENHANCEMENT);
            assertTrue("dc:requires Resources MUST BE of rdf:type fise:Enhancement",relatedTypes.hasNext());
        }
        //validate that fise:confidence has [0..1] values and are of type xsd:float
        Iterator<Triple> confidenceIterator = enhancements.filter(enhancement,Properties.ENHANCER_CONFIDENCE,null);
        boolean confidenceRequired = expectedValues.containsKey(Properties.ENHANCER_CONFIDENCE);
        if(confidenceIterator.hasNext()){ //confidence is optional
            RDFTerm confidenceResource = confidenceIterator.next().getObject();
            assertTrue("fise:confidence value MUST BE a TypedLiteral", confidenceResource instanceof Literal);
            assertTrue("fise:confidence MUST BE xsd:double",
                XSD.double_.equals(((Literal)confidenceResource).getDataType()));
            Double confidence = LiteralFactory.getInstance().createObject(Double.class, (Literal)confidenceResource);
            assertNotNull("Unable to convert TypedLiteral '"+confidenceResource+"' to a Java Double value",confidence);
            assertFalse("fise:confidence MUST HAVE [0..1] values",confidenceIterator.hasNext());
            //STANBOL-630: confidence [0..1]
            assertTrue("fise:confidence MUST BE <= 1 (value= '"+confidence
                + "',enhancement " +enhancement+")",
                1.0 >= confidence.doubleValue());
            assertTrue("fise:confidence MUST BE >= 0 (value= '"+confidence
                    +"',enhancement "+enhancement+")",
                    0.0 <= confidence.doubleValue());
            RDFTerm expectedConfidence = expectedValues.get(Properties.ENHANCER_CONFIDENCE);
            if(expectedConfidence != null){
                assertEquals("The fise:confidence for enhancement "
                    +enhancement+" does not have the expected value", expectedConfidence,confidenceResource);
            }
        } else {
            assertFalse("The required fise:confidence value is missing for enhancement "
                +enhancement,confidenceRequired);
        }
        //validate that the (optional) dc:type is an URI and that there are not multiple values
        Iterator<Triple> dcTypeIterator = enhancements.filter(enhancement, Properties.DC_TYPE, null);
        RDFTerm expectedDcType = expectedValues.get(Properties.DC_TYPE);
        if(dcTypeIterator.hasNext()){ //dc:type is optional
            RDFTerm dcTypeResource = dcTypeIterator.next().getObject();
            assertTrue("dc:type values MUST BE URIs",dcTypeResource instanceof IRI);
            if(expectedDcType != null) {
                assertEquals("The dc:type value is not the expected "+expectedDcType+"!",
                    expectedDcType,dcTypeResource);
            }
            assertFalse("Only a single dc:type value is allowed!", dcTypeIterator.hasNext());
        }
        //validate the fise:confidence-value introduced by STANBOL-631
        Iterator<Triple> confidenceLevelIterator = enhancements.filter(
            enhancement, Properties.ENHANCER_CONFIDENCE_LEVEL, null);
        RDFTerm expectedConfidenceValue = expectedValues.get(Properties.ENHANCER_CONFIDENCE_LEVEL);
        if(confidenceLevelIterator.hasNext()){
            RDFTerm confidenceLevelResource = confidenceLevelIterator.next().getObject();
            assertTrue("fise:confidence-level values MUST BE URIs but found "+confidenceLevelResource,
                confidenceLevelResource instanceof IRI);
            assertNotNull("The fise:confidence-level value MUST BE one of the four "
                + "values defined in the ontology! (found: "+ confidenceLevelResource
                + " | enhancement " + enhancement+")",
                CONFIDENCE_LEVEL_ENUM.getConfidenceLevel((IRI)confidenceLevelResource));
            assertFalse("The fise:confidence-level property is functional and MUST "
                + "HAVE only a single value (enhancement " +
                    enhancement+")!",confidenceLevelIterator.hasNext());
        } else {
            assertNull("fise:confidence-level "+expectedConfidenceValue
                + "expected for Enhancement "+enhancement
                + "but no 'fise:confidence-level' value present!", expectedConfidenceValue);
        }
        
    }
    /**
     * Validates all fise:TopicAnnotations contained by the parsed enhancements
     * graph.
     * @param enhancements the enhancement graph
     * @param expectedValues the expected values of all validated TopicAnnotations.
     * Properties are used as keys. Typical example would be fise:extracted-from
     * with the id of the ContentItem as value; dc-terms:creator with the
     * {@link Class#getName()} as value.
     * @return the number of found and validated TopicAnnotations.
     */
    @SuppressWarnings("unchecked")
    public static int validateAllTopicAnnotations(Graph enhancements,Map<IRI,RDFTerm> expectedValues) {
        expectedValues = expectedValues == null ? Collections.EMPTY_MAP : expectedValues;
        Iterator<Triple> topicAnnotationIterator = enhancements.filter(null,
                RDF_TYPE, ENHANCER_TOPICANNOTATION);
        int topicAnnotationCount = 0;
        while (topicAnnotationIterator.hasNext()) {
            IRI topicAnnotation = (IRI) topicAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateTopicAnnotation(enhancements, topicAnnotation, 
                expectedValues);
            topicAnnotationCount++;
        }
        return topicAnnotationCount;
    }
    
    /**
     * Checks if a fise:TopicAnnotation is valid as defined by 
     * <a herf="https://issues.apache.org/jira/browse/STANBOL-617">STANBOL-617</a>. 
     * NOTE that this also validates all fise:Enhancement related requirements by 
     * calling {@link #validateEnhancement(Graph, IRI, Map)}
     * @param enhancements the enhancements graph
     * @param topicAnnotation the topic annotation to validate
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateTopicAnnotation(Graph enhancements, IRI topicAnnotation, Map<IRI,RDFTerm> expectedValues){
        //validate the rdf:type
        Iterator<Triple> rdfTypeIterator = enhancements.filter(topicAnnotation, RDF_TYPE, ENHANCER_TOPICANNOTATION);
        assertTrue("Parsed Enhancement "+topicAnnotation +" is missing the fise:TopicAnnotation type ",
            rdfTypeIterator.hasNext());
        
        //TopicAnnotations need to be linked to TextAnnotations describing the
        //section of the text that has a specific Topic.
        //If the topic is for the whole text the TextAnnotation will have no
        //selected-text value
        Iterator<Triple> relationToTextAnnotationIterator = enhancements.filter(
            topicAnnotation, DC_RELATION, null);
        // check if the relation to the text annotation is set
        assertTrue(relationToTextAnnotationIterator.hasNext());
        while (relationToTextAnnotationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            IRI referredTextAnnotation = (IRI) relationToTextAnnotationIterator.next().getObject();
            assertTrue(enhancements.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_TEXTANNOTATION).hasNext());
        }
    
        // test if an entity (the topic) is referred (NOTE: in contrast to
        // fise:EntityAnnotations this property is NOT required - cardinality [0..*]
        Iterator<Triple> entityReferenceIterator = enhancements.filter(topicAnnotation,
                ENHANCER_ENTITY_REFERENCE, null);
        RDFTerm expectedReferencedEntity = expectedValues.get(ENHANCER_ENTITY_REFERENCE);
        while(entityReferenceIterator.hasNext()){ //check possible multiple references
            RDFTerm entityReferenceResource = entityReferenceIterator.next().getObject();
            // test if the reference is an URI
            assertTrue("fise:entity-reference value MUST BE of URIs",entityReferenceResource instanceof IRI);
            if(expectedReferencedEntity != null && expectedReferencedEntity.equals(entityReferenceResource)){
                expectedReferencedEntity = null; //found
            }
        }
        assertNull("EntityAnnotation "+topicAnnotation+"fise:entity-reference has not the expected value "
                +expectedReferencedEntity+"!", expectedReferencedEntity);
        
        //test if the entity label is set (required)
        Iterator<Triple> entityLabelIterator = enhancements.filter(topicAnnotation, ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
        RDFTerm expectedEntityLabel = expectedValues.get(ENHANCER_ENTITY_LABEL);
        while(entityLabelIterator.hasNext()){
            RDFTerm entityLabelResource =  entityLabelIterator.next().getObject();
            assertTrue("fise:entity-label values MUST BE PlainLiterals (EntityAnnotation: "+topicAnnotation+")!",
                entityLabelResource instanceof Literal);
            if(expectedEntityLabel != null && expectedEntityLabel.equals(entityLabelResource)){
                expectedEntityLabel = null;
            }
        }
        assertNull("The expected EntityLabel "+expectedEntityLabel+" was not found",
            expectedEntityLabel);
        
        // test fise:entity-type(s). NOTE: this is not required - cardinality [0..*]
        Iterator<Triple> entityTypeIterator = enhancements.filter(topicAnnotation, Properties.ENHANCER_ENTITY_TYPE, null);
        RDFTerm expectedEntityType = expectedValues.get(Properties.ENHANCER_ENTITY_TYPE);
        if(entityTypeIterator.hasNext()){
            RDFTerm entityTypeResource = entityTypeIterator.next().getObject();
            assertTrue("fise:entity-type values MUST BE URIs",entityTypeResource instanceof IRI);
            if(expectedEntityType != null && expectedEntityType.equals(entityTypeResource)){
                expectedEntityType = null; //found
            }
        }
        assertNull("The expected fise:entity-type value "+expectedEntityType+" was not found!", expectedEntityType);
        
        //test all properties required by fise:Enhancement
        validateEnhancement(enhancements, topicAnnotation, expectedValues);
    }
    
}
