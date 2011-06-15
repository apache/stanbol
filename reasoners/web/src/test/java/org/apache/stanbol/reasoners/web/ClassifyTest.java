/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.web;

import static org.junit.Assert.fail;

import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio, alberto musetti
 */
public class ClassifyTest extends StanbolTestBase {

    static boolean enginesReady;
    static boolean timedOut;

    private static final Logger log = LoggerFactory.getLogger(ClassifyTest.class);

    @Before
    public void checkEnginesReady() throws Exception {

        // Check only once per test run
        if(enginesReady) {
            return;
        }

        // If we timed out previously, don't waste time checking again
        if(timedOut) {
            fail("Timeout in previous check of enhancement engines, cannot run tests");
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getClassify method, of class Classify.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testOntologyClassify() throws OWLOntologyCreationException {
        //        Form form = new Form();
        //        File inputfile = new File("./src/main/resources/TestFile/ProvaParent.owl");
        //        String scopeiri = "http://150.146.88.63:9090/kres/ontology/User";
        //        String recipeiri ="http://localhost:9999/recipe/http://kres.iks-project.eu/ontology/meta/rmi_config.owl%23ProvaParentRecipe";
        //
        //        //form.add("scope", scopeiri);
        //        form.add("recipe", recipeiri);
        //        form.add("file", inputfile);
        //
        //        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
        //
        //        System.out.println(response);
        //        if(response.getStatus()==200){
        //        OWLOntology model = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(response.getEntityInputStream());
        //        Iterator<OWLAxiom> ax = model.getAxioms().iterator();
        //        System.out.println("AXIOM COUNT:"+model.getAxiomCount());
        //        while(ax.hasNext())
        //            System.out.println(ax.next());
        //
        //            assertEquals(200,response.getStatus());
        //        }else
        //            fail("Some errors occurred");
    }

    /**
     * Test of getConsistencyCheckViaURL method, of class Classify.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testOntologyClassifyViaURL() throws OWLOntologyCreationException {
        //        Form form = new Form();
        //        String inputiri = "http://www.ontologydesignpatterns.org/cp/owl/agentrole.owl";
        //        String scopeiri = "http://150.146.88.63:9090/kres/ontology/User";
        //        String recipeiri ="http://localhost:9999/recipe/http://kres.iks-project.eu/ontology/meta/rmi_config.owl%23ProvaParentRecipe";
        //
        //        //form.add("scope", scopeiri);
        //        form.add("recipe",recipeiri);
        //        form.add("input-graph", inputiri);
        //
        //        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
        //
        //        System.out.println(response);
        //        if(response.getStatus()==200){
        //        OWLOntology model = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(response.getEntityInputStream());
        //        Iterator<OWLAxiom> ax = model.getAxioms().iterator();
        //        System.out.println("AXIOM COUNT:"+model.getAxiomCount());
        //        while(ax.hasNext())
        //            System.out.println(ax.next());
        //
        //            assertEquals(200,response.getStatus());
        //        }else
        //            fail("Some errors occurred");
    }

}