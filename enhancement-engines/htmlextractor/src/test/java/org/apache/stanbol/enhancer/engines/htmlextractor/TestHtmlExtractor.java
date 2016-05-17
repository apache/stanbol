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
package org.apache.stanbol.enhancer.engines.htmlextractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.ClerezzaRDFUtils;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.ExtractorException;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlExtractionRegistry;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlExtractor;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlParser;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.InitializationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class TestHtmlExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(TestHtmlExtractor.class);
    
    private static HtmlParser parser;
    
    private static HtmlExtractionRegistry registry;
    
    // define the Nepomuks NIE namespace locally here
    private static final String NIE_NS = "http://www.semanticdesktop.org/ontologies/2007/01/19/nie#";
    
    @BeforeClass
    public static void oneTimeSetup() throws IOException {
        try {
            registry = new HtmlExtractionRegistry("htmlextractors.xml");
        }
        catch (InitializationException e) {
            LOG.error("Registry Initialization Error: " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        parser = new HtmlParser();

    }
    
    /**
     * This tests the RDFa extraction.
     *
     * @throws ExtractorException if there is an error during extraction
     * @throws IOException if there is an error when reading the document
     */
    @Test
    public void testRdfaExtraction() throws Exception {
        HtmlExtractor extractor = new HtmlExtractor(registry, parser);
        Graph model = new SimpleGraph();
        String testFile = "test-rdfa.html";
        // extract text from RDFa annotated html
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        extractor.extract("file://" + testFile,in,null, "text/html", model);

        // show triples
        int tripleCounter = model.size();
        LOG.debug("RDFa triples: {}",tripleCounter);
        printTriples(model);
        assertEquals(8, tripleCounter);
        ClerezzaRDFUtils.makeConnected(model, new IRI("file://" + testFile), new IRI(NIE_NS+"contains"));
    }
    
    /** This tests some Microformat extraction
     * 
     * @throws ExtractorException if there is an error during extraction
     * @throws IOException if there is an error when reading the document
     */
    @Test
    public void testMFExtraction() throws Exception {
        HtmlExtractor extractor = new HtmlExtractor(registry, parser);
        Graph model = new SimpleGraph();
        String testFile = "test-MF.html";

        // extract text from RDFa annotated html
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        extractor.extract("file://" + testFile,in,null, "text/html", model);

        // show triples
        int tripleCounter = model.size();
        LOG.debug("Microformat triples: {}",tripleCounter);
        printTriples(model);
        assertEquals(127, tripleCounter);
        ClerezzaRDFUtils.makeConnected(model, new IRI("file://" + testFile), new IRI(NIE_NS+"contains"));
    }

    /** This test some extraction of microdata from an HTML-5 document
     * 
     * @throws Exception
     */
    @Test
    public void testMicrodataExtraction() throws Exception {
      HtmlExtractor extractor = new HtmlExtractor(registry, parser);
      Graph model = new SimpleGraph();
      String testFile = "test-microdata.html";

      // extract text from RDFa annotated html
      InputStream in = getResourceAsStream(testFile);
      assertNotNull("failed to load resource " + testFile, in);

      extractor.extract("file://" + testFile,in,null, "text/html", model);

      // show triples
      int tripleCounter = model.size();
      LOG.debug("Microdata triples: {}",tripleCounter);
      printTriples(model);
      assertEquals(91, tripleCounter);
      ClerezzaRDFUtils.makeConnected(model, new IRI("file://" + testFile), new IRI(NIE_NS+"contains"));
    }
    
    /** This tests the merging of disconnected graphs under a single root
     * 
     * @throws Exception
     */
    @Test
    public void testRootExtraction() throws Exception {
        HtmlExtractor extractor = new HtmlExtractor(registry, parser);
        Graph model = new SimpleGraph();
        String testFile = "test-MultiRoot.html";

        // extract text from RDFa annotated html
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        extractor.extract("file://" + testFile,in,null, "text/html", model);

        // show triples
        int tripleCounter = model.size();
        LOG.debug("Triples: {}",tripleCounter);
        printTriples(model);
        Set<BlankNodeOrIRI> roots = ClerezzaRDFUtils.findRoots(model);
        assertTrue(roots.size() > 1);
        ClerezzaRDFUtils.makeConnected(model, new IRI("file://" + testFile), new IRI(NIE_NS+"contains"));
        roots = ClerezzaRDFUtils.findRoots(model);
        assertEquals(1,roots.size());
    }
    
    private InputStream getResourceAsStream(String testResultFile) {
        return this.getClass().getClassLoader().getResourceAsStream(
                testResultFile);
    }

    private void printTriples(Graph model) {
        for (Triple t: model) {
            LOG.debug(t.toString());
        }
    }    
}
