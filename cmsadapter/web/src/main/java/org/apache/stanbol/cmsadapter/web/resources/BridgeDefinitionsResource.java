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

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.cmsadapter.core.mapping.MappingConfigurationImpl;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingConfiguration;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/cmsadapter/bridgeDefinitions")
public class BridgeDefinitionsResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(BridgeDefinitionsResource.class);

    private static final String MAPPING_ENGINE_COMPONENT_FACTORY_FILTER = "(component.factory=org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngineFactory)";

    private MappingEngine engine;

    public BridgeDefinitionsResource(@Context ServletContext context) {
        try {
            BundleContext bundleContext = (BundleContext) context.getAttribute(BundleContext.class.getName());
            ServiceReference serviceReference = bundleContext.getServiceReferences(null,
                MAPPING_ENGINE_COMPONENT_FACTORY_FILTER)[0];
            ComponentFactory componentFactory = (ComponentFactory) bundleContext.getService(serviceReference);
            @SuppressWarnings("rawtypes")
            ComponentInstance componentInstance = componentFactory.newInstance(new Hashtable());
            this.engine = (MappingEngine) componentInstance.getInstance();

        } catch (InvalidSyntaxException e) {
            logger.warn("Mapping engine instance could not be instantiated", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    /**
     * Takes connection information to access the content managament system and executes the bridge
     * definitions. After completing processing of bridges, generated ontology is stored through <b>Store</b>
     * component.
     * 
     * @param connectionInfo
     *            Information to access content management system when needed. It also includes the URI of the
     *            ontology to be generated
     * @param bridgeDefinitions
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response registeBridgeDefinitions(@FormParam("connectionInfo") ConnectionInfo connectionInfo,
                                             @FormParam("bridgeDefinitions") BridgeDefinitions bridgeDefinitions,
                                             @Context HttpHeaders headers) {

        if(connectionInfo == null) {
            logger.warn("No specified connection info");
            throw new IllegalArgumentException("No specified connection info");
        }
        if(bridgeDefinitions == null) {
            logger.warn("No specified bridge definitions");
            throw new IllegalArgumentException("No specified bridge definitions");
        }
        String ontologyURI = connectionInfo.getOntologyURI();
        try {
            MappingConfiguration conf = new MappingConfigurationImpl();
            conf.setBridgeDefinitions(bridgeDefinitions);
            conf.setConnectionInfo(connectionInfo);
            conf.setOntologyURI(ontologyURI);
            engine.mapCR(conf);
            return Response.status(Status.OK).build();

        } catch (RepositoryAccessException e) {
            logger.warn("Cannot access to repository", e);
        }

        ResponseBuilder rb = Response.status(Status.INTERNAL_SERVER_ERROR);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
}