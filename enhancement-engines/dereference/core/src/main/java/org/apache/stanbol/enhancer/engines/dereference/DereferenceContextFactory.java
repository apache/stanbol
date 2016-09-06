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

package org.apache.stanbol.enhancer.engines.dereference;

import java.util.Map;

/**
 * Factory used by the {@link EntityDereferenceEngine} to create
 * {@link DereferenceContext} instances for enhancement requests.
 * @author Rupert Westenthaler
 *
 */
public interface DereferenceContextFactory {

    /**
     * Creates a Dereference Context for the given DereferenceEngine configuration
     * and the request specific enhancement properties
     * @param engine the engine the context is built for
     * @param enhancementProperties the request specific enhancement properties
     * @return the dereference context
     * @throws DereferenceConfigurationException if the request specific configuration
     * is invalid or not supported.
     */
    DereferenceContext createContext(EntityDereferenceEngine engine, 
            Map<String,Object> enhancementProperties) 
                    throws DereferenceConfigurationException;
    
}
