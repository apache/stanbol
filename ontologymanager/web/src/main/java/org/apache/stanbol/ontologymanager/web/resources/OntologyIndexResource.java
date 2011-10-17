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

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontonet/ontology/get")
public class OntologyIndexResource extends BaseStanbolResource {

    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected Serializer serializer;

    protected ClerezzaOntologyStorage storage;

    public OntologyIndexResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
        this.storage = (ClerezzaOntologyStorage) ContextHelper.getServiceFromContext(
            ClerezzaOntologyStorage.class, servletContext);
        this.serializer = (Serializer) ContextHelper.getServiceFromContext(Serializer.class, servletContext);
    }

    @GET
    @Produces("application/rdf+xml")
    public Response getOntology(@QueryParam("iri") String ontologyIri) {

        OWLOntologyManager tmpmgr;
        OfflineConfiguration offline = (OfflineConfiguration) ContextHelper.getServiceFromContext(
            OfflineConfiguration.class, servletContext);
        if (offline == null) throw new IllegalStateException(
                "OfflineConfiguration missing in ServletContext");
        else tmpmgr = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                .getOntologySourceLocations().toArray(new IRI[0]));
        
        IRI iri = null;
        try {
            iri = IRI.create(ontologyIri);
        } catch (Exception ex) {
            throw new WebApplicationException(NOT_FOUND);
        }
        OntologyIndex index = onm.getOntologyIndex();
        if (!index.isOntologyLoaded(iri))
        // No such ontology registered, so return 404.
        return Response.status(NOT_FOUND).build();

        OWLOntology ont = index.getOntology(iri);

        StringDocumentTarget tgt = new StringDocumentTarget();
        try {
            tmpmgr.saveOntology(ont, new RDFXMLOntologyFormat(), tgt);
        } catch (OWLOntologyStorageException e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        return Response.ok(tgt.toString()).build();
    }

    @GET
    @Produces("text/turtle")
    public Response getOntologyT(@QueryParam("iri") String ontologyIri) {

        OWLOntologyManager tmpmgr;
        OfflineConfiguration offline = (OfflineConfiguration) ContextHelper.getServiceFromContext(
            OfflineConfiguration.class, servletContext);
        if (offline == null) throw new IllegalStateException(
                "OfflineConfiguration missing in ServletContext");
        else tmpmgr = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                .getOntologySourceLocations().toArray(new IRI[0]));
        
        IRI iri = null;
        try {
            iri = IRI.create(ontologyIri);
        } catch (Exception ex) {
            throw new WebApplicationException(NOT_FOUND);
        }
        OntologyIndex index = onm.getOntologyIndex();
        if (!index.isOntologyLoaded(iri))
        // No such ontology registered, so return 404.
        return Response.status(NOT_FOUND).build();

        OWLOntology ont = index.getOntology(iri);
        StringDocumentTarget tgt = new StringDocumentTarget();
        try {
            tmpmgr.saveOntology(ont, new TurtleOntologyFormat(), tgt);
        } catch (OWLOntologyStorageException e) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        return Response.ok(tgt.toString()).build();
    }

}
