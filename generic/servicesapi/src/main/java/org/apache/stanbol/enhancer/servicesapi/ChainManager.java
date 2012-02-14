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
package org.apache.stanbol.enhancer.servicesapi;

import java.util.List;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Interface that allows to lookup {@link ServiceReference} and
 * {@link Chain} services based on the name.
 */
public interface ChainManager {
    /**
     * The name of the default fault {@link Chain} as used by the algorithm to
     * determine the Chain returned by {@link #getDefault()}.<p>
     * See the specification of enhancement chains for details.
     */
    String DEFAULT_CHAIN_NAME = "default";

    /**
     * Getter for the names of all currently active enhancement chains. This is 
     * a snapshot and this set will change if {@link Chain}s become 
     * active/inactive.
     * <p>
     * Users of this method should keep in mind to check if the
     * {@link ServiceReference}s and/or {@link Chain}s retrieved
     * by the names in the returned set may no longer be available. Therefore
     * it is strongly recommended to checks for <code>null</code> values on
     * results of subsequent calls to {@link #getReference(String)} or
     * {@link #getChain(String)}. 
     * @return the set with all names of currently active chains.
     */
    Set<String> getActiveChainNames();
    /**
     * Getter for the ServiceReference of the Chain for the parsed
     * name
     * @param name The name - MUST NOT be <code>null</code> empty and tracked
     * by this tracker
     * @return the {@link ServiceReference} or <code>null</code> if no Chain
     * with the given name is active
     * @throws IllegalArgumentException if the parsed name is <code>null</code>,
     * empty or not tracked by this tracker instance.
     */
    ServiceReference getReference(String name);
    /**
     * Getter for all ServiceReferences of the Chains registered for
     * the parsed name. The list of references is sorted by 
     * {@link Constants#SERVICE_RANKING}.
     * @param name The name - MUST NOT be <code>null</code> empty and tracked
     * by this tracker
     * @return the list of {@link ServiceReference}s sorted by 
     * {@link Constants#SERVICE_RANKING} with the highest ranking in the first
     * position. If no chain for the parsed name is active an empty list is
     * returned.
     * with the given name is active
     * @throws IllegalArgumentException if the parsed name is <code>null</code>,
     * empty or not tracked by this tracker instance.
     */
    List<ServiceReference> getReferences(String name) throws IllegalArgumentException;
    /**
     * Getter for the Chain with the highest {@link Constants#SERVICE_RANKING}
     * registered for the parsed name.
     * @param name the name of the Chain
     * @return the Chain or <code>null</code> if no Chain with this name is
     * registered as OSGI service.
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as name.
     */
    Chain getChain(String name);
    /**
     * Getter for the {@link Chain} service for the parsed
     * service Reference. This method allows to also retrieve the service for
     * other chains than the one with the highest service ranking by using
     * <code><pre>
     *     for(ServiceReference chainRef : tracker.getReferences("test")){
     *         Chain chain = tracker.getChain(chainRef)
     *         if(chain != null) { //may become inactive in the meantime
     *             //start the catastrophic chain of events that caused the Big Bang
     *         }
     *     }
     * </pre></code>
     * @param chainReference the service reference for a tracked chain
     * @return the referenced {@link Chain} or <code>null</code>
     * if no longer available.
     */
    Chain getChain(ServiceReference chainReference);
    /**
     * Checks if at least a single Chain with the parsed name is currently
     * registered as OSGI service.
     * @param name the name
     * @return the state
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as name.
     */
    boolean isChain(String name);
    /**
     * Getter for the default Chain. This is the Chain that MUST BE used to
     * enhance {@link ContentItem} if the no Chain was explicitly parsed in the
     * enhancement request. <p>
     * The default Chain is the Chain with the value of the property 
     * {@link Chain#PROPERTY_NAME} is equals to {@link #DEFAULT_CHAIN_NAME} and 
     * the highest <code>{@link Constants#SERVICE_RANKING}</code>.
     * If no Chain with the name "default" exists the Chain with the highest 
     * service ranking (regardless of its name) is considered the default Chain.
     * @return the default Chain or <code>null</code> if no Chain is available
     */
    Chain getDefault();
}
