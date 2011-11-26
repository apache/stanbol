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

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyCollectorModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.web.util.OntologyRenderUtils;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
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
        if (temp.equals(scope.getCoreSpace().getID())) {
            return Response.ok(scope.getCoreSpace().asOWLOntology(merge)).build();
        } else if (temp.equals(scope.getCustomSpace().getID())) {
            return Response.ok(scope.getCustomSpace().asOWLOntology(merge)).build();
        }

        OWLOntology o = null;
        IRI ontologyIri = IRI.create(ontologyId);
        OntologySpace spc = scope.getCustomSpace();
        if (spc != null && spc.hasOntology(ontologyIri)) {
            o = spc.getOntology(ontologyIri, merge);
        } else {
            spc = scope.getCoreSpace();
            if (spc != null && spc.hasOntology(ontologyIri)) o = spc.getOntology(ontologyIri, merge);
        }
        if (o == null) return Response.status(NOT_FOUND).build();
        return Response.ok(o).build();
    }

    /**
     * Returns an RDF/XML representation of the ontology identified by logical IRI <code>ontologyid</code>, if
     * it is loaded within the scope <code>[baseUri]/scopeid</code>.
     * 
     * @param scopeid
     * @param ontologyid
     * @param uriInfo
     * @return, or a status 404 if either the scope is not registered or the ontology is not loaded within
     *          that scope.
     */
    // @GET
    // @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
    // KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getScopeOntology(@PathParam("scopeid") String scopeid,
                                     @PathParam("ontologyId") String ontologyid,
                                     @Context UriInfo uriInfo) {

        log.info("Caught request for ontology {} in scope {}", ontologyid, scopeid);

        if (!ontologyid.equals("all")) {

            // First of all, it could be a simple request for the space root!

            String absur = uriInfo.getAbsolutePath().toString();
            log.debug("Absolute URL Path {}", absur);
            log.debug("Ontology ID {}", ontologyid);
            URI uri = URI.create(absur.substring(0, absur.lastIndexOf(ontologyid) - 1));

            IRI sciri = IRI.create(uri);
            IRI ontiri = IRI.create(ontologyid);

            // TODO: hack (ma anche no)
            if (!ontiri.isAbsolute()) ontiri = IRI.create(absur);

            ScopeRegistry reg = onm.getScopeRegistry();
            OntologyScope scope = reg.getScope(scopeid);
            if (scope == null) return Response.status(NOT_FOUND).build();

            // First of all, it could be a simple request for the space root!
            String temp = scopeid + "/" + ontologyid;
            if (temp.equals(scope.getCoreSpace().getID())) {
                return Response.ok(scope.getCoreSpace().asOWLOntology()).build();
            } else if (temp.equals(scope.getCustomSpace().getID())) {
                return Response.ok(scope.getCustomSpace().asOWLOntology()).build();
                // } else if (scope.getSessionSpace(IRI.create(temp)) != null) {
                // return Response.ok(scope.getSessionSpace(IRI.create(temp)).asOWLOntology()).build();
            }

            /*
             * BEGIN debug code, uncomment only for local testing OWLOntology test = null, top = null; test =
             * scope.getCustomSpace().getOntology(ontiri); System.out.println("Ontology " + ontiri); for
             * (OWLImportsDeclaration imp : test.getImportsDeclarations()) System.out.println("\timports " +
             * imp.getIRI()); top = scope.getCoreSpace().getTopOntology();
             * System.out.println("Core root for scope " + scopeid); for (OWLImportsDeclaration imp :
             * top.getImportsDeclarations()) System.out.println("\timports " + imp.getIRI()); END debug code
             */

            OWLOntology ont = null;
            // By default, always try retrieving the ontology from the custom space
            // first.
            OntologySpace space = scope.getCustomSpace();
            if (space != null) ont = space.getOntology(ontiri);
            if (space == null || ont == null) {
                space = scope.getCoreSpace();
                if (space != null) ont = space.getOntology(ontiri);
            }

            if (ont == null) {
                OWLOntologyManager tmpmgr;
                OfflineConfiguration offline = (OfflineConfiguration) ContextHelper.getServiceFromContext(
                    OfflineConfiguration.class, servletContext);
                if (offline == null) throw new IllegalStateException(
                        "OfflineConfiguration missing in ServletContext");
                else tmpmgr = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                        .getOntologySourceLocations().toArray(new IRI[0]));

                final Set<OWLOntology> ontologies = scope.getCustomSpace().getOntologies(true);
                // scope.getSessionSpace(ontiri).getOntologies(true);

                OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                    @Override
                    public Set<OWLOntology> getOntologies() {
                        return ontologies;
                    }
                };
                OWLOntologyMerger merger = new OWLOntologyMerger(provider);
                try {
                    ont = merger.createMergedOntology(tmpmgr, ontiri);
                } catch (OWLOntologyCreationException e) {
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }

            }
            if (ont == null) {
                return Response.status(NOT_FOUND).build();
            }
            String res = null;
            try {
                res = OntologyRenderUtils.renderOntology(ont, new RDFXMLOntologyFormat(), sciri.toString(),
                    onm);
            } catch (OWLOntologyStorageException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
            return Response.ok(res).build();

        } else {
            ScopeRegistry reg = onm.getScopeRegistry();
            String scopeID = uriInfo.getAbsolutePath().toString();
            scopeID = scopeID.substring(0, scopeID.lastIndexOf("/"));
            OntologyScope scope = reg.getScope(scopeID);

            if (scope == null) return Response.status(404).build();

            final Set<OWLOntology> customOntologies = scope.getCustomSpace().getOntologies(true);

            final Set<OWLOntology> coreOntologies = scope.getCoreSpace().getOntologies(true);

            final Set<OntologySpace> sessionSpaces = scope.getSessionSpaces();

            // Creo un manager per gestire tutte le ontologie
            final OWLOntologyManager man = OWLManager.createOWLOntologyManager();

            // Creo un set con tutte le ontologie dello scope
            OWLOntologySetProvider provider = new OWLOntologySetProvider() {
                @Override
                public Set<OWLOntology> getOntologies() {
                    Set<OWLOntology> ontologies = new HashSet<OWLOntology>();

                    // Inserisco le core ontologies
                    for (OWLOntology ontology : coreOntologies) {
                        OWLOntology ont;
                        try {
                            ont = man.createOntology();
                            Set<OWLAxiom> axioms = ontology.getAxioms();
                            for (OWLAxiom axiom : axioms) {

                                if (!axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)
                                    && !axiom.isOfType(AxiomType.DATATYPE_DEFINITION)
                                    && !axiom.isOfType(AxiomType.DATA_PROPERTY_DOMAIN)
                                    && !axiom.isOfType(AxiomType.DATA_PROPERTY_RANGE)) {
                                    man.addAxiom(ont, axiom);
                                }
                            }
                            ontologies.add(ont);
                        } catch (OWLOntologyCreationException e) {
                            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                        }
                    }

                    // Inserisco le custom ontology
                    for (OWLOntology ontology : customOntologies) {

                        OWLOntology ont;
                        try {
                            ont = man.createOntology();
                            Set<OWLAxiom> axioms = ontology.getAxioms();
                            for (OWLAxiom axiom : axioms) {

                                if (!axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)
                                    && !axiom.isOfType(AxiomType.DATATYPE_DEFINITION)
                                    && !axiom.isOfType(AxiomType.DATA_PROPERTY_DOMAIN)
                                    && !axiom.isOfType(AxiomType.DATA_PROPERTY_RANGE)) {
                                    man.addAxiom(ont, axiom);
                                }
                            }
                            ontologies.add(ont);
                        } catch (OWLOntologyCreationException e) {
                            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                        }

                    }

                    // Inserisco le session ontologies;
                    for (OntologySpace ontologySpace : sessionSpaces) {
                        Set<OWLOntology> sessionOntologies = ontologySpace.getOntologies(true);
                        for (OWLOntology ontology : sessionOntologies) {

                            OWLOntology ont;
                            try {
                                ont = man.createOntology();
                                Set<OWLAxiom> axioms = ontology.getAxioms();
                                for (OWLAxiom axiom : axioms) {

                                    if (!axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
                                        man.addAxiom(ont, axiom);
                                    }
                                }

                                ontologies.add(ont);
                            } catch (OWLOntologyCreationException e) {
                                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                            }

                        }
                    }
                    return ontologies;
                }
            };

            // Faccio il merger delle ontolgoie
            OWLOntologyMerger merger = new OWLOntologyMerger(provider);
            OWLOntology ontology;
            try {
                ontology = merger
                        .createMergedOntology(man, IRI.create("http://kres.iks-project.eu/classify"));
            } catch (OWLOntologyCreationException ex) {
                throw new WebApplicationException(ex, INTERNAL_SERVER_ERROR);
            }

            return Response.ok(ontology).build();
        }

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
    public void unloadOntology(@PathParam("scopeid") String scopeId,
                               @PathParam("uri") String ontologyid,
                               @Context UriInfo uriInfo,
                               @Context HttpHeaders headers) {

        if (ontologyid != null && !ontologyid.equals("")) {
            String scopeURI = uriInfo.getAbsolutePath().toString().replace(ontologyid, "");
            IRI scopeIri = IRI.create(uriInfo.getBaseUri() + "ontology/" + scopeId);

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
    }

}
