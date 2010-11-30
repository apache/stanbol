/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.reasoners;

import java.io.File;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import com.hp.hpl.jena.query.ResultSet;
import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elvio
 */
public class KReSRunSPARQLTest {

    public OWLOntologyManager owlmanager;
    public OWLOntology owl;
    public HashMap<String,String> sparqlprefix;

    public KReSRunSPARQLTest() throws OWLOntologyCreationException {
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
     * Test of getSPARQLprefix method, of class KReSRunSPARQL.
     */
    @Test
    public void testGetSPARQLprefix() {

        HashMap<String,String> map = new HashMap();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        KReSRunSPARQL instance = new KReSRunSPARQL(owl,map);
        HashMap expResult = map;
        HashMap result = instance.getSPARQLprefix();
        if(!result.isEmpty()){
            assertEquals(expResult, result);
        }else{
        // TODO review the generated test code and remove the default call to fail.
            fail("Some errors occur in getSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of addSPARQLprefix method, of class KReSRunSPARQL.
     */
    @Test
    public void testAddSPARQLprefix() {
        
        String label = "mylabel";
        String prefix = "<http://prova.mylabel.org#>";
        HashMap<String,String> map = new HashMap();
        map.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        KReSRunSPARQL instance = new KReSRunSPARQL(owl,map);
        boolean result = instance.addSPARQLprefix(label, prefix);
        if(result){
            HashMap<String, String> mymap = instance.getSPARQLprefix();
      
            assertEquals(prefix, mymap.get(label));
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in addSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of removeSPARQLprefix method, of class KReSRunSPARQL.
     */
    @Test
    public void testRemoveSPARQLprefix() {

        HashMap<String,String> map = new HashMap();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");

        KReSRunSPARQL instance = new KReSRunSPARQL(owl,map);
        boolean result = instance.removeSPARQLprefix("ex");

        if(result){
            HashMap<String, String> mymap = instance.getSPARQLprefix();
            assertEquals(false, mymap.containsKey("ex"));
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in removeSPARQLprefix of KReSRunSPARQL.");
        }
    }

    /**
     * Test of runSPARQL method, of class KReSRunSPARQL.
     */
    @Test
    public void testRunSPARQL() {
        HashMap<String,String> map = new HashMap();
        map.put("rdfs","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("xsd","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("owl","<http://www.w3.org/2000/01/rdf-schema#>");
        map.put("rdf","<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
        map.put("ex","<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#>");
        String query = "SELECT * WHERE {?p rdf:type ex:Person .}";
        KReSRunSPARQL instance = new KReSRunSPARQL(owl,map);
        ResultSet result = instance.runSPARQL(query);

        if(result!=null){
            int m = 0;
            while(result.hasNext()){
                result.next();
                m++;
            }
        assertEquals(3, m);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur in runSPARQL of KReSRunSPARQL");
        }
    }

}