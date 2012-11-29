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

package org.apache.stanbol.reengineer.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.InputStream;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.datasources.DataSourceFactory;
import org.apache.stanbol.reengineer.base.api.datasources.InvalidDataSourceForTypeSelectedException;
import org.apache.stanbol.reengineer.base.api.datasources.NoSuchDataSourceExpection;
import org.apache.stanbol.reengineer.base.api.datasources.RDB;
import org.apache.stanbol.reengineer.base.api.settings.ConnectionSettings;
import org.apache.stanbol.reengineer.base.api.settings.DBConnectionSettings;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.api.util.UnsupportedReengineerException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import org.apache.stanbol.commons.ldviewable.Viewable;
import com.sun.jersey.multipart.FormDataParam;

@Path("/reengineer")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class ReengineerResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected ReengineerManager reengineeringManager;

    protected TcManager tcManager;

    public ReengineerResource(@Context ServletContext servletContext) {
        tcManager = (TcManager) ContextHelper.getServiceFromContext(TcManager.class, servletContext);
        reengineeringManager = (ReengineerManager) ContextHelper.getServiceFromContext(
            ReengineerManager.class, servletContext);
        if (reengineeringManager == null) {
            throw new IllegalStateException("ReengineeringManager missing in ServletContext");
        }
    }

    @GET
    @Path("/reengineers/count")
    public Response countReengineers(@Context HttpHeaders headers) {

        return Response.ok(reengineeringManager.countReengineers()).build();

    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("/reengineers")
    public Response listReengineers(@Context HttpHeaders headers) {
        Collection<Reengineer> reengineers = reengineeringManager.listReengineers();
        MGraph mGraph = new SimpleMGraph();
        UriRef semionRef = new UriRef("http://semion.kres.iksproject.eu#Semion");
        for (Reengineer semionReengineer : reengineers) {
            UriRef hasReengineer = new UriRef("http://semion.kres.iksproject.eu#hasReengineer");
            Literal reenginnerLiteral = LiteralFactory.getInstance().createTypedLiteral(
                semionReengineer.getClass().getCanonicalName());
            mGraph.add(new TripleImpl(semionRef, hasReengineer, reenginnerLiteral));
        }

        // return Response.ok(mGraph).build();
        ResponseBuilder rb = Response.ok(mGraph);
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response reengineering(@FormDataParam("output-graph") String outputGraph,
                                  @FormDataParam("input-type") String inputType,
                                  @FormDataParam("input") InputStream input,
                                  @Context HttpHeaders headers,
                                  @Context HttpServletRequest httpServletRequest) {

        log.debug("Reengineering: " + inputType);
        int reengineerType = -1;
        try {
            reengineerType = ReengineerType.getType(inputType);
        } catch (UnsupportedReengineerException e) {
            // Response.status(404).build();
            ResponseBuilder rb = Response.status(Status.NOT_FOUND);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }

        try {
            DataSource dataSource = DataSourceFactory.createDataSource(reengineerType, input);

            try {
                OWLOntology ontology;
                // System.out.println("STORE PROVIDER : "+storage);
                // System.out.println("OUTGRAPH: "+outputGraph);
                String servletPath = httpServletRequest.getLocalAddr();
                // System.out.println("SERVER PATH : "+servletPath);
                servletPath = "http://" + servletPath + "/kres/graphs/" + outputGraph + ":"
                              + httpServletRequest.getLocalPort();
                if (outputGraph == null || outputGraph.equals("")) {
                    ontology = reengineeringManager.performReengineering(servletPath, null, dataSource);
                    // return Response.ok().build();
                    ResponseBuilder rb = Response.ok();
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();

                } else {
                    ontology = reengineeringManager.performReengineering(servletPath,
                        IRI.create(outputGraph), dataSource);

                    store(ontology);
                    // return Response.ok(ontology).build();
                    ResponseBuilder rb = Response.ok(ontology);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            } catch (ReengineeringException e) {
                e.printStackTrace();
                // return Response.status(500).build();
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

        } catch (NoSuchDataSourceExpection e) {
            // return Response.status(415).build();
            ResponseBuilder rb = Response.status(415);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();

        } catch (InvalidDataSourceForTypeSelectedException e) {
            // return Response.status(204).build();
            ResponseBuilder rb = Response.status(204);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }

    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/db")
    public Response reengineeringDB(@QueryParam("db") String physicalDBName,
                                    @QueryParam("jdbc") String jdbcDriver,
                                    @QueryParam("protocol") String protocol,
                                    @QueryParam("host") String host,
                                    @QueryParam("port") String port,
                                    @QueryParam("username") String username,
                                    @QueryParam("password") String password,
                                    @QueryParam("output-graph") String outputGraph,
                                    @Context HttpHeaders headers,
                                    @Context HttpServletRequest httpServletRequest) {

        log.info("There are " + tcManager.listMGraphs().size() + " mGraphs");

        // UriRef uri = ContentItemHelper.makeDefaultUri(databaseURI, databaseURI.getBytes());
        ConnectionSettings connectionSettings = new DBConnectionSettings(protocol, host, port,
                physicalDBName, username, password, null, jdbcDriver);
        DataSource dataSource = new RDB(connectionSettings);

        String servletPath = httpServletRequest.getLocalAddr();
        servletPath = "http://" + servletPath + "/kres/graphs/" + outputGraph + ":"
                      + httpServletRequest.getLocalPort();

        if (outputGraph != null && !outputGraph.equals("")) {
            OWLOntology ontology;
            try {
                ontology = reengineeringManager.performReengineering(servletPath, IRI.create(outputGraph),
                    dataSource);
                // return Response.ok(ontology).build();
                ResponseBuilder rb = Response.ok(ontology);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();

            } catch (ReengineeringException e) {
                // return Response.status(500).build();

                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

        } else {
            try {
                reengineeringManager.performReengineering(servletPath, null, dataSource);
                // return Response.ok().build();
                ResponseBuilder rb = Response.ok();
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } catch (ReengineeringException e) {
                // return Response.status(500).build();
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/db/schema")
    public Response reengineeringDBSchema(@FormParam("output-graph") String outputGraph,
                                          @FormParam("db") String physicalDBName,
                                          @FormParam("jdbc") String jdbcDriver,
                                          @FormParam("protocol") String protocol,
                                          @FormParam("host") String host,
                                          @FormParam("port") String port,
                                          @FormParam("username") String username,
                                          @FormParam("password") String password,
                                          @Context HttpHeaders headers,
                                          @Context HttpServletRequest httpServletRequest) {

        log.info("There are " + tcManager.listMGraphs().size() + " mGraphs");

        // UriRef uri = ContentItemHelper.makeDefaultUri(databaseURI, databaseURI.getBytes());
        ConnectionSettings connectionSettings = new DBConnectionSettings(protocol, host, port,
                physicalDBName, username, password, null, jdbcDriver);
        DataSource dataSource = new RDB(connectionSettings);

        String servletPath = httpServletRequest.getLocalAddr();
        servletPath = "http://" + servletPath + "/kres/graphs/" + outputGraph + ":"
                      + httpServletRequest.getLocalPort();

        if (outputGraph != null && !outputGraph.equals("")) {
            OWLOntology ontology;
            try {
                ontology = reengineeringManager.performSchemaReengineering(servletPath,
                    IRI.create(outputGraph), dataSource);
                /*
                 * MediaType mediaType = headers.getMediaType(); String res =
                 * OntologyRenderUtils.renderOntology(ontology, mediaType.getType());
                 */
                // return Response.ok(ontology).build();
                ResponseBuilder rb = Response.ok(ontology);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } catch (ReengineeringException e) {
                // return Response.status(500).build();
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        } else {
            try {
                reengineeringManager.performSchemaReengineering(servletPath, null, dataSource);
                // return Response.ok().build();
                ResponseBuilder rb = Response.ok();
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } catch (ReengineeringException e) {
                // return Response.status(500).build();
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }

    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/schema")
    public Response schemaReengineering(@FormDataParam("output-graph") String outputGraph,
                                        @FormDataParam("input-type") String inputType,
                                        @FormDataParam("input") InputStream input,
                                        @Context HttpHeaders headers,
                                        @Context HttpServletRequest httpServletRequest) {

        int reengineerType = -1;
        try {
            reengineerType = ReengineerType.getType(inputType);
        } catch (UnsupportedReengineerException e) {
            ResponseBuilder rb = Response.status(Status.NOT_FOUND);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }

        try {
            DataSource dataSource = DataSourceFactory.createDataSource(reengineerType, input);

            try {
                OWLOntology ontology;

                String servletPath = httpServletRequest.getLocalAddr();
                servletPath = "http://" + servletPath + "/kres/graphs/" + outputGraph + ":"
                              + httpServletRequest.getLocalPort();
                if (outputGraph == null) {
                    ontology = reengineeringManager.performSchemaReengineering(servletPath, null, dataSource);
                    ResponseBuilder rb = Response.ok();
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    ontology = reengineeringManager.performSchemaReengineering(servletPath,
                        IRI.create(outputGraph), dataSource);
                    ResponseBuilder rb = Response.ok(ontology);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            } catch (ReengineeringException e) {
                // return Response.status(500).build();
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

        } catch (NoSuchDataSourceExpection e) {
            ResponseBuilder rb = Response.status(415);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } catch (InvalidDataSourceForTypeSelectedException e) {
            ResponseBuilder rb = Response.status(204);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }

    }

    /**
     * Borrowed from ontonet. TODO avoid explicitly storing from the Web resource
     * 
     * @param o
     */
    private void store(OWLOntology o) {
        // // Why was it using two converters earlier?
        // JenaToOwlConvert converter = new JenaToOwlConvert();
        // OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
        // MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
        TripleCollection mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
        MGraph mg2 = null;
        IRI iri = OWLUtils.extractOntologyID(o).getOntologyIRI();
        UriRef ref = new UriRef(iri.toString());
        try {
            mg2 = tcManager.createMGraph(ref);
        } catch (EntityAlreadyExistsException ex) {
            log.info("Entity " + ref + " already exists in store. Replacing...");
            mg2 = tcManager.getMGraph(ref);
        }

        mg2.addAll(mg);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

}
