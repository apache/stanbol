/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.web;

import java.util.Properties;

import org.apache.stanbol.commons.testing.jarexec.JarExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author elvio
 */
public class EnrichmentTest {

    public EnrichmentTest() {
    }

    public static final int __PORT = 9999;

    public static final String __JAR_FOLDER = "target";

    public static final String __PROP_JAR_NAME_REGEXP = "org.apache.stanbol.reasoners.*jar$";

    public static final String __TEST_URI = "http://localhost:" + __PORT + "/";
    public static final String __ROOT_URI = __TEST_URI + "enrichment";
    private static JarExecutor server;
    private Client client;
    private WebResource webres;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Properties properties = new Properties();
        properties.put(JarExecutor.PROP_SERVER_PORT, String.valueOf(__PORT));
        properties.put(JarExecutor.PROP_JAR_FOLDER, __JAR_FOLDER);
        properties.put(JarExecutor.PROP_JAR_NAME_REGEXP, __PROP_JAR_NAME_REGEXP);

        server = JarExecutor.getInstance(properties);
        server.start();
    }

    @Before
    public void setUp() {
        //RuleStore store = new KReSRuleStore();
        //server.setAttribute("kresRuleStore", store);

        client = Client.create();
        webres = client.resource(__ROOT_URI);

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of ontologyEnrichmentViaIRI method, of class Enrichment.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testOntologyEnrichmentViaIRI() throws OWLOntologyCreationException {
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
     * Test of ontologyEnrichment method, of class Enrichment.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testOntologyEnrichment() throws OWLOntologyCreationException {
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

}