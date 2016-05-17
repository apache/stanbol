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
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
//import com.sun.jersey.api.view.ImplicitProduces;
//import com.sun.jersey.multipart.BodyPart;
//import com.sun.jersey.multipart.FormDataBodyPart;
//import com.sun.jersey.multipart.FormDataMultiPart;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;

import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.util.OWL2Constants;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.commons.web.viewable.Viewable;
//import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector.GraphMultiplexer;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.Multiplexer;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyHandleException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OrphanOntologyKeyException;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.sources.owlapi.OntologyContentInputSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the basic HTTP methods for storing and managing ontologies regardless of them belonging to a
 * specific network, scope or session.
 * 
 * @author anuzzolese, alexdma
 * 
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonet")
// @ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RootResource extends AbstractOntologyAccessResource {

    protected Multiplexer descriptor;

    protected Multiplexer getDescriptor() {
        if (descriptor == null) {
            if (ontologyProvider == null) throw new IllegalStateException(
                    "Tried to obtain a multiplexer before an ontology provider was ready. This shouldn't happen.");
            descriptor = new GraphMultiplexer(ontologyProvider.getMetaGraph(Graph.class));
        }
        return descriptor;
    }

    public class OntologyStats extends ResultData {

        private Set<OntologyCollector> handles;

        private Set<OWLOntologyID> identifiers;

        public Map<OWLOntologyID,OntologyProvider.Status> dependencies;

        private OWLOntology o;

        private UriInfo uriInfo;

        private OWLOntologyID submitted;

        public OntologyStats(UriInfo uriInfo,
                             OWLOntologyID key,
                             OWLOntology o,
                             Set<OWLOntologyID> identifiers,
                             Map<OWLOntologyID,OntologyProvider.Status> dependencies,
                             Set<OntologyCollector> handles) {
            // this.servletContext = context;
            this.uriInfo = uriInfo;
            this.dependencies = dependencies;
            this.submitted = key;
            this.o = o;
            this.identifiers = identifiers;
            this.handles = handles;
        }

        public Set<String> getAliases() {
            Set<String> aliases = new HashSet<String>();
            for (OWLOntologyID alias : identifiers)
                // if (!o.getOntologyID().equals(alias))
                aliases.add(OntologyUtils.encode(alias));
            return Collections.unmodifiableSet(aliases);
        }

        public Map<String,String> getDependencies() {
            Map<String,String> deps = new HashMap<String,String>();
            for (OWLOntologyID dep : dependencies.keySet()) {
                deps.put(OntologyUtils.encode(dep), dependencies.get(dep).toString());
            }
            return Collections.unmodifiableMap(deps);
        }

        public OWLOntologyID getRepresentedOntologyKey() {
            return submitted;
        }

        public Set<String> getScopeHandles() {
            Set<String> handles = new HashSet<String>();
            for (OntologyCollector handle : this.handles)
                if (handle instanceof OntologySpace) handles.add(handle.getID());
            return handles;
        }

        public Set<String> getSessionHandles() {
            Set<String> handles = new HashSet<String>();
            for (OntologyCollector handle : this.handles)
                if (handle instanceof Session) handles.add(handle.getID());
            return handles;
        }

        public int getTotalAxioms() {
            return o.getAxiomCount();
        }

        public String stringForm(OWLOntologyID ontologyID) {
            return OntologyUtils.encode(ontologyID);
        }
    }

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected ScopeManager onManager;

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    @Reference
    protected OntologyProvider<?> ontologyProvider;

    /*
     * Placeholder for the OntologyProvider to be fetched from the servlet context.
     */
    @Reference
    protected RegistryManager registryManager;

    @Reference
    protected SessionManager sessionManager;

    public RootResource() {
        super();
    }

    /*
     * TODO before implementing removal, we need OWL dependency checks. Also, this is quite a strong method
     * and would be best implemented with RESTful authentication.
     */
    // @DELETE
    public Response clear(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @PUT
    @Path("/{ontologyId:.+}")
    public Response createOntologyEntry(@PathParam("ontologyId") String ontologyId,
                                        @Context HttpHeaders headers,
                                        @Context UriInfo uriInfo) {
        OWLOntologyID key = OntologyUtils.decode(ontologyId);
        ResponseBuilder rb;
        if (ontologyProvider.listAllRegisteredEntries().contains(key)) {
            rb = Response.status(CONFLICT);
        } else {
            ontologyProvider.createBlankOntologyEntry(key);
            rb = Response.created(uriInfo.getRequestUri());
        }
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @DELETE
    @Path("/{ontologyId:.+}")
    public Response deleteOntology(@PathParam("ontologyId") String ontologyId, @Context HttpHeaders headers) {
        OWLOntologyID key = OntologyUtils.decode(ontologyId);
        ResponseBuilder rb;
        try {
            if (!ontologyProvider.hasOntology(key)) {
                rb = Response.status(NOT_FOUND);
            } else {
                try {
                    // TODO check aliases!
                    ontologyProvider.removeOntology(key);
                    rb = Response.ok();
                } catch (OntologyHandleException e) {
                    rb = Response.status(CONFLICT);
                }
            }
        } catch (OrphanOntologyKeyException e) {
            log.warn("Orphan ontology key {}. No associated graph found in store.", e.getOntologyKey());
            rb = Response.status(NOT_FOUND);
        }
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Set<String> getAliases(final OWLOntologyID ontologyId) {
        // TODO use rdfViewable instead of Viewable to make separation of
        // presentation and application logic cleaner
        return AccessController.doPrivileged(new PrivilegedAction<Set<String>>() {
            @Override
            public Set<String> run() {
                Set<String> aliases = new HashSet<String>();
                for (OWLOntologyID alias : ontologyProvider.listAliases(ontologyId)) {
                    aliases.add(OntologyUtils.encode(alias));
                }
                return aliases;
            }
        });
    }

    private Graph getGraph(String ontologyId, boolean merged, URI requestUri) {
        long before = System.currentTimeMillis();

        OWLOntologyID key = OntologyUtils.decode(ontologyId);

        log.debug("Will try to retrieve ontology {} from provider.", key);
        /*
         * Export directly to Graph since the OWLOntologyWriter uses (de-)serializing converters for the
         * other formats.
         * 
         * Use oTemp for the "real" graph and o for the graph that will be exported. This is due to the fact
         * that in o we want to change import statements, but we do not want these changes to be stored
         * permanently.
         */
        Graph o = null, oTemp = null;
        try {
            oTemp = ontologyProvider.getStoredOntology(key, Graph.class, merged);
        } catch (Exception ex) {
            log.warn("Retrieval of ontology with ID " + key + " failed.", ex);
        }

        if (oTemp == null) {
            log.debug("Ontology {} missing from provider. Trying libraries...", key);
            // TODO remove once registry supports OWLOntologyID as public key.
            IRI iri = URIUtils.sanitize(IRI.create(ontologyId));
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
                    oTemp = registryManager.getLibrary(smallest).getOntology(iri, Graph.class);
                } catch (RegistryContentException e) {
                    log.warn("The content of library " + smallest + " could not be accessed.", e);
                }
            }
        }

        // This is needed because we need to change import statements. No need to use a more efficient but
        // resource-intensive IndexedGraph, since both o and oTemp will be GC'ed after serialization.
        if (oTemp != null) {
            o = new SimpleGraph(oTemp);
        }

        if (o == null) {
            log.debug("Ontology {} not found in any ontology provider or library.", ontologyId);
            return null;
        }

        log.debug("Retrieved ontology {} .", ontologyId);

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
        while (imports.hasNext()) {
            oldImports.add(imports.next());
        }
        for (Triple t : oldImports) {
            // construct new statement
            String s = ((org.apache.clerezza.commons.rdf.IRI) t.getObject()).getUnicodeString();
            if (s.contains("::")) {
                s = s.substring(s.indexOf("::") + 2, s.length());
            }
            org.apache.clerezza.commons.rdf.IRI target = new org.apache.clerezza.commons.rdf.IRI(base + "/" + s);
            o.add(new TripleImpl(t.getSubject(), OWL.imports, target));
            // remove old statement
            o.remove(t);
        }

        // Versioning.
        OWLOntologyID id = OWLUtils.extractOntologyID(o);
        if (id != null && !id.isAnonymous() && id.getVersionIRI() == null) {
            org.apache.clerezza.commons.rdf.IRI viri = new org.apache.clerezza.commons.rdf.IRI(requestUri.toString());
            log.debug("Setting version IRI for export : {}", viri);
            o.add(new TripleImpl(new org.apache.clerezza.commons.rdf.IRI(id.getOntologyIRI().toString()), new org.apache.clerezza.commons.rdf.IRI(
                    OWL2Constants.OWL_VERSION_IRI), viri));
        }

        log.debug("Exported as Clerezza ImmutableGraph in {} ms. Handing over to writer.", System.currentTimeMillis()
                                                                                  - before);
        return o;
    }

    public Set<String> getHandles(final OWLOntologyID ontologyId) {
        // TODO use rdfViewable instead of Vieable to make separation of
        // presentation and application logic cleaner
        return AccessController.doPrivileged(new PrivilegedAction<Set<String>>() {
            @Override
            public Set<String> run() {
                Set<String> handles = new HashSet<String>();
                if (onManager != null) {
                    for (Scope scope : onManager.getRegisteredScopes()) {
                        if (scope.getCoreSpace().hasOntology(ontologyId)
                            || scope.getCustomSpace().hasOntology(ontologyId)) {
                            handles.add(scope.getID());
                        }
                    }
                }
                if (sessionManager != null) {
                    for (String sesId : sessionManager.getRegisteredSessionIDs()) {
                        if (sessionManager.getSession(sesId).hasOntology(ontologyId)) {
                            handles.add(sesId);
                        }
                    }
                }
                return handles;
            }
        });

    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public Response getMetadata(@PathParam("ontologyId") String ontologyId,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers) {
        ResponseBuilder rb;
        org.apache.clerezza.commons.rdf.IRI me = new org.apache.clerezza.commons.rdf.IRI(getPublicBaseUri() + "ontonet/" + ontologyId);
        Graph mImmutableGraph = new SimpleGraph();
        for (String alias : getAliases(OntologyUtils.decode(ontologyId))) {
            mImmutableGraph.add(new TripleImpl(new org.apache.clerezza.commons.rdf.IRI(getPublicBaseUri() + "ontonet/" + alias), OWL.sameAs, me));
        }
        rb = Response.ok(mImmutableGraph);
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Produces({RDF_XML, TURTLE, X_TURTLE, APPLICATION_JSON, RDF_JSON})
    public Response getMetaGraph(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(ontologyProvider.getMetaGraph(ImmutableGraph.class));
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public SortedSet<OWLOntologyID> getOntologies() {
        // As this method is invoked from the template it would be too late
        // to handle AccessControlExceptionS
        // TODO use rdfViewable instead of Vieable to make separation of
        // presentation and application logic cleaner
        return AccessController.doPrivileged(new PrivilegedAction<SortedSet<OWLOntologyID>>() {
            @Override
            public SortedSet<OWLOntologyID> run() {
                // No orphans included.
                SortedSet<OWLOntologyID> filtered = new TreeSet<OWLOntologyID>();
                Set<OWLOntologyID> orphans = ontologyProvider.listOrphans();
                for (OWLOntologyID id : ontologyProvider.getPublicKeys()) {
                    if (id != null && !orphans.contains(id)) {
                        filtered.add(id);
                    }
                }
                return filtered;
            }
        });
    }

    public Set<OWLOntologyID> getOrphans() {
        // As this method is invoked from the template it would be too late
        // to handle AccessControlExceptionS
        // TODO use rdfViewable instead of Vieable to make separation of
        // presentation and application logic cleaner
        return AccessController.doPrivileged(new PrivilegedAction<Set<OWLOntologyID>>() {

            @Override
            public Set<OWLOntologyID> run() {
                return ontologyProvider.listOrphans();
            }
        });
    }

    private OWLOntology getOWLOntology(String ontologyId, boolean merge, URI requestUri) {
        long before = System.currentTimeMillis();
        IRI iri = URIUtils.sanitize(IRI.create(ontologyId));
        log.debug("Will try to retrieve ontology {} from provider.", iri);
        // TODO be selective: if the ontology is small enough, use OWLOntology otherwise export to ImmutableGraph.
        OWLOntology o = null;
        try {
            // XXX Guarantee that there MUST always be an entry for any decoded ontology ID submitted.
            OWLOntologyID id = OntologyUtils.decode(ontologyId);
            o = ontologyProvider.getStoredOntology(id, OWLOntology.class, merge);
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
        // Rewrite import statements - no ontology collector to do it for us here.
        URI base = URI.create(getPublicBaseUri() + "ontonet/");
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();

        // TODO manage import rewrites better once the container ID is fully configurable.
        for (OWLImportsDeclaration oldImp : o.getImportsDeclarations()) {
            changes.add(new RemoveImport(o, oldImp));
            String s = oldImp.getIRI().toString();
            if (s.contains("::")) {
                s = s.substring(s.indexOf("::") + 2, s.length());
            }
            IRI target = IRI.create(base + s);
            changes.add(new AddImport(o, df.getOWLImportsDeclaration(target)));
        }

        // Versioning.
        OWLOntologyID id = o.getOntologyID();
        if (!id.isAnonymous() && id.getVersionIRI() == null) {
            IRI viri = IRI.create(requestUri);
            log.debug("Setting version IRI for export : {}", viri);
            changes.add(new SetOntologyID(o, new OWLOntologyID(id.getOntologyIRI(), viri)));
        }

        o.getOWLOntologyManager().applyChanges(changes);
        log.debug("Exported as Clerezza ImmutableGraph in {} ms. Handing over to writer.", System.currentTimeMillis()
                                                                                  - before);
        return o;
    }

    public int getSize(final OWLOntologyID ontologyId) {
        // TODO use rdfViewable instead of Viewable to make separation of
        // presentation and application logic cleaner
        return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            @Override
            public Integer run() {
                return getDescriptor().getSize(ontologyId);
            }
        });

    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(value = {APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON})
    public Response getStandaloneGraph(@PathParam("ontologyId") String ontologyId,
                                       @DefaultValue("false") @QueryParam("meta") boolean meta,
                                       @DefaultValue("false") @QueryParam("merge") boolean merged,
                                       @Context UriInfo uriInfo,
                                       @Context HttpHeaders headers) {

        if (meta) {
            return getMetadata(ontologyId, uriInfo, headers);
        }

        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) {
            rb = Response.status(BAD_REQUEST);
        }
        OWLOntologyID key = OntologyUtils.decode(ontologyId);
        if (ontologyProvider.listOrphans().contains(key)) {
            rb = Response.status(NO_CONTENT);
        } else {
            Graph o = getGraph(ontologyId, merged, uriInfo.getRequestUri());
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
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
    public Response getStandaloneOntology(@PathParam("ontologyId") String ontologyId,
                                          @DefaultValue("false") @QueryParam("merge") boolean merged,
                                          @Context UriInfo uriInfo,
                                          @Context HttpHeaders headers) {
        ResponseBuilder rb;
        if (ontologyId == null || ontologyId.isEmpty()) {
            rb = Response.status(BAD_REQUEST);
        }
        OWLOntologyID key = OntologyUtils.decode(ontologyId);
        if (ontologyProvider.listOrphans().contains(key)) {
            rb = Response.status(NO_CONTENT);
        } else {
            OWLOntology o = getOWLOntology(ontologyId, merged, uriInfo.getRequestUri());
            rb = o == null ? Response.status(NOT_FOUND) : Response.ok(o);
        }
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Path("/{ontologyId:.+}")
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response loadOntologyContent(@PathParam("ontologyId") String ontologyId,
                                        MultiPartBody data,
                                        @Context HttpHeaders headers) {
        ResponseBuilder rb = performLoadOntology(data, headers,
            Origin.create(OntologyUtils.decode(ontologyId)));
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    protected ResponseBuilder performLoadOntology(MultiPartBody data, HttpHeaders headers, Origin<?>... keys) {
        log.debug(" post(MultiPartBody data)");
        ResponseBuilder rb = null;

        IRI location = null;
        byte[] file = null; // If found, it takes precedence over location.
        String format = null;
        List<OWLOntologyID> aliases = new ArrayList<OWLOntologyID>();

        if (data.getFormFileParameterValues("file").length > 0) {
            file = data.getFormFileParameterValues("file")[0].getContent();
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
                log.error("Malformed IRI for " + value, ex);
                throw new WebApplicationException(ex, BAD_REQUEST);
            }
        }
        if (data.getTextParameterValues("alias").length > 0) {
            for (String value : data.getTextParameterValues("alias")) {
                if (!"null".equals(value)) {
                    try {
                        aliases.add(OntologyUtils.decode(value));
                    } catch (Exception ex) {
                        log.error("Malformed public key for " + value, ex);
                        throw new WebApplicationException(ex, BAD_REQUEST);
                    }
                }
            }
        }

        log.debug("Parameters:");
        log.debug("file: {}", file != null && file.length > 0 ? "NOT-NULL" : "null");
        log.trace("file data: {}", file);
        log.debug("url: {}", location);
        log.debug("format: {}", format);
        log.debug("alias: {}", aliases);

        // Then add the file
        OWLOntologyID key = null;
        if (file != null && file.length > 0) {

            /*
             * Because the ontology provider's load method could fail after only one attempt without resetting
             * the stream, we might have to do that ourselves.
             */
            List<String> formats;
            if (format != null && !format.trim().isEmpty()) {
                formats = Collections.singletonList(format);
            } else // The RESTful API has its own list of preferred formats
            {
                formats = Arrays.asList(RDF_XML, TURTLE, X_TURTLE, N3, N_TRIPLE, OWL_XML, FUNCTIONAL_OWL,
                    MANCHESTER_OWL, RDF_JSON);
            }
            log.debug("Will try {} supported formats", formats.size());
            int unsupported = 0, failed = 0;
            Iterator<String> itf = formats.iterator();
            if (!itf.hasNext()) {
                throw new OntologyLoadingException("No suitable format found or defined.");
            }
            do {
                String f = itf.next();
                try {
                    // Re-instantiate the stream on every attempt
                    InputStream content = new BufferedInputStream(new ByteArrayInputStream(file));
                    // ClerezzaOWLUtils.guessOntologyID(new FileInputStream(file), Parser.getInstance(), f);
                    OWLOntologyID guessed = OWLUtils.guessOntologyID(content, Parser.getInstance(), f);

                    if (guessed != null && !guessed.isAnonymous() && ontologyProvider.hasOntology(guessed)) {
                        rb = Response.status(Status.CONFLICT);
                        this.submitted = guessed;
                        if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
                            rb.entity(new Viewable("/imports/409.ftl", this));
                            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=utf-8");
                        }
                        break;
                    } else {
                        content = new BufferedInputStream(new ByteArrayInputStream(file));
                        key = ontologyProvider.loadInStore(content, f, true, keys);
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
                }catch(ConcurrentModificationException e){
                    log.error("Exception logged", e);
                    failed++;
                } catch (Exception e) { // SAXParseException and others
                    log.debug(">>> FAILURE format {} (parse error)", f);
                    log.debug("Caught exception {} : {}", e.getClass(), e.getLocalizedMessage());
                    log.trace("Exception logged", e);
                    failed++;
                }
            } while ((key == null/* || key.isAnonymous() */) && itf.hasNext());
            if ((key == null || key.isAnonymous()) && rb == null) {
                if (failed > 0) {
                    throw new WebApplicationException(BAD_REQUEST);
                } else if (unsupported > 0) {
                    throw new WebApplicationException(UNSUPPORTED_MEDIA_TYPE);
                }
            }
        } else if (location != null) {
            try { // Here we try every format supported by the Java API
                key = ontologyProvider.loadInStore(location, null, true, keys);
            } catch (Exception e) {
                log.error("Failed to load ontology from " + location, e);
                Throwable cause = e.getCause();
                String html = "<h1>400 Bad Request</h1>" + "<p>Failed to load ontology from <a href=\""
                              + location + "\" target=\"_blank\">" + location + "</a></p>";
                if (cause != null) html += "<p>logged cause was: "
                                           + cause.getLocalizedMessage().replace("<", "&lt;")
                                                   .replace(">", "&gt;") + "</p>";
                return Response.status(BAD_REQUEST).type(TEXT_HTML).entity(html);
            }
        } else if (!aliases.isEmpty()) // No content but there are aliases.
        {
            for (Origin<?> origin : keys) {
                if (origin.getReference() instanceof OWLOntologyID) {
                    OWLOntologyID primary = ((OWLOntologyID) origin.getReference());
                    if (ontologyProvider.getStatus(primary) != org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider.Status.NO_MATCH) {
                        for (OWLOntologyID alias : aliases) {
                            try {
                                if (ontologyProvider.addAlias(primary, alias) && key == null) {
                                    key = alias;
                                }
                            } catch (IllegalArgumentException ex) {
                                log.warn("Cannot add alias");
                                log.warn(" ... ontology key: {}", primary);
                                log.warn(" ... alias: {}", alias);
                                log.warn(" ... reason: ", ex);
                                continue;
                            }
                        }
                    }
                }
            }
        } else {
            log.error("Bad request");
            log.error(" file is: {}", file);
            throw new WebApplicationException(BAD_REQUEST);
        }

        if (key != null && !key.isAnonymous()) {
            String uri = OntologyUtils.encode(key);
            if (uri != null && !uri.isEmpty()) {
                rb = Response.ok();
                if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
                    rb.entity(new Viewable("index", this));
                    rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=utf-8");
                }
            } else {
                rb = Response.ok();
            }
        } else if (rb == null) {
            rb = Response.status(Status.INTERNAL_SERVER_ERROR);
        }
        return rb;
    }

    /**
     * Helper method to make sure a ResponseBuilder is created on every conditions, so that it is then
     * possible to enable CORS on it afterwards.
     * 
     * @param ontologyId
     * @return
     */
    protected ResponseBuilder performShowOntology(String ontologyId) {
        if (ontologyId == null || ontologyId.isEmpty()) {
            return Response.status(BAD_REQUEST);
        }
        OWLOntologyID key = OntologyUtils.decode(ontologyId);
        if (ontologyProvider.listOrphans().contains(key)) {
            return Response.status(NO_CONTENT);
        }
        OWLOntology o = getOWLOntology(ontologyId, false, uriInfo.getRequestUri());
        if (o == null) {
            return Response.status(NOT_FOUND);
        }

        // Assemble dependency list
        Map<OWLOntologyID,OntologyProvider.Status> deps = new HashMap<OWLOntologyID,OntologyProvider.Status>();
        for (OWLOntologyID dep : getDescriptor().getDependencies(key)) {
            deps.put(dep, ontologyProvider.getStatus(dep));
        }

        Set<OntologyCollector> handles = new HashSet<OntologyCollector>();
        if (onManager != null) {
            for (Scope scope : onManager.getRegisteredScopes()) {
                if (scope.getCoreSpace().hasOntology(key)) {
                    handles.add(scope.getCoreSpace());
                }
                if (scope.getCustomSpace().hasOntology(key)) {
                    handles.add(scope.getCustomSpace());
                }
            }
        }
        if (sessionManager != null) {
            for (String sesId : sessionManager.getRegisteredSessionIDs()) {
                if (sessionManager.getSession(sesId).hasOntology(key)) {
                    handles.add(sessionManager.getSession(sesId));
                }
            }
        }
        return Response.ok(new Viewable("ontology", new OntologyStats(uriInfo, key, o, ontologyProvider
                .listAliases(key), deps, handles)));
    }

    @POST
    @Consumes({MULTIPART_FORM_DATA})
    @Produces({TEXT_HTML, TEXT_PLAIN, RDF_XML, TURTLE, X_TURTLE, N3})
    public Response postOntology(MultiPartBody data, @Context HttpHeaders headers) {
        ResponseBuilder rb = performLoadOntology(data, headers);
        // rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/{ontologyId:.+}")
    @Produces(TEXT_HTML)
    public Response showOntology(@PathParam("ontologyId") String ontologyId,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo uriInfo) {
        ResponseBuilder rb = performShowOntology(ontologyId);
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        // addCORSOrigin(servletContext, rb, headers);
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
            OWLOntologyID key = null;
            try {
                key = ontologyProvider.loadInStore(content, headers.getMediaType().toString(), true);
                rb = Response.ok();
            } catch (UnsupportedFormatException e) {
                log.warn(
                    "POST method failed for media type {}. This should not happen (should fail earlier)",
                    headers.getMediaType());
                rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
            } catch (IOException e) {
                throw new WebApplicationException(e, BAD_REQUEST);
            }
            // An exception should have been thrown earlier, but just in case.
            if (key == null || key.isAnonymous()) {
                rb = Response.status(Status.INTERNAL_SERVER_ERROR);
            }
        } else if (OWL_XML_TYPE.equals(mt) || FUNCTIONAL_OWL_TYPE.equals(mt)
                   || MANCHESTER_OWL_TYPE.equals(mt)) {
            try {
                OntologyInputSource<OWLOntology> src = new OntologyContentInputSource(content);
                ontologyProvider.loadInStore(src.getRootOntology(), true);
                rb = Response.ok();
            } catch (OWLOntologyCreationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else {
            rb = Response.status(UNSUPPORTED_MEDIA_TYPE);
        }

        // addCORSOrigin(servletContext, rb, headers);
        Response r = rb.build();
        log.debug("POST request for ontology addition completed in {} ms with status {}.",
            (System.currentTimeMillis() - before), r.getStatus());
        return r;
    }
}
