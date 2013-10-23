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
package org.apache.stanbol.commons.ldpath.clerezza;

import static junit.framework.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.parser.Configuration;

public class ClerezzaBackendTest {
    /**
     * Avoids that the parser closes the {@link ZipInputStream} after the
     * first entry
     */
    private static class UncloseableStream extends FilterInputStream {

        public UncloseableStream(InputStream in) {
            super(in);
        }
        @Override
        public void close() throws IOException {
        }
    }
    
    private Logger log = LoggerFactory.getLogger(ClerezzaBackendTest.class);

    private static final String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
    private static final String NS_DBP = "http://dbpedia.org/property/";
    private static final String NS_DBO = "http://dbpedia.org/ontology/";
    private static final UriRef SKOS_CONCEPT = new UriRef(NS_SKOS+"Concept");
    
    private static MGraph graph;
    
    private ClerezzaBackend backend;
    private LDPath<Resource> ldpath;
    @BeforeClass
    public static void readTestData() throws IOException {
        ParsingProvider parser = new JenaParserProvider();
        //NOTE(rw): the new third parameter is the base URI used to resolve relative paths
        graph = new IndexedMGraph();
        InputStream in = ClerezzaBackendTest.class.getClassLoader().getResourceAsStream("testdata.rdf.zip");
        assertNotNull(in);
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in));
        InputStream uncloseable = new UncloseableStream(zipIn);
        ZipEntry entry;
        while((entry = zipIn.getNextEntry()) != null){
            if(entry.getName().endsWith(".rdf")){
                parser.parse(graph,uncloseable, SupportedFormat.RDF_XML,null);
            }
        }
        assertTrue(graph.size() > 0);
        zipIn.close();
        
    }
    
    @Before
    public void initBackend(){
        if(backend == null){
            backend = new ClerezzaBackend(graph);
        }
        if(ldpath == null){
            Configuration<Resource> config = new Configuration<Resource>();
            config.addNamespace("dbp-prop", NS_DBP);
            config.addNamespace("dbp-ont", NS_DBO);
            ldpath = new LDPath<Resource>(backend);
        }
    }
    
    @Test
    public void testUriAndListImplemetnation() throws LDPathParseException {
        UriRef nationalChampionship = new UriRef("http://cv.iptc.org/newscodes/subjectcode/15073031");
        //this program tests:
        // * UriRef transformers
        // * #listSubjects(..) implementation
        // * #listObjects(..)  implementation
        Map<String,Collection<?>> results = ldpath.programQuery(nationalChampionship, 
            getReader("skos:broaderTransitive = (skos:broaderTransitive | ^skos:narrowerTransitive)+;"));
        Set<Resource> expected = new HashSet<Resource>(Arrays.asList(
            new UriRef("http://cv.iptc.org/newscodes/subjectcode/15000000"),
            new UriRef("http://cv.iptc.org/newscodes/subjectcode/15073000")));
        Collection<?> broaderTransitive = results.get(NS_SKOS+"broaderTransitive");
        for(Object concept : broaderTransitive){
            assertNotNull(concept);
            assertTrue(concept instanceof UriRef);
            assertTrue(expected.remove(concept));
        }
        assertTrue("missing: "+expected,expected.isEmpty());
    }
    @Test
    public void testStringTransformer() throws LDPathParseException {
        UriRef nationalChampionship = new UriRef("http://cv.iptc.org/newscodes/subjectcode/15073031");
        Map<String,Collection<?>> results = ldpath.programQuery(nationalChampionship, 
            getReader("label = skos:prefLabel[@en-GB] :: xsd:string;"));
        Set<String> expected = new HashSet<String>(Arrays.asList(
            "national championship 1st level"));
        Collection<?> broaderTransitive = results.get("label");
        for(Object concept : broaderTransitive){
            assertNotNull(concept);
            assertTrue(concept instanceof String);
            assertTrue(expected.remove(concept));
        }
        assertTrue(expected.isEmpty());
        
    }
    @Test
    public void testDataTypes() throws LDPathParseException {
        UriRef hallein = new UriRef("http://dbpedia.org/resource/Hallein");        

        StringBuilder program = new StringBuilder();
        program.append("@prefix dbp-prop : <").append(NS_DBP).append(">;");
        program.append("@prefix dbp-ont : <").append(NS_DBO).append(">;");
        program.append("doubleTest = dbp-ont:areaTotal :: xsd:double;"); //Double
        program.append("decimalTest = dbp-ont:areaTotal :: xsd:decimal;"); //BigDecimal
        program.append("intTest = dbp-prop:areaCode :: xsd:int;"); //Integer
        program.append("longTest = dbp-prop:population :: xsd:long;"); //Long
        program.append("uriTest = foaf:homepage :: xsd:anyURI;"); //xsd:anyUri
        
        Map<String,Object> expected = new HashMap<String,Object>();
        expected.put("doubleTest", new Double(2.698E7));
        expected.put("decimalTest", new BigDecimal("2.698E7"));
        expected.put("intTest", new Integer(6245));
        expected.put("longTest", new Long(19473L));
        expected.put("uriTest", "http://www.hallein.gv.at");
        
        Map<String,Collection<?>> results = ldpath.programQuery(hallein, 
            getReader(program.toString()));
        assertNotNull(results);
        for(Entry<String,Collection<?>> resultEntry : results.entrySet()){
            assertNotNull(resultEntry);
            Object expectedResult = expected.get(resultEntry.getKey());
            assertNotNull(resultEntry.getKey()+" is not an expected key",expectedResult);
            assertTrue(resultEntry.getValue().size() == 1);
            Object resultValue = resultEntry.getValue().iterator().next();
            assertNotNull(resultValue);
            assertTrue(expectedResult.getClass().isAssignableFrom(resultValue.getClass()));
            assertEquals(resultValue, expectedResult);
        }
    }
//    @Test
//    public void testTest(){
//        for(Iterator<Triple> it = graph.filter(null, RDF.type, SKOS_CONCEPT);it.hasNext();){
//            log.info("Concept: {}",it.next().getSubject());
//        }
//    }
    public static final Reader getReader(String string) {
        if(string == null){
            throw new IllegalArgumentException("The parsed string MUST NOT be NULL!");
        }
        try {
            return new InputStreamReader(new ByteArrayInputStream(string.getBytes("utf-8")), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Encoding 'utf-8' is not supported by this system!",e);
        }
    }

}
