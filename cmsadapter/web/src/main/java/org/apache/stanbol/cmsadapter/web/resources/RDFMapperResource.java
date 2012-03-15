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
package org.apache.stanbol.cmsadapter.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeManager;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.web.utils.RestUtil;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * This resource provides functionalities for bidirectional mapping between external RDF data and JCR/CMIS
 * content repositories. In other words, it is possible to populate content repository based on an external
 * RDF. On the other direction, it enables generation of RDF using the structure of content repository. The
 * mapping operation is done by {@link RDFBridge}s.
 */
@Path("/cmsadapter/map")
public class RDFMapperResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(RDFMapperResource.class);
    private Parser clerezzaParser;
    private RDFBridgeManager bridgeManager;

    public RDFMapperResource(@Context ServletContext context) {
        clerezzaParser = ContextHelper.getServiceFromContext(Parser.class, context);
        bridgeManager = ContextHelper.getServiceFromContext(RDFBridgeManager.class, context);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/rdf")
    public Response handleCorsPreflightRDF(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/cms")
    public Response handleCorsPreflightCMS(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this), TEXT_HTML);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Allows clients to map specified RDF to the content repository. In the first step the RDF data is
     * annotated according to RDF Bridges loaded in the OSGI environment. Additional annotations provide
     * selection of certain resources from RDF data and creation/update of related content repository object.
     * See Javadoc of {@link DefaultRDFBridgeImpl} for possible configuration options of default
     * {@link RDFBridge} implementation. Either a raw RDF can be given in <code>serializedGraph</code>
     * parameter or URL of an external RDF data can given in <code>url</code> parameter. However,
     * <code>serializedGraph</code> has a higher priority.
     * 
     * @param sessionKey
     *            session key to obtain a previously created session to be used to connect a content
     *            repository
     * @param serializedGraph
     *            is the serialized RDF graph in <b>application/rdf+xml" format that is desired to be
     *            transformed into repository objects
     * @param url
     *            URL of the external RDF data.
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    @Path("/rdf")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response mapRawRDFToRepository(@FormParam("sessionKey") String sessionKey,
                                          @FormParam("serializedGraph") String serializedGraph,
                                          @FormParam("url") String url,
                                          @Context HttpHeaders headers) throws MalformedURLException,
                                                                       IOException {

        sessionKey = RestUtil.nullify(sessionKey);
        if (sessionKey == null) {
            logger.warn("Sessin key should not be null");
            return Response.status(Status.BAD_REQUEST).entity("Session key should not be null").build();
        }

        long start = System.currentTimeMillis();
        Graph g;
        if (serializedGraph != null && !serializedGraph.trim().isEmpty()) {
            g = clerezzaParser.parse(new ByteArrayInputStream(serializedGraph.getBytes()),
                SupportedFormat.RDF_XML);
        } else if (url != null && !url.trim().isEmpty()) {
            URLConnection uc = (new URL(url)).openConnection();
            g = clerezzaParser.parse(uc.getInputStream(), SupportedFormat.RDF_XML);
        } else {
            logger.warn("There is no RDF data source specified");
            return Response.status(Status.BAD_REQUEST).entity("There is no RDF data source specified")
                    .build();
        }

        Response r = mapRDF(g, sessionKey, headers);
        logger.info("RDF mapping finished in: {} seconds", ((System.currentTimeMillis() - start) / 1000));
        return r;
    }

    /**
     * Same with {@link #mapRawRDFToRepository(String, String, String)}. But this service allows clients to
     * submit external RDF data through a {@link File} specified in <code>rdfFile</code> parameter.
     * 
     * @param sessionKey
     *            session key to obtain a previously created session to be used to connect a content
     *            repository
     * @param rdfFile
     *            {@link File} containing the RDF to be mapped to the content repository
     * @param rdfFileInfo
     *            Information related with RDF file
     * @return
     * @throws IOException
     */
    @Path("/rdf")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response mapRDFToRepositoryFromFile(@QueryParam("sessionKey") String sessionKey,
                                               @FormDataParam("rdfFile") File rdfFile,
                                               @FormDataParam("rdfFile") FormDataContentDisposition rdfFileInfo,
                                               @Context HttpHeaders headers) throws IOException {

        sessionKey = RestUtil.nullify(sessionKey);
        if (sessionKey == null) {
            logger.warn("Sessin key should not be null");
            return Response.status(Status.BAD_REQUEST).entity("Session key should not be null").build();
        }

        long start = System.currentTimeMillis();
        Graph g;
        if (rdfFile != null) {
            InputStream is = new ByteArrayInputStream(FileUtils.readFileToByteArray(rdfFile));
            g = clerezzaParser.parse(is, SupportedFormat.RDF_XML);
        } else {
            logger.warn("There is RDF file specified");
            return Response.status(Status.BAD_REQUEST).entity("There is no RDF file specified").build();
        }

        Response r = mapRDF(g, sessionKey, headers);
        logger.info("RDF mapping finished in: {} seconds", ((System.currentTimeMillis() - start) / 1000));
        return r;
    }

    private Response mapRDF(Graph g, String sessionKey, HttpHeaders headers) {
        try {
            bridgeManager.storeRDFToRepository(sessionKey, g);
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to obtain a session from repository", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to obtain a session from repository").build();
        } catch (RDFBridgeException e) {
            logger.warn(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        ResponseBuilder rb = Response.ok().entity("RDF data has been mapped to the content repository");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * This service provides obtaining an RDF from the content repository based on the {@link RDFBridge}
     * instances in the environment. Target content repository objects are determined according to path
     * configurations of RDF Bridges. In the first step, content repository objects are converted into an RDF.
     * This process is realized by {@link RDFMapper}. For JCR and CMIS repositories there are two
     * implementations of this interface namely, <code>JCRRDFMapper</code> and <code>CMISRDFMapper</code>. At
     * the end of first step, generated RDF contains only <b>CMS Vocabulary</b> annotations. Afterwards,
     * additional assertions are added based on RDF Bridges.
     * 
     * @param sessionKey
     *            session key to obtain a previously created session to be used to connect a content
     *            repository
     * @param baseURI
     *            base URI for the RDF to be generated
     * @param store
     *            if this boolean parameter is set as <code>true</code>, generated RDF is stored persistently
     *            in Stanbol environment
     * @param update
     *            precondition to consider this parameter is setting <code>true</code> for <code>store</code>
     *            parameter. If so; if this parameter is set as <code>true</code> previously store RDF having
     *            the identified by the URI passed in <code>baseURI</code> parameter is updated. However, if
     *            there is no stored RDF a new one is created. If it is not set explicitly, its default value
     *            is <code>true</code>
     * 
     * @return generated {@link MGraph} wrapped in a {@link Response} in "application/rdf+xml" format
     */
    @Path("/cms")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SupportedFormat.RDF_XML)
    public Response mapRepositoryToRDF(@FormParam("sessionKey") String sessionKey,
                                       @FormParam("baseURI") String baseURI,
                                       @FormParam("store") boolean store,
                                       @FormParam("update") @DefaultValue("true") boolean update,
                                       @Context HttpHeaders headers) {

        sessionKey = RestUtil.nullify(sessionKey);
        if (sessionKey == null) {
            logger.warn("Sessin key should not be null");
            return Response.status(Status.BAD_REQUEST).entity("Session key should not be null").build();
        }

        try {
            long start = System.currentTimeMillis();
            MGraph generatedGraph = bridgeManager.generateRDFFromRepository(baseURI, sessionKey, store,
                update);
            logger.info("CMS mapping finished in: {} seconds", ((System.currentTimeMillis() - start) / 1000));
            ResponseBuilder rb = Response.ok(generatedGraph, SupportedFormat.RDF_XML);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } catch (RepositoryAccessException e) {
            String message = e.getMessage();
            logger.warn(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
        } catch (RDFBridgeException e) {
            logger.warn("Error while generating RDF from repository", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while generating RDF from repository").build();
        }
    }
}