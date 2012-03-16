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

package org.apache.stanbol.rules.web.resources;

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;

/**
 *
 * @author elvio, andrea.nuzzolese
 */
@Path("/rulestore")
public class RuleStoreResource extends BaseStanbolResource {
    
    private RuleStoreImpl ruleStore;

   /**
     * To get the RuleStoreImpl where are stored the rules and the recipes
     *
     * @param servletContext {To get the context where the REST service is running.}
     */
    public RuleStoreResource(@Context ServletContext servletContext){
       this.ruleStore = (RuleStoreImpl) servletContext.getAttribute(RuleStoreImpl.class.getName());
       if (ruleStore == null) {
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
    public Response getRuleStore(@Context HttpHeaders headers){
        ResponseBuilder rb = Response.ok(this.ruleStore);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

}
