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
package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.renderers.ScopeSetRenderer;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * The main Web resource of the KReS ontology manager. All the scopes, sessions and ontologies are accessible
 * as subresources of ONMRootResource.<br>
 * <br>
 * This resource allows a GET method for obtaining an RDF representation of the set of registered scopes and a
 * DELETE method for clearing the scope set and ontology store accordingly.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/ontology")
public class OntologyNetworkResource extends BaseStanbolResource {

    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(getClass());
    
    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected ServletContext servletContext;
    protected ClerezzaOntologyStorage storage;

    public OntologyNetworkResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
		this.storage = (ClerezzaOntologyStorage) ContextHelper.getServiceFromContext(ClerezzaOntologyStorage.class, servletContext);
    }

    /**
     * RESTful DELETE method that clears the entire scope registry and managed ontology store.
     */
    @DELETE
    public void clearOntologies() {
        // First clear the registry...
        ScopeRegistry reg = onm.getScopeRegistry();
        for (OntologyScope scope : reg.getRegisteredScopes())
            reg.deregisterScope(scope);
        // ...then clear the store.
        // TODO : the other way around?
        onm.getOntologyStore().clear();
    }

    /**
     * Default GET method for obtaining the set of (both active and, optionally, inactive) ontology scopes
     * currently registered with this instance of KReS.
     * 
     * @param inactive
     *            if true, both active and inactive scopes will be included. Default is false.
     * @param headers
     *            the HTTP headers, supplied by the REST call.
     * @param servletContext
     *            the servlet context, supplied by the REST call.
     * @return a string representation of the requested scope set, in a format acceptable by the client.
     */
    @GET
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getScopes(@DefaultValue("false") @QueryParam("with-inactive") boolean inactive,
                              @Context HttpHeaders headers,
                              @Context ServletContext servletContext) {

        ScopeRegistry reg = onm.getScopeRegistry();

        Set<OntologyScope> scopes = inactive ? reg.getRegisteredScopes() : reg.getActiveScopes();

        OWLOntology ontology = ScopeSetRenderer.getScopes(scopes);

        return Response.ok(ontology).build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getView() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

}
