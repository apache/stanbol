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

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * The REST resource of an OntoNet {@link Session} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/session/{id}")
public class SessionByIdResource extends BaseStanbolResource {

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected ServletContext servletContext;

    protected SessionManager sesMgr;

    protected Session session;

    public SessionByIdResource(@PathParam(value = "id") String sessionId,
                               @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        sesMgr = onm.getSessionManager();
        session = sesMgr.getSession(sessionId);
    }

    /**
     * Gets the OWL ontology form of the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param uriInfo
     * @param headers
     * @return the ontology if the session exists, otherwise {@link Status#NOT_FOUND}.
     */
    @GET
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response asOntology(@PathParam("id") String sessionId,
                               @Context UriInfo uriInfo,
                               @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        return Response.ok(session.asOWLOntology(false)).build();
    }

    /**
     * Used to create an OntoNet session with a specified identifier.
     * 
     * @param sessionId
     *            the identifier of the session to be created.
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the creation was successful, or {@link Status#CONFLICT} if a session with
     *         that ID already exists.
     */
    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        try {
            session = sesMgr.createSession(sessionId);
        } catch (DuplicateSessionIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Destroys the session and unmanages its ontologies (which are also lost unless stored).
     * 
     * @param sessionId
     *            the session identifier
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the deletion was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all.
     */
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        sesMgr.destroySession(sessionId);
        session = null;
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Gets the ontology with the given identifier in its version managed by the session.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return the requested managed ontology, or {@link Status#NOT_FOUND} if either the sessionn does not
     *         exist, or the if the ontology either does not exist or is not managed.
     */
    @GET
    @Path(value = "/{ontologyId:.+}")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getManagedOntology(@PathParam("id") String sessionId,
                                       @PathParam("ontologyId") String ontologyId,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        OWLOntology o = session.getOntology(IRI.create(ontologyId));
        if (o == null) return Response.status(NOT_FOUND).build();
        return Response.ok(o).build();
    }

    /**
     * Tells the session that it should manage the ontology obtained by parsing the supplied content.<br>
     * <br>
     * Note that the PUT method cannot be used, as it is not possible to predict what ID the ontology will
     * have until it is parsed.
     * 
     * @param content
     *            the ontology content
     * @return {@link Status#OK} if the addition was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session is locked or cannot modified for some
     *         other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error occurs.
     */
    @POST
    @Consumes(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    @Produces(MediaType.TEXT_PLAIN)
    public Response manageOntology(InputStream content) {
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new OntologyContentInputSource(content));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Tells the session that it should manage the ontology obtained by dereferencing the supplied IRI.<br>
     * <br>
     * Note that the PUT method cannot be used, as it is not possible to predict what ID the ontology will
     * have until it is parsed.
     * 
     * @param content
     *            the ontology physical IRI
     * @return {@link Status#OK} if the addition was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session is locked or cannot modified for some
     *         other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error occurs.
     */
    @POST
    @Consumes(value = MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response manageOntology(String iri) {
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new RootOntologyIRISource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Tells the session to no longer manage the ontology with the supplied <i>logical</i> identifier. The
     * ontology will be lost if not stored or not managed by another collector.
     * 
     * @param sessionId
     *            the session identifier.
     * @param ontologyId
     *            the ontology identifier.
     * @param uriInfo
     * @param headers
     * @return {@link Status#OK} if the removal was successful, {@link Status#NOT_FOUND} if there is no such
     *         session at all, {@link Status#FORBIDDEN} if the session or the ontology is locked or cannot
     *         modified for some other reason, {@link Status#INTERNAL_SERVER_ERROR} if some other error
     *         occurs.
     */
    @DELETE
    @Path(value = "/{ontologyId:.+}")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmanageOntology(@PathParam("id") String sessionId,
                                     @PathParam("ontologyId") String ontologyId,
                                     @Context UriInfo uriInfo,
                                     @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI iri = IRI.create(ontologyId);
        OWLOntology o = session.getOntology(iri);
        if (o == null) return Response.notModified().build();
        try {
            session.removeOntology(iri);
        } catch (IrremovableOntologyException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OntologyCollectorModificationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.status(OK).type(MediaType.TEXT_PLAIN).build();
    }

}
