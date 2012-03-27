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
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.cmsadapter.core.repository.SessionManager;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.web.utils.RestUtil;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.Viewable;

/**
 * Resource providing services for session management
 */
@Path("/cmsadapter/session")
public class SessionResource extends BaseStanbolResource {

    private SessionManager sessionManager;

    public SessionResource(@Context ServletContext context) {
        sessionManager = ContextHelper.getServiceFromContext(SessionManager.class, context);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
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
     * Returns a session key based on the given connection information. Returned key can be used in subsequent
     * operations which requires a connection to content repository
     * 
     * @param repositoryURL
     *            URL of the content repository. For <i>JCR</i> repositories <b>RMI</b> protocol, for
     *            <i>CMIS</i> repositories <b>AtomPub Binding</b> is used. This parameter should be set
     *            according to these connection methods.
     * @param workspaceName
     *            For JCR repositories this parameter determines the workspace to be connected. On the other
     *            hand for CMIS repositories repository ID should be set to this parameter. In case of not
     *            setting this parameter, for JCR default workspace is selected, for CMIS the first repository
     *            obtained through the session object is selected.
     * @param username
     *            Username to connect to content repository
     * @param password
     *            Password to connect to content repository
     * @param connectionType
     *            Connection type; either <b>JCR</b> or <b>CMIS</b>
     * @return a UUID representing matching with the session object obtained using the provided parameters
     * @throws RepositoryAccessException
     */
    @GET
    @Produces(TEXT_PLAIN)
    public Response createSessionByInfo(@QueryParam("repositoryURL") String repositoryURL,
                                        @QueryParam("workspaceName") String workspaceName,
                                        @QueryParam("username") String username,
                                        @QueryParam("password") String password,
                                        @QueryParam("connectionType") String connectionType,
                                        @Context HttpHeaders headers) throws RepositoryAccessException {

        repositoryURL = RestUtil.nullify(repositoryURL);
        workspaceName = RestUtil.nullify(workspaceName);
        username = RestUtil.nullify(username);
        password = RestUtil.nullify(password);
        connectionType = RestUtil.nullify(connectionType);

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should be set\n")
                    .build();
        }

        String sessionKey = sessionManager.createSessionKey(repositoryURL, workspaceName, username, password,
            connectionType);
        ResponseBuilder rb = Response.status(Status.CREATED).entity(sessionKey);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
}
