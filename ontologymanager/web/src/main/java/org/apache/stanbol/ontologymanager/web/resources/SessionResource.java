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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource of an OntoNet {@link Session} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/session/{id}")
public class SessionResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the session manager to be fetched from the servlet context.
     */
    protected SessionManager sesMgr;

    protected Session session;

    public SessionResource(@PathParam(value = "id") String sessionId, @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.sesMgr = (SessionManager) ContextHelper.getServiceFromContext(SessionManager.class,
            servletContext);
        // sesMgr = onm.getSessionManager();
        session = sesMgr.getSession(sessionId);
    }

    @GET
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response asOntologyGraph(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        // Export to Clerezza Graph, which can be rendered as JSON-LD.
        ResponseBuilder rb = Response.ok(session.export(Graph.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response asOntologyMixed(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        ResponseBuilder rb;
        // Export smaller graphs to OWLOntology due to the more human-readable rendering.
        if (merge) rb = Response.ok(session.export(Graph.class, merge));
        else rb = Response.ok(session.export(OWLOntology.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response asOntologyOWL(@PathParam("scopeid") String scopeid,
                                  @DefaultValue("false") @QueryParam("merge") boolean merge,
                                  @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        // Export to OWLOntology, the only to support OWL formats.
        ResponseBuilder rb = Response.ok(session.export(OWLOntology.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    public Response createSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        try {
            session = sesMgr.createSession(sessionId);
        } catch (DuplicateSessionIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        } catch (SessionLimitException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    public Response deleteSession(@PathParam("id") String sessionId,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        sesMgr.destroySession(sessionId);
        session = null;
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getManagedOntologyGraph(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        Graph o = session.getOntology(IRI.create(ontologyId), Graph.class, merge);
        if (o == null) return Response.status(NOT_FOUND).build();
        ResponseBuilder rb = Response.ok(o);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response getManagedOntologyMixed(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else if (merge) {
            Graph g = session.getOntology(IRI.create(ontologyId), Graph.class, merge);
            if (g == null) rb = Response.status(NOT_FOUND);
            else rb = Response.ok(g);
        } else {
            OWLOntology o = session.getOntology(IRI.create(ontologyId), OWLOntology.class, merge);
            if (o == null) rb = Response.status(NOT_FOUND);
            else rb = Response.ok(o);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response getManagedOntologyOWL(@PathParam("id") String sessionId,
                                          @PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merge,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        OWLOntology o = session.getOntology(IRI.create(ontologyId), OWLOntology.class, merge);
        if (o == null) return Response.status(NOT_FOUND).build();
        ResponseBuilder rb = Response.ok(o);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
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
    @Consumes(value = {RDF_XML, OWL_XML, N_TRIPLE, N3, TURTLE, X_TURTLE, FUNCTIONAL_OWL, MANCHESTER_OWL,
                       RDF_JSON})
    public Response manageOntology(InputStream content, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new GraphContentInputSource(content)
            // new OntologyContentInputSource(content)
            );
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        }
        log.debug("POST request for ontology addition completed in {} ms.",
            (System.currentTimeMillis() - before));
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    public Response manageOntology(String iri, @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new RootOntologyIRISource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
    public Response unmanageOntology(@PathParam("id") String sessionId,
                                     @PathParam("ontologyId") String ontologyId,
                                     @Context UriInfo uriInfo,
                                     @Context HttpHeaders headers) {
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI iri = IRI.create(ontologyId);
        OWLOntology o = session.getOntology(iri, OWLOntology.class);
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
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
