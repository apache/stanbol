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
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.ldviewable.Viewable;

@Path("/ontology/{ontologyPath:.+}/objectProperties")
public class OntologyObjectProperties extends BaseStanbolResource{
    private static final Logger logger = LoggerFactory.getLogger(OntologyObjectProperties.class);
    private static final String VIEWABLE_PATH = "/org/apache/stanbol/ontologymanager/store/rest/resources/ontologyObjectProperties";

    private PersistenceStore persistenceStore;

    // HTML View Variable
    private ObjectPropertiesForOntology metadata;

    public OntologyObjectProperties(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response retrieveOntologyObjectProperties(@PathParam("ontologyPath") String ontologyPath) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            if (ontologyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                ObjectPropertiesForOntology objectPropertiesForOntology = persistenceStore
                        .retrieveObjectPropertiesOfOntology(ontologyURI);
                response = Response.ok(objectPropertiesForOntology, MediaType.APPLICATION_XML_TYPE).build();
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
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response generateDatatypeProperty(@PathParam("ontologyPath") String ontologyPath,
                                             @FormParam("objectPropertyURI") String objectPropertyURI) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            if (ontologyURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                PropertyMetaInformation objectPropertyMetaInformation = persistenceStore
                        .generateObjectPropertyForOntology(ontologyURI, objectPropertyURI);
                response = Response.ok(objectPropertyMetaInformation, MediaType.APPLICATION_XML_TYPE).build();
            } catch (Exception e) {
                logger.error("Error ", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return response;
    }

    // HTML View Methods

    public ObjectPropertiesForOntology getMetadata() {
        return metadata;
    }

    @GET
    @Produces(MediaType.TEXT_HTML+";qs=2")
    public Viewable getViewable(@PathParam("ontologyPath") String ontologyPath) {
        Response response = retrieveOntologyObjectProperties(ontologyPath);
        metadata = (ObjectPropertiesForOntology) response.getEntity();
        return new Viewable(VIEWABLE_PATH, this);
    }

    @POST
    @Produces(MediaType.TEXT_HTML+";qs=2")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createAndRedirect(@PathParam("ontologyPath") String ontologyPath,
                                      @FormParam("objectPropertyURI") String objectPropertyURI) {
        Response response = this.generateDatatypeProperty(ontologyPath, objectPropertyURI);
        PropertyMetaInformation pmi = (PropertyMetaInformation) response.getEntity();
        try {
            return Response.seeOther(URI.create(pmi.getHref())).build();
        } catch (Exception e) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
}
