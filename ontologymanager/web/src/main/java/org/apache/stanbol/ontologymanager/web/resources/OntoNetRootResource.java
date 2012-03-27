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
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE_TYPE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.web.util.OntologyPrettyPrintResource;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * Provides the basic HTTP methods for storing and managing ontologies regardless of them belonging to a
 * specific network, scope or session.
 * 
 * @author anuzzolese, alexdma
 * 
 */

@Path("/ontonet")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class OntoNetRootResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected OntologyProvider<?> ontologyProvider;

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    protected RegistryManager registryManager;

    public OntoNetRootResource(@Context ServletContext servletContext) {
        super();
        this.servletContext = servletContext;
        this.ontologyProvider = (OntologyProvider<?>) ContextHelper.getServiceFromContext(
            OntologyProvider.class, servletContext);
        this.registryManager = (RegistryManager) ContextHelper.getServiceFromContext(RegistryManager.class,
            servletContext);
    }

    /*
     * TODO before implementing removal, we need OWL dependency checks. Also, this is quite a strong method
     * and would be best implemented with RESTful authentication.
     */
    // @DELETE
    public Response clear(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * TODO before implementing removal, we need OWL dependency checks.
     */
    // @DELETE
    // @Path("/{ontologyId:.+}")
    public Response deleteOntology(@PathParam("ontologyId") String ontologyid, @Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    private MGraph getGraph(String ontologyId, boolean merged) {
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
            return null;
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
        return o;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getManagedGraph(@PathParam("ontologyId") String ontologyId,
                                    @DefaultValue("false") @QueryParam("merge") boolean merged,
                                    @Context UriInfo uriInfo,
                                    @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            TripleCollection o = getGraph(ontologyId, merged);
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
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
    public Response getManagedOntology(@PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("merge") boolean merged,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            OWLOntology o = getOntology(ontologyId, merged);
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Set<String> getOntologies() {
        Set<String> filtered = new HashSet<String>();
        for (String s : ontologyProvider.getKeys()) {
            String s1 = s.split("::")[1];
            if (s1 != null && !s1.isEmpty()) filtered.add(s1);
        }
        return filtered;
    }

    private OWLOntology getOntology(String ontologyId, boolean merge) {
        IRI iri = IRI.create(ontologyId);
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        // TODO be selective: if the ontology is small enough, use OWLOntology otherwise export to Graph.
        OWLOntology o = null;
        try {
            o = (OWLOntology) ontologyProvider.getStoredOntology(iri, OWLOntology.class, merge);
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
            return null;
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
        return o;
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(FormDataMultiPart data, @Context HttpHeaders headers) {
        log.debug(" post(FormDataMultiPart data)");
        ResponseBuilder rb;

        IRI location = null;
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
            }
        }
        // Then add the file
        String key = null;
        if (file != null && file.canRead() && file.exists()) {
            try {
                InputStream content = new FileInputStream(file);
                key = ontologyProvider.loadInStore(content, format, null, true);
            } catch (UnsupportedFormatException e) {
                log.warn(
                    "POST method failed for media type {}. This should not happen (should fail earlier)",
                    headers.getMediaType());
                rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
            } catch (IOException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else if (location != null) {
            try {
                key = ontologyProvider.loadInStore(location, null, null, true);
            } catch (Exception e) {
                log.error("Failed to load ontology from " + location, e);
                throw new WebApplicationException(e, BAD_REQUEST);
            }
        } else {
            log.error("Bad request");
            log.error(" file is: {}", file);
            throw new WebApplicationException(BAD_REQUEST);
        }

        if (key != null && !key.isEmpty()) {
            // FIXME ugly but will have to do for the time being
            String uri = key.split("::")[1];
            if (uri != null && !uri.isEmpty()) rb = Response.seeOther(URI.create("/ontonet/" + uri));
            else rb = Response.ok();
        } else rb = Response.status(Status.INTERNAL_SERVER_ERROR);

        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // FIXME return an appropriate response e.g. 303
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response showOntology(@PathParam("ontologyId") String ontologyId, @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) rb = Response.status(BAD_REQUEST);
        else {
            OWLOntology o = getOntology(ontologyId, false);
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
     * POSTs an ontology content as application/x-www-form-urlencoded
     * 
     * @param content
     * @param headers
     * @return
     */
    @POST
    @Consumes(value = {RDF_XML, TURTLE, X_TURTLE, N3, N_TRIPLE, OWL_XML, FUNCTIONAL_OWL, MANCHESTER_OWL,
                       RDF_JSON})
    public Response storeOntology(InputStream content, @Context HttpHeaders headers) {
        long before = System.currentTimeMillis();
        ResponseBuilder rb;

        MediaType mt = headers.getMediaType();
        if (RDF_XML_TYPE.equals(mt) || TURTLE_TYPE.equals(mt) || X_TURTLE_TYPE.equals(mt)
            || N3_TYPE.equals(mt) || N_TRIPLE_TYPE.equals(mt) || RDF_JSON_TYPE.equals(mt)) {
            String key = null;
            try {
                key = ontologyProvider.loadInStore(content, headers.getMediaType().toString(), null, true);
                rb = Response.ok();
            } catch (UnsupportedFormatException e) {
                log.warn(
                    "POST method failed for media type {}. This should not happen (should fail earlier)",
                    headers.getMediaType());
                rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
            } catch (IOException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
            // An exception should have been thrown earlier, but just in case.
            if (key == null || key.isEmpty()) rb = Response.status(Status.INTERNAL_SERVER_ERROR);
        } else if (OWL_XML_TYPE.equals(mt) || FUNCTIONAL_OWL_TYPE.equals(mt)
                   || MANCHESTER_OWL_TYPE.equals(mt)) {
            try {
                OntologyInputSource<OWLOntology,OWLOntologyManager> src = new OntologyContentInputSource(
                        content);
                ontologyProvider.loadInStore(src.getRootOntology(), null, true);
                rb = Response.ok();
            } catch (OWLOntologyCreationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else rb = Response.status(UNSUPPORTED_MEDIA_TYPE);

        addCORSOrigin(servletContext, rb, headers);
        Response r = rb.build();
        log.debug("POST request for ontology addition completed in {} ms with status {}.",
            (System.currentTimeMillis() - before), r.getStatus());
        return r;
    }
}
