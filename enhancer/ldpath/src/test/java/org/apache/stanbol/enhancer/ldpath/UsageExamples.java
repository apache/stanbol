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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.stanbol.enhancer.ldpath.ContentItemBackendTest.getTestResource;
import static org.apache.stanbol.enhancer.ldpath.ContentItemBackendTest.parseRdfData;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.io.IOUtils;
import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses the "example.*" files to build a contentItem. This contains a big
 * number of Text/EntityAnnotation and is used here to provide useage examples
 * of the Stanbol Enhancer LDPath functions.<p>
 * In addition setting the {@value #ITERATIONS} parameter to a value >= 100
 * is a good option to do performance testing. Lower values suffer from
 * JIT optimisation. Indexing times on my Machine (1:340ms , 10:100ms, 
 * 100:66ms, 1000:60ms)
 * 
 * @author Rupert Westenthaler
 *
 */
public class UsageExamples {
    
    private static final Logger log = LoggerFactory.getLogger(UsageExamples.class);

    private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    private static int ITERATIONS = 10;
    
    private static ContentItem ci;
    private ContentItemBackend backend;
    private LDPath<RDFTerm> ldpath;
    private static double indexingTime;
    
    @BeforeClass
    public static void readTestData() throws IOException {
        //add the metadata
        ParsingProvider parser = new JenaParserProvider();
        //create the content Item with the HTML content
        Graph rdfData = parseRdfData(parser,"example.rdf.zip");
        IRI contentItemId = null;
        Iterator<Triple> it = rdfData.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
        while(it.hasNext()){
            RDFTerm r = it.next().getObject();
            if(contentItemId == null){
                if(r instanceof IRI){
                    contentItemId = (IRI)r;
                }
            } else {
                assertEquals("multiple ContentItems IDs contained in the RDF test data", 
                    contentItemId,r);
            }
        }
        assertNotNull("RDF data doe not contain an Enhancement extracted form " +
                "the content item",contentItemId);
        
        InputStream in = getTestResource("example.txt");
        assertNotNull("Example Plain text content not found",in);
        byte[] textData = IOUtils.toByteArray(in);
        IOUtils.closeQuietly(in);
        ci = ciFactory.createContentItem(contentItemId, 
            new ByteArraySource(textData, "text/html; charset=UTF-8"));
        ci.getMetadata().addAll(rdfData);
    }
    @Before
    public void initBackend(){
        if(backend == null){
            backend = new ContentItemBackend(ci);
        }
        if(ldpath == null){
            ldpath = new LDPath<RDFTerm>(backend, EnhancerLDPath.getConfig());
        }
    }

    /**
     * This provides some example on how to select persons extracted from
     * a contentItem
     * @throws LDPathParseException
     */
    @Test
    public void exampleExtractedPersons() throws LDPathParseException {
        StringBuilder program = new StringBuilder();
        program.append("personMentions = fn:textAnnotation()" +
        		"[dc:type is dbpedia-ont:Person]/fise:selected-text :: xsd:string;");
        //this uses the labels of suggested person with the highest confidence
        //but also the selected-text as fallback if no entity is suggested.
        program.append("personNames = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Person]/fn:first(fn:suggestion(\"1\")/fise:entity-label,fise:selected-text) :: xsd:string;");
        program.append("linkedPersons = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Person]/fn:suggestedEntity(\"1\") :: xsd:anyURI;");
        //this selects only linked Artists
        program.append("linkedArtists = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Person]/fn:suggestion()" +
                "[fise:entity-type is dbpedia-ont:Artist]/fise:entity-reference :: xsd:anyURI;");
        Program<RDFTerm> personProgram = ldpath.parseProgram(new StringReader(program.toString()));
        log.info("- - - - - - - - - - - - - ");
        log.info("Person Indexing Examples");
        Map<String,Collection<?>> result = execute(personProgram);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        logResults(result);
    }
    /**
     * Execute the ldpath program  {@link #ITERATIONS} times and adds the
     * average execution time to {@link #indexingTime}
     * @param personProgram
     * @return the results
     */
    private Map<String,Collection<?>> execute(Program<RDFTerm> personProgram) {
        long start = System.currentTimeMillis();
        Map<String,Collection<?>> result = personProgram.execute(backend, ci.getUri());
        for(int i=1;i<ITERATIONS;i++){
            result = personProgram.execute(backend, ci.getUri());
        }
        double duration = ((double)(System.currentTimeMillis()-start))/((double)Math.max(1, ITERATIONS));
        log.info("processing time {}ms (average over {} iterations)",duration,Math.max(1, ITERATIONS));
        indexingTime = indexingTime+duration;
        return result;
    }
    /**
     * This provides some example on how to select persons extracted from
     * a contentItem
     * @throws LDPathParseException
     */
    @Test
    public void exampleExtractedPlaces() throws LDPathParseException {
        StringBuilder program = new StringBuilder();
        program.append("locationMentions = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Place]/fise:selected-text :: xsd:string;");
        //this uses the labels of suggested places with the highest confidence
        //but also the selected-text as fallback if no entity is suggested.
        program.append("locationNames = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Place]/fn:first(fn:suggestion(\"1\")/fise:entity-label,fise:selected-text) :: xsd:string;");
        program.append("linkedPlaces = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Place]/fn:suggestedEntity(\"1\") :: xsd:anyURI;");
        //this selects only linked Artists
        program.append("linkedCountries = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Place]/fn:suggestion()" +
                "[fise:entity-type is dbpedia-ont:Country]/fise:entity-reference :: xsd:anyURI;");
        Program<RDFTerm> personProgram = ldpath.parseProgram(new StringReader(program.toString()));
        log.info("- - - - - - - - - - - - -");
        log.info("Places Indexing Examples");
        Map<String,Collection<?>> result = execute(personProgram);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        logResults(result);
    }
    /**
     * This provides some example on how to select persons extracted from
     * a contentItem
     * @throws LDPathParseException
     */
    @Test
    public void exampleExtractedOrganization() throws LDPathParseException {
        StringBuilder program = new StringBuilder();
        program.append("orgMentions = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Organisation]/fise:selected-text :: xsd:string;");
        //this uses the labels of suggested organisations with the highest confidence
        //but also the selected-text as fallback if no entity is suggested.
        program.append("orgNames = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Organisation]/fn:first(fn:suggestion(\"1\")/fise:entity-label,fise:selected-text) :: xsd:string;");
        program.append("linkedOrgs = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Organisation]/fn:suggestedEntity(\"1\") :: xsd:anyURI;");
        //this selects only linked education organisations
        //NOTE: this does not use a limit on suggestion(.)!
        program.append("linkedEducationOrg = fn:textAnnotation()" +
                "[dc:type is dbpedia-ont:Organisation]/fn:suggestion()" +
                "[fise:entity-type is dbpedia-ont:EducationalInstitution]/fise:entity-reference :: xsd:anyURI;");
        Program<RDFTerm> personProgram = ldpath.parseProgram(new StringReader(program.toString()));
        log.info("- - - - - - - - - - - - -");
        log.info("Places Indexing Examples");
        Map<String,Collection<?>> result = execute(personProgram);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        logResults(result);
    }    
    /**
     * This provides some example on how to select persons extracted from
     * a contentItem
     * @throws LDPathParseException
     */
    @Test
    public void exampleExtractedConcepts() throws LDPathParseException {
        StringBuilder program = new StringBuilder();
        program.append("conceptNames = fn:entityAnnotation()" +
                "[fise:entity-type is skos:Concept]/fise:entity-label :: xsd:anyURI;");
        //this uses the labels of suggested person with the highest confidence
        //but also the selected-text as fallback if no entity is suggested.
        program.append("linkedConcepts = fn:entityAnnotation()" +
                "[fise:entity-type is skos:Concept]/fise:entity-reference :: xsd:anyURI;");
        Program<RDFTerm> personProgram = ldpath.parseProgram(new StringReader(program.toString()));
        log.info("- - - - - - - - - - - - -");
        log.info("Concept Indexing Examples");
        Map<String,Collection<?>> result = execute(personProgram);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        logResults(result);
    }
    
    protected static void logResults(Map<String,Collection<?>> result){
        for(Entry<String,Collection<?>> field : result.entrySet()){
            log.info("Field {}: {} values",field.getKey(),field.getValue().size());
            for(Object value : field.getValue()){
                log.info("    {} (type: '{}')",value,value.getClass().getSimpleName());
            }
        }
    }
    @AfterClass
    public static void printDuration(){
        log.info("- - - - - - - - - - - - - - - - - - - - - - - - - ");
        log.info("Indexing Time: {}ms (average over {} iterations)",indexingTime,Math.max(1, ITERATIONS));
    }
}
