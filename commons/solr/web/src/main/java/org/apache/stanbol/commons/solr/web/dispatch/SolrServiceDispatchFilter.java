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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.solr.core.CoreContainer;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.apache.stanbol.commons.solr.web.impl.SolrDispatchFilterComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 * Rather than using directly a {@link ServiceReference} to parse the
 * {@link CoreContainer} to be used for the dispatch filter this allows to
 * parse the {@link SolrConstants#PROPERTY_CORE_NAME name} or an own
 * {@link Filter} that is used to {@link ServiceTracker track} {@link CoreContainer}
 * instances registered in the OSGI environment. In case of the empty
 * Constructor a simple Class filter is used to track for CoreContainer services.<p>
 * The CoreContainer to be used for the dispatch filter is searched during the
 * the execution of the Servlets 
 * {@link javax.servlet.Filter#init(javax.servlet.FilterConfig)}.<p>
 * This implementation does NOT remove the Filter or change the {@link CoreContainer}
 * on any change in the OSGI environment. See {@link SolrDispatchFilterComponent}
 * if you need this functionality.
 * @author Rupert Westenthaler
 */
public class SolrServiceDispatchFilter extends DelegatingSolrDispatchFilter {

    private static final Comparator<ServiceReference> SERVICE_REFERENCE_COMPARATOR = 
        ServiceReferenceRankingComparator.INSTANCE;
    
    private BundleContext context;
    
    private ServiceReference coreContainerRef;
    
    private Filter filter;
    
    /**
     * Creates a tracking Solr dispatch filter for the CoreContainer with the
     * parsed {@link SolrConstants#PROPERTY_SERVER_NAME} value
     * @param context the context
     * @param solrServerName the name of the CoreContainer (value of the {@link SolrConstants#PROPERTY_SERVER_NAME})
     * @param stc An optional {@link ServiceTrackerCustomizer} used for the tracking
     * the {@link CoreContainer}
     * @throws InvalidSyntaxException if the created {@link Filter} for the parsed name is invalid
     */
    public SolrServiceDispatchFilter(BundleContext context, String solrServerName,ServiceTrackerCustomizer stc) throws InvalidSyntaxException{
        super();
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        this.context = context;
        if(solrServerName == null || solrServerName.isEmpty()){
            throw new IllegalArgumentException("The parsed SolrServer name MUST NOT be NULL nor empty!");
        }
        String filterString = String.format("(&(%s=%s)(%s=%s))",
            Constants.OBJECTCLASS,CoreContainer.class.getName(),
            SolrConstants.PROPERTY_SERVER_NAME,solrServerName);
        filter = context.createFilter(filterString);
    }
    /**
     * Creates a tracking Solr dispatch filter using the parsed filter to select
     * services. Note that the filter MUST assure that all tracked services are
     * {@link CoreContainer} instances!
     * @param context the context
     * @param filter the Filter that selects the {@link CoreContainer} service
     * to be used for Request dispatching.
     * the {@link CoreContainer}
     */
    public SolrServiceDispatchFilter(BundleContext context, Filter filter){
        super();
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        this.context = context;
        if(filter == null){
            throw new IllegalArgumentException("The parsed Filter for tracking CoreContainer instances MUST NOT be NULL!");
        }
        this.filter = filter;
    }
    /**
     * Creates a Dispatch filter for CoreContainer registered as OSGI services.
     * In case more than one {@link CoreContainer} is available the one with the
     * highest {@link Constants#SERVICE_RANKING} will be used. Instances with 
     * no or the same Service rank are not sorted.
     * @param context the context used to look for the CoreContainer
     */
    public SolrServiceDispatchFilter(BundleContext context){
        super();
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        this.context = context;
        this.filter = null;
    }
    @Override
    protected CoreContainer getCoreContainer() {
        ungetCoreContainer(); //unget the previouse used service
        ServiceReference[] references;
        try {
            references = filter == null ? 
                    context.getServiceReferences(CoreContainer.class.getName(), null) :
                        context.getServiceReferences((String)null, filter.toString());
        } catch (InvalidSyntaxException e) {
            references = null;
            //can not be happen, because we created the filter already in the
            //constructor and only need to parse it again because BundleContext
            //is missing a Method to parse a Filter object when getting
            //ServiceReferences
        }
        if(references == null || references.length == 0){
            throw new IllegalStateException("Unable to find CoreContainer instance "+
                (filter != null ? ("for filter"+filter.toString()) : "")+"!");
        } else {
            if(references.length > 1){
                Arrays.sort(references, ServiceReferenceRankingComparator.INSTANCE);
            }
            this.coreContainerRef = references[0];
        }
        Object service = context.getService(coreContainerRef);
        if(service instanceof CoreContainer){
            return (CoreContainer)service;
        } else {
            throw new IllegalStateException("The parsed Filter '"+
                filter.toString()+" selected a service '"+service+"'(class: "+
                service.getClass().getName()+") that is not compatiple with "+
                CoreContainer.class.getName()+"!");
        }
    }
    @Override
    protected void ungetCoreContainer() {
        if(coreContainerRef != null){
            context.ungetService(coreContainerRef);
        }
        coreContainerRef = null;
    }

}
