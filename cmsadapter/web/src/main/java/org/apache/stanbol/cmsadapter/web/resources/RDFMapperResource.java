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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.cmsadapter.core.helper.TcManagerClient;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeManager;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * This resource is currently used to pass RDF data to CMS Adapter so that RDF data will be annotated with
 * "CMS vocabulary" annotations according to {@link RDFBridge}s. Afterwards, this annotated RDF is transformed
 * into nodes/object in the content repository.
 */

@Path("/cmsadapter/map")
public class RDFMapperResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(RDFMapperResource.class);
    private Parser clerezzaParser;
    private RDFBridgeManager bridgeManager;
    private TcManager tcManager;

    public RDFMapperResource(@Context ServletContext context) {
        clerezzaParser = ContextHelper.getServiceFromContext(Parser.class, context);
        bridgeManager = ContextHelper.getServiceFromContext(RDFBridgeManager.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    /**
     * Allows clients to map specified RDF to the content repository. In the first step the RDF data is
     * annotated according to RDF Bridges loaded in the OSGI environment. Additional annotations provide
     * selection of certain resources from RDF data and creation/update of related content repository object.
     * Either a raw RDF can be given in <code>serializedGraph</code> parameter or URL of an external RDF data
     * can given in <code>url</code> parameter. However, <code>serializedGraph</code> has a higher priority.
     * 
     * @param repositoryURL
     *            URL of the content repository. For JCR repositories <b>RMI protocol</b>, for CMIS
     *            repositories <b>AtomPub Binding</b> is used. This parameter should be set according to these
     *            connection methods.
     * @param workspaceName
     *            For JCR repositories this parameter determines the workspace to be connected. On the other
     *            hand for CMIS repositories <b>repository ID</b> should be set to this parameter. In case of
     *            not setting this parameter, for JCR <b>default workspace</b> is selected, for CMIS the
     *            <b>first repository</b> obtained through the session object is selected.
     * @param username
     *            Username to connect to content repository
     * @param password
     *            Password to connect to content repository
     * @param connectionType
     *            Connection type; either <b>JCR</b> or <b>CMIS</b>
     * @param serializedGraph
     *            is the serialized RDF graph that is desired to be transformed into repository objects
     * @param url
     *            URL of the external RDF data.
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    @Path("/rdf")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response mapRawRDF(@FormParam("repositoryURL") String repositoryURL,
                              @FormParam("workspaceName") String workspaceName,
                              @FormParam("username") String username,
                              @FormParam("password") String password,
                              @FormParam("connectionType") String connectionType,
                              @FormParam("serializedGraph") String serializedGraph,
                              @FormParam("url") String url) throws MalformedURLException, IOException {

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            logger.warn("Repository URL, username, password and connection type parameters should not be null");
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should not be null")
                    .build();
        }
        ConnectionInfo connectionInfo = formConnectionInfo(repositoryURL, workspaceName, username, password,
            connectionType);

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

        Response r = mapRDF(g, connectionInfo);
        logger.info("RDF mapping finished in: {} seconds", ((System.currentTimeMillis() - start) / 1000));
        return r;
    }

    /**
     * Same with {@link #mapRawRDF(String, String, String, String, String, String, String)}. But this service
     * allows clients to submit external RDF data through a {@link File} specified in <code>rdfFile</code>
     * parameter.
     * 
     * @param repositoryURL
     *            URL of the content repository. For JCR repositories <b>RMI protocol</b>, for CMIS
     *            repositories <b>AtomPub Binding</b> is used. This parameter should be set according to these
     *            connection methods.
     * @param workspaceName
     *            For JCR repositories this parameter determines the workspace to be connected. On the other
     *            hand for CMIS repositories <b>repository ID</b> should be set to this parameter. In case of
     *            not setting this parameter, for JCR <b>default workspace</b> is selected, for CMIS the
     *            <b>first repository</b> obtained through the session object is selected.
     * @param username
     *            Username to connect to content repository
     * @param password
     *            Password to connect to content repository
     * @param connectionType
     *            Connection type; either <b>JCR</b> or <b>CMIS</b>
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
    public Response mapRDFFromFile(@QueryParam("repositoryURL") String repositoryURL,
                                   @QueryParam("workspaceName") String workspaceName,
                                   @QueryParam("username") String username,
                                   @QueryParam("password") String password,
                                   @QueryParam("connectionType") String connectionType,
                                   @FormDataParam("rdfFile") File rdfFile,
                                   @FormDataParam("rdfFile") FormDataContentDisposition rdfFileInfo) throws IOException {

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            logger.warn("Repository URL, username, password and connection type parameters should not be null");
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should not be null")
                    .build();
        }
        ConnectionInfo connectionInfo = formConnectionInfo(repositoryURL, workspaceName, username, password,
            connectionType);
        long start = System.currentTimeMillis();
        Graph g;
        if (rdfFile != null) {
            InputStream is = new ByteArrayInputStream(FileUtils.readFileToByteArray(rdfFile));
            g = clerezzaParser.parse(is, SupportedFormat.RDF_XML);
        } else {
            logger.warn("There is RDF file specified");
            return Response.status(Status.BAD_REQUEST).entity("There is no RDF file specified").build();
        }

        Response r = mapRDF(g, connectionInfo);
        logger.info("RDF mapping finished in: {} miliseconds", ((System.currentTimeMillis() - start) / 1000));
        if (r.getStatus() == Response.Status.OK.getStatusCode()) {
            return get();
        } else {
            return r;
        }
    }

    private Response mapRDF(Graph g, ConnectionInfo connectionInfo) {
        try {
            bridgeManager.storeRDFToRepository(connectionInfo, g);
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to obtain a session from repository", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to obtain a session from repository").build();
        } catch (RDFBridgeException e) {
            logger.warn(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok().entity("RDF data has been mapped to the content repository").build();
    }

    /**
     * This service provides obtaining an RDF from the content repository based on the {@link RDFBridge}
     * instances in the environment. Target content content repository objects are determined according to
     * path configurations of RDF Bridges. In the first step, content repository objects are converted into an
     * RDF. This process is realized by {@link RDFMapper}. For JCR and CMIS repositories there are two
     * implementations of this interface namely, <code>JCRRDFMapper</code> and <code>CMISRDFMapper</code>. At
     * the end of first step, generated RDF contains only <b>CMS Vocabulary</b> annotations. Afterwards,
     * additional assertions are added based on RDF Bridges.
     * 
     * 
     * @return generated {@link MGraph} wrapped in a {@link Response} in "application/rdf+xml" format
     */
    @Path("/cms")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SupportedFormat.RDF_XML)
    public Response mapCMS(@FormParam("repositoryURL") String repositoryURL,
                           @FormParam("workspaceName") String workspaceName,
                           @FormParam("username") String username,
                           @FormParam("password") String password,
                           @FormParam("connectionType") String connectionType,
                           @FormParam("baseURI") String baseURI,
                           @FormParam("store") boolean store) {

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            logger.warn("Repository URL, username, password and connection type parameters should not be null");
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should not be null")
                    .build();
        }
        ConnectionInfo connectionInfo = formConnectionInfo(repositoryURL, workspaceName, username, password,
            connectionType);

        try {
            long start = System.currentTimeMillis();
            MGraph generatedGraph = bridgeManager.generateRDFFromRepository(baseURI, connectionInfo);
            logger.info("CMS mapping finished in: {} seconds", ((System.currentTimeMillis() - start) / 1000));

            TcManagerClient tcManagerClient = new TcManagerClient(tcManager);
            if (store) {
                if (tcManagerClient.modelExists(baseURI)) {
                    logger.info("Deleting the triple collection having base URI: {}", baseURI);
                    tcManager.deleteTripleCollection(new UriRef(baseURI));
                }
                logger.info("Saving the triple collection having base URI: {}", baseURI);
                MGraph persistentGraph = tcManager.createMGraph(new UriRef(baseURI));
                persistentGraph.addAll(generatedGraph);
            }

            return Response.ok(generatedGraph, SupportedFormat.RDF_XML).build();
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to obtain a session from repository", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to obtain a session from repository").build();
        } catch (RDFBridgeException e) {
            logger.warn("Error while generating RDF from repository", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error while generating RDF from repository").build();
        }
    }

    private ConnectionInfo formConnectionInfo(String repositoryURL,
                                              String workspaceName,
                                              String username,
                                              String password,
                                              String connectionType) {
        ConnectionInfo cInfo = new ConnectionInfo();
        cInfo.setConnectionType(connectionType);
        cInfo.setPassword(password);
        cInfo.setRepositoryURL(repositoryURL);
        cInfo.setUsername(username);
        cInfo.setWorkspaceName(workspaceName);
        return cInfo;
    }
}
