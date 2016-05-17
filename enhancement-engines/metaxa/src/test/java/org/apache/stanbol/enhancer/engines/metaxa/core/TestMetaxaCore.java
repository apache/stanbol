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
package org.apache.stanbol.enhancer.engines.metaxa.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.Variable;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.vocabulary.NMO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * {@link TestMetaxaCore} is a test class for {@link MetaxaCore}.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
public class TestMetaxaCore {

    /**
     * This contains the logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestMetaxaCore.class);

    /**
     * This contains the Metaxa extractor to test.
     */
    private static MetaxaCore extractor;


    /**
     * This initializes the Aperture extractor.
     */
    @BeforeClass
    public static void oneTimeSetUp() throws IOException {
        extractor = new MetaxaCore("extractionregistry.xml");
    }

    /**
     * This tests the pdf extraction.
     *
     * @throws ExtractorException if there is an error during extraction
     * @throws IOException if there is an error when reading the document
     */
    @Test
    public void testPdfExtraction() throws Exception {

        String testFile = "test.pdf";
        String testResultFile = "pdf-res.txt";

        // extract text from pdf
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        Model m = extractor.extract(in, new URIImpl("file://" + testFile), "application/pdf");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 = getResourceAsStream(testResultFile);
        assertNotNull("failed to load resource " + testResultFile, in2);

        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        assertEquals(cleanup(expectedText), cleanup(text));

        // show triples
        int tripleCounter = this.printTriples(m);
        assertEquals(11, tripleCounter);
    }

    /**
     * This tests the html extraction.
     *
     * @throws ExtractorException if there is an error during extraction
     * @throws IOException if there is an error when reading the document
     */
    @Test
    public void testHtmlExtraction() throws Exception {

        String testFile = "test.html";
        String testResultFile = "html-res.txt";

        // extract text from html
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        Model m = extractor.extract(in, new URIImpl("file://" + testFile), "text/html");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 = getResourceAsStream(testResultFile);
        assertNotNull("failed to load resource " + testResultFile, in2);

        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        assertEquals(cleanup(expectedText), cleanup(text));

        // show triples
        int tripleCounter = this.printTriples(m);
        assertEquals(28, tripleCounter);
    }

    /**
     * This tests the html extraction.
     *
     * @throws ExtractorException if there is an error during extraction
     * @throws IOException if there is an error when reading the document
     */
    @Test
    public void testRdfaExtraction() throws Exception {
        String testFile = "test-rdfa.html";
        String testResultFile = "rdfa-res.txt";

        // extract text from RDFa annotated html
        InputStream in = getResourceAsStream(testFile);
        assertNotNull("failed to load resource " + testFile, in);

        Model m = extractor.extract(in, new URIImpl("file://" + testFile), "text/html");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 = getResourceAsStream(testResultFile);
        assertNotNull("failed to load resource " + testResultFile, in2);

        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        assertEquals(cleanup(expectedText), cleanup(text));

        // show triples
        int tripleCounter = this.printTriples(m);
        assertEquals(10, tripleCounter);
    }
    
    @Test
    public void testMailExtraction() throws Exception {
      String testFile = "mail-multipart-test.eml";
      InputStream in = getResourceAsStream(testFile);
      assertNotNull("failed to load resource " + testFile, in);
      Model m = extractor.extract(in, new URIImpl("file://" + testFile), "message/rfc822");
      boolean textContained = m.contains(Variable.ANY, NMO.plainTextMessageContent, Variable.ANY);
      assertTrue(textContained);
    }

    /**
     * This prints out the Stanbol Enhancer triples that would be created for the metadata
     * contained in the given model.
     *
     * @param m a {@link Model}
     *
     * @return an {@code int} with the number of added triples
     */
    private int printTriples(Model m) {

        int tripleCounter = 0;

        HashMap<BlankNode, BlankNode> blankNodeMap = new HashMap<BlankNode, BlankNode>();

        ClosableIterator<Statement> it = m.iterator();
        while (it.hasNext()) {
            Statement oneStmt = it.next();

            BlankNodeOrIRI subject = (BlankNodeOrIRI) MetaxaEngine.asClerezzaResource(oneStmt.getSubject(), blankNodeMap);
            IRI predicate = (IRI) MetaxaEngine.asClerezzaResource(oneStmt.getPredicate(), blankNodeMap);
            RDFTerm object = MetaxaEngine.asClerezzaResource(oneStmt.getObject(), blankNodeMap);

            if (null != subject
                    && null != predicate
                    && null != object) {
                Triple t =
                        new TripleImpl(subject, predicate, object);
                LOG.debug("adding " + t);
                tripleCounter++;
            } else {
                LOG.debug("skipped " + oneStmt.toString());
            }
        }
        it.close();

        return tripleCounter;
    }

    /**
     * Cleanup strings for comparison, by removing non-printable chars.
     *
     * @param txt a {@link String} with the text to clean
     *
     * @return a {@link String} with the result
     */
    private String cleanup(String txt) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < txt.length(); i++) {
            final char c = txt.charAt(i);
            if (c >= ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private InputStream getResourceAsStream(String testResultFile) {
        return this.getClass().getClassLoader().getResourceAsStream(
                testResultFile);
    }

}
