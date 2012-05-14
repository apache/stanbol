package org.apache.stanbol.enhancer.test.helper;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;

public class EnhancementStructureHelper {

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
    @SuppressWarnings("unchecked")
    public static int validateAllTextAnnotations(TripleCollection enhancements, String content, Map<UriRef,Resource> expectedValues) {
        expectedValues = expectedValues == null ? Collections.EMPTY_MAP : expectedValues;
        Iterator<Triple> textAnnotationIterator = enhancements.filter(null,
                RDF_TYPE, ENHANCER_TEXTANNOTATION);
        // test if a textAnnotation is present
        assertTrue(textAnnotationIterator.hasNext());
        int textAnnotationCount = 0;
        while (textAnnotationIterator.hasNext()) {
            UriRef textAnnotation = (UriRef) textAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateTextAnnotation(enhancements, textAnnotation,content,expectedValues);
            textAnnotationCount++;
        }
        return textAnnotationCount;
    }

    /**
     * Validates the parsed TextAnnotation with a fise:selected-text. This
     * method also validates rules defined by fise:Enhancement by calling
     * {@link #validateEnhancement(TripleCollection, UriRef, Map)}<p>
     * NOTE: this method MUST NOT be used to validate fise:TextAnnotations that
     * do NOT select a part of the text - meaning TextAnnotations about the
     * whole parsed content.
     * @param enhancements the enhancements graph containing the text annotation
     * @param textAnnotation the TextAnnotation to validate
     * @param content the enhanced content
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateTextAnnotation(TripleCollection enhancements, UriRef textAnnotation, String content, Map<UriRef,Resource> expectedValues) {
        Iterator<Triple> selectedTextIterator = enhancements.filter(textAnnotation,
                ENHANCER_SELECTED_TEXT, null);
        // check if the selected text is added
        assertTrue("TextAnnotations MUST have a fise:selected-text value",selectedTextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        Resource selectedTextResource = selectedTextIterator.next().getObject();
        assertTrue("fise:selected-text MUST BE of type PlainLiteral",selectedTextResource instanceof PlainLiteral);
        Literal selectedText = (Literal)selectedTextResource;
        assertTrue("The parsed content MUST contain the fise:selected-text value '"
            +selectedText.getLexicalForm()+"'!",content.contains(selectedText.getLexicalForm()));
        Resource expectedSelectedText = expectedValues.get(ENHANCER_SELECTED_TEXT);
        if(expectedSelectedText != null){
            assertEquals("The fise:selected-text is not the expected value "+expectedSelectedText+"!",
                expectedSelectedText, selectedText);
        }
        Resource selectionContextResource;
        // test if context is added
        Iterator<Triple> selectionContextIterator = enhancements.filter(textAnnotation,
                ENHANCER_SELECTION_CONTEXT, null);
        if(selectionContextIterator.hasNext()) { //context is optional
            // test if the selected text is part of the TEXT_TO_TEST
            selectionContextResource = selectionContextIterator.next().getObject();
            assertTrue("The fise:selection-context MUST BE of type PlainLiteral",selectionContextResource instanceof PlainLiteral);
            //check that the content contains the context
            assertTrue("The fise:selection-context MUST BE contained in the Content | context= "+ selectionContextResource,
            content.contains(((Literal)selectionContextResource).getLexicalForm()));
            //check that the context contains the selected text
            assertTrue("The fise:selected-text value MUST BE containted within the fise:selection-context value",
                ((Literal)selectionContextResource).getLexicalForm().contains(
                    selectedText.getLexicalForm()));
        } else {
            selectionContextResource = null;
        }
        Resource expectedSelectionContext = expectedValues.get(ENHANCER_SELECTION_CONTEXT);
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
        TypedLiteral startPosLiteral;
        TypedLiteral endPosLiteral;
        if(startPosIterator.hasNext()){
            assertNotNull("If fise:start is present the fise:selection-context MUST also be present!",
                selectionContextResource);
            Resource resource = startPosIterator.next().getObject();
            //only a single start position is supported
            assertFalse("fise:start MUST HAVE only a single value!",startPosIterator.hasNext());
            assertTrue("fise:start MUST be a typed Literal!",resource instanceof TypedLiteral);
            startPosLiteral = (TypedLiteral) resource;
            assertEquals("fise:start MUST use xsd:int as data type",XSD.int_, startPosLiteral.getDataType());
            resource = null;
            Integer start = LiteralFactory.getInstance().createObject(Integer.class, startPosLiteral);
            assertNotNull("Unable to parse Integer from TypedLiteral "+startPosLiteral,start);
            //now get the end
            //end must be defined if start is present
            assertTrue("If fise:start is present also fise:end MUST BE defined!",endPosIterator.hasNext());
            resource = endPosIterator.next().getObject();
            //only a single end position is supported
            assertFalse("fise:end MUST HAVE only a single value!",endPosIterator.hasNext());
            assertTrue("fise:end values MUST BE TypedLiterals",resource instanceof TypedLiteral);
            endPosLiteral = (TypedLiteral) resource;
            assertEquals("fise:end MUST use xsd:int as data type",XSD.int_, endPosLiteral.getDataType());
            resource = null;
            Integer end = LiteralFactory.getInstance().createObject(Integer.class, endPosLiteral);
            assertNotNull("Unable to parse Integer from TypedLiteral "+endPosLiteral,end);
            endPosLiteral = null;
            //check for equality of the selected text and the text on the selected position in the content
            //System.out.println("TA ["+start+"|"+end+"]"+selectedText.getLexicalForm()+"<->"+content.substring(start,end));
            assertEquals("the substring [fise:start,fise:end] does not correspond to "
                + "the fise:selected-text value '"+selectedText.getLexicalForm()
                + "' of this TextAnnotation!",content.substring(start, end), selectedText.getLexicalForm());
        } else {
            assertNull("If fise:selection-context is present also fise:start AND fise:end MUST BE present!",selectionContextResource);
            assertFalse("if fise:end is presnet also fise:start AND fise:selection-context MUST BE present!",endPosIterator.hasNext());
            startPosLiteral = null;
            endPosLiteral = null;
        }
        Resource expectedStartPos = expectedValues.get(ENHANCER_START);
        if(expectedStartPos != null){
            assertEquals("The fise:start value is not the expected "+expectedStartPos,
                expectedStartPos, startPosLiteral);
        }
        Resource expectedEndPos = expectedValues.get(ENHANCER_END);
        if(expectedEndPos != null){
            assertEquals("The fise:end value is not the expected "+expectedEndPos,
                expectedEndPos, endPosLiteral);
        }
        
        //validate fise:Enhancement specific rules
        validateEnhancement(enhancements, textAnnotation, expectedValues);
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
    public static int validateAllEntityAnnotations(TripleCollection enhancements,Map<UriRef,Resource> expectedValues) {
        expectedValues = expectedValues == null ? Collections.EMPTY_MAP : expectedValues;
        Iterator<Triple> entityAnnotationIterator = enhancements.filter(null,
                RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            UriRef entityAnnotation = (UriRef) entityAnnotationIterator.next().getSubject();
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
     * {@link #validateEnhancement(TripleCollection, UriRef, Map)}
     * @param enhancements the enhancements graph
     * @param entityAnnotation the entity annotation to validate
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateEntityAnnotation(TripleCollection enhancements, UriRef entityAnnotation,Map<UriRef,Resource> expectedValues) {
        Iterator<Triple> relationToTextAnnotationIterator = enhancements.filter(
                entityAnnotation, DC_RELATION, null);
        // check if the relation to the text annotation is set
        //TODO: currently it is not required that all EntityAnnotations are linked to
        //      an TextAnnotation, because EntityAnnotations are also used for 
        //      Topics (that do not explicitly occur in texts.
        //      This might change as soon there is an own Topic type!
        //assertTrue(relationToTextAnnotationIterator.hasNext());
        while (relationToTextAnnotationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            UriRef referredTextAnnotation = (UriRef) relationToTextAnnotationIterator.next().getObject();
            assertTrue(enhancements.filter(referredTextAnnotation, RDF_TYPE,
                    ENHANCER_TEXTANNOTATION).hasNext());
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = enhancements.filter(entityAnnotation,
                ENHANCER_ENTITY_REFERENCE, null);
        assertTrue("fise:entity-reference MUST BE present! (EntityAnnotation: '"
                +entityAnnotation+"')'",entityReferenceIterator.hasNext());
        Resource expectedReferencedEntity = expectedValues.get(ENHANCER_ENTITY_REFERENCE);
        while(entityReferenceIterator.hasNext()){ //check possible multiple references
            Resource entityReferenceResource = entityReferenceIterator.next().getObject();
            // test if the reference is an URI
            assertTrue("fise:entity-reference value MUST BE of URIs",entityReferenceResource instanceof UriRef);
            if(expectedReferencedEntity != null && expectedReferencedEntity.equals(entityReferenceResource)){
                expectedReferencedEntity = null; //found
            }
        }
        assertNull("EntityAnnotation "+entityAnnotation+"fise:entity-reference has not the expected value "
                +expectedReferencedEntity+"!", expectedReferencedEntity);
        
        //test if the entity label is set
        Iterator<Triple> entityLabelIterator = enhancements.filter(entityAnnotation, ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
        Resource expectedEntityLabel = expectedValues.get(ENHANCER_ENTITY_LABEL);
        while(entityLabelIterator.hasNext()){
            Resource entityLabelResource =  entityLabelIterator.next().getObject();
            assertTrue("fise:entity-label values MUST BE PlainLiterals (EntityAnnotation: "+entityAnnotation+")!",
                entityLabelResource instanceof PlainLiteral);
            if(expectedEntityLabel != null && expectedEntityLabel.equals(entityLabelResource)){
                expectedEntityLabel = null;
            }
        }
        assertNull("The expected EntityLabel "+expectedEntityLabel+" was not found",
            expectedEntityLabel);
        
        //test the optional entity types
        Iterator<Triple> entityTypeIterator = enhancements.filter(entityAnnotation, Properties.ENHANCER_ENTITY_TYPE, null);
        Resource expectedEntityType = expectedValues.get(Properties.ENHANCER_ENTITY_TYPE);
        if(entityTypeIterator.hasNext()){
            Resource entityTypeResource = entityTypeIterator.next().getObject();
            assertTrue("fise:entity-type values MUST BE URIs",entityTypeResource instanceof UriRef);
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
     * this method is called by {@link #validateEntityAnnotation(TripleCollection, UriRef, Map)}
     * and {@link #validateTextAnnotation(TripleCollection, UriRef, String)}.
     * @param enhancements the enhancements graph
     * @param enhancement the fise:Enhancement to validate
     * @param expectedValues expected values (properties for the values are used as keys)
     */
    public static void validateEnhancement(TripleCollection enhancements, UriRef enhancement, Map<UriRef,Resource> expectedValues){
        //validate the creator
        Iterator<Triple> creatorIterator = enhancements.filter(enhancement, Properties.DC_CREATOR, null);
        assertTrue("Enhancements MUST HAVE a creator",creatorIterator.hasNext());
        Resource creatorResource = creatorIterator.next().getObject();
        assertTrue("Creator MUST BE an TypedLiteral (found '"+creatorResource.getClass().getSimpleName()+"')!",
            creatorResource instanceof TypedLiteral);
        assertEquals("The dc:creator value MUST be of dataType xsd:string",
            XSD.string,((TypedLiteral)creatorResource).getDataType());
        Resource expectedCreator = expectedValues.get(Properties.DC_CREATOR);
        if(expectedCreator != null){
            assertEquals("Creator is not the expected value!",expectedCreator, creatorResource);
        }
        assertFalse("only a single creater MUST BE present for an Enhancement", creatorIterator.hasNext());
        //validate the optional contributor
        Resource expectedContributor = expectedValues.get(DCTERMS.contributor);
        Iterator<Triple> contributorIterator = enhancements.filter(enhancement, DCTERMS.contributor, null);
        while(contributorIterator.hasNext()){
            Resource contributorResource = contributorIterator.next().getObject();
            assertTrue("Creator MUST BE an UriRef (found '"+contributorResource.getClass().getSimpleName()+"')!",
                contributorResource instanceof UriRef);
            if(expectedContributor != null && expectedContributor.equals(expectedContributor)){
                expectedContributor = null; //found
            }
        }
        assertNull("The expected contributor '"+expectedContributor+"'was not present in the Enhancement",
            expectedContributor);
        //validate creation date
        Iterator<Triple> createdIterator = enhancements.filter(enhancement, Properties.DC_CREATED, null);
        assertTrue("The creation date MUST BE present for an Enhancement", createdIterator.hasNext());
        Resource createdResource = createdIterator.next().getObject();
        assertTrue("Creation date MUST be a typed Literal", createdResource instanceof TypedLiteral);
        assertTrue("Creation date MUST have the dataTyoe xsd:dateTime",
            XSD.dateTime.equals(((TypedLiteral)createdResource).getDataType()));
        Date creationDate = LiteralFactory.getInstance().createObject(Date.class, (TypedLiteral)createdResource);
        assertNotNull("Unable to convert "+createdResource+" to a Java Date object",creationDate);
        assertTrue("CreationDate MUST NOT be in the Future",new Date().after(creationDate));
        assertFalse("Only a single createnDate MUST BE present", createdIterator.hasNext());
        //validate optional modification date if present
        Iterator<Triple> modDateIterator = enhancements.filter(enhancement, DCTERMS.modified, null);
        while(modDateIterator.hasNext()){
            Resource modDateResurce = modDateIterator.next().getObject();
            assertTrue("Creation date MUST be a typed Literal", modDateResurce instanceof TypedLiteral);
            assertTrue("Creation date MUST have the dataTyoe xsd:dateTime",
                XSD.dateTime.equals(((TypedLiteral)modDateResurce).getDataType()));
            Date modDate = LiteralFactory.getInstance().createObject(Date.class, (TypedLiteral)modDateResurce);
            assertNotNull("Unable to convert "+modDateResurce+" to a Java Date object",modDate);
            assertTrue("CreationDate MUST NOT be in the Future",new Date().after(modDate));
        }
        //validate the fise:extracted-from
        Iterator<Triple> extractedIterator = enhancements.filter(enhancement, Properties.ENHANCER_EXTRACTED_FROM, null);
        assertTrue("The fise:extracted-from property MUST BE present for an Enhancement", extractedIterator.hasNext());
        Resource extractedResource = extractedIterator.next().getObject();
        assertTrue("Creator MUST BE an UriRef (found '"+extractedResource.getClass().getSimpleName()+"')!",
            extractedResource instanceof UriRef);
        Resource expectedExtractedFrom = expectedValues.get(Properties.ENHANCER_EXTRACTED_FROM);
        if(expectedExtractedFrom != null){
            assertEquals("Creator is not the expected value!",extractedResource, expectedExtractedFrom);
        }
        assertFalse("only a single creater MUST BE present for an Enhancement", extractedIterator.hasNext());
        //validate that all dc:requires and dc:relation link to resources of type fise:Enhancement
        Iterator<Triple> relatedIterator = enhancements.filter(enhancement, Properties.DC_RELATION, null);
        while(relatedIterator.hasNext()){
            Resource relatedResource = relatedIterator.next().getObject();
            assertTrue("dc:relation values MUST BE URIs", relatedResource instanceof UriRef);
            Iterator<Triple> relatedTypes = enhancements.filter((UriRef)relatedResource, RDF_TYPE, TechnicalClasses.ENHANCER_ENHANCEMENT);
            assertTrue("dc:relation Resources MUST BE of rdf:type fise:Enhancement",relatedTypes.hasNext());
        }
        Iterator<Triple> requiresIterator = enhancements.filter(enhancement, Properties.DC_REQUIRES, null);
        while(requiresIterator.hasNext()){
            Resource requiredResource = requiresIterator.next().getObject();
            assertTrue("dc:requires values MUST BE URIs", requiredResource instanceof UriRef);
            Iterator<Triple> relatedTypes = enhancements.filter((UriRef)requiredResource, RDF_TYPE, TechnicalClasses.ENHANCER_ENHANCEMENT);
            assertTrue("dc:requires Resources MUST BE of rdf:type fise:Enhancement",relatedTypes.hasNext());
        }
        //validate that fise:confidence has [0..1] values and are of type xsd:float
        Iterator<Triple> confidenceIterator = enhancements.filter(enhancement,Properties.ENHANCER_CONFIDENCE,null);
        if(confidenceIterator.hasNext()){ //confidence is optional
            Resource confidenceResource = confidenceIterator.next().getObject();
            assertTrue("fise:confidence value MUST BE a TypedLiteral", confidenceResource instanceof TypedLiteral);
            assertTrue("fise:confidence MUST BE xsd:double",
                XSD.double_.equals(((TypedLiteral)confidenceResource).getDataType()));
            Double value = LiteralFactory.getInstance().createObject(Double.class, (TypedLiteral)confidenceResource);
            assertNotNull("Unable to convert TypedLiteral '"+confidenceResource+"' to a Java Float value",value);
            assertTrue("fise:confidence value "+value+" MUST BE > 0",0f < value);
            assertFalse("fise:confidence MUST HAVE [0..1] values",confidenceIterator.hasNext());
        }
        //validate that the (optional) dc:type is an URI and that there are not multiple values
        Iterator<Triple> dcTypeIterator = enhancements.filter(enhancement, Properties.DC_TYPE, null);
        Resource expectedDcType = expectedValues.get(Properties.DC_TYPE);
        if(dcTypeIterator.hasNext()){ //dc:type is optional
            Resource dcTypeResource = dcTypeIterator.next().getObject();
            assertTrue("dc:type values MUST BE URIs",dcTypeResource instanceof UriRef);
            if(expectedDcType != null) {
                assertEquals("The dc:type value is not the expected "+expectedDcType+"!",
                    expectedDcType,dcTypeResource);
            }
            assertFalse("Only a single dc:type value is allowed!", dcTypeIterator.hasNext());
        }
    }
    
}
