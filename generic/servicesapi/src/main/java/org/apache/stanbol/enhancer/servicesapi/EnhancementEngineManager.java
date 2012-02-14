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
 * {@link EnhancementEngine} services based on the name.
 */
public interface EnhancementEngineManager {

    /**
     * Getter for the ServiceReference of the EnhancementEngine for the parsed
     * name
     * @param name The name - MUST NOT be <code>null</code> empty and tracked
     * by this tracker
     * @return the {@link ServiceReference} or <code>null</code> if no Engine
     * with the given name is active
     * @throws IllegalArgumentException if the parsed name is <code>null</code>,
     * empty or not tracked by this tracker instance.
     */
    ServiceReference getReference(String name);
    /**
     * Getter for all ServiceReferences of the EnhancementEngines registered for
     * the parsed name. The list of references is sorted by 
     * {@link Constants#SERVICE_RANKING}.
     * @param name The name - MUST NOT be <code>null</code> empty and tracked
     * by this tracker
     * @return the list of {@link ServiceReference}s sorted by 
     * {@link Constants#SERVICE_RANKING} with the highest ranking in the first
     * position. If no engine for the parsed name is active an empty list is
     * returned.
     * with the given name is active
     * @throws IllegalArgumentException if the parsed name is <code>null</code>,
     * empty or not tracked by this tracker instance.
     */
    List<ServiceReference> getReferences(String name) throws IllegalArgumentException;
    /**
     * Getter for the EnhancementEngine for the parsed name
     * @param name The name - MUST NOT be <code>null</code> empty and tracked
     * by this tracker
     * @return The {@link EnhancementEngine} or <code>null</code> if no Engine
     * with the given name is active
     * @throws IllegalArgumentException if the parsed name is <code>null</code>,
     * empty or not tracked by this tracker instance.
     */
    EnhancementEngine getEngine(String name);
    /**
     * Checks if an {@link EnhancementEngine} with the parsed name is active
     * @param name the name
     * @return the state
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as name.
     */
    boolean isEngine(String name);
    /**
     * Getter for all active and tracked engine names. This is a snapshot and
     * this set will change if {@link EnhancementEngine}s become active/inactive.
     * <p>
     * Users of this method should keep in mind to check if the
     * {@link ServiceReference}s and/or {@link EnhancementEngine}s retrieved
     * by the names in the returned set may no longer be available. Therefore
     * it is strongly recommended to checks for <code>null</code> values on
     * results of subsequent calls to {@link #getReference(String)} or
     * {@link #getEngine(String)}. 
     * @return the set with all names of currently active engines.
     */
    Set<String> getActiveEngineNames();
    /**
     * Getter for the {@link EnhancementEngine} service for the parsed
     * service Reference. This method allows to also retrieve the service for
     * other engines than the one with the highest service ranking by using
     * <code><pre>
     *     for(ServiceReference engineRef : tracker.getReferences("test")){
     *         EnhancementEngine engine = tracker.getEngine(engineRef)
     *         if(engine != null) { //may become inactive in the meantime
     *             //save the world by using this engine!
     *         }
     *     }
     * </pre></code>
     * @param engineReference the service reference for an engine tracked by this
     * component
     * @return the referenced {@link EnhancementEngine} or <code>null</code>
     * if no longer available.
     */
    EnhancementEngine getEngine(ServiceReference engineReference);
}
