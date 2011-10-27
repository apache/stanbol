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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.cmsadapter.core.mapping.ContenthubFeederManager;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeederException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.web.utils.RestUtil;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * This resource provides services to submit content repository objects into Contenthub component. Submitted
 * content items are also enhanced by <b>Stanbol Enhancer</b>. This service also enables deletion of content
 * items from Contenthub.
 * 
 * It basically delegates the request suitable {@link ContenthubFeeder} instance.
 * 
 * @author suat
 * 
 */
@Path("/cmsadapter/contenthubfeed")
public class ContenthubFeedResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(ContenthubFeedResource.class);

    ContenthubFeederManager feederManager;

    public ContenthubFeedResource(@Context ServletContext context) {
        feederManager = ContextHelper.getServiceFromContext(ContenthubFeederManager.class, context);
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    /**
     * This service enables submission of content repository objects to Contenthub. Connection to the content
     * repository is established by the provided connection information. This service makes possible to submit
     * content items through either their IDs or paths in the content repository. Enhancements of content
     * items are obtained through <b>Stanbol Enhancer</b> before submitting them to Contenthub.
     * 
     * <p>
     * If <code>id</code> parameter is set, the target object is obtained from the content repository
     * according to its ID. If <code>path</code> parameter is set, first the ID of target object is obtained
     * from the content repository and then the retrieved ID is used in submission of content item. When
     * <code>path</code> parameter is set, it is also possible to process all content repository objects under
     * the specified path by setting <code>recursive</code> parameter as <code>true</code>.
     * 
     * <p>
     * For some cases, it is necessary to know the property of the content repository object that keeps the
     * actual content e.g while processing a nt:unstructured typed JCR content repository object. Such custom
     * properties are specified within the <code>contentProperties</code> parameter.
     * 
     * 
     * @param repositoryURL
     * @param workspaceName
     * @param username
     * @param password
     * @param connectionType
     * @param id
     *            content repository ID of the content item to be submitted
     * @param path
     *            content repository path of the content item to be submitted
     * @param recursive
     *            this parameter is used together with <code>path</code> parameter. Its default value is
     *            <code>false</code>. If it is set as <code>true</code>. All content repository objects under
     *            the specified path are processed.
     * @param contentProperties
     *            this parameter indicates the list of properties that are possibly holding the actual
     *            content. Possible values are passed as comma separated. Its default value is <b>content,
     *            skos:definition</b>.
     * 
     * @return
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response submitObjectsToContenthub(@FormParam("repositoryURL") String repositoryURL,
                                              @FormParam("workspaceName") String workspaceName,
                                              @FormParam("username") String username,
                                              @FormParam("password") String password,
                                              @FormParam("connectionType") String connectionType,
                                              @FormParam("id") String id,
                                              @FormParam("path") String path,
                                              @FormParam("recursive") @DefaultValue("false") boolean recursive,
                                              @FormParam("contentProperties") @DefaultValue("skos:definition,content") String contentProperties) throws RepositoryAccessException,
                                                                                                                                                ContenthubFeederException {

        repositoryURL = RestUtil.nullify(repositoryURL);
        workspaceName = RestUtil.nullify(workspaceName);
        username = RestUtil.nullify(username);
        password = RestUtil.nullify(password);
        connectionType = RestUtil.nullify(connectionType);
        id = RestUtil.nullify(id);
        path = RestUtil.nullify(path);
        contentProperties = RestUtil.nullify(contentProperties);

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            logger.warn("Repository URL, username, password and connection type parameters should not be null");
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should not be null")
                    .build();
        }

        List<String> contentFieldList = parseContentProperties(contentProperties);

        ContenthubFeeder feeder = feederManager.getContenthubFeeder(
            formConnectionInfo(repositoryURL, workspaceName, username, password, connectionType),
            contentFieldList);

        if (id != null) {
            feeder.submitContentItemByID(id);
        } else if (path != null) {
            if (!recursive) {
                feeder.submitContentItemByPath(path);
            } else {
                feeder.submitContentItemsUnderPath(path);
            }
        } else {
            return Response.status(Status.BAD_REQUEST)
                    .entity("There is no parameter specified to select content repository objects\n").build();
        }

        return Response.ok().build();
    }

    /**
     * This service enables deletion of content items from Contenthub. Connection to the content repository is
     * established by the provided connection information. This service makes possible to delete content items
     * through either their IDs or paths in the content repository.
     * 
     * <p>
     * If <code>id</code> parameter is set, the content item is directly tried to be deleted from Contenthub.
     * If <code>path</code> parameter is set, the ID of the target object is first obtained from the content
     * repository according to its path. Then retrieved ID is used to delete related content item from
     * Contenthub.
     * 
     * @param repositoryURL
     * @param workspaceName
     * @param username
     * @param password
     * @param connectionType
     * @param id
     *            content repository ID of the content item to be submitted
     * @param path
     *            content repository path of the content item to be submitted
     * @param recursive
     *            this parameter is used together with <code>path</code> parameter. Its default value is
     *            <code>false</code>. If it is set as <code>true</code>. All content repository objects under
     *            the specified path are processed.
     * @return
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteObjectsFromContenthub(@FormParam("repositoryURL") String repositoryURL,
                                                @FormParam("workspaceName") String workspaceName,
                                                @FormParam("username") String username,
                                                @FormParam("password") String password,
                                                @FormParam("connectionType") String connectionType,
                                                @FormParam("id") String id,
                                                @FormParam("path") String path,
                                                @FormParam("recursive") @DefaultValue("false") boolean recursive) throws RepositoryAccessException,
                                                                                                                 ContenthubFeederException {

        repositoryURL = RestUtil.nullify(repositoryURL);
        workspaceName = RestUtil.nullify(workspaceName);
        username = RestUtil.nullify(username);
        password = RestUtil.nullify(password);
        connectionType = RestUtil.nullify(connectionType);
        id = RestUtil.nullify(id);
        path = RestUtil.nullify(path);

        if (repositoryURL == null || username == null || password == null || connectionType == null) {
            logger.warn("Repository URL, username, password and connection type parameters should not be null");
            return Response
                    .status(Status.BAD_REQUEST)
                    .entity(
                        "Repository URL, username, password and connection type parameters should not be null")
                    .build();
        }

        ContenthubFeeder feeder = feederManager.getContenthubFeeder(formConnectionInfo(repositoryURL,
            workspaceName, username, password, connectionType));

        if (id != null) {
            feeder.deleteContentItemByID(id);
        } else if (path != null) {
            if (!recursive) {
                feeder.deleteContentItemByPath(path);
            } else {
                feeder.deleteContentItemsUnderPath(path);
            }
        } else {
            return Response.status(Status.BAD_REQUEST)
                    .entity("There is no parameter specified to select content repository objects\n").build();
        }

        return Response.ok().build();
    }

    private ConnectionInfo formConnectionInfo(String repositoryURL,
                                              String workspaceName,
                                              String username,
                                              String password,
                                              String connectionType) {
        ConnectionInfo cInfo = new org.apache.stanbol.cmsadapter.servicesapi.repository.ConnectionInfo(
                repositoryURL, workspaceName, username, password, connectionType);
        return cInfo;
    }

    private List<String> parseContentProperties(String contentProperties) {
        List<String> fieldsList = new ArrayList<String>();
        if (contentProperties != null) {
            String[] fields = contentProperties.split(",");
            for (String field : fields) {
                String f = field.trim();
                if (!f.equals("")) {
                    fieldsList.add(f);
                }
            }
        }
        return fieldsList;
    }
}
