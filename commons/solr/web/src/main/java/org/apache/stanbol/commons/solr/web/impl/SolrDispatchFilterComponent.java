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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.apache.stanbol.commons.solr.web.dispatch.DelegatingSolrDispatchFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that allows to publishes a Solr {@link CoreContainer} with a given
 * name at a given Path by using a {@link SolrDispatchFilter}.<p>
 * This allows more customisation for publishing Solr {@link CoreContainer} as
 * the {@link SolrServerPublishingComponent}, but also requires a specific
 * configuration for earch {@link CoreContainer} to be published.
 * @see SolrServerPublishingComponent
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE, //this requires the CoreContainer name
    specVersion="1.1",
    metatype = true,
    immediate = false
    )
@Service(value=SolrDispatchFilterComponent.class)
public class SolrDispatchFilterComponent {
    
    private final Logger log = LoggerFactory.getLogger(SolrDispatchFilterComponent.class);

    @Property
    public static final String PROPERTY_SERVER_NAME = "org.apache.stanbl.commons.solr.web.dispatchfilter.name";
    
    @Property
    public static final String PROPERTY_PREFIX_PATH = "org.apache.stanbl.commons.solr.web.dispatchfilter.prefix";
    
    /**
     * Will only work within Felix!
     */
    @Reference
    protected ExtHttpService extHttpService;
    
    private ComponentContext context;
    
    private SolrDispatchFilter dispatchFilter;
    private ServiceTracker tracker;
    
    private List<ServiceReference> coreContainerRefs = Collections.synchronizedList(new ArrayList<ServiceReference>());
    
    private CoreContainer coreContainer;
    
    private ServiceTrackerCustomizer trackerCustomizer = new ServiceTrackerCustomizer() {
        
        @Override
        public void removedService(ServiceReference ref, Object service) {
            synchronized (coreContainerRefs) {
                coreContainerRefs.remove(ref);
                if(service.equals(coreContainer)){
                    if(!coreContainerRefs.isEmpty()){
                        coreContainer = (CoreContainer)tracker.getService(coreContainerRefs.get(0));
                    } else {
                        coreContainer = null;
                    }
                    updateFilter(coreContainer);
                } //else the remove does not affect the currently used CoreContainer
            }
            context.getBundleContext().ungetService(ref);
        }
        
        @Override
        public void modifiedService(ServiceReference ref, Object service) {
            //maybe the serviceRanking was modified
            if(coreContainerRefs.size()>1){
                Collections.sort(coreContainerRefs,SERVICE_REFERENCE_COMPARATOR);
                Object bestService = tracker.getService(coreContainerRefs.get(0));
                if(!bestService.equals(coreContainer)){
                    coreContainer = (CoreContainer) bestService;
                    updateFilter(coreContainer);
                } //else ... no change needed
            }// else -> property changes to the only registered CoreContainer
            //are of no interest
        }
        
        @Override
        public Object addingService(ServiceReference ref) {
            Object service = context.getBundleContext().getService(ref);
            if(service instanceof CoreContainer){
                coreContainerRefs.add(ref);
                if(coreContainerRefs.size() > 1){
                    Collections.sort(coreContainerRefs,SERVICE_REFERENCE_COMPARATOR);
                }
                if(ref.equals(coreContainerRefs.get(0))){
                    coreContainer = (CoreContainer)service;
                    updateFilter(coreContainer);
                }
                return service;
            } else { //wrong Filter used to track CoreConatiners!
                throw new IllegalStateException("ServiceTracker selected Service "+
                    service+" that is no instanceof CoreContainer! " +
                    "Please report this on the STANBOL issue tracker of the " +
                    "stanbol-dev mailing list or ");
            }
        }
    };

    /**
     * This is the prefix the Servlet {@link Filter} is registerd (e.g. 
     * '/solr/.*'
     */
    private String prefix;

    /**
     * The properties to be used when registering the Servlet {@link Filter}.
     */
    private Dictionary<String,Object> filterPrpoerties;

    private static final Comparator<ServiceReference> SERVICE_REFERENCE_COMPARATOR = 
        ServiceReferenceRankingComparator.INSTANCE;

    protected String serverName;
   
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException, ServletException {
        this.context = context;
        BundleContext bc = context.getBundleContext();
        Object value = context.getProperties().get(PROPERTY_SERVER_NAME);
        if(value == null || value.toString().isEmpty()) {
            throw new ConfigurationException(PROPERTY_SERVER_NAME, "The configured CoreContainer name MUST NOT be NULL nor empty!");
        }
        serverName = value.toString();
        String filterString = String.format("(&(%s=%s)(%s=%s))",
            Constants.OBJECTCLASS,CoreContainer.class.getName(),
            SolrConstants.PROPERTY_SERVER_NAME,serverName);
        try {
            tracker = new ServiceTracker(bc, bc.createFilter(filterString), trackerCustomizer);
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException(PROPERTY_SERVER_NAME, 
                "Unable to build Filter for parsed CoreContainer name '"+serverName+"'",e);
        }
        value = context.getProperties().get(PROPERTY_PREFIX_PATH);
        final String prefixPath;
        if(value != null){
            prefix = value.toString();
            if(prefix.charAt(0) != '/'){
                prefix = '/'+prefix;
            }
            prefixPath = prefix;
            if(!prefix.endsWith("*")){ //TODO: check if this is a good idea
                prefix = prefix+"/.*";
            }
        } else {
            prefixPath = null;
            prefix = "/.*";
        }
        filterPrpoerties = new Hashtable<String,Object>();
        if(prefixPath != null){
            filterPrpoerties.put("path-prefix", prefixPath);
        }
        //now start tracking! ...
        //  ... as soon as the first CoreContainer is tracked the Filter will
        //      be created and added to the ExtHttpService
        tracker.open();
    }
    
    /**
     * A change was made to the tracked CoreContainer (adding ,removal, ranking change).
     * This removes and re-add the Servlet filter to apply such changes.
     */
    protected void updateFilter(CoreContainer service) {
        if(dispatchFilter != null){
            extHttpService.unregisterFilter(dispatchFilter);
            dispatchFilter = null;
        }
        if(service != null){
            dispatchFilter = new SolrFilter(service);
            try {
                extHttpService.registerFilter(dispatchFilter, prefix, filterPrpoerties, 0, null);
            } catch (ServletException e) {
                throw new IllegalStateException("Unable to register SolrDispatchFilter for" +
                		"CoreContainer with name"+serverName+" (prefix: "+
                		prefix+"| properties: "+filterPrpoerties+").",e);
            }
            log.info("Add ServletFilter for SolrServer {} and prefix {}",
                serverName,prefix);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context){
        updateFilter(null); //removes the filter
        coreContainer = null;
        serverName = null;
        prefix = null;
        filterPrpoerties = null;
        
        
    }

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
