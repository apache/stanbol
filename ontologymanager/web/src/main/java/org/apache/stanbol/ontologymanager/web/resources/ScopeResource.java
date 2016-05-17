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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

import org.apache.clerezza.jaxrs.utils.form.FormFile;
import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.IrremovableOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.SetInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.StoredOntologySource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphSource;
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
 * The REST resource of an OntoNet {@link OntologyScope} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonet/ontology/{scopeid}")
public class ScopeResource extends AbstractOntologyAccessResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    @Reference
    protected ScopeManager onm;
    @Reference
    protected OntologyProvider<TcProvider> ontologyProvider;

    /*
     * Placeholder for the RegistryManager to be fetched from the servlet context.
     */
    @Reference
    protected RegistryManager regMgr;

    protected Scope scope;

    public ScopeResource() {
        super();
        // log.info("<init> with scope {}", scopeId);
        //
        // this.servletContext = servletContext;
        // this.onm = (ScopeManager) ContextHelper.getServiceFromContext(ScopeManager.class, servletContext);
        // this.regMgr = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
        // servletContext);
        // this.ontologyProvider = (OntologyProvider<TcProvider>) ContextHelper.getServiceFromContext(
        // OntologyProvider.class, servletContext);

        // if (scopeId == null || scopeId.isEmpty()) {
        // log.error("Missing path parameter scopeid={}", scopeId);
        // throw new WebApplicationException(NOT_FOUND);
        // }
        // scope = onm.getScope(scopeId);

        // // Skip null checks: the scope might be created with a PUT
        // if (scope == null) {
        // log.error("Scope {} not found", scopeId);
        // throw new WebApplicationException(NOT_FOUND);
        // }
    }

    @GET
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response asOntologyGraph(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {

        scope = onm.getScope(scopeid);

        if (scope == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        // Export to Clerezza ImmutableGraph, which can be rendered as JSON-LD.
        ResponseBuilder rb = Response.ok(scope.export(ImmutableGraph.class, merge, prefix));
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response asOntologyMixed(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        if (scope == null) return Response.status(NOT_FOUND).build();
        // Export smaller graphs to OWLOntology due to the more human-readable rendering.
        ResponseBuilder rb;
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        if (merge) rb = Response.ok(scope.export(ImmutableGraph.class, merge, prefix));
        else rb = Response.ok(scope.export(OWLOntology.class, merge, prefix));
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response asOntologyOWL(@PathParam("scopeid") String scopeid,
                                  @DefaultValue("false") @QueryParam("merge") boolean merge,
                                  @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        if (scope == null) return Response.status(NOT_FOUND).build();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        // Export to OWLOntology due to the more human-readable rendering.
        ResponseBuilder rb = Response.ok(scope.export(OWLOntology.class, merge, prefix));
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @DELETE
    public Response deregisterScope(@PathParam("scopeid") String scopeid,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers,
                                    @Context ServletContext servletContext) {
        scope = onm.getScope(scopeid);

        onm.deregisterScope(scope);
        scope = null;
        ResponseBuilder rb = Response.ok();
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<String> getCoreOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (OWLOntologyID id : scope.getCoreSpace().listManagedOntologies())
            result.add(OntologyUtils.encode(id));
        return result;
    }

    @GET
    @Path("/core")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getCoreSpaceGraph(@PathParam("scopeid") String scopeid,
                                      @DefaultValue("false") @QueryParam("merge") boolean merge,
                                      @Context UriInfo uriInfo,
                                      @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        OntologySpace space = scope.getCoreSpace();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        ImmutableGraph o = space.export(ImmutableGraph.class, merge, prefix);
        ResponseBuilder rb = Response.ok(o);
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/core")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response getCoreSpaceOWL(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        OntologySpace space = scope.getCoreSpace();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        OWLOntology o = space.export(OWLOntology.class, merge, prefix);
        ResponseBuilder rb = Response.ok(o);
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    private URI getCreatedResource(String ontologyIRI) {
        return URI.create("/" + ontologyIRI);
    }

    public SortedSet<String> getCustomOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (OWLOntologyID id : scope.getCustomSpace().listManagedOntologies())
            result.add(OntologyUtils.encode(id));
        return result;
    }

    @GET
    @Path("/custom")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getCustomSpaceGraph(@PathParam("scopeid") String scopeid,
                                        @DefaultValue("false") @QueryParam("merge") boolean merge,
                                        @Context UriInfo uriInfo,
                                        @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        OntologySpace space = scope.getCustomSpace();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        ImmutableGraph o = space.export(ImmutableGraph.class, merge, prefix);
        ResponseBuilder rb = Response.ok(o);
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/custom")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response getCustomSpaceOWL(@PathParam("scopeid") String scopeid,
                                      @DefaultValue("false") @QueryParam("merge") boolean merge,
                                      @Context UriInfo uriInfo,
                                      @Context HttpHeaders headers) {
        scope = onm.getScope(scopeid);

        OntologySpace space = scope.getCustomSpace();
        IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
        OWLOntology o = space.export(OWLOntology.class, merge, prefix);
        ResponseBuilder rb = Response.ok(o);
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@PathParam("scopeid") String scopeid, @Context HttpHeaders headers) {
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);

        if (scope == null) rb = Response.status(NOT_FOUND);
        else rb = Response.ok(new Viewable("index", this)); // TODO move to a dedicated class
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Set<Library> getLibraries() {
        return regMgr.getLibraries();
    }

    public SortedSet<String> getManageableOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        // for (String s : ontologyProvider.getPublicKeys()) {
        // // String s1 = s.split("::")[1];
        // if (s != null && !s.isEmpty()) result.add(s);
        // }
        for (OWLOntologyID id : ontologyProvider.listPrimaryKeys())
            result.add(OntologyUtils.encode(id));
        for (OWLOntologyID id : scope.getCoreSpace().listManagedOntologies())
            result.remove(OntologyUtils.encode(id));
        for (OWLOntologyID id : scope.getCustomSpace().listManagedOntologies())
            result.remove(OntologyUtils.encode(id));
        return result;
    }

    /*
     * Needed for freemarker
     */
    public Scope getScope() {
        return scope;
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        // enableCORS(servletContext, rb, headers, GET, POST, PUT, DELETE, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/core")
    public Response handleCorsPreflightCore(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        // enableCORS(servletContext, rb, headers, GET, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/custom")
    public Response handleCorsPreflightCustom(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        // enableCORS(servletContext, rb, headers, GET, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/{ontologyId:.+}")
    public Response handleCorsPreflightOntology(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        // enableCORS(servletContext, rb, headers, GET, DELETE, OPTIONS);
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
    @Path("/{ontologyId:.+}")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response managedOntologyGetGraph(@PathParam("scopeid") String scopeid,
                                            @PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        log.debug("Absolute URL Path {}", uriInfo.getRequestUri());
        log.debug("Ontology ID {}", ontologyId);
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);

        if (scope == null) rb = Response.status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
            ImmutableGraph o = null;
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            OntologySpace spc = scope.getCustomSpace();
            if (spc != null && spc.hasOntology(id)) {
                o = spc.getOntology(id, ImmutableGraph.class, merge, prefix);
            } else {
                spc = scope.getCoreSpace();
                if (spc != null && spc.hasOntology(id)) o = spc.getOntology(id, ImmutableGraph.class, merge, prefix);
            }
            if (o == null) rb = Response.status(NOT_FOUND);
            else rb = Response.ok(o);
        }

        // addCORSOrigin(servletContext, rb, headers);
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
    @Path("/{ontologyId:.+}")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response managedOntologyGetOWL(@PathParam("scopeid") String scopeid,
                                          @PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merge,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        log.debug("Absolute URL Path {}", uriInfo.getRequestUri());
        log.debug("Ontology ID {}", ontologyId);
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);

        if (scope == null) rb = Response.status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
            OWLOntology o = null;
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            OntologySpace spc = scope.getCustomSpace();
            if (spc != null && spc.hasOntology(id)) {
                o = spc.getOntology(id, OWLOntology.class, merge, prefix);
            } else {
                spc = scope.getCoreSpace();
                if (spc != null && spc.hasOntology(id)) o = spc.getOntology(id, OWLOntology.class, merge,
                    prefix);
            }
            if (o == null) rb = Response.status(NOT_FOUND);
            else rb = Response.ok(o);
        }
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response managedOntologyShow(@PathParam("scopeid") String scopeid,
                                        @PathParam("ontologyId") String ontologyId,
                                        @Context HttpHeaders headers) {
        ResponseBuilder rb;

        scope = onm.getScope(scopeid);
        if (scope == null) rb = Response.status(NOT_FOUND);
        else if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else if (!ontologyProvider.hasOntology(OntologyUtils.decode(ontologyId))) rb = Response
                .status(NOT_FOUND);
        else {
            IRI prefix = IRI.create(getPublicBaseUri() + "ontonet/ontology/");
            OWLOntology o = scope.getCustomSpace().getOntology(OntologyUtils.decode(ontologyId),
                OWLOntology.class, false, prefix);
            if (o == null) o = scope.getCoreSpace().getOntology(OntologyUtils.decode(ontologyId),
                OWLOntology.class, false, prefix);
            if (o == null) rb = Response.status(NOT_FOUND);
            else try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                o.getOWLOntologyManager().saveOntology(o, new ManchesterOWLSyntaxOntologyFormat(), out);
                rb = Response.ok(new Viewable("ontology",
                        new OntologyPrettyPrintResource(uriInfo, out, scope)));
            } catch (OWLOntologyStorageException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Unloads an ontology from an ontology scope.
     * 
     * @param scopeId
     * @param ontologyid
     * @param uriInfo
     * @param headers
     */
    @DELETE
    @Path("/{ontologyId:.+}")
    public Response managedOntologyUnload(@PathParam("scopeid") String scopeid,
                                          @PathParam("ontologyId") String ontologyId,
                                          @PathParam("scopeid") String scopeId,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);
        if (ontologyId != null && !ontologyId.trim().isEmpty()) {
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            OntologySpace cs = scope.getCustomSpace();
            if (!cs.hasOntology(id)) rb = Response.notModified(); // ontology not managed
            else try {
                onm.setScopeActive(scopeId, false);
                cs.removeOntology(id);
                rb = Response.ok();
            } catch (IrremovableOntologyException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (UnmodifiableOntologyCollectorException e) {
                throw new WebApplicationException(e, FORBIDDEN);
            } catch (OntologyCollectorModificationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            } finally {
                onm.setScopeActive(scopeId, true);
            }
        } else rb = Response.status(BAD_REQUEST); // null/blank ontology ID
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Tells the scope that it should manage the ontology obtained by parsing the supplied content.<br>
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
    public Response manageOntology(InputStream content,
                                   @PathParam("scopeid") String scopeid,
                                   @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);
        if (scope == null) rb = Response.status(NOT_FOUND); // Always check session first
        else try {
            MediaType mt = headers.getMediaType();
            log.debug("POST content claimed to be of type {}.", mt);
            OWLOntologyID key = scope.getCustomSpace().addOntology(
            /*
             * For the time being, REST services operate in-memory (i.e. no TcProvider is supplied to the
             * input source). This means that only the final processed graph is stored.
             * 
             * TODO : we might find a reason to change that in the future.
             */
            new GraphContentInputSource(content, mt.toString(), ontologyProvider.getStore()));
            if (key == null || key.isAnonymous()) {
                log.error("FAILED parse with media type {}.", mt);
                throw new WebApplicationException(INTERNAL_SERVER_ERROR);
            }
            // FIXME ugly but will have to do for the time being
            log.debug("SUCCESS parse with media type {}.", mt);
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
        }
        // addCORSOrigin(servletContext, rb, headers);
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
    public Response manageOntology(String iri,
                                   @PathParam("scopeid") String scopeid,
                                   @Context HttpHeaders headers) {
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);
        if (scope == null) rb = Response.status(NOT_FOUND);
        else try {
            OWLOntologyID key = scope.getCustomSpace().addOntology(new RootOntologySource(IRI.create(iri)));
            URI created = getCreatedResource(OntologyUtils.encode(key));
            rb = Response.created(created);
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(MultiPartBody data,
                                 @PathParam("scopeid") String scopeid,
                                 @Context HttpHeaders headers) {
        log.info(" post(MultiPartBody data) scope: {}", scopeid);
        ResponseBuilder rb;
        scope = onm.getScope(scopeid);

        // TODO remove and make sure it is set across the method
        rb = Response.status(BAD_REQUEST);

        IRI location = null, library = null;
        FormFile file = null; // If found, it takes precedence over location.
        String format = null;
        Set<String> keys = new HashSet<String>();

        // for (BodyPart bpart : data.getBodyParts()) {
        // log.debug("is a {}", bpart.getClass());
        // if (bpart instanceof FormDataBodyPart) {
        // FormDataBodyPart dbp = (FormDataBodyPart) bpart;
        // String name = dbp.getName();
        // if (name.equals("file")) file = bpart.getEntityAs(File.class);
        // else {
        // String value = dbp.getValue();
        // if (name.equals("format") && !value.equals("auto")) format = value;
        // else if (name.equals("url")) try {
        // URI.create(value); // To throw 400 if malformed.
        // location = IRI.create(value);
        // } catch (Exception ex) {
        // log.error("Malformed IRI for " + value, ex);
        // throw new WebApplicationException(ex, BAD_REQUEST);
        // }
        // else if (name.equals("library") && !"null".equals(value)) try {
        // URI.create(value); // To throw 400 if malformed.
        // library = IRI.create(value);
        // } catch (Exception ex) {
        // log.error("Malformed IRI for " + value, ex);
        // throw new WebApplicationException(ex, BAD_REQUEST);
        // }
        // else if (name.equals("stored") && !"null".equals(value)) {
        // log.info("Request to manage ontology with key {}", value);
        // keys.add(value);
        // }
        // }
        //
        // }
        // }

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

        log.debug("Parameters:");
        log.debug("file: {}", file);
        log.debug("url: {}", location);
        log.debug("format: {}", format);
        log.debug("keys: {}", keys);

        boolean fileOk = file != null;
        // if(fileOk && !(file.canRead() && file.exists())){
        // log.error("File is not accessible: {}", file);
        // throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        // }
        if (fileOk || location != null || library != null) { // File and location take precedence

            // src = new GraphContentInputSource(content, format, ontologyProvider.getStore());

            // Then add the file
            OntologyInputSource<?> src = null;
            if (fileOk) {

                /*
                 * Because the ontology provider's load method could fail after only one attempt without
                 * resetting the stream, we might have to do that ourselves.
                 */
                List<String> formats;
                if (format != null && !format.trim().isEmpty()) formats = Collections.singletonList(format);
                else // The RESTful API has its own list of preferred formats
                formats = Arrays.asList(RDF_XML, TURTLE, X_TURTLE, N3, N_TRIPLE, OWL_XML,
                        FUNCTIONAL_OWL, MANCHESTER_OWL, RDF_JSON);
                int unsupported = 0, failed = 0;
                Iterator<String> itf = formats.iterator();
                if (!itf.hasNext()) throw new OntologyLoadingException("No suitable format found or defined.");
                do {
                    String f = itf.next();
                    try {
                        // Re-instantiate the stream on every attempt
                        InputStream content = new ByteArrayInputStream(file.getContent());
                        // ClerezzaOWLUtils.guessOntologyID(new FileInputStream(file), Parser.getInstance(),
                        // f);
                        OWLOntologyID guessed = OWLUtils.guessOntologyID(content, Parser.getInstance(), f);
                        log.debug("guessed ontology id: {}", guessed);
                        if (guessed != null && !guessed.isAnonymous()
                            && ontologyProvider.hasOntology(guessed)) {
                            // rb = Response.status(Status.CONFLICT);
                            this.submitted = guessed;
                            if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
                                rb.entity(new Viewable("conflict.ftl", new ScopeResultData())); 
                                rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=utf-8");
                            }
                            break;
                        } else {
                            content = new ByteArrayInputStream(file.getContent());
                            log.debug("Recreated input stream for format {}", f);
                            src = new GraphContentInputSource(content, f, ontologyProvider.getStore());
                        }
                    } catch (UnsupportedFormatException e) {
                        log.warn(
                            "POST method failed for media type {}. This should not happen (should fail earlier)",
                            headers.getMediaType());
                        // rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
                        unsupported++;
                    } catch (IOException e) {
                        log.debug(">>> FAILURE format {} (I/O error)", f);
                        failed++;
                    } catch (Exception e) { // SAXParseException and others
                        log.debug(">>> FAILURE format {} (parse error)", f);
                        failed++;
                    }
                } while (src == null && itf.hasNext());
            }

            if (src != null) {
                OWLOntologyID key = scope.getCustomSpace().addOntology(src);
                if (key == null || key.isAnonymous()) throw new WebApplicationException(INTERNAL_SERVER_ERROR);
                // FIXME ugly but will have to do for the time being
                String uri = // key.split("::")[1];
                OntologyUtils.encode(key);
                // uri = uri.substring((ontologyProvider.getGraphPrefix() + "::").length());
                if (uri != null && !uri.isEmpty()) {
                    rb = Response.seeOther(URI.create("/ontonet/ontology/" + scope.getID() + "/" + uri));
                } else rb = Response.ok();
            } else if (rb == null) rb = Response.status(INTERNAL_SERVER_ERROR);
        }

        if (!keys.isEmpty()) {
            for (String key : keys)
                scope.getCustomSpace().addOntology(new StoredOntologySource(OntologyUtils.decode(key)));
            rb = Response.seeOther(URI.create("/ontonet/ontology/" + scope.getID()));
        }
        // else throw new WebApplicationException(BAD_REQUEST);
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // FIXME return an appropriate response e.g. 201
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * At least one between corereg and coreont must be present. Registry iris supersede ontology iris.
     * 
     * @param scopeid
     * @param coreRegistry
     *            a. If it is a well-formed IRI it supersedes <code>coreOntology</code>.
     * @param coreOntologies
     * @param customRegistry
     *            a. If it is a well-formed IRI it supersedes <code>customOntology</code>.
     * @param customOntologies
     * @param activate
     *            if true, the new scope will be activated upon creation.
     * @param uriInfo
     * @param headers
     * @return
     */
    @PUT
    @Consumes(MediaType.WILDCARD)
    public Response registerScope(@PathParam("scopeid") String scopeid,
                                  @QueryParam("corereg") final List<String> coreRegistries,
                                  @QueryParam("coreont") final List<String> coreOntologies,
                                  @DefaultValue("false") @QueryParam("activate") boolean activate,
                                  @Context HttpHeaders headers) {
        log.debug("Request URI {}", uriInfo.getRequestUri());

        scope = onm.getScope(scopeid);
        List<OntologyInputSource<?>> srcs = new ArrayList<OntologyInputSource<?>>(coreOntologies.size()
                                                                                  + coreRegistries.size());
        // First thing, check registry sources.
        if (coreRegistries != null) for (String reg : coreRegistries)
            if (reg != null && !reg.isEmpty()) try {
                // Library IDs are sanitized differently
                srcs.add(new LibrarySource(URIUtils.desanitize(IRI.create(reg)), regMgr));
            } catch (Exception e1) {
                throw new WebApplicationException(e1, BAD_REQUEST);
                // Bad or not supplied core registry, try the ontology.
            }

        // Then ontology sources
        if (coreOntologies != null) for (String ont : coreOntologies)
            if (ont != null && !ont.isEmpty()) try {
                srcs.add(new RootOntologySource(IRI.create(ont)));
            } catch (OWLOntologyCreationException e2) {
                // If this fails too, throw a bad request.
                throw new WebApplicationException(e2, BAD_REQUEST);
            }

        // Now the creation.
        try {
            // Expand core sources
            List<OntologyInputSource<?>> expanded = new ArrayList<OntologyInputSource<?>>();
            for (OntologyInputSource<?> coreSrc : srcs)
                if (coreSrc != null) {
                    if (coreSrc instanceof SetInputSource) {
                        for (Object o : ((SetInputSource<?>) coreSrc).getOntologies()) {
                            OntologyInputSource<?> src = null;
                            if (o instanceof OWLOntology) src = new RootOntologySource((OWLOntology) o);
                            else if (o instanceof Graph) src = new GraphSource(
                                    (Graph) o);
                            if (src != null) expanded.add(src);
                        }
                    } else expanded.add(coreSrc); // Must be denoting a single ontology
                }
            scope = onm.createOntologyScope(scopeid, expanded.toArray(new OntologyInputSource[0]));
            // Setup and register the scope. If no custom space was set, it will
            // still be open for modification.
            scope.setUp();
            onm.setScopeActive(scopeid, activate);
        } catch (DuplicateIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        } catch (Exception ex) {
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }

        ResponseBuilder rb = Response.created(uriInfo.getAbsolutePath());
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public class ScopeResultData extends ResultData {

        public OWLOntologyID getRepresentedOntologyKey() {
            log.info("getRepresentedOntologyKey {}",ScopeResource.this.getRepresentedOntologyKey());
            return ScopeResource.this.getRepresentedOntologyKey();
        }

        public String stringForm(OWLOntologyID ontologyID) {
            return OntologyUtils.encode(ontologyID);
        }
    }

}
