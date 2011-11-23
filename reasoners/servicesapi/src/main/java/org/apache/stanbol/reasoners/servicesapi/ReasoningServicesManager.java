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

import java.util.Set;

/**
 * Classes which implements this interface provide the {@see ReasoningService} mapped 
 * to a given key string (generally used as path for a REST endpoint)
 * 
 * @author enridaga
 *
 */
public interface ReasoningServicesManager {

    /**
     * The number of available {@see ReasoningService}s
     * @return
     */
    public abstract int size();

    /**
     * The {@see ReasoningService} mapped 
     * to the given key string
     * 
     * @param path
     * @return
     * @throws UnboundReasoningServiceException
     */
    public abstract ReasoningService<?,?,?> get(String path) throws UnboundReasoningServiceException;

    /**
     * The unmodifiable set of available {@see ReasoningService}s
     */
    public abstract Set<ReasoningService<?,?,?>> asUnmodifiableSet();

}
