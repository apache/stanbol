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
package org.apache.stanbol.enhancer.engines.langid;

import static junit.framework.Assert.assertEquals;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.tika.language.LanguageIdentifier;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * {@link LangIdEngineTest} is a test class for {@link TextCategorizer}.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id: LangIdTest.java 1145590 2011-07-12 13:26:39Z wkasper $
 */
public class LangIdEngineTest {

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    private static final String TEST_FILE_NAME = "en.txt";
    /**
     * This contains the text used for testing
     */
    private static String text;
    /**
     * This initializes the text categorizer.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws IOException {
        LanguageIdentifier.initProfiles();
        InputStream in = LangIdEngineTest.class.getClassLoader().getResourceAsStream(
            TEST_FILE_NAME);
        assertNotNull("failed to load resource " + TEST_FILE_NAME, in);
        text = IOUtils.toString(in);
    }

    /**
     * Tests the language identification.
     *
     * @throws IOException if there is an error when reading the text
     */
    @Test
    public void testLangId() throws IOException {
        LanguageIdentifier tc = new LanguageIdentifier(text);
        String language = tc.getLanguage();
        assertEquals("en", language);
    }
    /**
     * Test the engine and validates the created enhancements
     * @throws EngineException
     * @throws IOException
     * @throws ConfigurationException
     */
    @Test
    public void testEngine() throws EngineException, IOException, ConfigurationException {
        LangIdEnhancementEngine langIdEngine = new LangIdEnhancementEngine();
        ComponentContext context =  new MockComponentContext();
        context.getProperties().put(EnhancementEngine.PROPERTY_NAME, "langid");
        langIdEngine.activate(context);
        ContentItem ci = ciFactory.createContentItem(new StringSource(text));
        langIdEngine.computeEnhancements(ci);
        HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            langIdEngine.getClass().getName()));
        int textAnnotationCount = validateAllTextAnnotations(ci.getMetadata(), text, expectedValues);
        assertEquals("A single TextAnnotation is expected", 1,textAnnotationCount);
        //even through this tests do not validate service quality but rather
        //the correct integration of the CELI service as EnhancementEngine
        //we expect the "en" is detected for the parsed text
        assertEquals("The detected language for text '"+text+"' MUST BE 'en'",
            "en",EnhancementEngineHelper.getLanguage(ci));

        int entityAnnoNum = validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
        assertEquals("No EntityAnnotations are expected",0, entityAnnoNum);

    }
}
