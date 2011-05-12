/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.web.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;

/**
 *
 * @author elvio
 */
@Path("/rulestore")
public class RuleStoreResource extends BaseStanbolResource {
    
    private RuleStoreImpl kresRuleStore;

   /**
     * To get the RuleStoreImpl where are stored the rules and the recipes
     *
     * @param servletContext {To get the context where the REST service is running.}
     */
    public RuleStoreResource(@Context ServletContext servletContext){
       this.kresRuleStore = (RuleStoreImpl) servletContext.getAttribute(RuleStoreImpl.class.getName());
       if (kresRuleStore == null) {
            throw new IllegalStateException(
                    "KReSRuleStore with stored rules and recipes is missing in ServletContext");
        }
    }

   /**
     * To get the RuleStoreImpl in the serveletContext.
     * @return {An object of type RuleStoreImpl.}
     */
    @GET
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/rdf+xml")
    public Response getRuleStore(){
        return Response.ok(this.kresRuleStore).build();
    }

}
