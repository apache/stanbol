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

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resource represents ontologies loaded within a scope.
 * 
 * @author alexdma
 * 
 */
@Path("/ontonet/ontology/{scopeid}/{ontologyId:.+}")
public class ScopeOntologyResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected OntologyScope scope;

    public ScopeOntologyResource(@PathParam(value = "scopeid") String scopeId,
                                 @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        scope = onm.getScopeRegistry().getScope(scopeId);
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
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getManagedOntology(@PathParam("scopeid") String scopeId,
                                       @PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("merge") boolean merge,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        if (scope == null) return Response.status(NOT_FOUND).build();

        // First of all, it could be a simple request for the space root!

        String absur = uriInfo.getRequestUri().toString();
        log.debug("Absolute URL Path {}", absur);
        log.debug("Ontology ID {}", ontologyId);

        IRI ontiri = IRI.create(ontologyId);

        // TODO: hack (ma anche no)
        if (!ontiri.isAbsolute()) ontiri = IRI.create(absur);

        if (scope == null) return Response.status(NOT_FOUND).build();

        // First of all, it could be a simple request for the space root!
        String temp = scopeId + "/" + ontologyId;
        OntologySpace space = scope.getCoreSpace();
        if (temp.equals(space.getID())) return Response.ok(space.export(Graph.class, merge)).build();
        else {
            space = scope.getCustomSpace();
            if (temp.equals(space.getID())) return Response.ok(space.export(Graph.class, merge)).build();
        }

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
     * Unloads an ontology from an ontology scope.
     * 
     * @param scopeId
     * @param ontologyid
     * @param uriInfo
     * @param headers
     */
    @DELETE
    public Response unloadOntology(@PathParam("scopeid") String scopeId,
                                   @PathParam("uri") String ontologyid,
                                   @Context UriInfo uriInfo,
                                   @Context HttpHeaders headers) {

        if (ontologyid != null && !ontologyid.equals("")) {
            // String scopeURI = uriInfo.getAbsolutePath().toString().replace(ontologyid, "");
            // IRI scopeIri = IRI.create(uriInfo.getBaseUri() + "ontology/" + scopeId);
            IRI ontIri = IRI.create(ontologyid);
            ScopeRegistry reg = onm.getScopeRegistry();
            OntologyScope scope = reg.getScope(scopeId);
            OntologySpace cs = scope.getCustomSpace();
            if (cs.hasOntology(ontIri)) {
                try {
                    reg.setScopeActive(scopeId, false);
                    cs.removeOntology(ontIri);
                    reg.setScopeActive(scopeId, true);
                } catch (OntologyCollectorModificationException e) {
                    reg.setScopeActive(scopeId, true);
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }
            }
        }
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
