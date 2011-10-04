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

package org.apache.stanbol.contenthub.servicesapi.search.engine;

import java.util.Map;

/**
 * This interface provides search engine specific properties.
 * 
 * @author cihan
 * 
 */
public interface EngineProperties {

    /**
     * The name of the property indicating the processing order of the {@link SearchEngine}, in case there are
     * multiple number of {@link SearchEngine}s.
     */
    String PROCESSING_ORDER = "org.apache.stanbol.search.servicesapi.engine.processing_order";

    /**
     * Possible value of the property indicating the processing order of the {@link SearchEngine}, in case
     * there are multiple number of {@link SearchEngine}s.
     */
    Integer PROCESSING_POST = 200;

    /**
     * Possible value of the property indicating the processing order of the {@link SearchEngine}, in case
     * there are multiple number of {@link SearchEngine}s.
     */
    Integer PROCESSING_DEFAULT = 100;

    /**
     * Possible value of the property indicating the processing order of the {@link SearchEngine}, in case
     * there are multiple number of {@link SearchEngine}s.
     */
    Integer PROCESSING_PRE = 0;

    /**
     * Retrieves all properties of a {@link SearchEngine}.
     * 
     * @return Map of key:value pairs
     */
    Map<String,Object> getEngineProperties();

}
