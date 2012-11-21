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
package org.apache.stanbol.ontologymanager.store.rest.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.apache.stanbol.ontologymanager.store.model.ImportsForOntology;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;

import org.apache.stanbol.commons.ldviewable.Viewable;

@Path("/ontology/{ontologyPath:.+}/imports")
public class OntologyImports extends BaseStanbolResource {

    private static final String VIEWABLE_PATH = "/org/apache/stanbol/ontologymanager/store/rest/resources/ontologyImports";

    private PersistenceStore store;
    // HTML View Variable
    private ImportsForOntology metadata;

    public OntologyImports(@Context ServletContext context) {
        this.store = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getImports(@PathParam("ontologyPath") String ontologyPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            if (ontologyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                ImportsForOntology imports = store.retrieveOntologyImports(ontologyPath);
                return Response.ok(imports).build();
            } catch (Exception e) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseReadLockFor(ontologyPath);
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addImport(@PathParam("ontologyPath") String ontologyPath,
                              @FormParam("importURI") String importURI) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            if (ontologyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                store.addOntologyImport(ontologyURI, importURI);
                return Response.ok().build();
            } catch (Exception e) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
    }

    @DELETE
    public Response removeImport(@PathParam("ontologyPath") String ontologyPath,
                                 @QueryParam("importURI") String importURI) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            if (ontologyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                store.removeOntologyImport(ontologyURI, importURI);
                return Response.ok().build();
            } catch (Exception e) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
    }

    // HTMLViewMethod
    @GET
    @Produces(MediaType.TEXT_HTML + ";qs=2")
    public Viewable getHTMLImports(@PathParam("ontologyPath") String ontologyPath) {
        Response res = getImports(ontologyPath);
        this.metadata = (ImportsForOntology) res.getEntity();
        return new Viewable(VIEWABLE_PATH, this);
    }

    public ImportsForOntology getMetadata() {
        return metadata;
    }

    @POST
    @Produces(MediaType.TEXT_HTML+";qs=2")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addImportHTML(@PathParam("ontologyPath") String ontologyPath,
                                  @FormParam("importURI") String importURI) throws URISyntaxException {
        addImport(ontologyPath, importURI);
        this.metadata = (ImportsForOntology) getImports(ontologyPath).getEntity();
        return Response.seeOther(new URI(metadata.getOntologyMetaInformation().getHref() + "/imports"))
                .build();
    }

}
