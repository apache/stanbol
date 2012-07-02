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

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
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

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.SetInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.apache.stanbol.ontologymanager.web.util.OntologyPrettyPrintResource;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * The REST resource of an OntoNet {@link OntologyScope} whose identifier is known.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/ontology/{scopeid}")
public class ScopeResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    /*
     * Placeholder for the RegistryManager to be fetched from the servlet context.
     */
    protected RegistryManager regMgr;

    protected OntologyScope scope;

    public ScopeResource(@PathParam(value = "scopeid") String scopeId, @Context ServletContext servletContext) {
        super();
        log.info("<init> with scope {}", scopeId);

        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        this.regMgr = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
            servletContext);

        if (scopeId == null || scopeId.isEmpty()) {
            log.error("Missing path parameter scopeid={}", scopeId);
            throw new WebApplicationException(NOT_FOUND);
        }
        scope = onm.getScope(scopeId);

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
        if (scope == null) return Response.status(NOT_FOUND).build();
        // Export to Clerezza Graph, which can be rendered as JSON-LD.
        ResponseBuilder rb = Response.ok(scope.export(Graph.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE})
    public Response asOntologyMixed(@PathParam("scopeid") String scopeid,
                                    @DefaultValue("false") @QueryParam("merge") boolean merge,
                                    @Context HttpHeaders headers) {
        if (scope == null) return Response.status(NOT_FOUND).build();
        // Export smaller graphs to OWLOntology due to the more human-readable rendering.
        ResponseBuilder rb;
        if (merge) rb = Response.ok(scope.export(Graph.class, merge));
        else rb = Response.ok(scope.export(OWLOntology.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces(value = {MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response asOntologyOWL(@PathParam("scopeid") String scopeid,
                                  @DefaultValue("false") @QueryParam("merge") boolean merge,
                                  @Context HttpHeaders headers) {
        if (scope == null) return Response.status(NOT_FOUND).build();
        // Export to OWLOntology due to the more human-readable rendering.
        ResponseBuilder rb = Response.ok(scope.export(OWLOntology.class, merge));
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @DELETE
    public Response deregisterScope(@PathParam("scopeid") String scopeid,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers,
                                    @Context ServletContext servletContext) {
        onm.deregisterScope(scope);
        scope = null;
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<String> getCoreOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (IRI iri : scope.getCoreSpace().listManagedOntologies())
            result.add(iri.toString());
        return result;
    }

    public SortedSet<String> getCustomOntologies() {
        SortedSet<String> result = new TreeSet<String>();
        for (IRI iri : scope.getCustomSpace().listManagedOntologies())
            result.add(iri.toString());
        return result;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (scope == null) rb = Response.status(NOT_FOUND);
        else rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Set<Library> getLibraries() {
        return regMgr.getLibraries();
    }

    /*
     * Needed for freemarker
     */
    public OntologyScope getScope() {
        return scope;
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers, GET, POST, PUT, DELETE, OPTIONS);
        return rb.build();
    }

    @OPTIONS
    @Path("/{ontologyId:.+}")
    public Response handleCorsPreflightOntology(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers, GET, DELETE, OPTIONS);
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
    public Response managedOntologyGetGraph(@PathParam("ontologyId") String ontologyId,
                                            @DefaultValue("false") @QueryParam("merge") boolean merge,
                                            @Context UriInfo uriInfo,
                                            @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (scope == null) rb = Response.status(NOT_FOUND);

        else {
            // First of all, it could be a simple request for the space root!

            String absur = uriInfo.getRequestUri().toString();
            log.debug("Absolute URL Path {}", absur);
            log.debug("Ontology ID {}", ontologyId);

            IRI ontiri = IRI.create(ontologyId);

            // TODO: hack (ma anche no)
            if (!ontiri.isAbsolute()) ontiri = IRI.create(absur);

            // First of all, it could be a simple request for the space root!
            String temp = scope.getID() + "/" + ontologyId;
            OntologySpace space = scope.getCoreSpace();
            if (temp.equals(space.getID())) rb = Response.ok(space.export(Graph.class, merge));
            else {
                space = scope.getCustomSpace();
                if (temp.equals(space.getID())) rb = Response.ok(space.export(Graph.class, merge));
                else {
                    Graph o = null;
                    IRI ontologyIri = IRI.create(ontologyId);
                    OntologySpace spc = scope.getCustomSpace();
                    if (spc != null && spc.hasOntology(ontologyIri)) {
                        // o = spc.getOntology(ontologyIri, merge);
                        o = spc.getOntology(ontologyIri, Graph.class, merge);
                    } else {
                        spc = scope.getCoreSpace();
                        if (spc != null && spc.hasOntology(ontologyIri))
                        // o = spc.getOntology(ontologyIri, merge);
                        o = spc.getOntology(ontologyIri, Graph.class, merge);
                    }
                    if (o == null) return Response.status(NOT_FOUND).build();
                    else rb = Response.ok(o);
                }
            }
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
    @Path("/{ontologyId:.+}")
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response managedOntologyGetOWL(@PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merge,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {

        ResponseBuilder rb;
        if (scope == null) rb = Response.status(NOT_FOUND);

        else {
            // First of all, it could be a simple request for the space root!

            String absur = uriInfo.getRequestUri().toString();
            log.debug("Absolute URL Path {}", absur);
            log.debug("Ontology ID {}", ontologyId);

            IRI ontiri = IRI.create(ontologyId);

            // TODO: hack (ma anche no)
            if (!ontiri.isAbsolute()) ontiri = IRI.create(absur);

            // First of all, it could be a simple request for the space root!
            String temp = scope.getID() + "/" + ontologyId;
            OntologySpace space = scope.getCoreSpace();
            if (temp.equals(space.getID())) rb = Response.ok(space.export(OWLOntology.class, merge));
            else {
                space = scope.getCustomSpace();
                if (temp.equals(space.getID())) rb = Response.ok(space.export(OWLOntology.class, merge));
                else {
                    OWLOntology o = null;
                    IRI ontologyIri = IRI.create(ontologyId);
                    OntologySpace spc = scope.getCustomSpace();
                    if (spc != null && spc.hasOntology(ontologyIri)) {
                        o = spc.getOntology(ontologyIri, OWLOntology.class, merge);
                    } else {
                        spc = scope.getCoreSpace();
                        if (spc != null && spc.hasOntology(ontologyIri)) o = spc.getOntology(ontologyIri,
                            OWLOntology.class, merge);
                    }
                    if (o == null) return Response.status(NOT_FOUND).build();
                    else rb = Response.ok(o);
                }
            }
        }

        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    // @GET
    // @Path("/{ontologyId:.+}")
    // @Produces(TEXT_HTML)
    public Response managedOntologyShow(@PathParam("ontologyId") String ontologyId,
                                        @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (scope == null) rb = Response.status(NOT_FOUND);
        else if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            OWLOntology o = scope.getCustomSpace().getOntology(IRI.create(ontologyId), OWLOntology.class,
                false);
            if (o == null) o = scope.getCoreSpace().getOntology(IRI.create(ontologyId), OWLOntology.class,
                false);
            if (o == null) rb = Response.status(NOT_FOUND);
            else try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                o.getOWLOntologyManager().saveOntology(o, new TurtleOntologyFormat(), out);
                rb = Response.ok(new Viewable("ontology", new OntologyPrettyPrintResource(servletContext,
                        uriInfo, out)));
            } catch (OWLOntologyStorageException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        }
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
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
    public Response managedOntologyUnload(@PathParam("uri") String ontologyid,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {

        if (ontologyid != null && !ontologyid.equals("")) {
            IRI ontIri = IRI.create(ontologyid);
            String scopeId = scope.getID();
            OntologySpace cs = scope.getCustomSpace();
            if (cs.hasOntology(ontIri)) {
                try {
                    onm.setScopeActive(scopeId, false);
                    cs.removeOntology(ontIri);
                    onm.setScopeActive(scopeId, true);
                } catch (OntologyCollectorModificationException e) {
                    onm.setScopeActive(scopeId, true);
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }
            }
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
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
    public Response manageOntology(InputStream content, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        if (scope == null) return Response.status(NOT_FOUND).build();
        try {
            scope.getCustomSpace().addOntology(
            /*
             * For the time being, REST services operate in-memory (i.e. no TcProvider is supplied to the
             * input source). This means that only the final processed graph is stored.
             * 
             * TODO : we might find a reason to change that in the future.
             */
            new GraphContentInputSource(content, headers.getMediaType().toString())
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
        if (scope == null) return Response.status(NOT_FOUND).build();
        try {
            scope.getCustomSpace().addOntology(new RootOntologyIRISource(IRI.create(iri)));
        } catch (UnmodifiableOntologyCollectorException e) {
            throw new WebApplicationException(e, FORBIDDEN);
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(FormDataMultiPart data, @Context HttpHeaders headers) {
        log.debug(" post(FormDataMultiPart data)");
        ResponseBuilder rb;

        IRI location = null, library = null;
        File file = null; // If found, it takes precedence over location.
        String format = null;
        for (BodyPart bpart : data.getBodyParts()) {
            log.debug("is a {}", bpart.getClass());
            if (bpart instanceof FormDataBodyPart) {
                FormDataBodyPart dbp = (FormDataBodyPart) bpart;
                String name = dbp.getName();
                if (name.equals("file")) file = bpart.getEntityAs(File.class);
                else if (name.equals("format") && !dbp.getValue().equals("auto")) format = dbp.getValue();
                else if (name.equals("url")) try {
                    URI.create(dbp.getValue()); // To throw 400 if malformed.
                    location = IRI.create(dbp.getValue());
                } catch (Exception ex) {
                    log.error("Malformed IRI for " + dbp.getValue(), ex);
                    throw new WebApplicationException(ex, BAD_REQUEST);
                }
                else if (name.equals("library") && !"null".equals(dbp.getValue())) try {
                    URI.create(dbp.getValue()); // To throw 400 if malformed.
                    library = IRI.create(dbp.getValue());
                } catch (Exception ex) {
                    log.error("Malformed IRI for " + dbp.getValue(), ex);
                    throw new WebApplicationException(ex, BAD_REQUEST);
                }

            }
        }
        boolean fileOk = file != null && file.canRead() && file.exists();
        if (fileOk || location != null || library != null) { // File and location take precedence
            // Then add the file
            OntologyInputSource<?,?> src = null;
            if (fileOk) {
                try {

                    InputStream content = new FileInputStream(file);
                    src = new GraphContentInputSource(content, format);
                } catch (UnsupportedFormatException e) {
                    log.warn(
                        "POST method failed for media type {}. This should not happen (should fail earlier)",
                        headers.getMediaType());
                    rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
                } catch (Exception e) {
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }
            } else if (location != null) {
                try {
                    src = new RootOntologyIRISource(location);
                } catch (Exception e) {
                    log.error("Failed to load ontology from " + location, e);
                    throw new WebApplicationException(e, BAD_REQUEST);
                }
            } else if (library != null) { // This comes last, since it will most likely have a value.
                try {
                    src = new LibrarySource(library, regMgr);
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
                String key = scope.getCustomSpace().addOntology(src);
                if (key == null || key.isEmpty()) throw new WebApplicationException(INTERNAL_SERVER_ERROR);
                // FIXME ugly but will have to do for the time being
                String uri = key.split("::")[1];
                if (uri != null && !uri.isEmpty()) {
                    rb = Response.seeOther(URI.create("/ontonet/ontology/" + scope.getID() + "/" + uri));
                } else rb = Response.ok();
            } else rb = Response.status(INTERNAL_SERVER_ERROR);
        } else throw new WebApplicationException(BAD_REQUEST);
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // FIXME return an appropriate response e.g. 303
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * At least one between corereg and coreont must be present. Registry iris supersede ontology iris.
     * 
     * @param scopeid
     * @param coreRegistry
     *            a. If it is a well-formed IRI it supersedes <code>coreOntology</code>.
     * @param coreOntology
     * @param customRegistry
     *            a. If it is a well-formed IRI it supersedes <code>customOntology</code>.
     * @param customOntology
     * @param activate
     *            if true, the new scope will be activated upon creation.
     * @param uriInfo
     * @param headers
     * @return
     */
    @PUT
    @Consumes(MediaType.WILDCARD)
    public Response registerScope(@PathParam("scopeid") String scopeid,
                                  @QueryParam("corereg") String coreRegistry,
                                  @QueryParam("coreont") String coreOntology,
                                  @QueryParam("customreg") String customRegistry,
                                  @QueryParam("customont") String customOntology,
                                  @DefaultValue("false") @QueryParam("activate") boolean activate,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers,
                                  @Context ServletContext servletContext) {
        log.debug("Request URI {}", uriInfo.getRequestUri());

        OntologyScope scope;
        OntologyInputSource<?,?> coreSrc = null, custSrc = null;

        // First thing, check the core source.
        if (coreRegistry != null && !coreRegistry.isEmpty()) try {
            coreSrc = new LibrarySource(IRI.create(coreRegistry.replace("%23", "#")), regMgr);
        } catch (Exception e1) {
            throw new WebApplicationException(e1, BAD_REQUEST);
            // Bad or not supplied core registry, try the ontology.
        }
        else if (coreOntology != null && !coreOntology.isEmpty()) try {
            coreSrc = new RootOntologyIRISource(IRI.create(coreOntology));
        } catch (Exception e2) {
            // If this fails too, throw a bad request.
            throw new WebApplicationException(e2, BAD_REQUEST);
        }

        // Don't bother if no custom was supplied at all...
        if (customRegistry != null && !customRegistry.isEmpty())
        // ...but if it was, be prepared to throw exceptions.
        try {
            coreSrc = new LibrarySource(IRI.create(customRegistry.replace("%23", "#")), regMgr);
        } catch (Exception e1) {
            throw new WebApplicationException(e1, BAD_REQUEST);
            // Bad or not supplied custom registry, try the ontology.
        }
        if (customOntology != null && !customOntology.isEmpty()) try {
            custSrc = new RootOntologyIRISource(IRI.create(customOntology));
        } catch (Exception e2) {
            // If this fails too, throw a bad request.
            throw new WebApplicationException(e2, BAD_REQUEST);
        }

        // Now the creation.
        try {
            // Expand core sources
            List<OntologyInputSource<?,?>> expanded = new ArrayList<OntologyInputSource<?,?>>();
            if (coreSrc != null) {
                if (coreSrc instanceof SetInputSource) {
                    for (Object o : ((SetInputSource<?>) coreSrc).getOntologies()) {
                        OntologyInputSource<?,?> src = null;
                        if (o instanceof OWLOntology) src = new RootOntologySource((OWLOntology) o);
                        else if (o instanceof TripleCollection) src = new GraphSource((TripleCollection) o);
                        if (src != null) expanded.add(src);
                    }
                } else expanded.add(coreSrc); // Must be denoting a single ontology
            }
            if (custSrc != null) {
                if (custSrc instanceof SetInputSource) {
                    for (Object o : ((SetInputSource<?>) custSrc).getOntologies()) {
                        OntologyInputSource<?,?> src = null;
                        if (o instanceof OWLOntology) src = new RootOntologySource((OWLOntology) o);
                        else if (o instanceof TripleCollection) src = new GraphSource((TripleCollection) o);
                        if (src != null) expanded.add(src);
                    }
                } else expanded.add(custSrc); // Must be denoting a single ontology
            }
            // Invoke the appropriate factory method depending on the
            // availability of a custom source.
            // scope = (custSrc != null) ? f.createOntologyScope(scopeid, coreSrc, custSrc) : f
            // .createOntologyScope(scopeid, coreSrc);
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
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
