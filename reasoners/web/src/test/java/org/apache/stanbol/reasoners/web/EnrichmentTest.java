/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.web;

import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 *
 * @author elvio, alberto musetti
 */
public class EnrichmentTest extends StanbolTestBase{

    private Client client;
    private WebResource webres;
    private static final Logger log = LoggerFactory.getLogger(EnrichmentTest.class);

    @Before
    public void setUp() {
        //RuleStore store = new KReSRuleStore();
        //server.setAttribute("kresRuleStore", store);

        client = Client.create();
        webres = client.resource(this.serverBaseUrl);

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