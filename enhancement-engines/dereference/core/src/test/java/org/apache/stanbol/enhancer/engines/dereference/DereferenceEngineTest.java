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
package org.apache.stanbol.enhancer.engines.dereference;

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.ENTITY_REFERENCES;
import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.FILTER_ACCEPT_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.FILTER_CONTENT_LANGUAGES;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.SKOS_CONCEPT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: convert this to an integration test!
 * @author Rupert Westenthaler
 */
public class DereferenceEngineTest {
    
    private final static Logger log = LoggerFactory.getLogger(DereferenceEngineTest.class);

    //TODO: test implementations of EntityDereferencer
    static EntityDereferencer asyncDereferencer = new TestDereferencer(Executors.newFixedThreadPool(4));
    static EntityDereferencer syncDereferencer = new TestDereferencer(null);

    /**
     * The metadata used by this test
     */
    private static TripleCollection testData;
    
    private static TripleCollection testMetadata;
    
    public static final UriRef NAME = new UriRef(NamespaceEnum.rdfs+"label");
    public static final UriRef TYPE = new UriRef(NamespaceEnum.rdf+"type");
    public static final UriRef REDIRECT = new UriRef(NamespaceEnum.rdfs+"seeAlso");
    
    public static final UriRef OTHER_ENTITY_REFERENCE = new UriRef(
        "http://www.example.org/stanbol/enhancer/dereference/test#other-entity-reference");

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    private static final LiteralFactory lf = LiteralFactory.getInstance();
    private static final UriRef SKOS_NOTATION = new UriRef(NamespaceEnum.skos+"notation");
    private static final Language LANG_EN = new Language("en");
    private static final Language LANG_DE = new Language("de");

    private static final int NUM_ENTITIES = 1000;
    
    public static final float PERCENTAGE_LINKED = 0.3f;
    public static final float PERCENTAGE_LINKED_OTHER = 0.2f;
    public static final float PERCENTAGE_PRESENT = 0.9f;
    
    @BeforeClass
    public static void setUpServices() throws IOException {
        testData = new IndexedMGraph();
        long seed = System.currentTimeMillis();
        log.info("Test seed "+ seed);
        Random random = new Random(seed);
        int numEntities = 0;
        for(int i = 0; i < NUM_ENTITIES ; i++){
            if(random.nextFloat() <= PERCENTAGE_PRESENT){ //do not create all entities
                UriRef uri = new UriRef("urn:test:entity"+i);
                testData.add(new TripleImpl(uri, RDF_TYPE, SKOS_CONCEPT));
                testData.add(new TripleImpl(uri, RDFS_LABEL, 
                    new PlainLiteralImpl("entity "+i, LANG_EN)));
                testData.add(new TripleImpl(uri, RDFS_LABEL, 
                    new PlainLiteralImpl("Entity "+i, LANG_DE)));
                testData.add(new TripleImpl(uri, SKOS_NOTATION, 
                    lf.createTypedLiteral(i)));
                numEntities++;
            }
        }
        log.info(" ... created {} Entities",numEntities);
        testMetadata = new IndexedMGraph();
        int numLinks = 0;
        int numOtherLinks = 0;
        for(int i = 0; i < NUM_ENTITIES ; i++){
            float r = random.nextFloat();
            if(r < PERCENTAGE_LINKED){
                UriRef enhancementUri = new UriRef("urn:test:enhancement"+i);
                UriRef entityUri = new UriRef("urn:test:entity"+i);
                //we do not need any other triple for testing in the contentItem
                testMetadata.add(new TripleImpl(enhancementUri, ENHANCER_ENTITY_REFERENCE, entityUri));
                numLinks++;
            } else if((r-PERCENTAGE_LINKED) < PERCENTAGE_LINKED_OTHER){
                UriRef enhancementUri = new UriRef("urn:test:enhancement"+i);
                UriRef entityUri = new UriRef("urn:test:entity"+i);
                //we do not need any other triple for testing in the contentItem
                testMetadata.add(new TripleImpl(enhancementUri, OTHER_ENTITY_REFERENCE, entityUri));
                numOtherLinks++;
            }
        }
        log.info("> created {} Entity references and {} references using an alternative proeprty ", 
            numLinks, numOtherLinks);

    }

    public static ContentItem getContentItem(final String id) throws IOException {
        ContentItem ci = ciFactory.createContentItem(new UriRef(id), new StringSource("Not used"));
        ci.getMetadata().addAll(testMetadata);
        return ci;
    }
    /**
     * Test {@link OfflineMode} functionality
     * @throws Exception
     */
    @Test
    public void testOfflineMode() throws Exception {
        ContentItem ci = getContentItem("urn:test:testOfflineMode");
        EntityDereferencer onlineDereferencer = new TestDereferencer(null){
          @Override
            public boolean supportsOfflineMode() {
                return false;
            }  
        };
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(EnhancementEngine.PROPERTY_NAME, "online");
        dict.put(FILTER_CONTENT_LANGUAGES, false);
        dict.put(FILTER_ACCEPT_LANGUAGES, false);
        EntityDereferenceEngine engine = new EntityDereferenceEngine(onlineDereferencer,
            new DereferenceEngineConfig(dict, null));
        //engine in online mode
        Assert.assertNotEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
        //set engine in offline mode
        engine.setOfflineMode(true);
        Assert.assertEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
    }

    @Test
    public void testSyncDereferencing() throws Exception {
        ContentItem ci = getContentItem("urn:test:testSyncDereferencing");
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(EnhancementEngine.PROPERTY_NAME, "sync");
        dict.put(FILTER_CONTENT_LANGUAGES, false);
        dict.put(FILTER_ACCEPT_LANGUAGES, false);
        EntityDereferenceEngine engine = new EntityDereferenceEngine(syncDereferencer,
            new DereferenceEngineConfig(dict, null));
        Assert.assertNotEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        validateDereferencedEntities(ci.getMetadata(), ENHANCER_ENTITY_REFERENCE);
    }

    @Test
    public void testAsyncDereferencing() throws Exception {
        ContentItem ci = getContentItem("urn:test:testSyncDereferencing");
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(EnhancementEngine.PROPERTY_NAME, "async");
        dict.put(FILTER_CONTENT_LANGUAGES, false);
        dict.put(FILTER_ACCEPT_LANGUAGES, false);
        EntityDereferenceEngine engine = new EntityDereferenceEngine(asyncDereferencer,
            new DereferenceEngineConfig(dict,null));
        Assert.assertNotEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        validateDereferencedEntities(ci.getMetadata(), ENHANCER_ENTITY_REFERENCE);
    }

    /**
     * Test for <a href="https://issues.apache.org/jira/browse/STANBOL-1334">STANBOL-1334</a>
     * @throws Exception
     */
    @Test
    public void testAsyncOtherEntityReferenceDereferencing() throws Exception {
        ContentItem ci = getContentItem("urn:test:testSyncDereferencing");
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(EnhancementEngine.PROPERTY_NAME, "async");
        dict.put(FILTER_CONTENT_LANGUAGES, false);
        dict.put(FILTER_ACCEPT_LANGUAGES, false);
        dict.put(ENTITY_REFERENCES,OTHER_ENTITY_REFERENCE.getUnicodeString());
        DereferenceEngineConfig config = new DereferenceEngineConfig(dict,null);
        EntityDereferenceEngine engine = new EntityDereferenceEngine(asyncDereferencer, config);
        Assert.assertNotEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        validateDereferencedEntities(ci.getMetadata(), OTHER_ENTITY_REFERENCE);
    }
    
    /**
     * Test for <a href="https://issues.apache.org/jira/browse/STANBOL-1334">STANBOL-1334</a>
     * @throws Exception
     */
    @Test
    public void testAsyncMultipleEntityReferenceDereferencing() throws Exception {
        ContentItem ci = getContentItem("urn:test:testSyncDereferencing");
        Dictionary<String,Object> dict = new Hashtable<String,Object>();
        dict.put(EnhancementEngine.PROPERTY_NAME, "async");
        dict.put(FILTER_CONTENT_LANGUAGES, false);
        dict.put(FILTER_ACCEPT_LANGUAGES, false);
        dict.put(ENTITY_REFERENCES, new String[]{
                OTHER_ENTITY_REFERENCE.getUnicodeString(), 
                ENHANCER_ENTITY_REFERENCE.getUnicodeString()});
        DereferenceEngineConfig config = new DereferenceEngineConfig(dict,null);
        EntityDereferenceEngine engine = new EntityDereferenceEngine(asyncDereferencer, config);
        Assert.assertNotEquals(engine.canEnhance(ci), EnhancementEngine.CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        validateDereferencedEntities(ci.getMetadata(), OTHER_ENTITY_REFERENCE, ENHANCER_ENTITY_REFERENCE);
    }
    
    private void validateDereferencedEntities(TripleCollection metadata, UriRef...entityReferenceFields) {
        MGraph expected = new IndexedMGraph();
        for(UriRef entityReferenceField : entityReferenceFields){
            Iterator<Triple> referenced = metadata.filter(null, entityReferenceField, null);
            while(referenced.hasNext()){
                UriRef entity = (UriRef)referenced.next().getObject();
                Iterator<Triple> entityTriples = testData.filter(entity, null, null);
                while(entityTriples.hasNext()){
                    expected.add(entityTriples.next());
                }
            }
        }
        MGraph notExpected = new IndexedMGraph(testData);
        notExpected.removeAll(expected);
        Assert.assertTrue(metadata.containsAll(expected));
        Assert.assertTrue(Collections.disjoint(metadata, notExpected));
    }

    private static class TestDereferencer implements EntityDereferencer {

        private final ExecutorService executorService;
        
        public TestDereferencer(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public boolean supportsOfflineMode() {
            return true;
        }

        @Override
        public ExecutorService getExecutor() {
            return executorService;
        }

        @Override
        public boolean dereference(UriRef entity, MGraph graph, Lock writeLock, DereferenceContext context) throws DereferenceException {
            Iterator<Triple> entityTriples = testData.filter(entity, null, null);
            if(entityTriples.hasNext()){
                writeLock.lock();
                try {
                    do {
                        graph.add(entityTriples.next());
                    } while (entityTriples.hasNext());
                } finally {
                    writeLock.unlock();
                }
                return true;
            } else {
                return false;
            }
        }
        
    }
    
}
