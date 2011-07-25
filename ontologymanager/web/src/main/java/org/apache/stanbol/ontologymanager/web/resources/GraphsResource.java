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

import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.NoSuchStoreException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.multipart.FormDataParam;

@Path("/ontonet/graphs")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class GraphsResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected ONManager onManager;
    protected ClerezzaOntologyStorage storage;

    protected TcManager tcManager;

    public GraphsResource(@Context ServletContext servletContext) {
        storage = (ClerezzaOntologyStorage) (servletContext.getAttribute(ClerezzaOntologyStorage.class
                .getName()));
        tcManager = (TcManager) servletContext.getAttribute(TcManager.class.getName());

        onManager = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        // onManager = (ONManager) (servletContext.getAttribute(ONManager.class.getName()));
        if (onManager == null) {
            throw new IllegalStateException("OntologyStorage missing in ServletContext");
        } else {
            storage = onManager.getOntologyStore();
        }
    }

    @GET
    @Path("/{graphid:.+}")
    public Response getGraph(@PathParam("graphid") String graphid,
                             @Context UriInfo uriInfo,
                             @Context HttpHeaders headers) {

        IRI ontologyID = IRI.create(graphid);

        // return Response.ok(tcManager.getMGraph(new UriRef(graphid))).build();
        try {
            return Response.ok(storage.getGraph(ontologyID)).build();
        } catch (NoSuchStoreException e) {
            return Response.status(NO_CONTENT).build();
        }

    }

    public String getNamespace() {
        return onManager.getOntologyNetworkNamespace();
    }

    public List<String> getStoredGraphs() {
        Set<IRI> iris = storage.listGraphs();

        ArrayList<String> graphs = new ArrayList<String>();
        for (IRI iri : iris) {
            graphs.add(iri.toString());
        }
        return graphs;
    }

    @GET
    @Path("/resume")
    @Produces({KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, KRFormat.OWL_XML, KRFormat.RDF_XML,
               KRFormat.TURTLE, KRFormat.RDF_JSON})
    public Response graphs(@Context HttpHeaders headers, @Context ServletContext servletContext) {
        Set<IRI> iris = storage.listGraphs();
        if (iris != null) {

            OWLOntologyManager manager = onManager.getOntologyManagerFactory().createOntologyManager(true);
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            OWLOntology ontology;
            try {
                ontology = manager.createOntology();

                String ns = onManager.getOntologyNetworkNamespace();

                OWLNamedIndividual storage = factory.getOWLNamedIndividual(IRI.create(ns + "Storage"));

                OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(ns + "hasGraph"));

                for (IRI iri : iris) {
                    iri = IRI.create(iri.toString().replace("<", "").replace(">", ""));
                    OWLNamedIndividual graph = factory.getOWLNamedIndividual(iri);
                    OWLObjectPropertyAssertionAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(p,
                        storage, graph);
                    manager.applyChange(new AddAxiom(ontology, axiom));
                }

                return Response.ok(ontology).build();
            } catch (OWLOntologyCreationException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }

        }

        return Response.status(NOT_FOUND).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response storeGraph(@FormDataParam("graph") InputStream graph, @FormDataParam("id") String id) {
        try {
            OWLOntology ontology = onManager.getOntologyManagerFactory().createOntologyManager(true)
                    .loadOntologyFromOntologyDocument(graph);
            storage.store(ontology, IRI.create(id));
            return Response.ok().build();
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }
}
