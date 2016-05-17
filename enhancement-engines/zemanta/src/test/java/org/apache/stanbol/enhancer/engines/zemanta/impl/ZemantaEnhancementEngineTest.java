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
package org.apache.stanbol.enhancer.engines.zemanta.impl;

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.stanbol.enhancer.engines.zemanta.impl.ZemantaEnhancementEngine.API_KEY_PROPERTY;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZemantaEnhancementEngineTest {
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

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final Logger log = LoggerFactory.getLogger(ZemantaEnhancementEngineTest.class);

    /**
     * This key was generated to support testing only. Please do only use it
     * for testing. For real usages of the engine you need to create your own
     * key!
     */
    private static final String ZEMANTA_TEST_APPLICATION_KEY = "2qsvcvkut8rhnqbhm35znn76";

    @BeforeClass
    public static void setUpServices() throws IOException, ConfigurationException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(API_KEY_PROPERTY, ZEMANTA_TEST_APPLICATION_KEY);
        properties.put(EnhancementEngine.PROPERTY_NAME, "zemanta");
        MockComponentContext context = new MockComponentContext(properties);
        zemantaEngine.activate(context);
    }

    @AfterClass
    public static void shutdownServices() {
        zemantaEngine.deactivate(null);
    }

    public static ContentItem wrapAsContentItem(final String text) throws IOException {
    	String id = "urn:org.apache.stanbol.enhancer:test:engines.zemanta:content-item-"
            + EnhancementEngineHelper.randomUUID().toString();
    	return ciFactory.createContentItem(new IRI(id), new StringSource(text));
    }

    @Test
    public void tesetBioText()  throws EngineException, IOException {
        ContentItem ci = wrapAsContentItem(BIO_DOMAIN_TEXT);
        try {
            zemantaEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }
        JenaSerializerProvider serializer = new JenaSerializerProvider();
        serializer.serialize(System.out, ci.getMetadata(), TURTLE);
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            zemantaEngine.getClass().getName()));
        //deactivate require fise:confidence values for fise:TextAnnotations, because
        //the one used to group the TopicAnnotations does not have a confidence value
        int textAnnoNum = validateAllTextAnnotations(ci.getMetadata(), BIO_DOMAIN_TEXT, expectedValues);
        log.info(textAnnoNum + " TextAnnotations found ...");
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        int entityAnnoNum = EnhancementStructureHelper.validateAllEntityAnnotations(ci.getMetadata(),expectedValues);
        log.info(entityAnnoNum + " EntityAnnotations found ...");
        int topicAnnoNum = EnhancementStructureHelper.validateAllTopicAnnotations(ci.getMetadata(),expectedValues);
        log.info(topicAnnoNum + " TopicAnnotations found ...");
    }

    public static void main(String[] args) throws Exception{
        ZemantaEnhancementEngineTest.setUpServices();
        ZemantaEnhancementEngineTest test = new ZemantaEnhancementEngineTest();
        test.tesetBioText();
        ZemantaEnhancementEngineTest.shutdownServices();
    }
}
