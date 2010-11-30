/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.manager;

import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.Client;
import eu.iksproject.kres.jersey.JettyServer;
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
public class RecipeTest {

    public static final int __PORT = 9999;
    public static final String __TEST_URI = "http://localhost:" + __PORT + "/";
    public static final String _ROOT_URI = __TEST_URI + "recipe";
    private static JettyServer server;
    private Client client;
    private WebResource webres;
    private WebResource webresall;

    public RecipeTest() {
    }

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
	webres = client.resource(_ROOT_URI);
        webresall = client.resource(_ROOT_URI+"/all");
                
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testGetRecipe() {
        WebResource webresget = webres.path("http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaParentRecipe");
        System.out.println(webresget);
        //OWLOntology owl = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(webres.get(String.class));

        System.out.println(webresget.get(String.class));
        ClientResponse head = webresget.head();

        if(head.getStatus()==200)
            assertEquals(200,head.getStatus());
        else
            fail("Some errors occurred");
    }

    /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testGetAllRecipes() {

        System.out.println(webresall);

        System.out.println(webresall.get(String.class));
        ClientResponse head = webresall.head();

        if(head.getStatus()==200)
            assertEquals(200,head.getStatus());
        else
            fail("Some errors occurred");
    }

   /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testAddRecipe() {

        Form form = new Form();
        form.add("recipe","http://demo/myrecipe");//"http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaAddRecipe");
        form.add("description","Try to add a recipe");
        ClientResponse response = webres.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,form);

        System.out.println(webres);

        if(response.getStatus()==200)
            assertEquals(200,response.getStatus());
        else
            fail("Some errors occurred");
    }

/**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testGetRecipe_2() {
        WebResource webresget = webres.path("http://demo/myrecipe");
        System.out.println(webresget);
        //OWLOntology owl = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(webres.get(String.class));

        System.out.println(webresget.get(String.class));
        ClientResponse head = webresget.head();

        if(head.getStatus()==200)
            assertEquals(200,head.getStatus());
        else
            fail("Some errors occurred");
    }

    /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testGetAllRecipes_2() {

        System.out.println(webresall);

        System.out.println(webresall.get(String.class));
        ClientResponse head = webresall.head();

        if(head.getStatus()==200)
            assertEquals(200,head.getStatus());
        else
            fail("Some errors occurred");
    }

    /**
     * Test of getRecipe method, of class GetRecipe.
     */
    @Test
    public void testDeleteRecipe() {

        String recipe = "http://demo/myrecipe";//http://kres.iks-project.eu/ontology/meta/rmi_config.owl#ProvaAddRecipe";

        WebResource webresdel = webres.queryParam("recipe", recipe);

        webresdel.delete();
     
        ClientResponse response = webresdel.head();
        int status = response.getStatus();
        System.out.println(response);
        if((status==200)||status == 405)
            assertTrue(true);
        else
            fail("Some errors occurred");
    }

}