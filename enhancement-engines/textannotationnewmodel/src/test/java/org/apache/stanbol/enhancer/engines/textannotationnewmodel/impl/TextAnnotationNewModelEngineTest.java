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
package org.apache.stanbol.enhancer.engines.textannotationnewmodel.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import org.apache.clerezza.commons.rdf.RDFTerm;

public class TextAnnotationNewModelEngineTest {
    
    public static final String SINGLE_SENTENCE = "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the University of Otago.";
    private static final String TEST_ENHANCEMENTS = "enhancement-results.rdf";
    
    private static final JenaParserProvider rdfParser = new JenaParserProvider();
    private static Graph origEnhancements;
    private static IRI ciUri;
    
    private ContentItem contentItem;
    
    private static TextAnnotationsNewModelEngine engine;
    
    private final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static ComponentContext ctx;
    
    @BeforeClass
    public static void init() throws IOException, ConfigurationException {
        InputStream in = TextAnnotationNewModelEngineTest.class.getClassLoader().getResourceAsStream(TEST_ENHANCEMENTS);
        Assert.assertNotNull("Unable to load reaource '"+TEST_ENHANCEMENTS+"' via Classpath",in);
        origEnhancements = new IndexedGraph();
        rdfParser.parse(origEnhancements, in, SupportedFormat.RDF_XML, null);
        Assert.assertFalse(origEnhancements.isEmpty());
        //parse the ID of the ContentItem form the enhancements
        Iterator<Triple> it = origEnhancements.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
        Assert.assertTrue(it.hasNext());
        RDFTerm id = it.next().getObject();
        Assert.assertTrue(id instanceof IRI);
        ciUri = (IRI)id;
        //validate that the enhancements in the file are valid
        //NOTE: the input data are no longer fully valid to test some features of this engine
        //      because of that this initial test is deactivated
//        EnhancementStructureHelper.validateAllTextAnnotations(
//            origEnhancements, SINGLE_SENTENCE, null,
//            false); //those do not yet contain fise:selection-prefix/suffix values

        //init the engine
        engine = new TextAnnotationsNewModelEngine();
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, "test-engine");
        config.put(TextAnnotationsNewModelEngine.PROPERTY_PREFIX_SUFFIX_SIZE, Integer.valueOf(10));
        ctx = new MockComponentContext(config);
        engine.activate(ctx);
    }
    
    @Before
    public void initTest() throws IOException {
        contentItem = ciFactory.createContentItem(ciUri, 
            new StringSource(SINGLE_SENTENCE), new IndexedGraph(origEnhancements));
    }
    
    @Test
    public void testTextAnnotationNewModel() throws EngineException {
        Assert.assertEquals(EnhancementEngine.ENHANCE_ASYNC, engine.canEnhance(contentItem));
        engine.computeEnhancements(contentItem);
        //validate
        Graph g = contentItem.getMetadata();
        Iterator<Triple> it = g.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        Assert.assertTrue(it.hasNext());
        while(it.hasNext()){
            BlankNodeOrIRI ta = it.next().getSubject();
            Assert.assertTrue(ta instanceof IRI);
            Map<IRI,RDFTerm> expected = new HashMap<IRI,RDFTerm>();
            expected.put(Properties.ENHANCER_EXTRACTED_FROM, contentItem.getUri());
            EnhancementStructureHelper.validateTextAnnotation(g, (IRI)ta, SINGLE_SENTENCE, expected,true);
        }
        
    }
    
    
    @After
    public void afterTest(){
        contentItem = null;
    }
    
    @AfterClass
    public static void cleanup(){
        engine.deactivate(ctx);
    }
}
