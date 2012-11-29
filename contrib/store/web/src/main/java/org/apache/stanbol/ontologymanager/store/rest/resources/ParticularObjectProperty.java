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

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
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

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.ldviewable.Viewable;

@Path("/ontology/{ontologyPath:.+}/objectProperties/{objectPropertyPath:.+}")
public class ParticularObjectProperty extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(ParticularObjectProperty.class);

    private static final String VIEWABLE_PATH = "/org/apache/stanbol/ontologymanager/store/rest/resources/particularObjectProperty";

    private PersistenceStore persistenceStore;

    // HTML View Variable
    private ObjectPropertyContext metadata;

    public ParticularObjectProperty(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response retrieveObjectPropertyContext(@PathParam("ontologyPath") String ontologyPath,
                                                  @PathParam("objectPropertyPath") String objectPropertyPath,
                                                  @DefaultValue("false") @QueryParam("withInferredAxioms") boolean withInferredAxioms) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();

            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                ObjectPropertyContext objectPropertyContext = persistenceStore.generateObjectPropertyContext(
                    objectPropertyURI, withInferredAxioms);
                response = Response.ok(objectPropertyContext, MediaType.APPLICATION_XML_TYPE).build();
            } catch (Exception e) {
                logger.error("Error ", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseReadLockFor(ontologyPath);
        }
        return response;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response makeSubClassOf(@PathParam("ontologyPath") String ontologyPath,
                                   @PathParam("objectPropertyPath") String objectPropertyPath,
                                   @FormParam("isFunctional") Boolean isFunctional,
                                   @FormParam("isTransitive") Boolean isTransitive,
                                   @FormParam("isSymmetric") Boolean isSymmetric,
                                   @FormParam("isInverseFunctional") Boolean isInverseFunctional) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                if (isFunctional != null || isTransitive != null || isSymmetric != null
                    || isInverseFunctional != null) {
                    persistenceStore.setPropertyAttributes(objectPropertyURI, isFunctional, isTransitive,
                        isSymmetric, isInverseFunctional);
                }
                ObjectPropertyContext objectPropertyContext = persistenceStore.generateObjectPropertyContext(
                    objectPropertyURI, false);
                response = Response.ok(objectPropertyContext, MediaType.APPLICATION_XML_TYPE).build();
            } catch (Exception e) {
                logger.error("Error ", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return response;
    }

    // FIXME Handle PS errors properly, correct return values

    @DELETE
    public void delete(@PathParam("ontologyPath") String ontologyPath,
                       @PathParam("objectPropertyPath") String objectPropertyPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String resourceURI = resourceManager.getResourceURIForPath(ontologyPath, objectPropertyPath);
            persistenceStore.deleteResource(resourceURI);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
    }

    // HTML View Methods
    public ObjectPropertyContext getMetadata() {
        return metadata;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getViewable(@PathParam("ontologyPath") String ontologyPath,
                                @PathParam("objectPropertyPath") String objectPropertyPath,
                                @DefaultValue("false") @QueryParam("withInferredAxioms") boolean withInferredAxioms) {
        Response response = retrieveObjectPropertyContext(ontologyPath, objectPropertyPath,
            withInferredAxioms);
        metadata = (ObjectPropertyContext) response.getEntity();
        return new Viewable(VIEWABLE_PATH, this);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@PathParam("ontologyPath") String ontologyPath,
                           @PathParam("objectPropertyPath") String objectPropertyPath,
                           @FormParam("isFunctional") Boolean isFunctional,
                           @FormParam("isTransitive") Boolean isTransitive,
                           @FormParam("isSymmetric") Boolean isSymmetric,
                           @FormParam("isInverseFunctional") Boolean isInverseFunctional) {
        metadata = (ObjectPropertyContext) makeSubClassOf(ontologyPath, objectPropertyPath, isFunctional,
            isTransitive, isSymmetric, isInverseFunctional).getEntity();
        Response response = Response.seeOther(URI.create(metadata.getPropertyMetaInformation().getHref()))
                .build();
        return response;
    }
}
