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

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ResourceMetaInformationType;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.ldviewable.Viewable;

@Path("/ontology/{ontologyPath:.+}")
public class ParticularOntology extends BaseStanbolResource{
    private static final Logger logger = LoggerFactory.getLogger(ParticularOntology.class);

    private static final String VIEWABLE_PATH = "/org/apache/stanbol/ontologymanager/store/rest/resources/particularOntology";

    private PersistenceStore persistenceStore;

    // View Variables
    private OntologyMetaInformation metadata;

    public ParticularOntology(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }


    @GET
    @Produces("application/rdf+xml")
    public String retrieveOntology(@PathParam("ontologyPath") String ontologyPath,
                                   @DefaultValue("false") @QueryParam("withInferredAxioms") boolean withInferredAxioms) {
        String result = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);

            result = persistenceStore.retrieveOntology(ontologyURI, "RDF/XML", withInferredAxioms);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            lockManager.releaseReadLockFor(ontologyPath);
        }
        return result;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response retrieveOntologyMetadata(@PathParam("ontologyPath") String ontologyPath,
                                             @QueryParam("retrieveResourceWithURI") String retrieveResourceWithURI) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);

            if (retrieveResourceWithURI == null) {
                if (ontologyURI == null) {
                    throw new WebApplicationException(Status.NOT_FOUND);
                }
                try {
                    OntologyMetaInformation ontologyMetaInformation = persistenceStore
                            .retrieveOntologyMetaInformation(ontologyURI);

                    response = Response.ok(ontologyMetaInformation, MediaType.APPLICATION_XML_TYPE).build();
                } catch (Exception e) {
                    logger.error("Error ", e);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
            } else if (retrieveResourceWithURI != null) {
                try {
                    ResourceMetaInformationType resourceMetaInformationType = persistenceStore
                            .retrieveResourceWithURI(retrieveResourceWithURI);

                    if (resourceMetaInformationType != null) {
                        response = Response.ok(resourceMetaInformationType, MediaType.APPLICATION_XML_TYPE)
                                .build();
                    } else {
                        throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }
                } catch (Exception e) {
                    logger.error("Error ", e);
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        } finally {
            lockManager.releaseReadLockFor(ontologyPath);
        }
        return response;
    }

    // The Java method will process HTTP DELETE requests
    @DELETE
    public void delete(@PathParam("ontologyPath") String ontologyPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            persistenceStore.deleteOntology(ontologyURI);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
    }

    // HTML View Methods

    public OntologyMetaInformation getMetadata() {
        return metadata;
    }

    @GET
    @Produces(MediaType.TEXT_HTML+";qs=2")
    public Viewable getViewable(@PathParam("ontologyPath") String ontologyPath,
                                @QueryParam("retrieveResourceWithURI") String retrieveResourceWithURI) {
        Response response = retrieveOntologyMetadata(ontologyPath, retrieveResourceWithURI);
        metadata = (OntologyMetaInformation) response.getEntity();
        return new Viewable(VIEWABLE_PATH, this);
    }

    @POST
    @Produces("application/rdf+xml")
    public String mergeOntology(@PathParam("ontologyPath") String ontologyPath,
                                @FormParam("targetOntology") String targetOntology,
                                @FormParam("targetOntologyBaseURI") String targetOntologyBaseURI) {
        String result = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);

            result = persistenceStore
                    .mergeOntology(ontologyURI, targetOntology, targetOntologyBaseURI, false);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return result;
    }

}
