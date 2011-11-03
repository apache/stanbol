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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.owl.transformation.JenaToClerezzaConverter;
import org.apache.stanbol.owl.transformation.JenaToOwlConvert;
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

import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.multipart.FormDataParam;

@Path("/ontonet/graphs")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class StoredGraphsResource extends BaseStanbolResource {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected ONManager onManager;

    protected TcManager tcManager;

    public StoredGraphsResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        tcManager = (TcManager) servletContext.getAttribute(TcManager.class.getName());
        onManager = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
    }

    @GET
    @Path("/{graphid:.+}")
    public Response getGraph(@PathParam("graphid") String graphid,
                             @Context UriInfo uriInfo,
                             @Context HttpHeaders headers) {

        IRI ontologyID = IRI.create(graphid);
        return Response.ok(tcManager.getGraph(new UriRef(ontologyID.toString()))).build();
    }

    public String getNamespace() {
        return onManager.getOntologyNetworkNamespace();
    }

    public List<String> getStoredGraphs() {
        Set<UriRef> iris = tcManager.listGraphs();

        ArrayList<String> graphs = new ArrayList<String>();
        for (UriRef iri : iris) {
            graphs.add(iri.getUnicodeString());
        }
        return graphs;
    }

    @GET
    @Path("/resume")
    @Produces({KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, KRFormat.OWL_XML, KRFormat.RDF_XML,
               KRFormat.TURTLE, KRFormat.RDF_JSON})
    public Response graphs(@Context HttpHeaders headers, @Context ServletContext servletContext) {
        Set<UriRef> iris = tcManager.listGraphs();
        if (iris != null) {

            // OWLOntologyManager manager = onManager.getOntologyManagerFactory().createOntologyManager(true);
            OWLDataFactory factory = OWLManager.getOWLDataFactory();

            OWLOntologyManager manager;
            OfflineConfiguration offline = (OfflineConfiguration) ContextHelper.getServiceFromContext(
                OfflineConfiguration.class, servletContext);
            if (offline == null) throw new IllegalStateException(
                    "OfflineConfiguration missing in ServletContext");
            else manager = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                    .getOntologySourceLocations().toArray(new IRI[0]));

            OWLOntology ontology;
            try {
                ontology = manager.createOntology();

                String ns = onManager.getOntologyNetworkNamespace();

                OWLNamedIndividual storage = factory.getOWLNamedIndividual(IRI.create(ns + "Storage"));

                OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(ns + "hasGraph"));

                for (UriRef iri : iris) {
                    // iri = IRI.create(iri.toString().replace("<", "").replace(">", ""));
                    // This should remove quotes
                    OWLNamedIndividual graph = factory.getOWLNamedIndividual(IRI.create(iri
                            .getUnicodeString()));
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

        OWLOntologyManager manager;
        OfflineConfiguration offline = (OfflineConfiguration) ContextHelper.getServiceFromContext(
            OfflineConfiguration.class, servletContext);
        if (offline == null) throw new IllegalStateException("OfflineConfiguration missing in ServletContext");
        else manager = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                .getOntologySourceLocations().toArray(new IRI[0]));

        try {
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(graph);
            /* storage. */store(ontology, IRI.create(id));
            return Response.ok().build();
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

    private void store(OWLOntology o, IRI ontologyID) {

        JenaToOwlConvert converter = new JenaToOwlConvert();
        OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
        MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
        // MGraph mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
        MGraph mg2 = tcManager.createMGraph(new UriRef(ontologyID.toString()));
        mg2.addAll(mg);
    }
}
