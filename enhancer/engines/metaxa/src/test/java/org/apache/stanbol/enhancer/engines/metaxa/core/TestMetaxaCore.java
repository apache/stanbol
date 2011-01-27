package org.apache.stanbol.enhancer.engines.metaxa.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine;
import org.apache.stanbol.enhancer.engines.metaxa.core.MetaxaCore;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private static final Logger LOG =
        LoggerFactory.getLogger(TestMetaxaCore.class);

    /**
     * This contains the Metaxa extractor to test.
     */
    private static MetaxaCore extractor;


    /**
     * This initializes the Aperture extractor.
     */
    @BeforeClass
    public static void oneTimeSetUp()
            throws IOException {

        extractor = new MetaxaCore("extractionregistry.xml");
    }

    /**
     * This tests the pdf extraction.
     *
     * @throws ExtractorException
     *             if there is an error during extraction
     * @throws IOException
     *             if there is an error when reading the document
     */
    @Test
    public void testPdfExtraction() throws Exception {

        String testFile = "test.pdf";
        String testResultFile = "pdf-res.txt";

        // extract text from pdf
        InputStream in =
            this.getClass().getClassLoader().getResourceAsStream(
            testFile);
        Assert.assertNotNull("failed to load resource " + testFile, in);
        Model m = extractor.extract(
            in, "file://" + testFile, "application/pdf");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 =
            this.getClass().getClassLoader().getResourceAsStream(
            testResultFile);
        Assert.assertNotNull(
            "failed to load resource " + testResultFile, in2);
        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        Assert.assertEquals(cleanup(expectedText), cleanup(text));
        // show triples
        int tripleCounter = this.printTriples(m);
        Assert.assertEquals(11, tripleCounter);
    }

    /**
     * This tests the html extraction.
     *
     * @throws ExtractorException
     *             if there is an error during extraction
     * @throws IOException
     *             if there is an error when reading the document
     */
    @Test
    public void testHtmlExtraction() throws Exception {

        String testFile = "test.html";
        String testResultFile = "html-res.txt";

        // extract text from html
        InputStream in =
            this.getClass().getClassLoader().getResourceAsStream(
            testFile);
        Assert.assertNotNull("failed to load resource " + testFile, in);
        Model m = extractor.extract(
            in, "file://" + testFile, "text/html");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 =
            this.getClass().getClassLoader().getResourceAsStream(
            testResultFile);
        Assert.assertNotNull(
            "failed to load resource " + testResultFile, in2);
        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        Assert.assertEquals(cleanup(expectedText), cleanup(text));
        // show triples
        int tripleCounter = this.printTriples(m);
        Assert.assertEquals(28, tripleCounter);
    }

    /**
     * This tests the html extraction.
     *
     * @throws ExtractorException
     *             if there is an error during extraction
     * @throws IOException
     *             if there is an error when reading the document
     */
    @Test
    public void testRdfaExtraction() throws Exception {

        String testFile = "test-rdfa.html";
        String testResultFile = "rdfa-res.txt";

        // extract text from RDFa annotated html
        InputStream in =
            this.getClass().getClassLoader().getResourceAsStream(
            testFile);
        Assert.assertNotNull("failed to load resource " + testFile, in);
        Model m = extractor.extract(
            in, "file://" + testFile, "text/html");
        String text = MetaxaCore.getText(m);
        // get expected result
        InputStream in2 =
            this.getClass().getClassLoader().getResourceAsStream(
            testResultFile);
        Assert.assertNotNull(
            "failed to load resource " + testResultFile, in2);
        String expectedText = IOUtils.toString(in2, "utf-8");
        // test
        Assert.assertEquals(cleanup(expectedText), cleanup(text));
        // show triples
        int tripleCounter = this.printTriples(m);
        Assert.assertEquals(10, tripleCounter);
    }


    /**
     * This prints out the Stanbol Enhancer triples that would be created for the metadata
     * contained in the given model.
     *
     * @param m
     *            a {@link Model}
     * @return an {@code int} with the number of added triples
     */
    private int printTriples(Model m) {

        int tripleCounter = 0;

        HashMap<BlankNode,BNode> blankNodeMap = new HashMap<BlankNode,BNode>();

        ClosableIterator<Statement> it = m.iterator();
        while (it.hasNext()) {
            Statement oneStmt = it.next();

            NonLiteral subject = (NonLiteral) MetaxaEngine.asClerezzaResource(oneStmt.getSubject(), blankNodeMap);
            UriRef predicate = (UriRef) MetaxaEngine.asClerezzaResource(oneStmt.getPredicate(), blankNodeMap);
            Resource object = MetaxaEngine.asClerezzaResource(oneStmt.getObject(), blankNodeMap);

            if (null != subject
                    && null != predicate
                    && null != object) {
                Triple t =
                    new TripleImpl(subject, predicate, object);
                LOG.debug("adding " + t);
                tripleCounter++;
            }
            else {
                LOG.debug("skipped " + oneStmt.toString());
            }
        }
        it.close();

        return tripleCounter;
    }

    /**
     * Cleanup strings for comparison, by removing non-printable chars.
     *
     * @param txt
     *            a {@link String} with the text to clean
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
}
