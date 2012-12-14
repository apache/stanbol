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
package org.apache.stanbol.enhancer.engines.entityhublinking;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
/**
 * Abstract super class for EntitySearchers that need to track the OSGI service
 * used to lookup Entities. Used by the {@link EntityhubSearcher} and the 
 * {@link ReferencedSiteSearcher} implementation
 * @author Rupert Westenthaler
 *
 */
public abstract class TrackingEntitySearcher<T> implements EntitySearcher {
    
    private ServiceTracker searchServiceTracker;
    protected BundleContext bundleContext; 
    /**
     * Creates a new instance for the parsed parameter
     * @param context the BundleContexed used to create the {@link ServiceTracker}
     * listening for the SearchService
     * @param serviceClass
     * @param filterEntries
     */
    protected TrackingEntitySearcher(BundleContext context, Class<T> serviceClass,Map<String,String> filterEntries, ServiceTrackerCustomizer customizer){
        this.bundleContext = context;
        if(filterEntries == null || filterEntries.isEmpty()){
            searchServiceTracker = new ServiceTracker(context, serviceClass.getName(), customizer);
        } else {
            StringBuffer filterString = new StringBuffer();
            filterString.append(String.format("(&(objectclass=%s)",serviceClass.getName()));
            for(Entry<String,String> filterEntry : filterEntries.entrySet()){
                if(filterEntry.getKey() != null && !filterEntry.getKey().isEmpty() &&
                    filterEntry.getValue() != null && !filterEntry.getValue().isEmpty()){
                    filterString.append(String.format("(%s=%s)",
                        filterEntry.getKey(),filterEntry.getValue()));
                } else {
                    throw new IllegalArgumentException("Illegal filterEntry "+filterEntry+". Both key and value MUST NOT be NULL nor emtpty!");
                }
            }
            filterString.append(')');
            Filter filter;
            try {
                filter = context.createFilter(filterString.toString());
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(String.format(
                    "Unable to build Filter for '%s' (class=%s,filter=%s)", 
                    filterString,serviceClass,filterEntries),e);
            }
            searchServiceTracker = new ServiceTracker(context, filter, customizer);
        }
    }
    /**
     * Starts the tracking by calling {@link ServiceTracker#open()}
     */
    public void open(){
        searchServiceTracker.open();
    }
    /**
     * Getter for the Service used to search for Entities. If the service is
     * currently not available, than this method will return <code>null</code>
     * @return The service of <code>null</code> if not available
     */
    @SuppressWarnings("unchecked") //type is ensured by OSGI
    protected T getSearchService(){
        if(searchServiceTracker == null){
            throw new IllegalStateException("This TrackingEntitySearcher is already closed!");
        } else {
            return (T) searchServiceTracker.getService();
        }
    }
    
    /**
     * Closes the {@link ServiceTracker} used to track the service.
     */
    public void close(){
        searchServiceTracker.close();
        searchServiceTracker = null;
        bundleContext = null;
    }
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
