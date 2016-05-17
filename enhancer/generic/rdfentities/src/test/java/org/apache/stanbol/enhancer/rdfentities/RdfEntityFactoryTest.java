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
package org.apache.stanbol.enhancer.rdfentities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.enhancer.rdfentities.Rdf;
import org.apache.stanbol.enhancer.rdfentities.RdfEntity;
import org.apache.stanbol.enhancer.rdfentities.RdfEntityFactory;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Test;


/**
 * Tests the Factory, basic RdfEntity Methods and all features supported for
 * Interfaces.
 *
 * @author Rupert Westenthaler
 */
public class RdfEntityFactoryTest {

    @Test
    public void testRdfEntity() throws Exception {
        Graph graph = new SimpleGraph();
        RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
        String testUri = "urn:RdfEntityFactoryTest:TestEntity";
        IRI node = new IRI(testUri);
        RdfEntity rdfEntity = factory.getProxy(node, RdfEntity.class);
        //TODO: Test type statement
        //TODO: test getID Method
        assertEquals(rdfEntity.getId(), node);
        //TODO: Test equals
        RdfEntity rdfEntity2 = factory.getProxy(node,RdfEntity.class);
        assertEquals(rdfEntity, rdfEntity2);
        //TODO: Test hashCode
        assertEquals(rdfEntity.hashCode(), rdfEntity2.hashCode());
    }
    @Test
    public void testPrimitiveDataTypes() throws Exception {
        Graph graph = new SimpleGraph();
        RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
        String testUri = "urn:RdfEntityFactoryTest:TestEntity";
        IRI node = new IRI(testUri);
        TestRdfEntity testEntity = factory.getProxy(node, TestRdfEntity.class);

        testEntity.setBoolean(true);
        assertTrue(testEntity.getBoolean());

        testEntity.setInteger(10);
        assertEquals(new Integer(10), testEntity.getInteger());

        testEntity.setLong(20l);
        assertEquals(new Long(20), testEntity.getLong());

        //TODO: Not supported by org.apache.clerezza.rdf.core.impl.SimpleLiteralFactory!
        //testEntity.setFloat(0.1f);
        //assertTrue(new Float(0.1f).equals(testEntity.getFloat()));

        testEntity.setDouble(0.2);
        assertEquals(new Double(0.2), testEntity.getDouble());

        testEntity.setString("Test!");
        assertEquals("Test!", testEntity.getString());

        Date currentDate = new Date();
        testEntity.setDate(currentDate);
        assertEquals(currentDate, testEntity.getDate());

        testEntity.setIntegers(Arrays.asList(new Integer(1),new Integer(2), new Integer(3)));
        Collection<Integer> integers = testEntity.getIntegers();
        assertTrue(integers.contains(new Integer(1)));
        assertTrue(integers.contains(new Integer(2)));
        assertTrue(integers.contains(new Integer(3)));

        //test Remove
        integers.remove(new Integer(2));
        assertTrue(integers.contains(new Integer(1)));
        assertTrue(!integers.contains(new Integer(2)));
        assertTrue(integers.contains(new Integer(3)));

        //get an new Collection and repeat the test
        integers = testEntity.getIntegers();
        assertTrue(integers.contains(new Integer(1)));
        assertTrue(!integers.contains(new Integer(2)));
        assertTrue(integers.contains(new Integer(3)));

        //test Add
        integers.add(new Integer(-1));
        assertTrue(integers.contains(new Integer(-1)));
        assertTrue(integers.contains(new Integer(1)));
        assertTrue(integers.contains(new Integer(3)));

        //again get a new collection
        Collection<Integer>integers2 = testEntity.getIntegers();
        assertTrue(integers2.contains(new Integer(-1)));
        assertTrue(integers2.contains(new Integer(1)));
        assertTrue(integers2.contains(new Integer(3)));

        //remove/add an value in integers and test in integers2
        integers.remove(new Integer(3));
        integers.add(new Integer(0));
        assertTrue(integers2.contains(new Integer(-1)));
        assertTrue(integers2.contains(new Integer(0)));
        assertTrue(integers2.contains(new Integer(1)));
        assertTrue(!integers2.contains(new Integer(2)));
        assertTrue(!integers2.contains(new Integer(3)));
    }

    @Test
    public void testTypeStatements() throws Exception {
        Graph graph = new SimpleGraph();
        RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
        String testUri = "urn:RdfEntityFactoryTest:TestEntity";
        IRI node = new IRI(testUri);
        TestRdfEntity entity = factory.getProxy(node, TestRdfEntity.class, new Class[]{TestRdfEntity2.class});
        // test the if the proxy implements both interfaces
        assertTrue(entity instanceof TestRdfEntity);
        assertTrue(entity instanceof TestRdfEntity2);

        Set<String> typeStrings = getRdfTypes(graph, node);
        assertTrue(typeStrings.contains(TestRdfEntity.class.getAnnotation(Rdf.class).id()));
        assertTrue(typeStrings.contains(TestRdfEntity2.class.getAnnotation(Rdf.class).id()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        Graph graph = new SimpleGraph();
        RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
        String testUri = "urn:RdfEntityFactoryTest:TestEntity";
        String testUri2 = "urn:RdfEntityFactoryTest:TestEntity2";
        IRI node = new IRI(testUri);
        IRI node2 = new IRI(testUri2);
        TestRdfEntity entity = factory.getProxy(node, TestRdfEntity.class);
        TestRdfEntity2 entity2 = factory.getProxy(node2, TestRdfEntity2.class);

        URI testURI = new URI("urn:test:URI");
        entity.setURI(testURI);
        assertEquals(testURI, entity.getURI());

        URL testURL = new URL("http://www.iks-project.eu");
        entity.setURL(testURL);
        assertEquals(testURL, entity.getURL());

        entity.setIRI(node2);
        assertEquals(node2, entity.getIRI());

        entity2.setTestEntity(entity);
        assertEquals(entity, entity2.getTestEntity());

        Collection<TestRdfEntity> testEntities = entity2.getTestEntities();
        assertTrue(testEntities.isEmpty()); //check that entity is not in the collection
        Set<IRI> testIRIs = new HashSet<IRI>();
        int NUM = 10;
        for (int i=0;i<NUM;i++){
            IRI testNode = new IRI(testUri+':'+'_'+i);
            testIRIs.add(testNode);
            testEntities.add(factory.getProxy(testNode, TestRdfEntity.class));
        }
        //now get a new collection and test if the added entities are there
        Collection<IRI> resultIRIs = new ArrayList<IRI>(); //add to a list to check for duplicates
        for (TestRdfEntity e : entity2.getTestEntities()){
            assertTrue(e.getId() instanceof IRI); //I used IRIs for the generation ...
            resultIRIs.add((IRI)e.getId());
        }
        //now cross check
        assertTrue(testIRIs.containsAll(resultIRIs));
        assertTrue(resultIRIs.containsAll(testIRIs));
        //now one could try to remove some Elements ...
        // ... but things like that are already tested for Integers in testPrimitiveDataTypes
    }

    @Test
    public void testInterfaceHierarchies() throws Exception {
        Graph graph = new SimpleGraph();
        RdfEntityFactory factory = RdfEntityFactory.createInstance(graph);
        String testUri = "urn:RdfEntityFactoryTest:SubTestEntity";
        String testUri2 = "urn:RdfEntityFactoryTest:TestEntity2";
        String testUri3 = "urn:RdfEntityFactoryTest:TestEntity";
        IRI node = new IRI(testUri);
        IRI node2 = new IRI(testUri2);
        IRI node3 = new IRI(testUri3);
        SubTestRdfEntity entity = factory.getProxy(node, SubTestRdfEntity.class);
        TestRdfEntity entity2 = factory.getProxy(node2, TestRdfEntity.class, SubTestRdfEntity.class, TestRdfEntity2.class);
        TestRdfEntity entity3 = factory.getProxy(node3, TestRdfEntity.class);

        //Start with checking the types for entity2
        //first type cast to the hierarchy
        assertTrue(entity instanceof TestRdfEntity);
        assertTrue(entity instanceof RdfEntity);

        // test if the rdf:type triples are present in the Graph
        Set<String> typeStrings = getRdfTypes(graph, node);
        assertTrue(typeStrings.contains(SubTestRdfEntity.class.getAnnotation(Rdf.class).id()));
        assertTrue(typeStrings.contains(TestRdfEntity.class.getAnnotation(Rdf.class).id()));

        typeStrings = null;
        //now the same for entity2
        //first type cast to the hierarchy
        assertTrue(entity2 instanceof SubTestRdfEntity);
        assertTrue(entity2 instanceof TestRdfEntity2);
        assertTrue(entity2 instanceof RdfEntity);

        // test if the rdf:type triples are present in the Graph
        typeStrings = getRdfTypes(graph, node2);
        assertTrue(typeStrings.contains(SubTestRdfEntity.class.getAnnotation(Rdf.class).id()));
        assertTrue(typeStrings.contains(TestRdfEntity.class.getAnnotation(Rdf.class).id()));
        assertTrue(typeStrings.contains(TestRdfEntity2.class.getAnnotation(Rdf.class).id()));

        typeStrings = null;
        //Now check Entity3
        assertTrue(!(entity3 instanceof SubTestRdfEntity));
        assertTrue(entity3 instanceof TestRdfEntity);

        //Now create an new Entity for the same Node that implements SubEntity2
        SubTestRdfEntity entity4 = factory.getProxy(node3, SubTestRdfEntity.class);
        //check if entity4 implements SubTestRefEntity
        assertTrue(entity4 instanceof SubTestRdfEntity);

        //now check if the additional type was added to node3
        typeStrings = getRdfTypes(graph, node3);
        assertTrue(typeStrings.contains(SubTestRdfEntity.class.getAnnotation(Rdf.class).id()));
        assertTrue(typeStrings.contains(TestRdfEntity.class.getAnnotation(Rdf.class).id()));
        //and that entity3 still dose not implement SubTestEntity
        // ... because adding/removing rdf:type triples in the graph can not affect existing proxy instances!
        assertTrue(!(entity3 instanceof SubTestRdfEntity));
    }

    private static Set<String> getRdfTypes(Graph graph, IRI node) {
        Iterator<Triple> typeStatements = graph.filter(node, Properties.RDF_TYPE, null);
        Set<String> typeStrings = new HashSet<String>();
        while(typeStatements.hasNext()){
            RDFTerm type = typeStatements.next().getObject();
            assertTrue(type instanceof IRI);
            typeStrings.add(((IRI)type).getUnicodeString());
        }
        return typeStrings;
    }

    /**
     * Interface to test primitive Datatypes and Uri links.
     *
     * @author westei
     */
    @Rdf(id="urn:test:TestRdfEntity")
    public interface TestRdfEntity extends RdfEntity{
        @Rdf(id="urn:test:Integer")
        Integer getInteger();
        @Rdf(id="urn:test:Integer")
        void setInteger(Integer i);

        @Rdf(id="urn:test:Integers")
        Collection<Integer> getIntegers();
        @Rdf(id="urn:test:Integers")
        void setIntegers(Collection<Integer> is);

        @Rdf(id="urn:test:Long")
        Long getLong();
        @Rdf(id="urn:test:Long")
        void setLong(Long l);

        @Rdf(id="urn:test:Float")
        Float getFloat();
        @Rdf(id="urn:test:Float")
        void setFloat(Float f);

        @Rdf(id="urn:test:Double")
        Double getDouble();
        @Rdf(id="urn:test:Double")
        void setDouble(Double d);

        @Rdf(id="urn:test:Boolean")
        Boolean getBoolean();
        @Rdf(id="urn:test:Boolean")
        void setBoolean(Boolean b);

        @Rdf(id="urn:test:Date")
        Date getDate();
        @Rdf(id="urn:test:Date")
        void setDate(Date d);

        @Rdf(id="urn:test:String")
        String getString();
        @Rdf(id="urn:test:String")
        void setString(String string);

        @Rdf(id="urn:test:Calendar")
        Calendar getCalendar();
        @Rdf(id="urn:test:Calendar")
        void setCalendar(Calendar d);

        @Rdf(id="urn:test:URI")
        URI getURI();
        @Rdf(id="urn:test:URI")
        void setURI(URI uri);

        @Rdf(id="urn:test:URL")
        URL getURL();
        @Rdf(id="urn:test:URL")
        void setURL(URL uri);

        @Rdf(id="urn:test:IRI")
        IRI getIRI();
        @Rdf(id="urn:test:IRI")
        void setIRI(IRI uriRef);
    }

    /**
     * Interface to test relations to other RdfEntities.
     *
     * @author westei
     */
    @Rdf(id="urn:test:TestRdfEntity2")
    public interface TestRdfEntity2 extends RdfEntity {
        @Rdf(id="urn:test:RdfEntity")
        TestRdfEntity getTestEntity();
        @Rdf(id="urn:test:RdfEntity")
        void setTestEntity(TestRdfEntity testRdfEntity);

        @Rdf(id="urn:test:RdfEntities")
        Collection<TestRdfEntity> getTestEntities();
        @Rdf(id="urn:test:RdfEntities")
        void setTestEntities(Collection<TestRdfEntity> entities);
    }

    /**
     * Interface to test extends relations between Interfaces.
     *
     * @author westei
     */
    @Rdf(id="urn:test:SubTestRdfEntity")
    public interface SubTestRdfEntity extends TestRdfEntity {
        @Rdf(id="urn:test:RdfEntity2")
        TestRdfEntity2 getTestEntity2();
        @Rdf(id="urn:test:RdfEntity2")
        void setTestEntity2(TestRdfEntity2 entity2);
    }

//    public static void main(String[] args) throws Exception{
//        new RdfEntityFactoryTest().testTypeStatements();
//    }
}
