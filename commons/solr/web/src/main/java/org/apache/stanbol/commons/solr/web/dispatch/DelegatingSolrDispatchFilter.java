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
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SolrDispatchFilter} that does not create a new {@link CoreContainer}
 * on initialisation, but instead delegates to a already existing instance.<p>
 * Users of this Class MUST implement two methods <ul>
 * <li> {@link #getCoreContainer()}: Called during 
 * {@link Filter#init(javax.servlet.FilterConfig)} to get the {@link CoreContainer}
 * instance to be used for the Servlet filter.
 * <li> {@link #ungetCoreContainer()}: Called during the {@link Filter#destroy()}
 * method to indicate the the delegate is no longer needed by this Filter
 * {@link ServiceReference} provided by {@link #getCoreContainerReference()}
 * @author Rupert Westenthaler
 *
 */
public abstract class DelegatingSolrDispatchFilter extends org.apache.solr.servlet.SolrDispatchFilter {
    
    private final Logger log = LoggerFactory.getLogger(DelegatingSolrDispatchFilter.class);
    /**
     * The delegate (used to call {@link #ungetCoreContainer()} in case of
     * multiple calls to init)
     */
    private CoreContainer delegate = null;
    
    
    /**
     * Protected Constructor intended to be overwritten by sub classes
     */
    public DelegatingSolrDispatchFilter(){
        super();
    }
    @Override
    protected final CoreContainer createCoreContainer() {
       if(delegate == null){
           delegate = getCoreContainer();
       }
       return delegate;
    }
    
    @Override
    public final void destroy() {
        //we need NOT do shutdown the CoreContainer! Just release the
        //OSGI service!
        try {
            ungetCoreContainer();
        } catch (RuntimeException e) {
            log.error("RuntimeException during ungetCoreContainer ... ignored",e);
        }
    }
    /**
     * Getter for the {@link CoreContainer} used for the Solr dispatch filter
     * @return
     */
    protected abstract CoreContainer getCoreContainer();
    /**
     * Releases the {@link CoreContainer} no longer needed by the Solr dispatch
     * filter.
     */
    protected abstract void ungetCoreContainer();
    
}
