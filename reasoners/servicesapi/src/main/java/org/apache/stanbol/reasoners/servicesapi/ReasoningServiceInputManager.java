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
package org.apache.stanbol.reasoners.servicesapi;

import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;

/**
 * A {@see ReasoningServiceInputManager} must be able to collect {@see ReasoningServiceInputProvider}s and
 * traverse the whole data on {@see #getInputData(Class<T> type)}. 
 * Only input providers which support Type must be activated.
 * 
 * @author enridaga
 *
 */
public interface ReasoningServiceInputManager {

    /**
     * Add an input provider
     * 
     * @param provider
     */
    public void addInputProvider(ReasoningServiceInputProvider provider);

    /**
     * Remove the input provider
     * 
     * @param provider
     */
    public void removeInputProvider(ReasoningServiceInputProvider provider);

    /**
     * Get the input data. This should iterate over the collection from all the registered input providers.
     * 
     * Consider that this can be called more then once, to obtain more then one input depending on the type.
     * 
     * It is the Type of the object to instruct about its usage.
     * 
     * @param type
     * @return
     */
    public <T> Iterator<T> getInputData(Class<T> type);
    
    /**
     * Returns the immutable list of registered providers.
     * 
     * @return
     */
    public List<ReasoningServiceInputProvider> getProviders();
}
