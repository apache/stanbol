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
package org.apache.stanbol.enhancer.engines.langdetect;

import static org.junit.Assert.assertEquals;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;


import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

/**
 * {@link LanguageDetectionEngineTest} is a test class for {@link TextCategorizer}.
 *
 * @author Walter Kasper, DFKI
 */
public class LanguageDetectionEngineTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(LanguageDetectionEngineTest.class);

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    private static final String[] TEST_FILE_NAMES = {"en.txt","ja.txt","ko.txt","zh.txt"};
    
    private static LanguageIdentifier langId;
    
    /**
     * This initializes the text categorizer.
     * @throws LangDetectException 
     */
    @BeforeClass
    public static void oneTimeSetUp() throws IOException, LangDetectException {
        langId = new LanguageIdentifier();
    }

    /**
     * Tests the language identification.
     *
     * @throws IOException if there is an error when reading the text
     */
    @Test
    public void testLangId() throws LangDetectException, IOException {
        LOG.info("Testing: {}", Arrays.asList(TEST_FILE_NAMES));
        for (String file: TEST_FILE_NAMES) {
            String expectedLang = file.substring(0,2);
            InputStream in = LanguageDetectionEngineTest.class.getClassLoader().getResourceAsStream(file);
            assertNotNull("failed to load resource " + file, in);
            String text = IOUtils.toString(in, "UTF-8");
            in.close();
            String language = langId.getLanguage(text);
            if (!expectedLang.equals(language.substring(0,2))) {
                LOG.info("Expected: {}; Found {}",expectedLang,language);
            }
            assertEquals(expectedLang, language.substring(0,2));            
        }
    }
    
    /**
     * Test the engine and validates the created enhancements
     * @throws EngineException
     * @throws IOException
     * @throws ConfigurationException
     * @throws LangDetectException 
     */
    @Test
    public void testEngine() throws EngineException, ConfigurationException, LangDetectException, IOException {
        LOG.info("Testing engine: {}", TEST_FILE_NAMES[0]);
        InputStream in = LanguageDetectionEngineTest.class.getClassLoader().getResourceAsStream(TEST_FILE_NAMES[0]);
        assertNotNull("failed to load resource " + TEST_FILE_NAMES[0], in);
        String text = IOUtils.toString(in, "UTF-8");
        in.close();
        LanguageDetectionEnhancementEngine langIdEngine = new LanguageDetectionEnhancementEngine();
        ComponentContext context =  new MockComponentContext();
        context.getProperties().put(EnhancementEngine.PROPERTY_NAME, "langdetect");
        langIdEngine.activate(context);
        ContentItem ci = ciFactory.createContentItem(new StringSource(text));
        langIdEngine.computeEnhancements(ci);
        HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            langIdEngine.getClass().getName()));
        int textAnnotationCount = validateAllTextAnnotations(ci.getMetadata(), text, expectedValues);
        assertTrue("A TextAnnotation is expected", textAnnotationCount > 0);
        //even through this tests do not validate detection quality
        //we expect the "en" is detected as best guess for the parsed text
        assertEquals("The detected language for text '"+text+"' MUST BE 'en'",
            "en",EnhancementEngineHelper.getLanguage(ci));

        int entityAnnoNum = validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
        assertEquals("No EntityAnnotations are expected",0, entityAnnoNum);

    }
    
    @Test
    public void testNonTextContent() throws EngineException, ConfigurationException, LangDetectException, IOException {
        LanguageDetectionEnhancementEngine langIdEngine = new LanguageDetectionEnhancementEngine();
        ComponentContext context =  new MockComponentContext();
        context.getProperties().put(EnhancementEngine.PROPERTY_NAME, "langdetect");
        langIdEngine.activate(context);
        ContentItem ci = ciFactory.createContentItem(new StringSource("123"));
        langIdEngine.computeEnhancements(ci);
    }
}
