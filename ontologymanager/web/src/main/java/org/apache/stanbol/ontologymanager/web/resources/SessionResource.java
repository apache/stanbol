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

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.stanbol.commons.web.base.format.KRFormat.*;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.renderers.SessionRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.FormDataParam;

@Path("/ontonet/session")
public class SessionResource extends BaseStanbolResource {

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected ServletContext servletContext;

    public SessionResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
    }

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    public Response addOntology(@FormDataParam("scope") String scope,
                                @FormDataParam("import") InputStream importOntology,
                                @FormDataParam("session") String session,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers,
                                @Context ServletContext servletContext) {

        IRI scopeIRI = IRI.create(scope);
        IRI sessionIRI = IRI.create(session);

        OWLOntology ontology;
        try {
            ontology = onm.getOwlCacheManager().loadOntologyFromOntologyDocument(importOntology);

            ScopeRegistry scopeRegistry = onm.getScopeRegistry();

            OntologyScope ontologyScope = scopeRegistry.getScope(scope);
            SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionIRI);
            try {
                sos.addOntology(new RootOntologySource(ontology));
                return Response.ok().build();
            } catch (UnmodifiableOntologySpaceException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        } catch (OWLOntologyCreationException e1) {
            return Response.status(NOT_FOUND).build();
        }

    }

    /**
     * 
     * If the session param is missing, this method creates a new session, ignoring other params
     * 
     * @param scope
     * @param session
     * @param location
     * @param uriInfo
     * @param headers
     * @param servletContext
     * @return
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(value = {RDF_XML, OWL_XML, TURTLE, FUNCTIONAL_OWL, MANCHESTER_OWL, RDF_JSON})
    public Response addOntology(@FormParam("scope") String scope,
                                @FormParam("session") String session,
                                @FormParam("location") String location,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers,
                                @Context ServletContext servletContext) {
        if (session == null || session.equals("")) {
            return createSession(scope, uriInfo, headers);
        } else {
            IRI scopeIRI = IRI.create(scope);
            IRI sessionIRI = IRI.create(session);
            IRI ontologyIRI = IRI.create(location);
            ScopeRegistry scopeRegistry = onm.getScopeRegistry();

            OntologyScope ontologyScope = scopeRegistry.getScope(scope);
            SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionIRI);
            try {
                sos.addOntology(new RootOntologyIRISource(ontologyIRI));
                return Response.ok().build();
            } catch (UnmodifiableOntologySpaceException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            } catch (OWLOntologyCreationException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * This method creates a session.
     * 
     * @param scope
     * @param uriInfo
     * @param headers
     * @return
     */
    private Response createSession(String scope, UriInfo uriInfo, HttpHeaders headers) {
        if (scope == null || scope.equals("")) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
        Session ses = null;
        SessionManager mgr = onm.getSessionManager();

        /*
         * Create the KReS session to associate to the scope.
         */
        ses = mgr.createSession();

        /*
         * First get the scope registry.
         */
        ScopeRegistry scopeRegistry = onm.getScopeRegistry();

        /*
         * Then retrieve the ontology scope.
         */
        IRI scopeIRI = IRI.create(scope);
        OntologyScope ontologyScope = scopeRegistry.getScope(scope);

        /*
         * Finally associate the KReS session to the scope.
         */
        OntologySpaceFactory ontologySpaceFactory = onm.getOntologySpaceFactory();
        SessionOntologySpace sessionOntologySpace = ontologySpaceFactory.createSessionOntologySpace(scope);
        try {
            ontologyScope.addSessionSpace(sessionOntologySpace, ses.getID());
        } catch (UnmodifiableOntologySpaceException e) {
            throw new WebApplicationException(e);
        }

        return Response.ok(SessionRenderer.getSessionMetadataRDFasOntology(ses)).build();

    }

    /**
     * FIXME what are these path params anyway?
     * 
     * @param scope
     * @param session
     * @param deleteOntology
     * @param uriInfo
     * @param headers
     * @return
     */
    @DELETE
    public Response deleteSession(@QueryParam("scope") String scope,
                                  @QueryParam("session") String session,
                                  @QueryParam("delete") String deleteOntology,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {

        IRI scopeID = IRI.create(scope);
        IRI sessionID = IRI.create(session);

        if (deleteOntology != null) {
            IRI ontologyIRI = IRI.create(deleteOntology);

            ScopeRegistry scopeRegistry = onm.getScopeRegistry();

            OntologyScope ontologyScope = scopeRegistry.getScope(scope);
            SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionID);

            try {
                /*
                 * TODO : previous implementation reloaded the whole ontology before deleting it, thus
                 * treating this as a physical IRI. See if it still works this way
                 */
                OWLOntology o = sos.getOntology(ontologyIRI);
                if (o != null) sos.removeOntology(new RootOntologySource(o));
                return Response.ok().build();
            } catch (OntologySpaceModificationException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        } else {
            onm.getSessionManager().destroySession(sessionID);
            return Response.ok().build();
        }

    }

    @GET
    @Produces(TEXT_HTML)
    public Response getView() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

}
