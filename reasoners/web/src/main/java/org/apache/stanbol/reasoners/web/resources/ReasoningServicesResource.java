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
package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServicesManager;
import org.apache.stanbol.reasoners.servicesapi.UnboundReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.annotations.Documentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Home page of the /reasoners module
 * 
 * @author enridaga
 *
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/reasoners")
public class ReasoningServicesResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReasoningServicesManager reasoningServicesManager;

    public ReasoningServicesResource() {
        super();
    }

    public String getCurrentPath() {
        return uriInfo.getPath().replaceAll("[\\/]*$", "");
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getDocumentation(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        return rb.build();
    }

    private ReasoningService<?,?,?> service = null;
        
    @GET
    @Produces(TEXT_HTML)
    @Path("{service}")
    public Response getServiceDocumentation(@PathParam(value = "service") String serviceID,
                                            @Context HttpHeaders headers) {
    	try {
			this.service = this.getServicesManager().get(serviceID);
		} catch (UnboundReasoningServiceException e) {
			log.info("Service {} is not bound", serviceID);
			
			ResponseBuilder rb = Response.status(Status.NOT_FOUND);
	        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
	        return rb.build();
			
		}
    	 ResponseBuilder rb = Response.ok(new Viewable("service", this));
         rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
         return rb.build();
    }
    
    private ReasoningServicesManager getServicesManager() {
        log.debug("(getServicesManager()) ");
        return reasoningServicesManager;
    }

    public Set<ReasoningService<?,?,?>> getActiveServices() {
        log.debug("(getActiveServices()) There are {} reasoning services", getServicesManager().size());
        return getServicesManager().asUnmodifiableSet();
    }

    public ReasoningService<?, ?, ?> getService(){
    	return this.service;
    }

    public Map<String,String> getServiceDescription(){
    	return getServiceDescription(service);
    }
    
    public Map<String,String> getServiceDescription(ReasoningService<?,?,?> service){
    	Class<?> serviceC = service.getClass();
	 	String name;
		try {
			name = serviceC.getAnnotation(Documentation.class).name();
		} catch (NullPointerException e) {
    		log.warn("The service {} is not documented: missing name", serviceC);
			name="";
		}
	 	String description;
		try {
			description = serviceC.getAnnotation(Documentation.class).description();
		} catch (NullPointerException e) {
    		log.warn("The service {} is not documented: missing description", serviceC);
    		description="";
		}
	 	// String file = serviceC.getAnnotation(Documentation.class).file();
		Map<String,String> serviceProperties = new HashMap<String,String>();
		serviceProperties.put("name", name);
		serviceProperties.put("description", description);
		// serviceProperties.put("file", file);
		serviceProperties.put("path", service.getPath());
		return serviceProperties;
    }
    
    public List<Map<String,String>> getServicesDescription(){
    	List<Map<String,String>> descriptions = new ArrayList<Map<String,String>>();
    	for(ReasoningService<?, ?, ?> service : getActiveServices()){
    		descriptions.add(getServiceDescription(service));
    	}
    	return descriptions;
    }
}
