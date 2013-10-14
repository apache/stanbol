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
package org.apache.stanbol.entityhub.yard.sesame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;

import org.apache.stanbol.entityhub.model.sesame.RdfRepresentation;
import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.sesame.SesameYard;
import org.apache.stanbol.entityhub.yard.sesame.SesameYardConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Unit tests for testing {@link SesameYard} that do use contexts
 * 
 * @author Rupert Westenthaler
 *
 */
public class SesameContextTest {
    
    private static Repository repo = new SailRepository(new MemoryStore());
    private static ValueFactory sesameFactory = repo.getValueFactory();
    private static String EN = "en";
    private static String DE = "de";
    private static final Map<URI,List<? extends Yard>> expectedEntities = new HashMap<URI,List<? extends Yard>>();
    
    private static URI rdfType = sesameFactory.createURI(NamespaceEnum.rdf+"type");
    private static URI skosConcept = sesameFactory.createURI(NamespaceEnum.skos+"Concept");
    private static URI skosPrefLabel = sesameFactory.createURI(NamespaceEnum.skos+"preLabel");

    private static URI CONTEXT1 = sesameFactory.createURI("http://www.test.org/contex1");
    private static URI CONTEXT2 = sesameFactory.createURI("http://www.test.org/contex2");
    
    private static SesameYard yard1;
    private static SesameYard yard2;
    private static SesameYard unionYard;
    private static List<SesameYard> yards;
    
    @BeforeClass
    public static final void initYard() throws RepositoryException{
        repo.initialize();
        //create the graphs in Clerezza
        
        //init the ClerezzaYards for the created Clerezza graphs
        SesameYardConfig yard1config = new SesameYardConfig("context 1 yard");
        yard1config.setName("Yard over context 1");
        yard1config.setContextEnabled(true);
        yard1config.setContexts(new String[]{CONTEXT1.stringValue()});
        yard1 = new SesameYard(repo,yard1config);

        SesameYardConfig yard2config = new SesameYardConfig("context 2 yard");
        yard2config.setName("Yard over context 2");
        yard2config.setContextEnabled(true);
        yard2config.setContexts(new String[]{CONTEXT2.stringValue()});
        yard2 = new SesameYard(repo,yard2config);

        SesameYardConfig unionYardConfig = new SesameYardConfig("union yard");
        unionYardConfig.setName("Union Yard");
        unionYard = new SesameYard(repo, unionYardConfig);
        
        yards = Arrays.asList(yard1,yard2,unionYard);
        
        //add the test data (to the Repository to also test pre-existing data)
        RepositoryConnection con = repo.getConnection();
        con.begin();
        URI entity1 = sesameFactory.createURI("http://www.test.org/entity1");
        con.add(entity1,rdfType,skosConcept,CONTEXT1);
        con.add(entity1,skosPrefLabel,sesameFactory.createLiteral("test context one", EN),CONTEXT1);
        con.add(entity1,skosPrefLabel,sesameFactory.createLiteral("Test Context Eins", DE),CONTEXT1);
        expectedEntities.put(entity1, Arrays.asList(yard1,unionYard));
        
        URI entity2 = sesameFactory.createURI("http://www.test.org/entity2");
        con.add(entity2,rdfType,skosConcept,CONTEXT2);
        con.add(entity2,skosPrefLabel,sesameFactory.createLiteral("test context two", EN),CONTEXT2);
        con.add(entity2,skosPrefLabel,sesameFactory.createLiteral("Test Context Zwei", DE),CONTEXT2);
        expectedEntities.put(entity2, Arrays.asList(yard2,unionYard));
        con.commit();
        con.close();
    }
    /**
     * Checks the expected visibility of Entities to the different yards
     * @throws YardException 
     */
    @Test
    public void testRetrival() throws YardException{
        for(Entry<URI,List<? extends Yard>> entry : expectedEntities.entrySet()){
            for(Yard yard : yards){
                if(entry.getValue().contains(yard)){
                    validateEntity(yard, entry.getKey());
                } else {
                    Assert.assertFalse("Entity "+entry.getKey() 
                        + " is not expected in Yard " + yard.getName() + "!",
                        yard.isRepresentation(entry.getKey().stringValue()));
                }
            }
        }
    }
    /**
     * Test visibility of Entities added to specific contexts
     * @throws YardException
     */
    @Test
    public void testStoreToContextEnabledYard() throws YardException{
        //add a new entity to yard 2
        String context2added = "http://www.test.org/addedEntity";
        Representation rep = RdfValueFactory.getInstance().createRepresentation(
            context2added);
        rep.addReference(rdfType.stringValue(), skosConcept.stringValue());
        rep.addNaturalText(skosPrefLabel.stringValue(), "added Entity", "en");
        rep.addNaturalText(skosPrefLabel.stringValue(), "hinzugefüte Entity", "de");
        yard2.store(rep);
        //test visibility to other yards
        Assert.assertFalse(yard1.isRepresentation(context2added));
        Assert.assertTrue(yard2.isRepresentation(context2added));
        Assert.assertTrue(unionYard.isRepresentation(context2added));
        //remove it and test again
        yard2.remove(context2added);
        Assert.assertFalse(yard1.isRepresentation(context2added));
        Assert.assertFalse(yard2.isRepresentation(context2added));
        Assert.assertFalse(unionYard.isRepresentation(context2added));
    }
    
    /**
     * Used by {@link #testRetrival()} to validate that an Entity is correctly
     * retrieved by the tested {@link SesameYard}s.
     * @param entity key - URI; value - expected RDF data
     * @throws YardException 
     */
    private void validateEntity(Yard yard, URI subject) throws YardException {
        Representation rep = yard.getRepresentation(subject.stringValue());
        assertNotNull("The Representation for "+subject
            + "is missing in the "+yard.getId(), rep);
        assertTrue("RdfRepresentation expected", rep instanceof RdfRepresentation);
        //check the RDF type to validate that some data are present
        assertEquals(skosConcept.stringValue(), rep.getFirstReference(rdfType.stringValue()).getReference());
    }


    @AfterClass
    public static void cleanup() throws RepositoryException{
        for(SesameYard yard : yards){
            yard.close();
        }
        repo.shutDown();
    }
}
