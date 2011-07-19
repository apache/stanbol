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

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontology/{ontologyPath:.+}/individuals/{individualPath:.+}/propertyAssertions")
public class ParticularIndividualPropertyAssertions extends BaseStanbolResource{
    private static final Logger logger = LoggerFactory
            .getLogger(ParticularIndividualPropertyAssertions.class);

    private PersistenceStore persistenceStore;

    public ParticularIndividualPropertyAssertions(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    // FIXME Handle PS errors properly, correct return values
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{propertyPath:.+}")
    public Response addPropertyAssertions(@PathParam("ontologyPath") String ontologyPath,
                                          @PathParam("individualPath") String individualPath,
                                          @PathParam("propertyPath") String propertyPath,
                                          @FormParam("literalValues") List<String> literalValues,
                                          @FormParam("objectValues") List<String> objectValues) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String individualURI = resourceManager.getResourceURIForPath(ontologyPath, individualPath);
            String propertyURI = resourceManager.convertEntityRelativePathToURI(propertyPath);
            if (individualURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                if (literalValues != null && literalValues.size() != 0) {
                    for (String literalValue : literalValues) {
                        if (literalValue.equals("")) continue;
                        try {
                            persistenceStore.assertPropertyValue(individualURI, propertyURI, null,
                                literalValue);
                        } catch (Exception e) {
                            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                if (objectValues != null && objectValues.size() != 0) {
                    for (String objectValue : objectValues) {
                        if (objectValue.equals("")) continue;
                        try {
                            persistenceStore.assertPropertyValue(individualURI, propertyURI, objectValue,
                                null);
                        } catch (Exception e) {
                            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return Response.ok().build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{propertyPath:.+}/literals/")
    public Response deleteDatatypePropertyAssertions(@PathParam("ontologyPath") String ontologyPath,
                                                     @PathParam("individualPath") String individualPath,
                                                     @PathParam("propertyPath") String propertyPath,
                                                     @FormParam("value") String value) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String individualURI = resourceManager.getResourceURIForPath(ontologyPath, individualPath);
            String propertyURI = resourceManager.convertEntityRelativePathToURI(propertyPath);
            if (individualURI != null) {
                try {
                    persistenceStore.deletePropertyAssertion(individualURI, propertyURI, null, value);
                } catch (Exception e) {
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }
            }

        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("/{propertyPath:.+}/objects/{value:.+}")
    public Response deleteObjectPropertyAssertions(@PathParam("ontologyPath") String ontologyPath,
                                                   @PathParam("individualPath") String individualPath,
                                                   @PathParam("propertyPath") String propertyPath,
                                                   @PathParam("value") String value) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String individualURI = resourceManager.getResourceURIForPath(ontologyPath, individualPath);
            String propertyURI = resourceManager.convertEntityRelativePathToURI(propertyPath);
            String valueURI = resourceManager.convertEntityRelativePathToURI(value);
            if (individualURI != null) {
                try {
                    persistenceStore.deletePropertyAssertion(individualURI, propertyURI, valueURI, null);
                } catch (Exception e) {
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }
            }

        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return Response.ok().build();
    }
}
