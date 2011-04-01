/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.web;

import org.apache.stanbol.kres.jersey.JettyServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author elvio
 */
public class ConsistencyCheckTest {

    public ConsistencyCheckTest() {
    }

    public static final int __PORT = 9999;
    public static final String __TEST_URI = "http://localhost:" + __PORT + "/";
    public static final String __ROOT_URI = __TEST_URI + "check-consistency";
    private static JettyServer server;
    private Client client;
    private WebResource webres;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server = new JettyServer();
        server.start(__TEST_URI);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.stop();
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
     * Test of GetSimpleConsistencyCheck method, of class ConsistencyCheck.
     *
     */
    @Test
    public void testGetSimpleConsistencyCheck() {
//         WebResource webresget = webres.path("http://www.ontologydesignpatterns.org/cp/owl/agentrole.owl");//"http://www.loa-cnr.it/ontologies/DUL.owl");//"http://150.146.88.63:9090/kres/ontology/User");
//
//        System.out.println(webresget.get(String.class));
//        ClientResponse head = webresget.head();
//        System.out.println(head);
//        if(head.getStatus()==200)
//            assertEquals(200,head.getStatus());
//        else
//            fail("Some errors occurred");
    }

    /**
     * Test of getConsistencyCheck method, of class ConsistencyCheck.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testGetConsistencyCheck() {
//        Form form = new Form();
//        File inputfile = new File("./src/main/resources/TestFile/ProvaParent.owl");
//        String scopeiri = "http://150.146.88.63:9090/kres/ontology/User";
//        String recipeiri ="http://localhost:9999/recipe/http://kres.iks-project.eu/ontology/meta/rmi_config.owl%23ProvaParentRecipe";
//
//        //form.add("scope", scopeiri);
//        form.add("recipe",recipeiri);
//        form.add("file", inputfile);
//
//        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
//        System.out.println(response);
//        if(response.getStatus()==200)
//            assertEquals(200,response.getStatus());
//        else
//            fail("Some errors occurred");
    }

    /**
     * Test of getConsistencyCheckViaURL method, of class ConsistencyCheck.
     * FIXME - This test is commented bracuse it needs another service to be run. So it is classifyed as integration test.
     */
    @Test
    public void testGetConsistencyCheckViaURL() {
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
//        if(response.getStatus()==200)
//            assertEquals(200,response.getStatus());
//        else
//            fail("Some errors occurred");
    }

    @Test
    public void testCONFLICT(){

//        Form form = new Form();
//        File inputfile = new File("./src/main/resources/TestFile/ProvaParent.owl");
//        String inputiri = "http://www.loa-cnr.it/ontologies/DUL.owl";
//        String scopeiri = "http://150.146.88.63:9090/kres/ontology/User";
//        String recipeiri ="http://localhost:9999/recipe/http://kres.iks-project.eu/ontology/meta/rmi_config.owl%23ProvaParentRecipe";
//
//        //form.add("scope", scopeiri);
//        form.add("recipe",recipeiri);
//        form.add("input-graph", inputiri);
//        form.add("file", inputfile);
//
//        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
//        System.out.println(response);
//        if(response.getStatus()==409)
//            assertEquals(409,response.getStatus());
//        else
//            fail("Some errors occurred");

    }

//    @Test
//    public void testCONECTION_OWL_LINK(){
//
//        Form form = new Form();
//        File inputfile = new File("./src/main/resources/TestFile/ProvaParent.owl");
//        String reasonerurl = "http://150.146.88.63:9090/kres/ontology/User";
//        String scopeiri = "http://150.146.88.63:9090/kres/ontology/User";
//        String recipeiri ="http://localhost:9999/recipe/http://kres.iks-project.eu/ontology/meta/rmi_config.owl%23ProvaParentRecipe";
//
//        form.add("scope", scopeiri);
//        form.add("recipe",recipeiri);
//        form.add("owllink-endpoint", reasonerurl);
//        form.add("file", inputfile);
//
//        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
//        System.out.println(response);
//        if(response.getStatus()==200)
//            assertEquals(200,response.getStatus());
//        else
//            fail("Some errors occurred");
//
//    }
}