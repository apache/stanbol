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
package org.apache.stanbol.commons.solr.web.dispatch;


import javax.servlet.Filter;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Special version of the {@link SolrDispatchFilter} (implemented based on the
 * {@link DelegatingSolrDispatchFilter} abstract class) that does NOT create a
 * new {@link CoreContainer}, but retrieves it via a OSGI {@link ServiceReference}
 * from a {@link BundleContext}. This implementation also ensures that removing
 * the FIlter does NOT {@link CoreContainer#shutdown() shutdown} the 
 * {@link CoreContainer} but instead.
 * {@link BundleContext#ungetService(ServiceReference) releases} the reference.<p>
 * This does make is save to use this {@link Filter} in combination with a 
 * {@link CoreContainer} that is managed as a {@link SolrServerAdapter}.<p>
 * See <a href="">TODO</a> to see how to use {@link Filter}s in combination with
 * the OSGI {@link HttpService}.
 * @author Rupert Westenthaler
 *
 */
public class ReferencedSolrDispatchFilter extends DelegatingSolrDispatchFilter {
    
    private BundleContext context;
    private ServiceReference coreContainerRef;
    
    /**
     * Creates a new referenced Solr server dispatch filter. Referenced because
     * the {@link CoreContainer} is not created (as by the normal
     * {@link SolrDispatchFilter} implementation)
     * but looked up via the parsed {@link BundleContext} based on the
     * {@link ServiceReference}.
     * @param context the BundleContext used to get/unget the CoreContainer service
     * @param serviceReference A {@link ServiceReference} to a CoreContainer
     * registered as OSGI service.
     */
    public ReferencedSolrDispatchFilter(BundleContext context, ServiceReference serviceReference){
        super();
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        if(serviceReference != null){
            throw new IllegalArgumentException("The parsed SerivceReference MUST NOT be NULL!");
        }
        this.context = context;
        this.coreContainerRef = serviceReference;
    }
    
    @Override
    protected CoreContainer getCoreContainer() {
        if(coreContainerRef != null){
            Object service = context.getService(coreContainerRef);
            if(service instanceof CoreContainer){
                return (CoreContainer) service;
            } else if(service != null){ //incompatible service
                context.ungetService(coreContainerRef); //clean up
                coreContainerRef = null;
                throw new IllegalStateException("Service" +service+" returned by ServiceReference "+
                    coreContainerRef+" is not compatible to "+CoreContainer.class.getSimpleName());
            } else {
                String msg = "Unable to get Service for ServiceReference "+coreContainerRef;
                coreContainerRef = null; //clean up
                throw new IllegalStateException(msg);
            }
        } else {
            throw new IllegalStateException("ServiceRegerence was NULL. This indicated" +
            		"that this filter was already destroyed! Reusage of this filter" +
            		"is currently not supported by this implementation. If you need this" +
            		"please report to the Stanbol Development team.");
        }
    }
    
    @Override
    protected void ungetCoreContainer(){
        if(coreContainerRef != null){
            context.ungetService(coreContainerRef);
        }
        coreContainerRef = null;
    }
    
}
