/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.web;

import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.Client;
import javax.ws.rs.core.Response;
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
public class RuleTest {

    public RuleTest() {
    }

    public static final int __PORT = 9999;
    public static final String __TEST_URI = "http://localhost:" + __PORT + "/";
    public static final String _ROOT_URI = __TEST_URI + "rule";
    private static JettyServer server;
  

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

    }

    /**
     * Test of getRule method, of class Rule.
     */
    @Test
    public void testGetRule() {
        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
        Client client = Client.create();
        WebResource webresget = client.resource(_ROOT_URI);
        webresget = webresget.path("http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRule");
        
        webresget.get(String.class);
        ClientResponse head = webresget.head();
        System.err.println("get a single rule "+head);
        int status = head.getStatus();
        head.close();
        client.destroy();
        if(status==200){
            assertEquals(200,status);
        }else{
            fail("Some errors occurred");
        }
    }

    /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testGetAllRules() {
        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
        Client client = Client.create();
        WebResource webresall = client.resource(_ROOT_URI + "/all");
        
        webresall.get(String.class);
        ClientResponse head = webresall.head();
        System.err.println("get all rules "+head);

        int status = head.getStatus();
        head.close();
        client.destroy();
        if(status==200){
            assertEquals(200,status);
        }else{
            fail("Some errors occurred");
        }
    }

//     /**
//     * Test of addRuleToRecipe method, of class Rule.
//     */
//    @Test
//    public void testAddRuleToRecipe_2() {
//        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
//        Client client = Client.create();
//        WebResource webrespath_2 = client.resource(_ROOT_URI);
//        Form form_2 = new Form();
//        String recipenew = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRecipe";
//        String rulenew = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyRuleA";
//
//        form_2.add("recipe",recipenew);
//        form_2.add("rule",rulenew);
//
//        ClientResponse response = webrespath_2.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form_2);
//
//        System.err.println("add exist rule "+response);
//
//        int status = response.getStatus();
//        form_2.remove("recipe");
//        form_2.remove("rule");
//        form_2.clear();
//        response.close();
//        client.destroy();
//        if(status==200)
//            assertEquals(200,status);
//        else
//            fail("Some errors occurred");
//    }
//
// /**
//    * Test of addRuleToRecipe method, of class Rule.
//     */
//    @Test
//    public void testAddRuleToRecipe() {
//        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
//        Client client = Client.create();
//        WebResource webrespath_1 = client.resource(_ROOT_URI);
//        Form form = new Form();
//        String recipe = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRecipe";
//        String rule = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentNewRule";
//        String kres_syntax = "Body -> Head";
//        String description = "Prova aggiunta regola";
//        form.add("recipe",recipe);
//        form.add("rule",rule);
//        form.add("kres-syntax",kres_syntax);
//        form.add("description",description);
//
//        ClientResponse response = webrespath_1.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);
//
//        System.err.println("add a new rule "+response);
//        int status = response.getStatus();
//        form.remove("recipe");
//        form.remove("rule");
//        form.remove("kres-syntax");
//        form.remove("description");
//        form.clear();
//        response.close();
//        client.destroy();
//        if(status==200)
//            assertEquals(200,status);
//        else
//            fail("Some errors occurred");
//
//
//    }
//
//    /**
//     * Test of removeRule method, of class Rule.
//     */
//    @Test
//    public void testRemoveRule() {
//        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
//        Client client = Client.create();
//        WebResource webresdel = client.resource(_ROOT_URI);
//        String recipedel = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRecipe";
//        String ruledel = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyRuleA";
//
//       webresdel = webresdel.queryParam("recipe", recipedel).queryParam("rule", ruledel);
//
//       webresdel.delete();
//
//        ClientResponse response = webresdel.head();
//        System.err.println("removeRule "+response);
//        int status = response.getStatus();
//        response.close();
//        client.destroy();
//        if((status==200)||status == 405)
//            assertTrue(true);
//        else
//            fail("Some errors occurred");
//    }
//
//     /**
//     * Test of removeRule method, of class Rule.
//     */
//    @Test
//    public void testRemoveRule_2() {
//        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
//        Client client = Client.create();
//        WebResource webresdel = client.resource(_ROOT_URI);
//        String recipedel = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRecipe";
//        String ruledel = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentNewRule";
//        webresdel = webresdel.queryParam("recipe", recipedel).queryParam("rule", ruledel);
//
//       webresdel.delete();
//
//        ClientResponse response = webresdel.head();
//        System.err.println("removeRule "+response);
//        int status = response.getStatus();
//        response.close();
//        client.destroy();
//        if((status==200)||status == 405)
//            assertTrue(true);
//        else
//            fail("Some errors occurred");
//    }
//
//     /**
//     * Test of removeRule method, of class Rule.
//     */
//    @Test
//    public void testRemoveSingleRule() {
//        System.err.println("::::::::::::::::::::::::::::::::::::::::::::::::");
//        Client client = Client.create();
//        WebResource webresdel = client.resource(_ROOT_URI);
//        String ruledel = "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentNewRule";
//        webresdel = webresdel.queryParam("rule", ruledel);
//
//       webresdel.delete();
//
//        ClientResponse response = webresdel.head();
//        System.err.println("removeRule "+response);
//        int status = response.getStatus();
//        response.close();
//        client.destroy();
//        if((status==200)||status == 405)
//            assertTrue(true);
//        else
//            fail("Some errors occurred");
//    }

}