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
package org.apache.stanbol.enhancer.engines.opencalais.impl;

import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides JUnit tests for OpenCalaisEngine.
 * 
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */

public class TestOpenCalaisEngine {

    /**
     * This contains the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestOpenCalaisEngine.class);

    private static OpenCalaisEngine calaisExtractor;

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static String TEST_LICENSE_KEY = System.getProperty(OpenCalaisEngine.LICENSE_KEY);
    private static String TEST_TEXT = "Israeli PM Netanyahu pulls out of US nuclear summit\n"
          + "Israeli PM Benjamin Netanyahu has cancelled a visit to the US where he was to "
          + "attend a summit on nuclear security, Israeli officials say. Mr Netanyahu made "
          + "the decision after learning that Egypt and Turkey intended to raise the issue "
          + "of Israel's presumed nuclear arsenal, the officials said. Mr Obama is due to "
          + "host dozens of world leaders at the two-day conference, which begins in "
          + "Washington on Monday. Israel has never confirmed or denied that it possesses "
          + "atomic weapons. Israel's Intelligence and Atomic Energy Minister Dan Meridor "
          + "will take Netanyahu's place in the nuclear summit, Israeli radio said. More "
          + "than 40 countries are expected at the meeting, which will focus on preventing "
          + "the spread of nuclear weapons to militant groups.";

    @BeforeClass
    public static void oneTimeSetup() throws ConfigurationException {
        calaisExtractor = new OpenCalaisEngine();
        calaisExtractor.setCalaisTypeMap(new HashMap<IRI,IRI>());
        calaisExtractor.tcManager = TcManager.getInstance();
        if (TEST_LICENSE_KEY != null && TEST_LICENSE_KEY.matches("\\w+")) {
            calaisExtractor.setLicenseKey(TEST_LICENSE_KEY);
        }
    }

    public static ContentItem wrapAsContentItem(final String text) throws IOException {
        return ciFactory.createContentItem(new StringSource(text));
    }

    @Test
    public void testEntityExtraction() throws IOException, EngineException {
        String testFile = "calaisresult.owl";
        String format = "application/rdf+xml";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(testFile);
        Assert.assertNotNull("failed to load resource " + testFile, in);
        Graph model = calaisExtractor.readModel(in, format);
        Assert.assertNotNull("model reader failed with format: " + format, model);
        Collection<CalaisEntityOccurrence> entities;
        try {
            entities = calaisExtractor.queryModel(model);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }
        LOG.info("Found entities: {}", entities.size());
        LOG.debug("Entities:\n{}", entities);
        Assert.assertFalse("No entities found!", entities.isEmpty());
        //test the generation of the Enhancements
        ContentItem ci = wrapAsContentItem(TEST_TEXT);
        calaisExtractor.createEnhancements(entities, ci);
        Map<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, 
            LiteralFactory.getInstance().createTypedLiteral(
                calaisExtractor.getClass().getName()));
        //adding null as expected for confidence makes it a required property
        expectedValues.put(Properties.ENHANCER_CONFIDENCE, null);
        validateAllTextAnnotations(ci.getMetadata(), 
            TEST_TEXT, expectedValues);
        validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
    }

    @Test
    public void testCalaisConnection() throws IOException, EngineException {
        Assume.assumeNotNull(calaisExtractor.getLicenseKey());
        ContentItem ci = wrapAsContentItem(TEST_TEXT);
        ci.getMetadata().add(
            new TripleImpl(ci.getUri(), Properties.DC_LANGUAGE, LiteralFactory.getInstance()
                    .createTypedLiteral("en")));
        Graph model;
        try {
            model = calaisExtractor.getCalaisAnalysis(TEST_TEXT, "text/plain");
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }
        Assert.assertNotNull("No model", model);
        Collection<CalaisEntityOccurrence> entities;
        try {
            entities = calaisExtractor.queryModel(model);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }
        LOG.info("Found entities: {}", entities.size());
        LOG.debug("Entities:\n{}", entities);
        Assert.assertFalse("No entities found!", entities.isEmpty());
    }

    // problem with license keys for testing?
    // ask user to supply it as system property or whatever?

}
