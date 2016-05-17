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
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

import org.apache.clerezza.jaxrs.utils.form.FormFile;
import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.util.OWLUtils;
//import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.apache.stanbol.ontologymanager.servicesapi.collector.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.StoredOntologySource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.apache.stanbol.ontologymanager.web.util.OntologyPrettyPrintResource;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource of an OntoNet {@link Session} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/ontonet/session/{id}")
public class SessionResource extends AbstractOntologyAccessResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected ScopeManager onMgr;

    @Reference
    protected OntologyProvider<TcProvider> ontologyProvider;

    /*
     * Placeholder for the RegistryManager to be fetched from the servlet context.
     */
    @Reference
    protected RegistryManager regMgr;

    /*
     * Placeholder for the session manager to be fetched from the servlet context.
     */
    @Reference
    protected SessionManager sesMgr;

    protected Session session;

    public SessionResource() {
//        public SessionResource(@PathParam(value = "id") String sessionId, @Context ServletContext servletContext) {
//        this.servletContext = servletContext;
//        this.sesMgr = (SessionManager) ContextHelper.getServiceFromContext(SessionManager.class,
//            servletContext);
//        this.regMgr = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
//            servletContext);
//        this.ontologyProvider = (OntologyProvider<TcProvider>) ContextHelper.getServiceFromContext(
//            OntologyProvider.class, servletContext);
//        this.onMgr = (ScopeManager) ContextHelper.getServiceFromContext(ScopeManager.class, servletContext);
        
    }

    @GET
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response asOntologyGraph(@PathParam(value = "id") String sessionId,
                                    @PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export to Clerezza ImmutableGraph, which can be rendered as JSON-LD.
        ResponseBuilder rb = Response.ok(session.export(ImmutableGraph.class, merge, prefix));
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response asOntologyMixed(@PathParam(value = "id") String sessionId,
                                    @PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        ResponseBuilder rb;
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export smaller graphs to OWLOntology due to the more human-readable rendering.
        if (merge) rb = Response.ok(session.export(ImmutableGraph.class, merge, prefix));
        else rb = Response.ok(session.export(OWLOntology.class, merge, prefix));
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response asOntologyOWL(@PathParam(value = "id") String sessionId,
                                  @PathParam("scopeid") String scopeid,
                                  @DefaultValue("false") @QueryParam("merge") boolean merge,
                                  @Context HttpHeaders headers) {
        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        // Export to OWLOntology, the only to support OWL formats.
        ResponseBuilder rb = Response.ok(session.export(OWLOntology.class, merge, prefix));
//        addCORSOrigin(servletContext, rb, headers);
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
//                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        try {
            session = sesMgr.createSession(sessionId);
        } catch (DuplicateSessionIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        } catch (SessionLimitException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        }
        ResponseBuilder rb = Response.created(uriInfo.getRequestUri());
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response deleteSession(@PathParam("id") String sessionId,
//                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {
        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        sesMgr.destroySession(sessionId);
        session = null;
        ResponseBuilder rb = Response.ok();
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Produces({WILDCARD})
    public Response emptyPost(@PathParam("id") String sessionId, @Context HttpHeaders headers) {
        log.debug(" post(no data)");
        session = sesMgr.getSession(sessionId);
        for (Scope sc : getAllScopes()) { // First remove appended scopes not in the list
            String scid = sc.getID();
            if (getAppendedScopes().contains(scid)) {
                session.detachScope(scid);
                log.info("Removed scope \"{]\".", scid);
            }
        }
        ResponseBuilder rb = Response.ok();
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * Needed for freemarker
     */
    public Set<Scope> getAllScopes() {
        return onMgr.getRegisteredScopes();
    }

    /*
     * Needed for freemarker
     */
    public Set<Scope> getAppendableScopes() {
        Set<Scope> notAppended = new HashSet<Scope>();
        for (Scope sc : onMgr.getRegisteredScopes())
            if (!session.getAttachedScopes().contains(sc.getID())) notAppended.add(sc);
        return notAppended;
    }

    /*
     * Needed for freemarker
     */
    public Set<String> getAppendedScopes() {
        Set<String> appended = new HashSet<String>();
        for (Scope sc : onMgr.getRegisteredScopes())
            if (session.getAttachedScopes().contains(sc.getID())) appended.add(sc.getID());
        return appended;
    }

    private URI getCreatedResource(String ontologyIRI) {
        return URI.create("/" + ontologyIRI);
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@PathParam("id") String sessionId, @Context HttpHeaders headers) {
        ResponseBuilder rb;

        session = sesMgr.getSession(sessionId);
        if (session == null) rb = Response.status(NOT_FOUND);
        else rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<String> getManageableOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (OWLOntologyID id : ontologyProvider.listPrimaryKeys())
            result.add(OntologyUtils.encode(id));
        for (OWLOntologyID id : session.listManagedOntologies())
            result.remove(OntologyUtils.encode(id));
        return result;
    }

    public SortedSet<String> getOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (OWLOntologyID id : session.listManagedOntologies())
            result.add(OntologyUtils.encode(id));
        return result;
    }

    /*
     * Needed for freemarker
     */
    public Session getSession() {
        return session;
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers, GET, POST, PUT, DELETE, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/{ontologyId:.+}")
    public Response handleCorsPreflightOntology(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers, GET, DELETE, OPTIONS);
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
    public Response managedOntologyGetGraph(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
//                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {

        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        ImmutableGraph o = session.getOntology(OntologyUtils.decode(ontologyId), ImmutableGraph.class, merge, prefix);
        ResponseBuilder rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response managedOntologyGetMixed(@PathParam("id") String sessionId,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
//                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        ResponseBuilder rb;

        session = sesMgr.getSession(sessionId);
        if (session == null) rb = Response.status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            if (merge) {
                ImmutableGraph g = session.getOntology(id, ImmutableGraph.class, merge, prefix);
                rb = (g != null) ? Response.ok(g) : Response.status(NOT_FOUND);
            } else {
                OWLOntology o = session.getOntology(id, OWLOntology.class, merge, prefix);
                rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
            }
        }
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response managedOntologyGetOWL(@PathParam("id") String sessionId,
                                          @PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merge,
//                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {

        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
        OWLOntology o = session.getOntology(OntologyUtils.decode(ontologyId), OWLOntology.class, merge,
            prefix);
        ResponseBuilder rb = (o != null) ? Response.ok(o) : Response.status(NOT_FOUND);
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response managedOntologyShow(@PathParam("ontologyId") String ontologyId,
                                        @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (session == null) rb = Response.status(NOT_FOUND);
        else if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else if (!ontologyProvider.hasOntology(OntologyUtils.decode(ontologyId))) rb = Response
                .status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/session/");
            OWLOntology o = session.getOntology(OntologyUtils.decode(ontologyId), OWLOntology.class, false,
                prefix);
            if (o == null) rb = Response.status(NOT_FOUND);
            else try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                o.getOWLOntologyManager().saveOntology(o, new ManchesterOWLSyntaxOntologyFormat(), out);
                rb = Response.ok(new Viewable("ontology", new OntologyPrettyPrintResource(
                        uriInfo, out, session)));
            } catch (OWLOntologyStorageException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response managedOntologyUnload(@PathParam("id") String sessionId,
                                          @PathParam("ontologyId") String ontologyId,
//                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        ResponseBuilder rb;
        session = sesMgr.getSession(sessionId);
        if (session == null) rb = Response.status(NOT_FOUND);
        else {
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            if (!session.hasOntology(id)) rb = Response.notModified();
            else try {
                session.removeOntology(id);
                rb = Response.ok();
            } catch (IrremovableOntologyException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (UnmodifiableOntologyCollectorException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (OntologyCollectorModificationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response manageOntology(InputStream content, @PathParam("id") String sessionId, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;

        session = sesMgr.getSession(sessionId);
        String mt = headers.getMediaType().toString();
        if (session == null) rb = Response.status(NOT_FOUND); // Always check session first
        else try {
            log.debug("POST content claimed to be of type {}.", mt);
            OntologyInputSource<?> src;
            if (OWL_XML.equals(mt) || FUNCTIONAL_OWL.equals(mt) || MANCHESTER_OWL.equals(mt)) src = new OntologyContentInputSource(
                    content);
            else // content = new BufferedInputStream(content);
            src = new GraphContentInputSource(content, mt, ontologyProvider.getStore());
            log.debug("SUCCESS parse with media type {}.", mt);
            OWLOntologyID key = session.addOntology(src);
            if (key == null || key.isAnonymous()) {
                log.error("FAILED parse with media type {}.", mt);
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            }
            // FIXME ugly but will have to do for the time being
            log.debug("SUCCESS add ontology to session {}.", session.getID());
            log.debug("Storage key : {}", key);
            String uri = // key.split("::")[1];
            OntologyUtils.encode(key);
            // uri = uri.substring((ontologyProvider.getGraphPrefix() + "::").length());
            URI created = null;
            if (uri != null && !uri.isEmpty()) {
                created = getCreatedResource(uri);
                rb = Response.created(created);
            } else rb = Response.ok();
            log.info("POST request for ontology addition completed in {} ms.",
                (System.currentTimeMillis() - before));
            log.info("New resource URL is {}", created);
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            log.error("FAILED parse with media type {}.", mt);
            throw new WebApplicationException(e, BAD_REQUEST);
        }
//        addCORSOrigin(servletContext, rb, headers);
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
    public Response manageOntology(String iri, @PathParam("id") String sessionId, @Context HttpHeaders headers) {

        session = sesMgr.getSession(sessionId);
        if (session == null) return Response.status(NOT_FOUND).build();
        try {
            session.addOntology(new RootOntologySource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        ResponseBuilder rb = Response.ok();
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({WILDCARD})
    public Response postOntology(MultiPartBody data, @Context HttpHeaders headers) {
        log.debug(" post(FormDataMultiPart data)");
        long before = System.currentTimeMillis();
        ResponseBuilder rb;

        // TODO remove and make sure it is set across the method
        rb = Response.status(BAD_REQUEST);

        IRI location = null, library = null;
        FormFile file = null; // If found, it takes precedence over location.
        String format = null;
        Set<String> toAppend = null;
        Set<String> keys = new HashSet<String>();

//        for (BodyPart bpart : data.getBodyParts()) {
//            log.debug("Found body part of type {}", bpart.getClass());
//            if (bpart instanceof FormDataBodyPart) {
//                FormDataBodyPart dbp = (FormDataBodyPart) bpart;
//                String name = dbp.getName();
//                log.debug("Detected form parameter \"{}\".", name);
//                if (name.equals("file")) {
//                    file = bpart.getEntityAs(File.class);
//                } else {
//                    String value = dbp.getValue();
//                    if (name.equals("format") && !value.equals("auto")) {
//                        log.debug(" -- Expected format : {}", value);
//                        format = value;
//                    } else if (name.equals("url")) try {
//                        URI.create(value); // To throw 400 if malformed.
//                        location = IRI.create(value);
//                        log.debug(" -- Will load ontology from URL : {}", location);
//                    } catch (Exception ex) {
//                        log.error("Malformed IRI for " + value, ex);
//                        throw new WebApplicationException(ex, BAD_REQUEST);
//                    }
//                    else if (name.equals("library") && !"null".equals(value)) try {
//                        log.debug(" -- Library ID : {}", value);
//                        URI.create(value); // To throw 400 if malformed.
//                        library = IRI.create(value);
//                        log.debug(" ---- (is well-formed URI)");
//                    } catch (Exception ex) {
//                        log.error("Malformed IRI for " + value, ex);
//                        throw new WebApplicationException(ex, BAD_REQUEST);
//                    }
//                    else if (name.equals("stored") && !"null".equals(value)) {
//                        log.info("Request to manage ontology with key {}", value);
//                        keys.add(value);
//                    } else if (name.equals("scope")) {
//                        log.info("Request to append scope \"{}\".", value);
//                        if (toAppend == null) toAppend = new HashSet<String>();
//                        toAppend.add(value);
//                    }
//                }
//            }
//        }
        if (data.getFormFileParameterValues("file").length > 0) {
            file = data.getFormFileParameterValues("file")[0];
        }
        // else {
        if (data.getTextParameterValues("format").length > 0) {
            String value = data.getTextParameterValues("format")[0];
            if (!value.equals("auto")) {
                format = value;
            }
        }
        if (data.getTextParameterValues("url").length > 0) {
            String value = data.getTextParameterValues("url")[0];
            try {
                URI.create(value); // To throw 400 if malformed.
                location = IRI.create(value);
            } catch (Exception ex) {
                log.error("Malformed IRI for param url " + value, ex);
                throw new WebApplicationException(ex, BAD_REQUEST);
            }
        }
        if (data.getTextParameterValues("library").length > 0) {
            String value = data.getTextParameterValues("library")[0];
            try {
                URI.create(value); // To throw 400 if malformed.
                library = IRI.create(value);
            } catch (Exception ex) {
                log.error("Malformed IRI for param library " + value, ex);
                throw new WebApplicationException(ex, BAD_REQUEST);
            }
        }
        if (data.getTextParameterValues("stored").length > 0) {
            String value = data.getTextParameterValues("stored")[0];
            keys.add(value);
        }
        if (data.getTextParameterValues("scope").length > 0) {
            String value = data.getTextParameterValues("scope")[0];
            log.info("Request to append scope \"{}\".", value);
            if (toAppend == null) {
                toAppend = new HashSet<String>();
            }
            toAppend.add(value);
        }
        
        boolean fileOk = file != null;
        if (fileOk || location != null || library != null) { // File and location take precedence
            // Then add the file
            OntologyInputSource<?> src = null;
            if (fileOk) { // File first
                Collection<String> formats;
                if (format == null || "".equals(format.trim())) formats = OntologyUtils.getPreferredFormats();
                else formats = Collections.singleton(format);
                for (String f : formats)
                    try {
                        log.debug("Trying format {}.", f);
                        long b4buf = System.currentTimeMillis();
                        // Recreate the stream on each attempt
                        InputStream content = new BufferedInputStream(new ByteArrayInputStream(file.getContent()));
                        log.debug("Streams created in {} ms", System.currentTimeMillis() - b4buf);
                        log.debug("Creating ontology input source...");
                        b4buf = System.currentTimeMillis();
                        OWLOntologyID guessed = OWLUtils.guessOntologyID(content, Parser.getInstance(), f);
                        if (guessed != null && !guessed.isAnonymous()
                            && ontologyProvider.hasOntology(guessed)) {
                            rb = Response.status(Status.CONFLICT);
                            this.submitted = guessed;
                            if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
                                rb.entity(new Viewable("/imports/409", this));
                                rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=utf-8");
                            }
                        } else {
                            content = new BufferedInputStream(new ByteArrayInputStream(file.getContent()));
                            src = new GraphContentInputSource(content, f, ontologyProvider.getStore());
                        }
                        log.debug("Done in {} ms", System.currentTimeMillis() - b4buf);
                        log.info("SUCCESS parse with format {}.", f);
                        break;
                    } catch (OntologyLoadingException e) {
                        log.debug("FAILURE parse with format {}.", f);
                        continue;
                    } catch (IOException e) {
                        log.debug("FAILURE parse with format {} (I/O error).", f);
                        continue;
                    }
                log.debug("No more formats to try.");
            } else if (location != null) {
                try {
                    src = new RootOntologySource(location);
                } catch (Exception e) {
                    log.error("Failed to load ontology from " + location, e);
                    throw new WebApplicationException(e, BAD_REQUEST);
                }
            } else if (library != null) { // This comes last, since it will most likely have a value.
                try {
                    long beforeLib = System.currentTimeMillis();
                    log.debug("Creating library source for {}", library);
                    src = new LibrarySource(library, regMgr);
                    log.debug("Library source created in {} ms.", System.currentTimeMillis() - beforeLib);
                } catch (Exception e) {
                    log.error("Failed to load ontology library " + library, e);
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }
            } else {
                log.error("Bad request");
                log.error(" file is: {}", file);
                throw new WebApplicationException(BAD_REQUEST);
            }

            if (src != null) {
                log.debug("Adding ontology from input source {}", src);
                long b4add = System.currentTimeMillis();
                OWLOntologyID key = session.addOntology(src);
                if (key == null || key.isAnonymous()) throw new WebApplicationException(INTERNAL_SERVER_ERROR);
                // FIXME ugly but will have to do for the time being
                log.debug("Addition done in {} ms.", System.currentTimeMillis() - b4add);
                log.debug("Storage key : {}", key);
                String uri = // key.split("::")[1];
                OntologyUtils.encode(key);
                // uri = uri.substring((ontologyProvider.getGraphPrefix() + "::").length());
                if (uri != null && !uri.isEmpty()) rb = Response.seeOther(URI.create("/ontonet/session/"
                                                                                     + session.getID() + "/"
                                                                                     + uri));
                else rb = Response.seeOther(URI.create("/ontonet/session/" + session.getID()));
            } else if (rb == null) rb = Response.status(INTERNAL_SERVER_ERROR);
        }
        if (!keys.isEmpty()) {
            for (String key : keys)
                session.addOntology(new StoredOntologySource(OntologyUtils.decode(key)));
            rb = Response.seeOther(URI.create("/ontonet/session/" + session.getID()));
        } // Now check scopes
        if (toAppend != null
            && (!toAppend.isEmpty() || (toAppend.isEmpty() && !getAppendedScopes().isEmpty()))) {
            for (Scope sc : getAllScopes()) { // First remove appended scopes not in the list
                String scid = sc.getID();
                if (!toAppend.contains(scid) && getAppendedScopes().contains(scid)) {
                    session.detachScope(scid);
                    log.info("Removed scope \"{}\".", scid);
                }
            }
            for (String scid : toAppend) { // Then add all the scopes in the list
                if (!getAppendedScopes().contains(scid)) {
                    log.info("Appending scope \"{}\" to session \"{}\".", scid, session.getID());
                    session.attachScope(scid);
                    log.info("Appended scope \"{}\".", scid);
                }
            }
            rb = Response.seeOther(URI.create("/ontonet/session/" + session.getID()));
        }
        // else {
        // log.error("Nothing to do with session {}.", session.getID());
        // throw new WebApplicationException(BAD_REQUEST);
        // }
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//        addCORSOrigin(servletContext, rb, headers);
        log.info("POST ontology completed in {} ms.", System.currentTimeMillis() - before);
        return rb.build();
    }
}
