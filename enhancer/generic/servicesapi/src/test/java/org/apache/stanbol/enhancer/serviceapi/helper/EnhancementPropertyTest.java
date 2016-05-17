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
package org.apache.stanbol.enhancer.serviceapi.helper;

import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_NLP_LANGAUGE_DETECTION;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_NLP_POS;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_NLP_SENTENCE_DETECTION;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_NLP_TOKENIZING;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_POST_PROCESSING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ContentItemImpl;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * UnitTests for EnhancementProperties 
 * @author Rupert Westenthaler
 *
 */
public class EnhancementPropertyTest {

    private static final class TestEnhancementEngine implements EnhancementEngine, ServiceProperties {

        private String name;
        private Map<String,Object> props;

        TestEnhancementEngine(String name, Integer order) {
            this(name, Collections.<String,Object>singletonMap(
                ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, order));
        }

        TestEnhancementEngine(String name, Map<String,Object> props) {
            this.name = name;
            this.props = props;
        }
        
        @Override
        public int canEnhance(ContentItem ci) throws EngineException {
            return 0;
        }
    
        @Override
        public void computeEnhancements(ContentItem ci) throws EngineException {
        }
    
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public Map<String,Object> getServiceProperties() {
            return props;
        }
        
    };

    private static class TestContentItem extends ContentItemImpl {

        protected TestContentItem(String uri, String content) {
            super(new IRI(uri), new TestBlob(content), new IndexedGraph());
        }
        
    }
    
    private static class TestBlob implements Blob {

        private static final Charset UTF8 = Charset.forName("UTF8");
        private final byte[] data;

        TestBlob(String text){
            this.data = text.getBytes(UTF8);
        }
        
        @Override
        public String getMimeType() {
            return "text/plain";
        }

        @Override
        public InputStream getStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public Map<String,String> getParameter() {
            return Collections.singletonMap("charset", UTF8.name());
        }

        @Override
        public long getContentLength() {
            return data.length;
        }
        
    }
    /**
     * Simple Chain implementation that support chain scope enhancement properties
     * as used for some of the tests.
     */
    private static class TestChain implements Chain {

        private String name;
        private List<EnhancementEngine> engines;
        private Map<String,Map<String,Object>> chainProperties;

        TestChain(String name, List<EnhancementEngine> engines){
            this(name,engines,null);
        }

        TestChain(String name, List<EnhancementEngine> engines,
            Map<String,Map<String,Object>> chainProperties){
            this.name = name;
            this.engines = engines;
            this.chainProperties = chainProperties;
        }
        
        @Override
        public ImmutableGraph getExecutionPlan() throws ChainException {
            return ExecutionPlanHelper.calculateExecutionPlan(name, engines, 
                Collections.<String>emptySet(), Collections.<String>emptySet(),
                chainProperties);
        }

        @Override
        public Set<String> getEngines() throws ChainException {
            Set<String> names = new HashSet<String>(engines.size());
            for(EnhancementEngine engine : engines){
                names.add(engine.getName());
            }
            return names;
        }

        @Override
        public String getName() {
            return name;
        }
        
    }
    
    private static final String PROPERTY_MAX_SUGGESTIONS = "enhancer.maxSuggestions";
    private static final String PROPERTY_DEREFERENCE_LANGUAGES = "engine.dereference.language";
    
    // Some EnhancementEngines we will use in the test chains
    private static final EnhancementEngine langdetect = new TestEnhancementEngine(
        "langdetect",  ORDERING_NLP_LANGAUGE_DETECTION);
    private static final EnhancementEngine sentence = new TestEnhancementEngine(
        "sentence",  ORDERING_NLP_SENTENCE_DETECTION);
    private static final EnhancementEngine token = new TestEnhancementEngine(
        "token",ORDERING_NLP_TOKENIZING);
    private static final EnhancementEngine pos = new TestEnhancementEngine(
        "pos", ORDERING_NLP_POS);
    private static final EnhancementEngine linking = new TestEnhancementEngine(
        "linking", ORDERING_EXTRACTION_ENHANCEMENT);
    private static final EnhancementEngine dereference = new TestEnhancementEngine(
        "dereference", ORDERING_POST_PROCESSING);
    private static final List<EnhancementEngine> engines = Arrays.asList(
        langdetect, sentence, token, pos, linking, dereference);
    // The chain we will use for the tests
    private ContentItem contentItem;

    /**
     * creates the content item used by the test
     * @throws ChainException
     */
    @Before
    public void initContentItem() throws ChainException {
        contentItem = new TestContentItem("urn:apache.stanbol:enhancer:test1","This is a test");
        
    }
    /**
     * Initialises the ExecutionMetadata based on the chain used by the test.
     * This is typically done by the {@link EnhancementJobManager}, but as we do
     * not use one for the tests we need to do this part manually
     * @param chain
     * @throws ChainException
     */
    protected void initExecutionMetadata(Chain chain) throws ChainException {
        //init the ExecutionMetadata ... this is normally done by the EnhancementJobManager
        Graph em = ExecutionMetadataHelper.initExecutionMetadataContentPart(contentItem);
        ImmutableGraph ep = chain.getExecutionPlan();
        em.addAll(ep);
        ExecutionMetadataHelper.initExecutionMetadata(em, ep, 
            contentItem.getUri(), chain.getName(), false);
    }


    @Test
    public void testSimpleProperty() throws ChainException {
        initExecutionMetadata(new TestChain("test", engines));
        
        //(1) test a a enhancement property overridden with a engine specific one
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        assertNotNull("EnhancementProperties ContentPart was not initialised",ep);
        ep.put(PROPERTY_MAX_SUGGESTIONS, "5"); //global property

        //we expect enhancer.maxSuggestions=5 for the langdetect engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
            Assert.assertEquals("5", props.get(PROPERTY_MAX_SUGGESTIONS));
        }
    }
    @Test
    public void testMultiValueProperty() throws ChainException {
        initExecutionMetadata(new TestChain("test", engines));

        Collection<String> derefernceLanguages = Arrays.asList("en","de");
        //(1) test a a enhancement property overridden with a engine specific one
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        assertNotNull("EnhancementProperties ContentPart was not initialised",ep);
        ep.put(PROPERTY_DEREFERENCE_LANGUAGES, derefernceLanguages); //global property

        //we expect enhancer.maxSuggestions=5 for the langdetect engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_DEREFERENCE_LANGUAGES));
            Assert.assertEquals(derefernceLanguages, props.get(PROPERTY_DEREFERENCE_LANGUAGES));
        }
    }
    @Test
    public void testEngineSpecificProperties() throws ChainException {
        initExecutionMetadata(new TestChain("test", engines));

        Collection<String> derefernceLanguages = Arrays.asList("en","de");
        String maxSuggestions = "5";
        //(1) test a a enhancement property overridden with a engine specific one
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        assertNotNull("EnhancementProperties ContentPart was not initialised",ep);
        ep.put(linking.getName()+':'+PROPERTY_MAX_SUGGESTIONS, maxSuggestions);
        ep.put(dereference.getName()+':'+PROPERTY_DEREFERENCE_LANGUAGES, derefernceLanguages);
        
        //we expect enhancer.maxSuggestions=5 for the langdetect engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            if(engine.getName().equals(linking.getName()) ||engine.getName().equals(dereference.getName())){
                assertEquals(1, props.size());
            } else {
                assertTrue(props.isEmpty());
            }
            if(engine.getName().equals(linking.getName())){
                assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
                assertEquals(maxSuggestions, props.get(PROPERTY_MAX_SUGGESTIONS));
            } else if(engine.getName().equals(dereference.getName())){
                assertTrue(props.containsKey(PROPERTY_DEREFERENCE_LANGUAGES));
                Object value = props.get(PROPERTY_DEREFERENCE_LANGUAGES);
                assertTrue(value instanceof Collection<?>);
                assertTrue(derefernceLanguages.containsAll((Collection<?>)value));
                assertEquals(derefernceLanguages.size(), ((Collection<?>)value).size());
            } //else empty
        }
    }
    
    @Test
    public void testEngineSpecificPropertyOverrides() throws ChainException {
        initExecutionMetadata(new TestChain("test", engines));

        //(1) test a a enhancement property overridden with a engine specific one
        String specific = linking.getName();
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        assertNotNull("EnhancementProperties ContentPart was not initialised",ep);
        ep.put(PROPERTY_MAX_SUGGESTIONS, "5"); //global property
        ep.put(specific+':'+PROPERTY_MAX_SUGGESTIONS, "10");//engine specific
        
        //we expect enhancer.maxSuggestions=5 for the langdetect engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
            if(engine.getName().equals(specific)){
                Assert.assertEquals("10", props.get(PROPERTY_MAX_SUGGESTIONS));
            } else {
                Assert.assertEquals("5", props.get(PROPERTY_MAX_SUGGESTIONS));
            }
        }
    }
    /**
     * This tests if the {@link ExecutionPlanHelper} correctly adds Enhancement
     * Properties to generated Execution plans. <p>
     * NOTE: If this fails also tests testing chain level properties are expected
     * to fail. This only present to validate that the ExecutionPlan is correctly
     * generated by the {@link ExecutionPlanHelper}
     * @throws ChainException
     */
    @Test
    public void testExecutionPropertySupportOfExecutionPlanHelper() throws ChainException {
        //the value we are setting
        Collection<String> derefernceLanguages = Arrays.asList("en","de");
        Integer maxSuggestions = Integer.valueOf(5);
        
        IRI maxSuggestionsProperty = new IRI(NamespaceEnum.ehp + PROPERTY_MAX_SUGGESTIONS);
        IRI dereferenceLanguagesProperty = new IRI(NamespaceEnum.ehp + PROPERTY_DEREFERENCE_LANGUAGES);

        //set up the map with the enhancement properties we want to set for the
        //Enhancement Chain
        Map<String,Map<String,Object>> enhancementProperties = new HashMap<String,Map<String,Object>>();
        Map<String,Object> chainProperties = new HashMap<String,Object>();
        chainProperties.put(PROPERTY_MAX_SUGGESTIONS, maxSuggestions);
        enhancementProperties.put(null, chainProperties);
        Map<String,Object> linkingProperties = new HashMap<String,Object>();
        linkingProperties.put(PROPERTY_DEREFERENCE_LANGUAGES, derefernceLanguages);
        enhancementProperties.put(linking.getName(), linkingProperties);
        
        //create the ExecutionPlan
        ImmutableGraph ep = ExecutionPlanHelper.calculateExecutionPlan("test", engines, 
            Collections.<String>emptySet(), Collections.<String>emptySet(), 
            enhancementProperties);
        
        //now assert that the enhancement properties where correctly written
        //first the property we set on the chain level
        BlankNodeOrIRI epNode = ExecutionPlanHelper.getExecutionPlan(ep, "test");
        assertNotNull(epNode);
        Iterator<Triple> maxSuggestionValues = ep.filter(epNode, maxSuggestionsProperty, null);
        assertTrue(maxSuggestionValues.hasNext());
        RDFTerm maxSuggestionValue = maxSuggestionValues.next().getObject();
        assertFalse(maxSuggestionValues.hasNext());
        assertTrue(maxSuggestionValue instanceof Literal);
        assertEquals(maxSuggestions.toString(), ((Literal)maxSuggestionValue).getLexicalForm());
        assertEquals(maxSuggestions, LiteralFactory.getInstance().createObject(
            Integer.class, (Literal)maxSuggestionValue));
        //second the property we set for the linking engine
        boolean found = false;
        for(BlankNodeOrIRI ee : ExecutionPlanHelper.getExecutionNodes(ep, epNode)){
            String engineName = ExecutionPlanHelper.getEngine(ep, ee);
            if(linking.getName().equals(engineName)){
                found = true;
                Iterator<Triple> derefLangValues = ep.filter(ee, dereferenceLanguagesProperty, null);
                assertTrue(derefLangValues.hasNext());
                int numValues = 0;
                while(derefLangValues.hasNext()){
                    RDFTerm r = derefLangValues.next().getObject();
                    assertTrue(r instanceof Literal);
                    assertTrue(derefernceLanguages.contains(((Literal)r).getLexicalForm()));
                    numValues++;
                }
                assertEquals(derefernceLanguages.size(), numValues);
            }
        }
        assertTrue("ExecutionNode for the Linking Engine was not present!",found);
        //NOTE: this does not validate that there are no other (not expected)
        //      enhancement properties in the executionPlan
    }

    @Test
    public void testSimpleChainScopedProperty() throws ChainException {
        //set up the map with the enhancement properties we want to set for the
        //Enhancement Chain
        Map<String,Map<String,Object>> enhancementProperties = new HashMap<String,Map<String,Object>>();
        //set enhancer.maxSuggestions=5 as chain property (applies for all engines)
        Map<String,Object> chainProperties = new HashMap<String,Object>();
        chainProperties.put(PROPERTY_MAX_SUGGESTIONS, Integer.valueOf(5));
        enhancementProperties.put(null, chainProperties);
        initExecutionMetadata(new TestChain("test", engines, enhancementProperties));
        
        //we expect enhancer.maxSuggestions=5 for the all engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
            Assert.assertEquals("5", props.get(PROPERTY_MAX_SUGGESTIONS));
        }
    }
    
    @Test
    public void testEngineSpecificChainScopedProperty() throws ChainException {
        Collection<String> derefernceLanguages = Arrays.asList("en","de");
        String maxSuggestions = "5";
        //set up the map with the enhancement properties we want to set for the
        //Enhancement Chain
        Map<String,Map<String,Object>> enhancementProperties = new HashMap<String,Map<String,Object>>();
        //set enhancer.maxSuggestions=5 as chain property (applies for all engines)
        Map<String,Object> linkingProperties = new HashMap<String,Object>();
        linkingProperties.put(PROPERTY_MAX_SUGGESTIONS, Integer.valueOf(maxSuggestions));
        enhancementProperties.put(linking.getName(), linkingProperties);
        Map<String,Object> dereferenceProperties = new HashMap<String,Object>();
        dereferenceProperties.put(PROPERTY_DEREFERENCE_LANGUAGES, derefernceLanguages);
        enhancementProperties.put(dereference.getName(), dereferenceProperties);
        
        initExecutionMetadata(new TestChain("test", engines, enhancementProperties));
        
        //we expect enhancer.maxSuggestions=5 for the langdetect engine
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            if(engine.getName().equals(linking.getName()) ||engine.getName().equals(dereference.getName())){
                assertEquals(1, props.size());
            } else {
                assertTrue(props.isEmpty());
            }
            if(engine.getName().equals(linking.getName())){
                assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
                assertEquals(maxSuggestions, props.get(PROPERTY_MAX_SUGGESTIONS));
            } else if(engine.getName().equals(dereference.getName())){
                assertTrue(props.containsKey(PROPERTY_DEREFERENCE_LANGUAGES));
                Object value = props.get(PROPERTY_DEREFERENCE_LANGUAGES);
                assertTrue(value instanceof Collection<?>);
                assertTrue(derefernceLanguages.containsAll((Collection<?>)value));
                assertEquals(derefernceLanguages.size(), ((Collection<?>)value).size());
            } //else empty
        }
    }
    /**
     * This tests that chain scoped chain properties are overridden by 
     * chain scoped engine specific properties
     * @throws ChainException
     */
    @Test
    public void testChainScopedEngineSpecificPropertyOverrides() throws ChainException {
        //set enhancer.maxSuggestions=5 as chain property (applies for all engines)
        //and enhancer.maxSuggestions=10 for the linking engine
        Map<String,Map<String,Object>> enhancementProperties = new HashMap<String,Map<String,Object>>();
        Map<String,Object> chainProperties = new HashMap<String,Object>();
        chainProperties.put(PROPERTY_MAX_SUGGESTIONS, Integer.valueOf(5));
        enhancementProperties.put(null, chainProperties);
        Map<String,Object> linkingProperties = new HashMap<String,Object>();
        linkingProperties.put(PROPERTY_MAX_SUGGESTIONS, Integer.valueOf(10));
        enhancementProperties.put(linking.getName(), linkingProperties);
        
        initExecutionMetadata(new TestChain("test", engines, enhancementProperties));
        
        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
            if(engine.getName().equals(linking.getName())){
                Assert.assertEquals("10", props.get(PROPERTY_MAX_SUGGESTIONS));
            } else {
                Assert.assertEquals("5", props.get(PROPERTY_MAX_SUGGESTIONS));
            }
        }
    }
    
    @Test
    public void testRequestScopedPropertiesOverrideChainScopedProperties() throws ChainException {
        //This tests two cases:
        // 1. enhancer.maxSuggestions=5 set as chain scoped chain property overridden
        //    by enhancer.maxSuggestions=10 set as request scoped property for the
        //    linking engine
        Integer chainMaxSuggestion = 5;
        String requestMaySuggestion = "10";
        // 2. engine.dereference.language=[en,de] set as chain scoped dereference engine
        //    specific property is overridden by engine.dereference.language=[it,fr]
        //    set as request scoped property for the same engine
        Collection<String> chainDerefLang = Arrays.asList("en","de");
        Collection<String> requestDerefLang = Arrays.asList("it","fr");
        //set enhancer.maxSuggestions=5 as chain property (applies for all engines)
        //and enhancer.maxSuggestions=10 for the linking engine
        Map<String,Map<String,Object>> enhancementProperties = new HashMap<String,Map<String,Object>>();
        Map<String,Object> chainProperties = new HashMap<String,Object>();
        chainProperties.put(PROPERTY_MAX_SUGGESTIONS, chainMaxSuggestion);
        Map<String,Object> dereferenceProperties = new HashMap<String,Object>();
        dereferenceProperties.put(PROPERTY_DEREFERENCE_LANGUAGES, chainDerefLang);
        enhancementProperties.put(dereference.getName(), dereferenceProperties);
        enhancementProperties.put(null, chainProperties);

        initExecutionMetadata(new TestChain("test", engines, enhancementProperties));
        
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        assertNotNull("EnhancementProperties ContentPart was not initialised",ep);
        ep.put(linking.getName()+':'+PROPERTY_MAX_SUGGESTIONS, requestMaySuggestion);
        ep.put(dereference.getName()+':'+PROPERTY_DEREFERENCE_LANGUAGES, requestDerefLang);

        for(EnhancementEngine engine : engines){
            Map<String,Object> props = EnhancementEngineHelper.getEnhancementProperties(engine, contentItem);
            assertNotNull(props);
            assertEquals(engine.getName().equals(dereference.getName()) ? 2 : 1, props.size());
            Assert.assertTrue(props.containsKey(PROPERTY_MAX_SUGGESTIONS));
            if(engine.getName().equals(linking.getName())){
                Assert.assertEquals("10", props.get(PROPERTY_MAX_SUGGESTIONS));
            } else {
                Assert.assertEquals("5", props.get(PROPERTY_MAX_SUGGESTIONS));
            }
            if(engine.getName().equals(dereference.getName())){
                assertTrue(props.containsKey(PROPERTY_DEREFERENCE_LANGUAGES));
                Object value = props.get(PROPERTY_DEREFERENCE_LANGUAGES);
                assertTrue(value instanceof Collection<?>);
                assertTrue(requestDerefLang.containsAll((Collection<?>)value));
                assertEquals(requestDerefLang.size(), ((Collection<?>)value).size());

            }
        }

    }
    
}
