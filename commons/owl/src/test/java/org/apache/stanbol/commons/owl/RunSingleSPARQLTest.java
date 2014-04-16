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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.commons.owl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.query.QueryExecution;
import org.apache.stanbol.commons.owl.RunSingleSPARQL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.query.ResultSet;

/**
 *
 * @author elvio
 */
public class RunSingleSPARQLTest {

    public OWLOntologyManager owlmanager;
    public OWLOntology owl;
    public HashMap<String,String> sparqlprefix;

    public RunSingleSPARQLTest() throws OWLOntologyCreationException {
        this.owlmanager = OWLManager.createOWLOntologyManager();
        this.owl = owlmanager.loadOntologyFromOntologyDocument(new File("./src/main/resources/TestFile/ProvaParent.owl"));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSPARQLprefix method, of class RunSingleSPARQL.
     */
    @Test
    public void testGetSPARQLprefix() {

        Map<String,String> map = new HashMap<String,String>();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        RunSingleSPARQL instance = new RunSingleSPARQL(owl,map);
        Map<String,String> expResult = map;
        Map<String,String> result = instance.getSPARQLprefix();
        if(!result.isEmpty()){
            assertEquals(expResult, result);
        }else{
        // TODO review the generated test code and remove the default call to fail.
            fail("Some errors occur in getSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of addSPARQLprefix method, of class RunSingleSPARQL.
     */
    @Test
    public void testAddSPARQLprefix() {
        
        String label = "mylabel";
        String prefix = "<http://prova.mylabel.org#>";
        Map<String,String> map = new HashMap<String,String>();
        map.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        RunSingleSPARQL instance = new RunSingleSPARQL(owl,map);
        boolean result = instance.addSPARQLprefix(label, prefix);
        if(result){
            Map<String, String> mymap = instance.getSPARQLprefix();
      
            assertEquals(prefix, mymap.get(label));
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in addSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of removeSPARQLprefix method, of class RunSingleSPARQL.
     */
    @Test
    public void testRemoveSPARQLprefix() {

        Map<String,String> map = new HashMap<String,String>();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        RunSingleSPARQL instance = new RunSingleSPARQL(owl,map);
        boolean result = instance.removeSPARQLprefix("ex");

        if(result){
            Map<String, String> mymap = instance.getSPARQLprefix();
            assertEquals(false, mymap.containsKey("ex"));
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in removeSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of testCreateSPARQLQueryExecutionFactory() method, of class RunSingleSPARQL.
     */
    @Test
    public void testCreateSPARQLQueryExecutionFactory() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#>");
        String query = "SELECT * WHERE {?p rdf:type ex:Person .}";
        RunSingleSPARQL instance = new RunSingleSPARQL(owl,map);
        QueryExecution queryExecution = instance.createSPARQLQueryExecutionFactory(query);
        if (queryExecution == null) {
           fail("Some errors occurred in createSPARQLQueryExecutionFactory of KReSRunSPARQL");
        }
        ResultSet result = queryExecution.execSelect();

        if(result!=null){
            int m = 0;
            while(result.hasNext()){
                result.next();
                m++;
            }
        queryExecution.close();
        assertEquals(3, m);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in createSPARQLQueryExecutionFactory of KReSRunSPARQL");
        }
    }

}