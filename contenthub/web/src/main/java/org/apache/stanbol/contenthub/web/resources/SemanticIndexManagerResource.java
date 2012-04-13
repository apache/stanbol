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
package org.apache.stanbol.contenthub.web.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDProgram;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDProgramCollection;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * This class the the web resource to handle the RESTful requests and HTML view of the LDProgram management
 * facilities within Contenthub.
 * 
 * @author anil.pacaci
 * @author anil.sinaci
 * 
 */
@Path("/contenthub/ldpath")
public class SemanticIndexManagerResource extends BaseStanbolResource {

    private static final Logger logger = LoggerFactory.getLogger(SemanticIndexManagerResource.class);

    private SemanticIndexManager programManager;

    public SemanticIndexManagerResource(@Context ServletContext context) {
        programManager = ContextHelper.getServiceFromContext(SemanticIndexManager.class, context);
        if (programManager == null) {
            logger.error("Missing LDProgramManager = {}", programManager);
            throw new WebApplicationException(404);
        }
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/program")
    public Response handleCorsPreflightProgram(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/exists")
    public Response handleCorsPreflightExists(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    /**
     * HTTP GET method which returns all LDPath programs residing in Contenthub. LDPath programs are uniquely
     * identified by their names. Returning JSON string presents each LDPath program in string format aligned
     * with its name.
     * 
     * @param headers
     *            HTTP headers
     * @return JSON string of {@code name:program} pairs.
     */
    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    public Response retrieveAllPrograms(@Context HttpHeaders headers) {
        MediaType acceptedHeader = RestUtil.getAcceptedMediaType(headers);
        if (acceptedHeader.isCompatible(MediaType.TEXT_HTML_TYPE)) {
        	return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();
        } else{
	        LDProgramCollection ldProgramCollection = programManager.retrieveAllPrograms();
	        ResponseBuilder rb = Response.ok(ldProgramCollection, MediaType.APPLICATION_JSON);
	        addCORSOrigin(servletContext, rb, headers);
	        return rb.build();
        }
    }

    /**
     * HTTP POST method which saves an LDPath program into the persistent store of Contenthub.
     * 
     * @param programName
     *            Unique name to identify the LDPath program
     * @param program
     *            The LDPath program.
     * @param headers
     *            HTTP Headers
     * @return HTTP OK(200) or BAD REQUEST(400)
     * @throws LDPathException
     */
    @POST
    @Path("/program")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response submitProgram(@FormParam("name") String programName,
                                  @FormParam("program") String program,
                                  @Context HttpHeaders headers) throws LDPathException {

        try {
            programManager.submitProgram(programName, program);
        } catch (LDPathException e) {
            logger.error("LDPath program cannot be submitted", e);
            return Response.status(Status.BAD_REQUEST).entity(e).build();
        }
        ResponseBuilder rb = Response
                .ok("LDPath program has been successfully saved and corresponding Solr Core has been successfully created.");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP GET method to retrieve an LDPath program, given its name.
     * 
     * @param programName
     *            The name of the LDPath program to be retrieved.
     * @param headers
     *            HTTP headers
     * @return LDPath program in {@link String} format or HTTP NOT FOUND(404)
     */
    @GET
    @Path("/program")
    public Response getProgramByName(@QueryParam("name") String programName, @Context HttpHeaders headers) {
        String ldPathProgram = programManager.getProgramByName(programName);
        if (ldPathProgram == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else {
            ResponseBuilder rb = Response.ok(ldPathProgram);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    /**
     * HTTP DELETE method to delete an LDPath program.
     * 
     * @param programName
     *            The name of the LDPath program.
     * @param headers
     *            HTTP headers
     * @return HTTP OK(200)
     */
    @DELETE
    @Path("/program/{name}")
    public Response deleteProgram(@PathParam(value = "name") String programName, @Context HttpHeaders headers) {
        programManager.deleteProgram(programName);
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP GET method to check whether an LDPath program exists in Contenthub or not.
     * 
     * @param programName
     *            The name of the LDPath program.
     * @param headers
     *            HTTP headers
     * @return HTTP OK(200) or HTTP NOT FOUND(404)
     */
    @GET
    @Path("/exists")
    public Response isManagedProgram(@QueryParam("name") String programName, @Context HttpHeaders headers) {
        if (programManager.isManagedProgram(programName)) {
            ResponseBuilder rb = Response.ok();
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            ResponseBuilder rb = Response.status(Status.NOT_FOUND);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    // Helper methods for HTML view
    public List<LDProgram> getLdPrograms() {
        return programManager.retrieveAllPrograms().asList();
    }

}
