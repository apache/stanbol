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
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.JSON_LD;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.addActiveEngines;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancerUtils.buildEnginesMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.EnginesTracker;
import org.apache.stanbol.enhancer.servicesapi.impl.SingleEngineChain;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/enhancer/engine")
public class EnhancementEnginesRootResource extends BaseStanbolResource {

    @Reference
    private EnhancementJobManager jobManager;
    @Reference
    private ChainManager chainManager;
    @Reference
    private ContentItemFactory ciFactory;

    /**
     * Read-only list of active engines. Do not access directly as this is
     * lazy initialised after any change in the list of active {@link EnhancementEngine}. 
     * Use {@link #getActiveEngine(String)} and {@link #getActiveEngines()} instead. 
     */
    protected Map<String, Entry<ServiceReference,EnhancementEngine>> _enginesCache;
    
    /**
     * Tracks available EnhancementEngines
     */
    private EnginesTracker engineTracker;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        final BundleContext bc = ctx.getBundleContext();
        engineTracker = new EnginesTracker(bc, Collections.<String>emptySet(), 
            new ServiceTrackerCustomizer() {
                
                @Override
                public Object addingService(ServiceReference reference) {
                    Object service = bc.getService(reference);
                    if(service != null){
                        _enginesCache = null; //rebuild the cache on the next call
                    }
                    return service;
                }

                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    _enginesCache = null; //rebuild the cache on the next call
                }

                @Override
                public void removedService(ServiceReference reference, Object service) {
                    if(reference != null){
                        bc.ungetService(reference);
                        _enginesCache = null; //rebuild the cache on the next call
                    }
                }
                
            });
        engineTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(engineTracker != null){
            engineTracker.close();
            engineTracker = null;
        }
    }
    /**
     * Getter for the list of active EnhancementEngines
     * @return the list of active EnhancementEngines (both {@link ServiceReference}
     * and service).
     */
    protected Collection<Entry<ServiceReference,EnhancementEngine>> getActiveEngines(){
        Map<String,Entry<ServiceReference,EnhancementEngine>> engines = getEnginesMap();
        return engines.values();
    }

    /**
     * Getter for an active engine by name
     * @param name the name of the engine
     * @return the active EnhancementEngine (both {@link ServiceReference}
     * and service) or <code>null<code> if none is active with this name
     */
    protected Entry<ServiceReference,EnhancementEngine> getActiveEngine(String name){
        return getEnginesMap().get(name);
    }
    /**
     * @return
     */
    private Map<String,Entry<ServiceReference,EnhancementEngine>> getEnginesMap() {
        Map<String, Entry<ServiceReference,EnhancementEngine>> engines = _enginesCache;
        if(engines == null){
            engines = buildEnginesMap(engineTracker);
            this._enginesCache = Collections.unmodifiableMap(engines);
        }
        return engines;
    }
    
   /* @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
       // enableCORS(servletContext,res, headers);
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
        addActiveEngines(getActiveEngines(), graph, rootUrl);
        ResponseBuilder res = Response.ok(graph);
   //     addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    public Collection<EnhancementEngine> getEngines(){
        List<EnhancementEngine> engines = new ArrayList<EnhancementEngine>();
        for(Entry<ServiceReference,EnhancementEngine> entry : getActiveEngines()){
            engines.add(entry.getValue());
        }
        Collections.sort(engines, new Comparator<EnhancementEngine>() {
            @Override
            public int compare(EnhancementEngine o1, EnhancementEngine o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return engines;
    }
    public String getServicePid(String name){
        Entry<ServiceReference,EnhancementEngine> entry = getActiveEngine(name);
        if(entry != null){
            return (String)entry.getKey().getProperty(Constants.SERVICE_PID);
        } else {
            return null;
        }
    }
    public Integer getServiceRanking(String name){
        Entry<ServiceReference,EnhancementEngine> entry = getActiveEngine(name);
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
        Entry<ServiceReference,EnhancementEngine> entry = getActiveEngine(name);
        if(entry != null){
            return (Long)entry.getKey().getProperty(Constants.SERVICE_ID);
        } else {
            return null;
        }
    }
    
    @Path("{engineName}")
    public EngineResource getEngine(@PathParam(value = "engineName") String name) {
        return new EngineResource(name, jobManager, engineTracker, chainManager, 
                ciFactory, getLayoutConfiguration(), getUriInfo());
    }

    public static class EngineResource extends AbstractEnhancerResource {

        private final List<EnhancementEngine> engines;
        private final List<ServiceReference> engineRefs;
        private final String name;

        public EngineResource(
                String name,
                EnhancementJobManager jobManager,
                EnhancementEngineManager engineManager,
                ChainManager chainManager,
                ContentItemFactory ciFactory,
                LayoutConfiguration layoutConfiguration, 
                UriInfo uriInfo) {
            super(jobManager, engineManager, chainManager, ciFactory, 
                    layoutConfiguration, uriInfo);
            this.name = name;
            engineRefs = engineManager.getReferences(name);
            if (engineRefs == null || engineRefs.isEmpty()) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            engines = new ArrayList<EnhancementEngine>(engineRefs.size());
            for (Iterator<ServiceReference> it = engineRefs.iterator(); it.hasNext();) {
                EnhancementEngine engine = engineManager.getEngine(it.next());
                if (engine == null) { //removed in the meantime
                    it.remove();
                } else {
                    engines.add(engine);
                }
            }
            if (engines.isEmpty()) { //in the meantime deactivated ...
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        }


        /**
         * Wraps the engine with the highest service ranking with a
         * {@link SingleEngineChain}.
         *
         * @see
         * org.apache.stanbol.enhancer.jersey.resource.AbstractEnhancerResource#getChain()
         */
        @Override
        protected Chain getChain() {
            return new SingleEngineChain(engines.get(0));
        }

        /*   @OPTIONS
         public Response handleCorsPreflight(@Context HttpHeaders headers){
         ResponseBuilder res = Response.ok();
         enableCORS(servletContext,res, headers);
         return res.build();
         }*/
        @GET
        @Produces(TEXT_HTML)
        public Response get(@Context HttpHeaders headers) {
            ResponseBuilder res = Response.ok(new Viewable("index", this), TEXT_HTML);
            // addCORSOrigin(servletContext,res, headers);
            return res.build();
        }

        public EnhancementEngine getEngine() {
            return engines.get(0);
        }

        public List<EnhancementEngine> getEngines() {
            return engines;
        }

        public String getName() {
            return name;
        }

        public Integer getRanking() {
            Integer ranking = (Integer) engineRefs.get(0).getProperty(Constants.SERVICE_RANKING);
            if (ranking == null) {
                return new Integer(0);
            } else {
                return ranking;
            }
        }

        public Long getId() {
            return (Long) engineRefs.get(0).getProperty(Constants.SERVICE_ID);
        }

        public String getPid() {
            return (String) engineRefs.get(0).getProperty(Constants.SERVICE_PID);
        }

        public Integer getOrdering() {
            Integer ordering = null;
            if (engines.get(0) instanceof ServiceProperties) {
                ordering = (Integer) ((ServiceProperties) engines.get(0))
                        .getServiceProperties()
                        .get(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING);

            }
            if (ordering == null) {
                return new Integer(0);
            } else {
                return ordering;
            }
        }

        public boolean isMultipleEngines() {
            return engines.size() > 1;
        }

        public List<EnhancementEngine> getAdditionalEngines() {
            return engines.subList(1, engines.size());
        }
    }
}
