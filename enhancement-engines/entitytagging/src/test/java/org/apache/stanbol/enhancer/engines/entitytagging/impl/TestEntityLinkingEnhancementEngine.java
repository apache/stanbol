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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CREATOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateEntityAnnotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.rdfentities.RdfEntityFactory;
import org.apache.stanbol.enhancer.rdfentities.fise.TextAnnotation;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestEntityLinkingEnhancementEngine {
    
    private static final Logger log = LoggerFactory.getLogger(TestEntityLinkingEnhancementEngine.class);
    
    /**
     * The context for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String CONTEXT = "Dr. Patrick Marshall (1869 - November 1950) was a"
        + " geologist who lived in New Zealand and worked at the University of Otago.";
    /**
     * The person for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PERSON = "Patrick Marshall";
    /**
     * The organisation for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String ORGANISATION ="University of Otago";
    /**
     * The place for the tests (same as in TestOpenNLPEnhancementEngine)
     */
    public static final String PLACE = "New Zealand";

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    static NamedEntityTaggingEngine entityLinkingEngine;

    private static String userDir = System.getProperty("user.dir");
    
    @BeforeClass
    public static void setUpServices() throws IOException {
        //TODO: set user.dir to /target/test-files
        File testFiles = new File("./target/test-files");
        if(!testFiles.isDirectory()){
            if(!testFiles.mkdirs()){
                throw new IOException("Unable to create directory for test files "+testFiles);
            }
        }
        String testRootDir = testFiles.getCanonicalPath();
        log.info("Test 'user.dir' folder {}",testRootDir);
        System.getProperties().setProperty("user.dir", testRootDir);
        entityLinkingEngine = new NamedEntityTaggingEngine();
        //instead of calling activate we directly set the required fields
        //we need a data source for linking
        entityLinkingEngine.entityhub = new MockEntityhub();
        entityLinkingEngine.personState = true;
        entityLinkingEngine.personType = OntologicalClasses.DBPEDIA_PERSON.getUnicodeString();
        entityLinkingEngine.orgState = true;
        entityLinkingEngine.orgType = OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString();
        entityLinkingEngine.placeState = true;
        entityLinkingEngine.placeType = OntologicalClasses.DBPEDIA_PLACE.getUnicodeString();
        entityLinkingEngine.nameField = Properties.RDFS_LABEL.getUnicodeString();
        //not implemented
        entityLinkingEngine.dereferenceEntities = false;
    }

    @Before
    public void bindServices() throws IOException {
    }

    @After
    public void unbindServices() {
    }

    @AfterClass
    public static void shutdownServices() {
        System.getProperties().setProperty("user.dir", userDir);
    }

    public static ContentItem getContentItem(final String id, final String text) throws IOException {
        return ciFactory.createContentItem(new UriRef(id),new StringSource(text));
    }

    public static void getTextAnnotation(ContentItem ci, String name,String context,UriRef type){
        String content;
        try {
            content = IOUtils.toString(ci.getStream(),"UTF-8");
        } catch (IOException e) {
            //should never happen anyway!
            content = "";
        }
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        TextAnnotation textAnnotation = factory.getProxy(
                new UriRef("urn:iks-project:enhancer:test:text-annotation:"+randomUUID()), TextAnnotation.class);
        textAnnotation.setCreator(new UriRef("urn:iks-project:enhancer:test:dummyEngine"));
        textAnnotation.setCreated(new Date());
        textAnnotation.setSelectedText(name);
        textAnnotation.setSelectionContext(context);
        textAnnotation.getDcType().add(type);
        Integer start = content.indexOf(name);
        if (start < 0){ //if not found in the content
            //set some random numbers for start/end
            start = (int)Math.random()*100;
        }
        textAnnotation.setStart(start);
        textAnnotation.setEnd(start+name.length());
    }

    @Test
    public void testEntityLinkingEnhancementEngine() throws Exception{
        //create a content item
        ContentItem ci = getContentItem("urn:iks-project:enhancer:text:content-item:person", CONTEXT);
        //add three text annotations to be consumed by this test
        getTextAnnotation(ci, PERSON, CONTEXT, DBPEDIA_PERSON);
        getTextAnnotation(ci, ORGANISATION, CONTEXT, DBPEDIA_ORGANISATION);
        getTextAnnotation(ci, PLACE, CONTEXT, DBPEDIA_PLACE);
        //add the language
        ci.getMetadata().add(new TripleImpl(ci.getUri(), Properties.DC_LANGUAGE, new PlainLiteralImpl("en")));
        //perform the computation of the enhancements
        entityLinkingEngine.computeEnhancements(ci);
        int entityAnnotationCount = validateAllEntityAnnotations(ci);
        assertEquals(4, entityAnnotationCount);
    }
    
    private static int validateAllEntityAnnotations(ContentItem ci){
        Map<UriRef,Resource> expectedValues = new HashMap<UriRef,Resource>();
        expectedValues.put(ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(DC_CREATOR,LiteralFactory.getInstance().createTypedLiteral(
            entityLinkingEngine.getClass().getName()));
        Iterator<Triple> entityAnnotationIterator = ci.getMetadata().filter(null,
                RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        int entityAnnotationCount = 0;
        while (entityAnnotationIterator.hasNext()) {
            UriRef entityAnnotation = (UriRef) entityAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateEntityAnnotation(ci.getMetadata(), entityAnnotation, expectedValues);
            //fise:confidence now checked by EnhancementStructureHelper (STANBOL-630)
//            Iterator<Triple> confidenceIterator = ci.getMetadata().filter(entityAnnotation, ENHANCER_CONFIDENCE, null);
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
            UriRef ENTITYHUB_SITE = new UriRef(RdfResourceEnum.site.getUri());
            Iterator<Triple> entitySiteIterator = ci.getMetadata().filter(entityAnnotation, 
                ENTITYHUB_SITE, null);
            assertTrue("Expected entityhub:site value is missing (entityAnnotation "
                    +entityAnnotation+")",entitySiteIterator.hasNext());
            Resource siteResource = entitySiteIterator.next().getObject();
            assertTrue("entityhub:site values MUST BE Literals", siteResource instanceof Literal);
            assertEquals("'dbpedia' is expected as entityhub:site value", "dbpedia", ((Literal)siteResource).getLexicalForm());
            assertFalse("entityhub:site MUST HAVE only a single value", entitySiteIterator.hasNext());
            entityAnnotationCount++;
        }
        return entityAnnotationCount;
        
    }


}
