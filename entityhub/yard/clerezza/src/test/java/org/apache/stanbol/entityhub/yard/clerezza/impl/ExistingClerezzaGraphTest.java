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
package org.apache.stanbol.entityhub.yard.clerezza.impl;

import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SKOS;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for testing {@link ClerezzaYard} initialisation and usage in 
 * cases the configured {@link ClerezzaYardConfig#getGraphUri()} points to
 * already existing Clerezza {@link Graph}s and {@link ImmutableGraph} instances.<p>
 * This basically tests features added with STANBOL-662 and STANBOL-663
 * @author Rupert Westenthaler
 *
 */
public class ExistingClerezzaGraphTest {
    
    private static TcManager tcManager;
    private static Language EN = new Language("en");
    private static Language DE = new Language("de");
    private static final Map<IRI,Graph> entityData = new HashMap<IRI,Graph>();
    
    private static IRI READ_ONLY_GRAPH_URI = new IRI("http://www.test.org/read-only-grpah");
    private static IRI READ_WRITEGRAPH_URI = new IRI("http://www.test.org/read-write-grpah");
    
    private static ClerezzaYard readwriteYard;
    private static ClerezzaYard readonlyYard;
    
    @BeforeClass
    public static final void initYard(){
        initTestData();
        //create the graphs in Clerezza
        tcManager = TcManager.getInstance();
        Graph graph = tcManager.createGraph(READ_WRITEGRAPH_URI);
        //add the test data to the MGrpah
        for(Graph tc :entityData.values()){ 
            graph.addAll(tc);
        }
        //create the read only graph
        tcManager.createImmutableGraph(READ_ONLY_GRAPH_URI, graph);
        
        //init the ClerezzaYards for the created Clerezza graphs
        ClerezzaYardConfig readWriteConfig = new ClerezzaYardConfig("readWriteYardId");
        readWriteConfig.setName("Clerezza read/write Yard");
        readWriteConfig.setDescription("Tests config with pre-existing Graph");
        readWriteConfig.setGraphUri(READ_WRITEGRAPH_URI);
        readwriteYard = new ClerezzaYard(readWriteConfig);

        ClerezzaYardConfig readOnlyYardConfig = new ClerezzaYardConfig("readOnlyYardId");
        readOnlyYardConfig.setName("Clerezza read-only Yard");
        readOnlyYardConfig.setDescription("Tests config with pre-existing ImmutableGraph");
        readOnlyYardConfig.setGraphUri(READ_ONLY_GRAPH_URI);
        readonlyYard = new ClerezzaYard(readOnlyYardConfig);

    }
    /**
     * Checks that the {@link #entityData} are correctly accessible via the
     * ClerezzaYard initialised over {@link #READ_ONLY_GRAPH_URI} and
     * {@link #READ_WRITEGRAPH_URI}.
     */
    @Test
    public void testRetrival(){
        for(Entry<IRI,Graph> entity : entityData.entrySet()){
            validateEntity(readonlyYard,entity);
            validateEntity(readwriteYard,entity);
        }
    }
    
    @Test
    public void testModificationsOnReadWirteYard() throws YardException{
        RdfRepresentation rep = RdfValueFactory.getInstance().createRepresentation(
            "http://www.test.org/addedEntity");
        rep.addReference(RDF.type.getUnicodeString(), SKOS.Concept.getUnicodeString());
        rep.addNaturalText(SKOS.prefLabel.getUnicodeString(), "added Entity", "en");
        rep.addNaturalText(SKOS.prefLabel.getUnicodeString(), "hinzugefüte Entity", "de");
        
        readwriteYard.store(rep);
        //test that the data where added and modified in the read/wirte grpah
        validateEntity(readwriteYard,
            singletonMap(rep.getNode(), rep.getRdfGraph()).entrySet().iterator().next());
        readwriteYard.remove(rep.getId());
        Assert.assertNull("Representation "+rep.getId()+" was not correctly "
            + "deleted from "+readwriteYard.getId(),
            readwriteYard.getRepresentation(rep.getId()));
    }
    @Test(expected=YardException.class)
    public void testStoreOnReadOnlyYard() throws YardException{
        RdfRepresentation rep = RdfValueFactory.getInstance().createRepresentation(
            "http://www.test.org/addedEntity");
        rep.addReference(RDF.type.getUnicodeString(), SKOS.Concept.getUnicodeString());
        rep.addNaturalText(SKOS.prefLabel.getUnicodeString(), "added Entity", "en");
        rep.addNaturalText(SKOS.prefLabel.getUnicodeString(), "hinzugefüte Entity", "de");
        readonlyYard.store(rep);
    }
    @Test(expected=YardException.class)
    public void testRemovalOnReadOnlyYard() throws YardException{
        readonlyYard.remove(entityData.keySet().iterator().next().getUnicodeString());
    }
    
    /**
     * Used by {@link #testRetrival()} to validate that an Entity is correctly
     * retrieved by the tested {@link ClerezzaYard}s.
     * @param entity key - URI; value - expected RDF data
     */
    private void validateEntity(ClerezzaYard yard, Entry<IRI,Graph> entity) {
        Representation rep = yard.getRepresentation(entity.getKey().getUnicodeString());
        assertNotNull("The Representation for "+entity.getKey()
            + "is missing in the "+yard.getId(), rep);
        assertTrue("RdfRepresentation expected", rep instanceof RdfRepresentation);
        Graph repGraph = ((RdfRepresentation)rep).getRdfGraph();
        for(Iterator<Triple> triples = entity.getValue().iterator();triples.hasNext();){
            Triple triple = triples.next();
            assertTrue("Data of Representation "+entity.getKey()
                + "is missing the triple "+triple, repGraph.remove(triple));
        }
        assertTrue(repGraph.size()+" unexpected Triples are present in the "
            + "Representation of Entity "+entity.getKey(),repGraph.isEmpty());
    }
    
    
    /**
     * Initialises the {@link #entityData} used for this test (called in BeforeClass)
     */
    private static void initTestData() {
        IRI entity1 = new IRI("http://www.test.org/entity1");
        Graph entity1Data = new SimpleGraph();
        entity1Data.add(new TripleImpl(entity1,RDF.type, SKOS.Concept));
        entity1Data.add(new TripleImpl(entity1,SKOS.prefLabel, new PlainLiteralImpl("test", EN)));
        entity1Data.add(new TripleImpl(entity1,SKOS.prefLabel, new PlainLiteralImpl("Test", DE)));
        entityData.put(entity1, entity1Data);
        
        Graph entity2Data = new SimpleGraph();
        IRI entity2 = new IRI("http://www.test.org/entity2");
        entity2Data.add(new TripleImpl(entity2, RDF.type, SKOS.Concept));
        entity2Data.add(new TripleImpl(entity2,SKOS.prefLabel, new PlainLiteralImpl("sub-test", EN)));
        entity2Data.add(new TripleImpl(entity2,SKOS.prefLabel, new PlainLiteralImpl("Untertest", DE)));
        entity2Data.add(new TripleImpl(entity2,SKOS.broader, entity1));
        entityData.put(entity2, entity2Data);
    }

    @AfterClass
    public static void cleanup(){
        tcManager.deleteGraph(READ_ONLY_GRAPH_URI);
        tcManager.deleteGraph(READ_WRITEGRAPH_URI);
    }
}
