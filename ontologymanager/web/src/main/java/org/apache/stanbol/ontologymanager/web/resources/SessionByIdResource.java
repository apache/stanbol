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

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

@Path("/ontonet/session/{id}")
public class SessionByIdResource extends BaseStanbolResource {

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected ServletContext servletContext;

    protected Session session;

    public SessionByIdResource(@PathParam(value = "id") String sessionId,
                               @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        SessionManager mgr = onm.getSessionManager();
        session = mgr.getSession(sessionId);
    }

    @GET
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getSessionAsOntology(@PathParam("id") String sessionId,
                                         @Context UriInfo uriInfo,
                                         @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        // return Response.ok(SessionRenderer.getSessionMetadataRDFasOntology(ses)).build();
        return Response.ok(session.asOWLOntology(false)).build();
    }

    @POST
    @Consumes(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    @Produces(MediaType.TEXT_PLAIN)
    public Response addOntology(InputStream content) {
        try {
            session.addOntology(new OntologyContentInputSource(content));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    @POST
    @Consumes(value = MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addOntology(String iri) {
        try {
            session.addOntology(new RootOntologyIRISource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path(value = "/{ontologyId:.+}")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getSessionOntology(@PathParam("id") String sessionId,
                                       @PathParam("ontologyId") String ontologyId,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        OWLOntology o = session.getOntology(IRI.create(ontologyId));
        if (o == null) return Response.status(NOT_FOUND).build();
        return Response.ok(o).build();
    }

}
