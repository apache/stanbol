/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.resource;

import eu.iksproject.kres.rules.manager.KReSRuleStore;
import javax.ws.rs.core.Context;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 *
 * @author elvio
 */
@Path("/rulestore")
public class RuleStoreResource {
    
    private KReSRuleStore kresRuleStore;

   /**
     * To get the KReSRuleStore where are stored the rules and the recipes
     *
     * @param servletContext {To get the context where the REST service is running.}
     */
    public RuleStoreResource(@Context ServletContext servletContext){
       this.kresRuleStore = (KReSRuleStore) servletContext.getAttribute(KReSRuleStore.class.getName());
       if (kresRuleStore == null) {
            throw new IllegalStateException(
                    "KReSRuleStore with stored rules and recipes is missing in ServletContext");
        }
    }

   /**
     * To get the KReSRuleStore in the serveletContext.
     * @return {An object of type KReSRuleStore.}
     */
    @GET
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/rdf+xml")
    public Response getRuleStore(){
        return Response.ok(this.kresRuleStore).build();
    }

}
