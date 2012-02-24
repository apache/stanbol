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
package org.apache.stanbol.enhancer.ldpath;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.exception.LDPathParseException;

public class ContentItemBackendTest {
    /**
     * Avoids that the parser closes the {@link ZipInputStream} after the
     * first entry
     */
    protected static class UncloseableStream extends FilterInputStream {

        public UncloseableStream(InputStream in) {
            super(in);
        }
        @Override
        public void close() throws IOException {
        }
    }
    
    private Logger log = LoggerFactory.getLogger(ContentItemBackendTest.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static LiteralFactory lf = LiteralFactory.getInstance();
    
    private static String textContent;
    private static String htmlContent;
    private static ContentItem ci;
    private ContentItemBackend backend;
    private LDPath<Resource> ldpath;
    @BeforeClass
    public static void readTestData() throws IOException {
        //add the metadata
        ParsingProvider parser = new JenaParserProvider();
        //create the content Item with the HTML content
        MGraph rdfData = parseRdfData(parser,"metadata.rdf.zip");
        UriRef contentItemId = null;
        Iterator<Triple> it = rdfData.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
        while(it.hasNext()){
            Resource r = it.next().getObject();
            if(contentItemId == null){
                if(r instanceof UriRef){
                    contentItemId = (UriRef)r;
                }
            } else {
                assertEquals("multiple ContentItems IDs contained in the RDF test data", 
                    contentItemId,r);
            }
        }
        assertNotNull("RDF data doe not contain an Enhancement extracted form " +
        		"the content item",contentItemId);
        
        InputStream in = getTestResource("content.html");
        assertNotNull("HTML content not found",in);
        byte[] htmlData = IOUtils.toByteArray(in);
        IOUtils.closeQuietly(in);
        ci = new InMemoryContentItem(contentItemId.getUnicodeString(), 
            htmlData, "text/html; charset=UTF-8");
        htmlContent = new String(htmlData, UTF8);
        //create a Blob with the text content
        in = getTestResource("content.txt");
        byte[] textData = IOUtils.toByteArray(in);
        IOUtils.closeQuietly(in);
        assertNotNull("Plain text content not found",in);
        ci.addPart(new UriRef(ci.getUri().getUnicodeString()+"_text"), 
            new InMemoryBlob(textData, "text/plain; charset=UTF-8"));
        textContent = new String(textData, UTF8);
        //add the metadata
        ci.getMetadata().addAll(rdfData);
    }

    /**
     * @param parser
     * @return
     * @throws IOException
     */
    protected static MGraph parseRdfData(ParsingProvider parser,String name) throws IOException {
        MGraph rdfData = new IndexedMGraph();
        InputStream in = getTestResource(name);
        assertNotNull("File '"+name+"' not found",in);
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
        InputStream uncloseable = new UncloseableStream(zipIn);
        ZipEntry entry;
        while((entry = zipIn.getNextEntry()) != null){
            if(entry.getName().endsWith(".rdf")){
                parser.parse(rdfData,uncloseable, SupportedFormat.RDF_XML,null);
            }
        }
        assertTrue(rdfData.size() > 0);
        zipIn.close();
        return rdfData;
    }

    /**
     * @return
     */
    protected static InputStream getTestResource(String resourceName) {
        InputStream in = ContentItemBackendTest.class.getClassLoader().getResourceAsStream(resourceName);
        return in;
    }
    
    @Before
    public void initBackend(){
        if(backend == null){
            backend = new ContentItemBackend(ci);
        }
        if(ldpath == null){
            ldpath = new LDPath<Resource>(backend, EnhancerLDPath.getConfig());
        }
    }
    
    @Test
    public void testContent() throws LDPathParseException {
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), "fn:content(\"text/plain\")", null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        Resource r = result.iterator().next();
        assertTrue(r instanceof Literal);
        String content = ((Literal)r).getLexicalForm();
        assertEquals(content, textContent);
        
        result = ldpath.pathQuery(ci.getUri(), "fn:content(\"text/html\")", null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        r = result.iterator().next();
        assertTrue(r instanceof Literal);
        content = ((Literal)r).getLexicalForm();
        assertEquals(content, htmlContent);
    }
    @Test
    public void testTextAnnotationFunction() throws LDPathParseException {
        String path = "fn:textAnnotation(.)/fise:selected-text";
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 2);
        Set<String> expectedValues = new HashSet<String>(
                Arrays.asList("Bob Marley","Paris"));
        for(Resource r : result){
            assertTrue(r instanceof Literal);
            assertTrue(expectedValues.remove(((Literal)r).getLexicalForm()));
        }
        assertTrue(expectedValues.isEmpty());
        
        //test with a filter for the type
        //same as the 1st example bat rather using an ld-path construct for
        //filtering for TextAnnotations representing persons
        path = "fn:textAnnotation(.)[dc:type is dbpedia-ont:Person]/fise:selected-text";
        result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        Resource r = result.iterator().next();
        assertTrue(r instanceof Literal);
        assertEquals(((Literal)r).getLexicalForm(), "Bob Marley");

    }
    @Test
    public void testEntityAnnotation() throws LDPathParseException {
        String path = "fn:entityAnnotation(.)/fise:entity-reference";
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 4);
        Set<UriRef> expectedValues = new HashSet<UriRef>(
                Arrays.asList(
                    new UriRef("http://dbpedia.org/resource/Paris"),
                    new UriRef("http://dbpedia.org/resource/Bob_Marley"),
                    new UriRef("http://dbpedia.org/resource/Centre_Georges_Pompidou"),
                    new UriRef("http://dbpedia.org/resource/Paris,_Texas")));
        for(Resource r : result){
            assertTrue(r instanceof UriRef);
            log.info("Entity: {}",r);
            assertTrue(expectedValues.remove(r));
        }
        assertTrue(expectedValues.isEmpty());
        //and with a filter
        path = "fn:entityAnnotation(.)[fise:entity-type is dbpedia-ont:Person]/fise:entity-reference";
        result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        assertTrue(result.contains(new UriRef("http://dbpedia.org/resource/Bob_Marley")));
    }
    @Test
    public void testEnhancements() throws LDPathParseException {
        String path = "fn:enhancement(.)";
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 7);
        for(Resource r : result){
            assertTrue(r instanceof UriRef);
            log.info("Entity: {}",r);
        }
        //and with a filter
        path = "fn:enhancement(.)[rdf:type is fise:TextAnnotation]";
        result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 3);
//        assertTrue(result.contains(new UriRef("http://dbpedia.org/resource/Bob_Marley")));
        path = "fn:enhancement(.)/dc:language";
        result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        Resource r = result.iterator().next();
        assertTrue(r instanceof Literal);
        assertEquals("en",((Literal)r).getLexicalForm());
    }
    @Test
    public void testEntitySuggestions() throws LDPathParseException {
        //NOTE: Sort while supported by fn:suggestion is currently not
        //      supported by LDPath. Therefore the sort of fn:suggestion can
        //      currently only ensure the the top most {limit} entities are
        //      selected if the "limit" parameter is set.
        // Because this test checks first that all three suggestions for Paris
        // are returned and later that a limit of 2 only returns the two top
        // most.
        String path = "fn:textAnnotation(.)[dc:type is dbpedia-ont:Place]/fn:suggestion(.)";
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 3);
        Double lowestConfidence = null;
        //stores the lowest confidence suggestion for the 2nd part of this test
        UriRef lowestConfidenceSuggestion = null;
        path = "fise:confidence :: xsd:double";
        for(Resource r : result){
            assertTrue(r instanceof UriRef);
            log.info("confidence: {}",r);
            Double current = (Double)ldpath.pathTransform(r, path, null).iterator().next();
            assertNotNull(current);
            if(lowestConfidence == null || lowestConfidence > current){
                lowestConfidence = current;
                lowestConfidenceSuggestion = (UriRef) r;
            }
        }
        assertNotNull(lowestConfidenceSuggestion);
        path = "fn:textAnnotation(.)[dc:type is dbpedia-ont:Place]/fn:suggestion(.,\"2\")";
        Collection<Resource> result2 = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result2);
        assertFalse(result2.isEmpty());
        assertTrue(result2.size() == 2);
        //first check that all results of the 2nd query are also part of the first
        assertTrue(result.containsAll(result2));
        //secondly check that the lowest confidence suggestion is now missing
        assertFalse(result2.contains(lowestConfidenceSuggestion));
    }
    @Test
    public void testSuggestedEntity() throws LDPathParseException {
        //The suggestedEntity function can be used for twi usecases
        //(1) get the {limit} top rated linked Entities per parsed context
        //    In this example we parse all TextAnnotations
        //NOTE: '.' MUST BE used as first argument in this case
        String path = "fn:textAnnotation(.)/fn:suggestedEntity(.,\"1\")";
        Collection<Resource> result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 2);
        Set<UriRef> expectedValues = new HashSet<UriRef>(
                Arrays.asList(
                    new UriRef("http://dbpedia.org/resource/Paris"),
                    new UriRef("http://dbpedia.org/resource/Bob_Marley")));
        for(Resource r : result){
            assertTrue(r instanceof UriRef);
            log.info("Entity: {}",r);
            assertTrue(expectedValues.remove(r));
        }
        assertTrue(expectedValues.isEmpty());
        
        //(2) get the {limit} top rated Entities for all Annotations parsed
        //    as the first argument
        //NOTE: the selector parsing all Annotations MUST BE used as first
        //      argument
        path = "fn:suggestedEntity(fn:textAnnotation(.),\"1\")";
        result = ldpath.pathQuery(ci.getUri(), path, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.size() == 1);
        assertEquals(new UriRef("http://dbpedia.org/resource/Paris"),
            result.iterator().next());
        
    }
}
