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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
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
import java.util.ArrayList;
import java.util.List;

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
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologySetInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        scope = onm.getScopeRegistry().getScope(scopeId);

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

        ScopeRegistry reg = onm.getScopeRegistry();
        reg.deregisterScope(scope);
        scope = null;
        ResponseBuilder rb = Response.ok();
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

        ScopeRegistry reg = onm.getScopeRegistry();
        OntologyScopeFactory f = onm.getOntologyScopeFactory();

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
                if (coreSrc instanceof OntologySetInputSource) {
                    for (OWLOntology o : ((OntologySetInputSource) coreSrc).getOntologies()) {
                        expanded.add(new RootOntologySource(o));
                    }
                } else expanded.add(coreSrc);
            }
            if (custSrc != null) {
                if (custSrc instanceof OntologySetInputSource) for (OWLOntology o : ((OntologySetInputSource) custSrc)
                        .getOntologies())
                    expanded.add(new RootOntologySource(o));
                else expanded.add(custSrc);
            }
            // Invoke the appropriate factory method depending on the
            // availability of a custom source.
            // scope = (custSrc != null) ? f.createOntologyScope(scopeid, coreSrc, custSrc) : f
            // .createOntologyScope(scopeid, coreSrc);
            scope = f.createOntologyScope(scopeid, expanded.toArray(new OntologyInputSource[0]));
            // Setup and register the scope. If no custom space was set, it will
            // still be open for modification.
            scope.setUp();
            reg.registerScope(scope);
            reg.setScopeActive(scopeid, activate);
        } catch (DuplicateIDException e) {
            throw new WebApplicationException(e, CONFLICT);
        } catch (Exception ex) {
            throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
        }

        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
