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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.addActiveChains;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.buildChainsMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;


@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/enhancer/chain")
public class ChainsRootResource extends BaseStanbolResource {

    @Reference
    private EnhancementJobManager jobManager;
    @Reference
    private EnhancementEngineManager engineManager;
    @Reference
    private ChainManager chainManager;
    @Reference
    private ContentItemFactory ciFactory;
    @Reference
    private Serializer serializer;
    
    private Map<String, Entry<ServiceReference,Chain>> chains;
    private Chain defaultChain;
    
    @Activate
    public void activate(ComponentContext ctx) {
        defaultChain = chainManager.getDefault();
        chains = buildChainsMap(chainManager);
    }
    
    
    @Path("{chain}")
    public GenericEnhancerUiResource get(@PathParam(value = "chain") String chain) {
        return new GenericEnhancerUiResource(chain, jobManager, 
                engineManager, chainManager, ciFactory, serializer, 
                getLayoutConfiguration(), getUriInfo());
    }

    /*@OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext,res, headers);
        return res.build();
    }*/

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this),TEXT_HTML);
        //addCORSOrigin(servletContext,res, headers);
        return res.build();
    }
    @GET
    @Produces(value={APPLICATION_JSON,N3,N_TRIPLE,RDF_JSON,RDF_XML,TURTLE,X_TURTLE})
    public Response getEngines(@Context HttpHeaders headers){
        String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
        MGraph graph = new SimpleMGraph();
        addActiveChains(chains.values(),defaultChain,graph,rootUrl);
        ResponseBuilder res = Response.ok(graph);
        //addCORSOrigin(servletContext,res, headers);
        return res.build();
    }


    public Collection<Chain> getChains(){
        List<Chain> chains = new ArrayList<Chain>();
        for(Entry<ServiceReference,Chain> entry : this.chains.values()){
            chains.add(entry.getValue());
        }
        Collections.sort(chains, new Comparator<Chain>() {
            @Override
            public int compare(Chain o1, Chain o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return chains;
    }
    public String getServicePid(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        if(entry != null){
            return (String)entry.getKey().getProperty(Constants.SERVICE_PID);
        } else {
            return null;
        }
    }
    public Integer getServiceRanking(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        Integer ranking = null;
        if(entry != null){
            ranking = (Integer)entry.getKey().getProperty(Constants.SERVICE_RANKING);
        }
        if(ranking == null){
            return new Integer(0);
        } else {
            return ranking;
        }
    }
    public Long getServiceId(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        if(entry != null){
            return (Long)entry.getKey().getProperty(Constants.SERVICE_ID);
        } else {
            return null;
        }
    }
    public Chain getDefaultChain(){
        return defaultChain;
    }
    public boolean isDefault(String name){
        return defaultChain.getName().equals(name);
    }
    
    
}
