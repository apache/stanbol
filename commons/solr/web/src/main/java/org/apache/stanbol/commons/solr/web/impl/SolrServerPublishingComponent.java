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
package org.apache.stanbol.commons.solr.web.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_PUBLISH_REST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.apache.stanbol.commons.solr.web.dispatch.DelegatingSolrDispatchFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that publishes all Solr {@link CoreContainer} (registered as
 * OSGI service) where
 * {@link SolrConstants#PROPERTY_SERVER_PUBLISH_REST} is enabled via a
 * {@link SolrDispatchFilter} under <code>/{global-prefix}/{server-name}</code>
 * where <ul>
 * <li> <code>global-prefix</code> is the configured {@link #GLOBAL_PREFIX} value
 * <li> <code>server-name</code> is the {@link SolrConstants#PROPERTY_SERVER_NAME}
 * property of the {@link ServiceReference} for the {@link CoreContainer}.
 * <p>
 * Note that {@link CoreContainer} without a value for the 
 * {@link SolrConstants#PROPERTY_SERVER_NAME server name} properties are not
 * published.<p>
 * To publish specific {@link CoreContainer} at specific paths one can use the
 * {@link SolrDispatchFilterComponent}.
 * @see SolrDispatchFilterComponent
 * @author Rupert Westenthaler
 *
 */
@Component(
    metatype = true,
    immediate = true
    )
public class SolrServerPublishingComponent {
    
    private final Logger log = LoggerFactory.getLogger(SolrServerPublishingComponent.class);

    /**
     * The default value for {@link #GLOBAL_PREFIX}
     */
    private static final String DEFAUTL_GLOBAL_PREFIX = "/solr/";

    /**
     * The prefix used as prefix in front of the 
     * {@link SolrConstants#PROPERTY_SERVER_NAME}.<p>
     * <pre>
     *     http://{host}:{port}/{stanbol-prefix}/{global-prefix}/{solr-server-name}/{solr-core-name}
     * </pre>
     */
    @Property(value=DEFAUTL_GLOBAL_PREFIX)
    private static final String GLOBAL_PREFIX = "org.apache.stanbol.commons.solr.web.dispatchfilter.prefix";
    
    
    /**
     * The key is the name and the value represents a sorted list of
     * {@link ServiceReference} to {@link CoreContainer} based on the
     * {@link SolrConstants#PROPERTY_SERVER_RANKING} value.<p>
     * Used to synchronise calls from the {@link #tracker} (so that not to
     * update two Filters at the same time.
     */
    protected final Map<String,List<ServiceReference>> solrServerRefMap = new HashMap<String,List<ServiceReference>>();
    
    /**
     * The map with the registered Filter for a name.
     */
    private final Map<String,Filter> published = new HashMap<String,Filter>();
    /**
     * Will only work within Apache Felix. Registration of servlet {@link Filter}s
     * is not part of the standard OSGI {@link HttpService} and therefore each
     * OSGI implementation currently defines its own Interface to allow this.<p>
     * To make this available in every OSGI Environment we would need to implement
     * an own service with different implementation for supported OSGI
     * Environments - or to wait until the standard is updated to also support
     * filters.
     */
    @Reference
    protected ExtHttpService extHttpService;
    
    protected ComponentContext context;
    
    private ServiceTracker tracker;
    
    /**
     * the global prefix with an '/' at the begin and end
     */
    private String gloablPrefix;
        
    
    /**
     * Customiser for the {@link ServiceTracker} that tracks all {@link CoreContainer}
     * services. This needs to evaluate <ul>
     * <li> The {@link SolrConstants#PROPERTY_SERVER_NAME} to determine the path
     * under that the {@link CoreContainer} is registered. If no name is defined
     * the core will not be published!
     * <li> The {@link SolrConstants#PROPERTY_SERVER_PUBLISH_REST} to determine
     * if the Solr RESTful API of a {@link CoreContainer} should be published to 
     * the {@link ExtHttpService} via a servlet {@link Filter}
     * <li> The {@link SolrConstants#PROPERTY_SERVER_RANKING} to publish the
     * {@link CoreContainer} with the highest ranking in case two 
     * {@link CoreContainer} do use the same 
     * {@link SolrConstants#PROPERTY_SERVER_NAME} value
     * </ul>
     */
    private ServiceTrackerCustomizer trackerCustomizer = new ServiceTrackerCustomizer() {
        
        @Override
        public void removedService(ServiceReference ref, Object service) {
            if(isPublished(ref)){ //this ensures also a valid name
                remove(ref.getProperty(PROPERTY_SERVER_NAME).toString(),ref);
            } //else nothing to do
            context.getBundleContext().ungetService(ref);
        }

        
        @Override
        public void modifiedService(ServiceReference ref, Object service) {
            if(isPublished(ref)){
                addOrUpdate(ref.getProperty(PROPERTY_SERVER_NAME).toString(), ref,
                    (CoreContainer)service);
            } else { //the config might have changed to set publishREST to false
                Object value = ref.getProperty(PROPERTY_SERVER_NAME);
                if(value != null){
                    remove(value.toString(),ref);
                }
            }
        }
        private void addOrUpdate(String name, ServiceReference ref,CoreContainer server){
            synchronized (solrServerRefMap) {
                List<ServiceReference> refs = solrServerRefMap.get(name);
                ServiceReference oldBest = refs != null && !refs.isEmpty() ?
                        refs.get(0) : null;
                //maybe publishREST was set to TRUE or a name was added
                //so modified might also mean adding in this context
                if(refs == null){ //maybe this is even the first for this name
                    refs = new ArrayList<ServiceReference>(3);
                    solrServerRefMap.put(name, refs);
                }
                if(!refs.contains(ref)){ //check if we need to add the ref
                    refs.add(ref);
                }
                if(refs.size()>1){ //if more than one
                    //a change to the serviceRanking config might change 
                    //the order modified
                    Collections.sort(refs,ServiceReferenceRankingComparator.INSTANCE);
                }
                if(oldBest == null || !refs.get(0).equals(oldBest)){
                    //the first entry changed ... update the filter
                    updateFilter(name,ref,server);
                } //else ... no change needed
            }
        }
        private void remove(String name, ServiceReference ref){
            synchronized (solrServerRefMap) {
                List<ServiceReference> refs = solrServerRefMap.get(name);
                if(refs != null){
                    ServiceReference best = refs.get(0);
                    refs.remove(ref);
                    if(refs.isEmpty()){
                        solrServerRefMap.remove(name);
                        updateFilter(name,null,null);
                    } else if(!refs.get(0).equals(best)){
                        //here the equals check based on ServiceReferencees is OK
                        //because we only check if the first element in the list
                        //changed ... so even an instance check would be OK
                        updateFilter(name,refs.get(0),null);
                    }
                } // no cores for that name ... nothing to remove
            }
        }
        
        @Override
        public Object addingService(ServiceReference ref) {
            CoreContainer service = (CoreContainer)context.getBundleContext().getService(ref);
            if(isPublished(ref)){
                addOrUpdate(ref.getProperty(PROPERTY_SERVER_NAME).toString(), ref,service);
            }
            return context.getBundleContext().getService(ref);
        }
        /**
         * checks if the parsed {@link ServiceReference} refers to a 
         * {@link SolrCore} that needs to be published.
         * @param ref the ServiceReference
         * @return <code>true</code> if the referenced {@link CoreContainer}
         * needs to be published and defines the required
         * {@link SolrConstants#PROPERTY_SERVER_NAME server name} property.
         */
        private boolean isPublished(ServiceReference ref) {
            Object value = ref.getProperty(PROPERTY_SERVER_PUBLISH_REST);
            boolean publishState;
            if(value instanceof Boolean) {
                publishState = ((Boolean)value).booleanValue();
            } else if(value != null){
                publishState = Boolean.parseBoolean(value.toString());
            } else {
                publishState = SolrConstants.DEFAULT_PUBLISH_REST;
            }
            if(publishState){
                //if publishing is enabled also check for a valid name
                value = ref.getProperty(PROPERTY_SERVER_NAME);
                return value != null && !value.toString().isEmpty();
            } else {
                return false;
            }
        }
    };

   
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException, ServletException {
        this.context = context;
        BundleContext bc = context.getBundleContext();
        Object value = context.getProperties().get(GLOBAL_PREFIX);
        if(value != null){
            gloablPrefix = value.toString();
            if(gloablPrefix.charAt(0) != '/'){
                gloablPrefix = '/'+gloablPrefix;
            }
            if(gloablPrefix.charAt(gloablPrefix.length()-1) != '/') {
                gloablPrefix = gloablPrefix+'/';
            }
        } else {
            gloablPrefix = DEFAUTL_GLOBAL_PREFIX;
        }
        tracker = new ServiceTracker(bc,CoreContainer.class.getName(), 
                trackerCustomizer);
        //now start tracking! ...
        //  ... as soon as the first CoreContainer is tracked the Filter will
        //      be created and added to the ExtHttpService
        tracker.open();
    }
    
    /**
     * A change was made to the tracked CoreContainer (adding ,removal, ranking change).
     * This removes and re-add the Servlet filter to apply such changes.
     * @param name The name of the filter to be updated
     * @param ref The serviceReference for the new CoreContainer to be added for
     * the parsed name. <code>null</code> if the filter for that name needs only
     * to be removed
     * @param server The {@link CoreContainer} may be parsed in cases a reference
     * is already available. If not the {@link #tracker} is used to look it up
     * based on the parsed reference. This is basically a workaround for the
     * fact that if the call originates form 
     * {@link ServiceTrackerCustomizer#addingService(ServiceReference)} the
     * {@link CoreContainer} is not yet available via the tracker.
     */
    protected void updateFilter(String name, ServiceReference ref,CoreContainer server) {
        String serverPrefix = gloablPrefix+name;
        Filter filter = published.remove(name);
        if(filter != null) { 
            extHttpService.unregisterFilter(filter);
            filter = null;
            log.info("removed ServletFilter for SolrServer {} and prefix {}",
                name, serverPrefix);
        } // else no current filter for that name
        if(ref != null || server != null){ 
            if(server == null){
                server  = (CoreContainer)tracker.getService(ref);
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(CoreContainer.class.getClassLoader());
            try {
                filter = new SolrFilter(server);
            }finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            Dictionary<String,Object> filterPrpoerties = new Hashtable<String,Object>();
            filterPrpoerties.put("path-prefix", serverPrefix);
            String filterPrefix = serverPrefix+"/.*";
            try {
                extHttpService.registerFilter(filter, 
                    filterPrefix, filterPrpoerties, 0, null);
            } catch (ServletException e) {
                throw new IllegalStateException("Unable to register SolrDispatchFilter for" +
                		"CoreContainer with name"+name+" (prefix: "+
                		filterPrefix+"| properties: "+filterPrpoerties+").",e);
            }
            log.info("added ServletFilter for SolrServer {} and prefix {}",
                name,serverPrefix);
        } // else no new filter to add
    }

    @Deactivate
    protected void deactivate(ComponentContext context){
        //stop tracking
        tracker.close();
        tracker = null;
        //remove the filters
        synchronized (solrServerRefMap) {
            //copy keys to an array to avoid concurrent modifications
            for(String name : published.keySet().toArray(new String[published.size()])){
                updateFilter(name, null,null); //remove all active filters
            }
        }
        published.clear();
        solrServerRefMap.clear();
        context = null;
    }

    /**
     * Simple {@link DelegatingSolrDispatchFilter} implementation that uses the
     * {@link CoreContainer} parsed within the constructor
     * @author Rupert Westenthaler
     */
    private class SolrFilter extends DelegatingSolrDispatchFilter {

        private CoreContainer server;
        protected SolrFilter(CoreContainer server) {
            super();
            this.server = server;
        }
        @Override
        protected CoreContainer getCoreContainer() {
            return server;
        }
        @Override
        protected void ungetCoreContainer() {
            server = null;
        }

        
    }
    
}
