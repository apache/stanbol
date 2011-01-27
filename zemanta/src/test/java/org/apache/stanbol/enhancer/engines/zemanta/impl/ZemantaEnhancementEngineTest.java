package org.apache.stanbol.enhancer.engines.zemanta.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.engines.zemanta.impl.ZemantaEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZemantaEnhancementEngineTest {
    private static final Logger log = LoggerFactory.getLogger(ZemantaEnhancementEngineTest.class);

    /**
     * This key was generated to support testing only. Please do only use it
     * for testing. For real usages of the engine you need to create your own
     * key!
     */
    private static final String ZEMANTA_TEST_APPLICATION_KEY = "2qsvcvkut8rhnqbhm35znn76";

    /**
     * found on this Blog {@linkplain http://bcbio.wordpress.com/2009/01/04/extracting-keywords-from-biological-text-using-zemanta/}
     */
    public static final String BIO_DOMAIN_TEXT = "glh-2 encodes a putative DEAD-box RNA " +
    "helicase that contains six CCHC zinc fingers and is homologous to Drosophila VASA, " +
    "a germ-line-specific, ATP-dependent, RNA helicase; GLH-2 activity may also be required " +
    "for the wild-type morphology of P granules and for localization of several protein " +
    "components, but not accumulation of P granule mRNA components; GLH-2 interacts in " +
    "vitro with itself and with KGB-1, a JNK-like MAP kinase; GLH-2 is a constitutive P " +
    "granule component and thus, with the exception of mature sperm, is expressed in germ " +
    "cells at all stages of development; GLH-2 is cytoplasmic in oocytes and the early " +
    "embryo, while perinuclear in all later developmental stages as well as in the distal " +
    "and medial regions of the hermaphrodite gonad; GLH-2 is expressed at barely detectable " +
    "levels in males";


    static ZemantaEnhancementEngine zemantaEngine = new ZemantaEnhancementEngine();
    @BeforeClass
    public static void setUpServices() throws IOException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(ZemantaEnhancementEngine.API_KEY_PROPERTY, ZEMANTA_TEST_APPLICATION_KEY);
        MockComponentContext context = new MockComponentContext(properties);
        zemantaEngine.activate(context);
    }

    @AfterClass
    public static void shutdownServices() {
        zemantaEngine.deactivate(null);
    }
    public static ContentItem wrapAsContentItem(final String text) {
        return new ContentItem() {

            SimpleMGraph metadata = new SimpleMGraph();
            String id = "urn:org.apache.stanbol.enhancer:test:engines.zemanta:content-item-"+EnhancementEngineHelper.randomUUID().toString();

            public InputStream getStream() {
                return new ByteArrayInputStream(text.getBytes());
            }

            public String getMimeType() {
                return "text/plain";
            }

            public MGraph getMetadata() {
                return metadata;
            }

            public String getId() {
                return id;
            }
        };
    }
    @Test
    public void tesetBioText() throws Exception{
        ContentItem ci = wrapAsContentItem(BIO_DOMAIN_TEXT);
        zemantaEngine.computeEnhancements(ci);
        JenaSerializerProvider serializer = new JenaSerializerProvider();
        serializer.serialize(System.out, ci.getMetadata(), SupportedFormat.TURTLE);
        int textAnnoNum = checkAllTextAnnotations(ci.getMetadata(), BIO_DOMAIN_TEXT);
        log.info(textAnnoNum + " TextAnnotations found ...");
        int entityAnnoNum = checkAllEntityAnnotations(ci.getMetadata());
        log.info(textAnnoNum + " EntityAnnotations found ...");
    }
    /*
     * -----------------------------------------------------------------------
     * Helper Methods to check Text and EntityAnnotations
     * -----------------------------------------------------------------------
     */

    /**
     * @param g
     * @return
     */
    private int checkAllEntityAnnotations(MGraph g) {
        Iterator<Triple> entityAnnotationIterator = g.filter(null,
                Properties.RDF_TYPE, TechnicalClasses.ENHANCER_ENTITYANNOTATION);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            UriRef entityAnnotation = (UriRef) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            checkEntityAnnotation(g, entityAnnotation);
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
    }
    /**
     * Checks if an entity annotation is valid
     *
     * @param g
     * @param textAnnotation
     */
    private void checkEntityAnnotation(MGraph g, UriRef entityAnnotation) {
        Iterator<Triple> relationIterator = g.filter(
                entityAnnotation, Properties.DC_RELATION, null);
        Iterator<Triple> requiresIterator = g.filter(
                entityAnnotation, Properties.DC_REQUIRES, null);
        Iterator<Triple> dcTypeCategory = g.filter(
                entityAnnotation, Properties.DC_TYPE, TechnicalClasses.ENHANCER_CATEGORY);
        // check if the relation or an requires annotation set
        // also include the DC_TYPE ENHANCER_CATEGORY, because such entityEnhancements
        // do not need to have any values for DC_RELATION nor DC_REQUIRES
        assertTrue(relationIterator.hasNext() || requiresIterator.hasNext() || dcTypeCategory.hasNext());
        while (relationIterator.hasNext()) {
            // test if the referred annotations are text annotations
            UriRef referredTextAnnotation = (UriRef) relationIterator.next().getObject();
            assertTrue(g.filter(referredTextAnnotation, Properties.RDF_TYPE,
                    TechnicalClasses.ENHANCER_TEXTANNOTATION).hasNext());
        }

        // test if an entity is referred
        Iterator<Triple> entityReferenceIterator = g.filter(entityAnnotation,
                Properties.ENHANCER_ENTITY_REFERENCE, null);
        assertTrue(entityReferenceIterator.hasNext());
        // test if the reference is an URI
        assertTrue(entityReferenceIterator.next().getObject() instanceof UriRef);
        // test if there is only one entity referred
        //NOTE: The Zemanta Engine referrs several entities if they are marked as
        //      owl:sameAs by Zemanta
        //assertFalse(entityReferenceIterator.hasNext());

        // finally test if the entity label is set
        Iterator<Triple> entityLabelIterator = g.filter(entityAnnotation,
                Properties.ENHANCER_ENTITY_LABEL, null);
        assertTrue(entityLabelIterator.hasNext());
    }
    /**
     * @param g
     * @return
     */
    private int checkAllTextAnnotations(MGraph g, String content) {
        Iterator<Triple> textAnnotationIterator = g.filter(null,
                Properties.RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION);
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
     *
     * @param g
     * @param textAnnotation
     */
    private void checkTextAnnotation(MGraph g, UriRef textAnnotation, String content) {
        Iterator<Triple> selectedTextIterator = g.filter(textAnnotation,
                Properties.ENHANCER_SELECTED_TEXT, null);
        // check if the selected text is added
        assertTrue(selectedTextIterator.hasNext());
        // test if the selected text is part of the TEXT_TO_TEST
        Resource object = selectedTextIterator.next().getObject();
        assertTrue(object instanceof Literal);
        Literal selectedText = (Literal)object;
        object = null;
        assertTrue(content.indexOf(selectedText.getLexicalForm()) >= 0);
        // test if context is added
        //context not present for Zemanta
//        Iterator<Triple> selectionContextIterator = g.filter(textAnnotation,
//                Properties.ENHANCER_SELECTION_CONTEXT, null);
//        assertTrue(selectionContextIterator.hasNext());
//        // test if the selected text is part of the TEXT_TO_TEST
//        object = selectionContextIterator.next().getObject();
//        assertTrue(object instanceof Literal);
//        assertTrue(content.indexOf(((Literal)object).getLexicalForm()) >= 0);
//        object = null;
        //test start/end if present
        Iterator<Triple> startPosIterator = g.filter(textAnnotation,
                Properties.ENHANCER_START, null);
        Iterator<Triple> endPosIterator = g.filter(textAnnotation,
                Properties.ENHANCER_END, null);
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
            assertTrue(content.substring(start, end).equals(selectedText.getLexicalForm()));
        } else {
            //if no start position is present, there must also be no end position defined
            assertTrue(!endPosIterator.hasNext());
        }
    }

    public static void main(String[] args) throws Exception{
        ZemantaEnhancementEngineTest test = new ZemantaEnhancementEngineTest();
        test.setUpServices();
        test.tesetBioText();
        test.shutdownServices();
    }
}
