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

package org.apache.stanbol.entityhub.yard.sesame.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.yard.AbstractYard;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.yard.sesame.SesameYard;
import org.apache.stanbol.entityhub.yard.sesame.SesameYardConfig;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

/**
 * Add file description here!
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 * @author Rupert Westenthaler (rwesten@apache.org)
 */
@Component(
        configurationFactory=true,
        policy= ConfigurationPolicy.REQUIRE,
        specVersion="1.1",
        metatype = true,
        immediate = true
)
@Service(SesameYardComponent.class)
@Properties(value={
        @Property(name=Yard.ID),
        @Property(name=Yard.NAME),
        @Property(name=Yard.DESCRIPTION),
        @Property(name=SesameYardComponent.REPOSITORY_ID),
        @Property(name=SesameYard.CONTEXT_ENABLED, boolValue=true),
        @Property(name=SesameYard.CONTEXT_URI, cardinality=Integer.MAX_VALUE),
        @Property(name=SesameYard.INCLUDE_INFERRED, boolValue=true),
        @Property(name=AbstractYard.DEFAULT_QUERY_RESULT_NUMBER,intValue=-1),
        @Property(name=AbstractYard.MAX_QUERY_RESULT_NUMBER,
            intValue=SesameYardComponent.MAX_QUERY_RESULT_NUMBER)
})
public class SesameYardComponent implements ServiceTrackerCustomizer {

    static final int MAX_QUERY_RESULT_NUMBER = 1024;

    private static Logger log = LoggerFactory.getLogger(SesameYardComponent.class);

    /**
     * The ID of the Repository (key: <code>org.openrdf.repository.Repository.id</code>).
     * If defined this is used to filter a specific repository in case multiple are
     * registered. If not, the one with the highest <code>service.ranking</code> 
     * will get used.
     */
    public static final String REPOSITORY_ID = "org.openrdf.repository.Repository.id";

    private ServiceTracker repositoryTracker;

    private BundleContext bundleContext;

    private ServiceRegistration yardService;

    private Dictionary<String,Object> config;
    private SesameYardConfig yardConfig;

    /**
     * The ID of the tracked {@link Repository} or <code>null</code> if all
     * are tracked
     */
    private String repoId;
    /**
     * The ServiceReference of to the used Sesame {@link Repository}
     */
    private ServiceReference repoServiceReference;

    private SesameYard yard;


    /**
     * Register a service tracker for the KiWiRepositoryService; once it is there, register a new SesameYard.
     *
     * @param context
     * @throws ConfigurationException
     * @throws RepositoryException
     */
    @Activate
    protected final void activate(ComponentContext context) throws ConfigurationException, RepositoryException {
        @SuppressWarnings("unchecked")
        Dictionary<String,Object> config = context.getProperties();
        this.config = config;
        this.bundleContext = context.getBundleContext();

        repoId = (String)config.get(REPOSITORY_ID);
        if(repoId != null && repoId.trim().isEmpty()){
            repoId = null;
        }
        log.info(" - repository ID: {}",repoId);
        
        String filterStr;
        if(repoId != null){
        	filterStr = String.format("(&(objectClass=%s)(%s=%s))", 
                    Repository.class.getName(), 
                    REPOSITORY_ID, repoId);
        } else {
        	filterStr = String.format("(&(objectClass=%s))", Repository.class.getName());
        }

        Filter filter;
        try {
        	log.info(" - service Filter: {}", filterStr);
            filter = bundleContext.createFilter(filterStr);
        } catch (InvalidSyntaxException e){
            throw new ConfigurationException(REPOSITORY_ID, "Unable to build Service "
                + "Filter with parsed Repository ID '"+repoId+"' (filter: '"
                + filterStr +"')",e);
        }
        yardConfig = new SesameYardConfig(config);
        //check context is set 
        Object value = config.get(SesameYard.CONTEXT_ENABLED);
        if(value == null || value.toString().trim().isEmpty()){
            //not set ... set default to TRUE
            yardConfig.setContextEnabled(true); 
        } else if(!yardConfig.isContextEnabled()){
            //if set, check that is is enabled, as disabling this could allow
            //users to access all data in the Repository (something we do not
            //want to allow)!
            throw new ConfigurationException(SesameYard.CONTEXT_ENABLED, 
                "Sesame Contexts MUST BE enabled for the Kiwi TripleStore Yard");
        }
        repositoryTracker = new ServiceTracker(bundleContext, filter, this);
        log.info(" ... start tracking for Sesame Repositories");
        repositoryTracker.open();
    }

    @Deactivate
    protected final void deactivate(ComponentContext context) throws RepositoryException {
    	//closing the serviceTracker will also unregister the yard (if registered)
    	if(repositoryTracker != null) {
            repositoryTracker.close();
        }
    }


    @Override
    public Object addingService(ServiceReference serviceReference) {
        
        Repository repo = (Repository) bundleContext.getService(serviceReference);
        if(repo == null) {
            log.error("could not retrieve Sesame Repository for ServiceRefernece {} "
            		+ "(repository name: {})",
            		serviceReference, repoId);
        } else {
        	if(repoServiceReference != null) { //already a service registered
        		//check if the added service reference has a higher ranking
        		if(serviceReference.compareTo(repoServiceReference) > 0){
        			log.info("re-regsiter SesameYard because ServiceReference {} "
        					+ "has a higher ranking as the currently used one {}!",
        					serviceReference, repoServiceReference);
        			unregisterSesameYard();
        			registerSesameYard(serviceReference, repo);
        		} //else the added reference has a lower ranking ... nothing todo
        	} else { //first service is registered
        		registerSesameYard(serviceReference, repo);
        	}
        }
        return repo;
    }

	/**
	 * @param serviceReference
	 * @param repo
	 */
	protected void registerSesameYard(ServiceReference serviceReference,
			Repository repo) {
		yard = new SesameYard(repo, yardConfig);
		yardService = bundleContext.registerService(Yard.class.getName(), yard, config);
		repoServiceReference = serviceReference;
	}

    /**
     * 
     */
    private void unregisterSesameYard() {
        if(yardService != null){
            yardService.unregister();
            yardService = null;
            repoServiceReference = null;
        }
        if(yard != null){
	    	log.info(" - unregister Yard {} ", yard.getName());
	        yard.close();
	        yard = null;
        }

    }

    @Override
    public void modifiedService(ServiceReference serviceReference, Object o) {
        //ignored
    	log.info(" - ignore modifyService call for {}", serviceReference);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removedService(ServiceReference serviceReference, Object o) {
        if(serviceReference.equals(repoServiceReference)){
        	log.info(" - currently used Repository was removed (ref: {})", serviceReference);
            unregisterSesameYard();
        }
        ServiceReference[] serviceRefs = repositoryTracker.getServiceReferences();
        List<ServiceReference> others = serviceRefs == null ? Collections.<ServiceReference>emptyList() :
            Arrays.asList(serviceRefs);
        if(others.size() > 1){
        	Collections.sort(others); //sort by priority
        }
        boolean registered = false;
        for(Iterator<ServiceReference> refs = others.iterator(); !registered && refs.hasNext();){
        	ServiceReference ref = refs.next();
        	Repository repo = (Repository)repositoryTracker.getService(ref);
        	if(repo != null){
        		log.info(" - re-register Yard with other available repository {}", ref);
        		registerSesameYard(ref, repo);
        		registered = true;
        	}
        }
        bundleContext.ungetService(serviceReference);
    }
}
