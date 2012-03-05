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
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.SingleEngineChain;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.sun.jersey.api.view.Viewable;

@Path("/enhancer/engine/{engineName}")
public class EnhancementEngineResource extends AbstractEnhancerResource {

    
    private final List<EnhancementEngine> engines;
    private final List<ServiceReference> engineRefs;
    private final String name;
    
    public EnhancementEngineResource(@PathParam(value = "engineName") String name,
                                     @Context ServletContext context) {
        super(context);
        // bind the job manager by looking it up from the servlet request context
//        EnhancementEngineManager engineManager = 
//                ContextHelper.getServiceFromContext(EnhancementEngineManager.class, context);
        if(engineManager == null){
            throw new WebApplicationException(
                new IllegalStateException(
                    "EnhancementEngineManager service not available"));
        }
        this.name = name;
        engineRefs = engineManager.getReferences(name);
        if(engineRefs == null || engineRefs.isEmpty()){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        engines = new ArrayList<EnhancementEngine>(engineRefs.size());
        for(Iterator<ServiceReference> it = engineRefs.iterator();it.hasNext();){
            EnhancementEngine engine = engineManager.getEngine(it.next());
            if(engine == null){ //removed in the meantime
                it.remove();
            } else {
                engines.add(engine);
            }
        }
        if(engines.isEmpty()){ //in the meantime deactivated ...
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
    /**
     * Wraps the engine with the highest service ranking with a
     * {@link SingleEngineChain}.
     * @see org.apache.stanbol.enhancer.jersey.resource.AbstractEnhancerResource#getChain()
     */
    @Override
    protected Chain getChain() {
        return new SingleEngineChain(engines.get(0));
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext,res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this),TEXT_HTML);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    public EnhancementEngine getEngine(){
        return engines.get(0);
    }
    public List<EnhancementEngine> getEngines(){
        return engines;
    }
    public String getName(){
        return name;
    }
    public Integer getRanking(){
        Integer ranking = (Integer)engineRefs.get(0).getProperty(Constants.SERVICE_RANKING);
        if(ranking == null){
            return new Integer(0);
        } else {
            return ranking;
        }
    }
    public Long getId(){
        return (Long)engineRefs.get(0).getProperty(Constants.SERVICE_ID);
    }
    public String getPid(){
        return (String)engineRefs.get(0).getProperty(Constants.SERVICE_PID);
    }
    public Integer getOrdering(){
        Integer ordering = null;
        if(engines.get(0) instanceof ServiceProperties){
            ordering = (Integer)((ServiceProperties)engines.get(0))
                    .getServiceProperties()
                    .get(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING);
            
        }
        if(ordering == null){
            return new Integer(0);
        } else {
            return ordering;
        }
    }
    public boolean isMultipleEngines(){
        return engines.size() > 1;
    }
    public List<EnhancementEngine> getAdditionalEngines(){
        return engines.subList(1, engines.size());
    }
}
