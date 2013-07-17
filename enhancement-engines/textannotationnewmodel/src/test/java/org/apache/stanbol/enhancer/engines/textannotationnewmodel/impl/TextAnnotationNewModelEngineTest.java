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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
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

import org.apache.clerezza.rdf.core.Resource;

public class TextAnnotationNewModelEngineTest {
    
    public static final String SINGLE_SENTENCE = "Dr Patrick Marshall (1869 - November 1950) was a"
            + " geologist who lived in New Zealand and worked at the University of Otago.";
    private static final String TEST_ENHANCEMENTS = "enhancement-results.rdf";
    
    private static final JenaParserProvider rdfParser = new JenaParserProvider();
    private static MGraph origEnhancements;
    private static UriRef ciUri;
    
    private ContentItem contentItem;
    
    private static TextAnnotationsNewModelEngine engine;
    
    private final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static ComponentContext ctx;
    
    @BeforeClass
    public static void init() throws IOException, ConfigurationException {
        InputStream in = TextAnnotationNewModelEngineTest.class.getClassLoader().getResourceAsStream(TEST_ENHANCEMENTS);
        Assert.assertNotNull("Unable to load reaource '"+TEST_ENHANCEMENTS+"' via Classpath",in);
        origEnhancements = new IndexedMGraph();
        rdfParser.parse(origEnhancements, in, SupportedFormat.RDF_XML, null);
        Assert.assertFalse(origEnhancements.isEmpty());
        //parse the ID of the ContentItem form the enhancements
        Iterator<Triple> it = origEnhancements.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
        Assert.assertTrue(it.hasNext());
        Resource id = it.next().getObject();
        Assert.assertTrue(id instanceof UriRef);
        ciUri = (UriRef)id;
        //validate that the enhancements in the file are valid
        EnhancementStructureHelper.validateAllTextAnnotations(
            origEnhancements, SINGLE_SENTENCE, null,
            false); //those do not yet contain fise:selection-prefix/suffix values

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
            new StringSource(SINGLE_SENTENCE), new IndexedMGraph(origEnhancements));
    }
    
    @Test
    public void testTextAnnotationNewModel() throws EngineException {
        Assert.assertEquals(EnhancementEngine.ENHANCE_ASYNC, engine.canEnhance(contentItem));
        engine.computeEnhancements(contentItem);
        //validate
        MGraph g = contentItem.getMetadata();
        Iterator<Triple> it = g.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        Assert.assertTrue(it.hasNext());
        while(it.hasNext()){
            NonLiteral ta = it.next().getSubject();
            Assert.assertTrue(ta instanceof UriRef);
            Map<UriRef,Resource> expected = new HashMap<UriRef,Resource>();
            expected.put(Properties.ENHANCER_EXTRACTED_FROM, contentItem.getUri());
            EnhancementStructureHelper.validateTextAnnotation(g, (UriRef)ta, SINGLE_SENTENCE, expected,true);
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
