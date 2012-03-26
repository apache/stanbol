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
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontonet/{ontologyId:.+}")
public class StoredOntologyResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected OntologyProvider<?> ontologyProvider;

    /*
     * TODO before implementing removal, we need OWL dependency checks.
     */
    // @DELETE
    public Response remove(@PathParam("ontologyId") String ontologyid, @Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected RegistryManager registryManager;

    public StoredOntologyResource(@PathParam(value = "ontologyId") String ontologyId,
                                  @Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.ontologyProvider = (OntologyProvider<?>) ContextHelper.getServiceFromContext(
            OntologyProvider.class, servletContext);
        this.registryManager = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
            servletContext);
    }

    @GET
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getManagedGraph(@PathParam("ontologyId") String ontologyId,
                                    @DefaultValue("false") @QueryParam("merge") boolean merged,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {
        if (ontologyId == null) return Response.status(Status.BAD_REQUEST).build();
        IRI iri = IRI.create(ontologyId);
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        /*
         * Export directly to MGraph since the OWLOntologyWriter uses (de-)serializing converters for the
         * other formats.
         * 
         * Use oTemp for the "real" graph and o for the graph that will be exported. This is due to the fact
         * that in o we want to change import statements, but we do not want these changes to be stored
         * permanently.
         */
        MGraph o = null, oTemp = null;
        try {
            oTemp = ontologyProvider.getStoredOntology(iri, MGraph.class, merged);
        } catch (Exception ex) {
            log.warn("Retrieval of ontology with ID " + iri + " failed.", ex);
        }

        if (oTemp == null) {
            log.debug("Ontology {} missing from provider. Trying libraries...", iri);
            // See if we can touch a library. TODO: replace with event model on the ontology provider.
            int minSize = -1;
            IRI smallest = null;
            for (Library lib : registryManager.getLibraries(iri)) {
                int size = lib.getChildren().length;
                if (minSize < 1 || size < minSize) {
                    smallest = lib.getIRI();
                    minSize = size;
                }
            }
            if (smallest != null) {
                log.debug("Selected library for ontology {} is {} .", iri, smallest);
                try {
                    oTemp = registryManager.getLibrary(smallest).getOntology(iri, MGraph.class);
                } catch (RegistryContentException e) {
                    log.warn("The content of library " + smallest + " could not be accessed.", e);
                }
            }
        }

        if (oTemp != null) o = new IndexedMGraph(oTemp);

        if (o == null) {
            log.debug("Ontology {} not found in any ontology provider or library.", iri);
            return Response.status(NOT_FOUND).build();
        }

        log.debug("Retrieved ontology {} .", iri);

        // Rewrite imports
        String uri = uriInfo.getRequestUri().toString();
        URI base = URI.create(uri.substring(0, uri.lastIndexOf(ontologyId) - 1));

        // Rewrite import statements
        /*
         * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
         * going upOne() add "session" or "ontology" if needed).
         */
        Iterator<Triple> imports = o.filter(null, OWL.imports, null);
        Set<Triple> oldImports = new HashSet<Triple>();
        while (imports.hasNext())
            oldImports.add(imports.next());
        for (Triple t : oldImports) {
            // construct new statement
            String s = ((UriRef) t.getObject()).getUnicodeString();
            if (s.contains("::")) s = s.substring(s.indexOf("::") + 2, s.length());
            UriRef target = new UriRef(base + "/" + s);
            o.add(new TripleImpl(t.getSubject(), OWL.imports, target));
            // remove old statement
            o.remove(t);
        }

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
    @Produces(value = {RDF_XML, TURTLE, X_TURTLE, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, TEXT_PLAIN})
    public Response getManagedOntology(@PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("merge") boolean merged,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        if (ontologyId == null) return Response.status(Status.BAD_REQUEST).build();
        IRI iri = IRI.create(ontologyId);
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        // TODO be selective: if the ontology is small enough, use OWLOntology otherwise export to Graph.
        OWLOntology o = null;
        try {
            o = (OWLOntology) ontologyProvider.getStoredOntology(iri, OWLOntology.class, merged);
        } catch (Exception ex) {
            log.warn("Retrieval of ontology with ID " + iri + " failed.", ex);
        }

        if (o == null) {
            log.debug("Ontology {} missing from provider. Trying libraries...", iri);
            // See if we can touch a library. TODO: replace with event model on the ontology provider.
            int minSize = -1;
            IRI smallest = null;
            for (Library lib : registryManager.getLibraries(iri)) {
                int size = lib.getChildren().length;
                if (minSize < 1 || size < minSize) {
                    smallest = lib.getIRI();
                    minSize = size;
                }
            }
            if (smallest != null) {
                log.debug("Selected library for ontology {} is {} .", iri, smallest);
                try {
                    o = registryManager.getLibrary(smallest).getOntology(iri, OWLOntology.class);
                } catch (RegistryContentException e) {
                    log.warn("The content of library " + smallest + " could not be accessed.", e);
                }
            }
        }

        if (o == null) {
            log.debug("Ontology {} not found in any ontology provider or library.", iri);
            return Response.status(NOT_FOUND).build();
        }

        log.debug("Retrieved ontology {} .", iri);

        // Rewrite imports
        String uri = uriInfo.getRequestUri().toString();
        URI base = URI.create(uri.substring(0, uri.lastIndexOf(ontologyId) - 1));

        // Rewrite import statements
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        /*
         * TODO manage import rewrites better once the container ID is fully configurable (i.e. instead of
         * going upOne() add "session" or "ontology" if needed).
         */
        for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
            changes.add(new RemoveImport(o, oldImp));
            String s = oldImp.getIRI().toString();
            if (s.contains("::")) s = s.substring(s.indexOf("::") + 2, s.length());
            IRI target = IRI.create(base + "/" + s);
            changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
        }
        o.getOWLOntologyManager().applyChanges(changes);

        ResponseBuilder rb = Response.ok(o);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();

    }

}
