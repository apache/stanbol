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
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.JSON_LD;
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

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ChainsTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


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
    private ContentItemFactory ciFactory;
    @Reference
    private Serializer serializer;
    
    protected Map<String,Map.Entry<ServiceReference,Chain>> _chainCache;
    protected ChainsTracker chainTracker;
    
    @Activate
    public void activate(ComponentContext ctx) {
        final BundleContext bc = ctx.getBundleContext();
        chainTracker = new ChainsTracker(ctx.getBundleContext(), Collections.<String>emptySet(), 
            new ServiceTrackerCustomizer() {
                
                @Override
                public Object addingService(ServiceReference reference) {
                    Object service = bc.getService(reference);
                    if(service != null){
                        _chainCache = null; //rebuild the cache on the next call
                    }
                    return service;
                }
    
                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    _chainCache = null; //rebuild the cache on the next call
                }
    
                @Override
                public void removedService(ServiceReference reference, Object service) {
                    if(reference != null){
                        bc.ungetService(reference);
                        _chainCache = null; //rebuild the cache on the next call
                    }
                }
            });
        chainTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(chainTracker != null){
            chainTracker.close();
            chainTracker = null;
        }
    }
    
    /**
     * Getter for the list of active chain
     * @return the list of active chain (both {@link ServiceReference}
     * and service).
     */
    protected Collection<Entry<ServiceReference,Chain>> getActiveChains(){
        return getChainsMap().values();
    }

    /**
     * Getter for an active chain by name
     * @param name the name of the chain
     * @return the active {@link Chain} (both {@link ServiceReference}
     * and service) or <code>null<code> if none is active with this name
     */
    protected Entry<ServiceReference,Chain> getActiveChains(String name){
        return getChainsMap().get(name);
    }
    /**
     * @return
     */
    private Map<String,Entry<ServiceReference,Chain>> getChainsMap() {
        Map<String, Entry<ServiceReference,Chain>> chains = _chainCache;
        if(chains == null){
            chains = buildChainsMap(chainTracker);
            this._chainCache = Collections.unmodifiableMap(chains);
        }
        return chains;
    }

    @Path("{chain}")
    public GenericEnhancerUiResource get(@PathParam(value = "chain") String chain) {
        return new GenericEnhancerUiResource(chain, jobManager, 
                engineManager, chainTracker, ciFactory, serializer, 
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
    @Produces(value={JSON_LD, APPLICATION_JSON,N3,N_TRIPLE,RDF_JSON,RDF_XML,TURTLE,X_TURTLE})
    public Response getEngines(@Context HttpHeaders headers){
        String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
        Graph graph = new SimpleGraph();
        addActiveChains(getActiveChains(), chainTracker.getDefault(),graph,rootUrl);
        ResponseBuilder res = Response.ok(graph);
        //addCORSOrigin(servletContext,res, headers);
        return res.build();
    }


    public Collection<Chain> getChains(){
        List<Chain> chains = new ArrayList<Chain>();
        for(Entry<ServiceReference,Chain> entry : getActiveChains()){
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
        Entry<ServiceReference,Chain> entry = getActiveChains(name);
        if(entry != null){
            return (String)entry.getKey().getProperty(Constants.SERVICE_PID);
        } else {
            return null;
        }
    }
    public Integer getServiceRanking(String name){
        Entry<ServiceReference,Chain> entry = getActiveChains(name);
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
        Entry<ServiceReference,Chain> entry = getActiveChains(name);
        if(entry != null){
            return (Long)entry.getKey().getProperty(Constants.SERVICE_ID);
        } else {
            return null;
        }
    }
    public Chain getDefaultChain(){
        return chainTracker.getDefault();
    }
    public boolean isDefault(String name){
        return chainTracker.getDefault().getName().equals(name);
    }
    
    
}
