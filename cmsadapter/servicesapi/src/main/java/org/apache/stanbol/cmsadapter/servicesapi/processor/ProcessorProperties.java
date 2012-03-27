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
package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.Map;

/**
 * Implementors of this Interface can override their default processing order.
 * @author cihan
 *
 */
public interface ProcessorProperties {
    String PROCESSING_ORDER = "org.apache.stanbol.cmsadapter.servicesapi.processor.processing_order";

    Integer OBJECT_TYPE = 0;
    Integer CMSOBJECT_POST = 30;
    Integer CMSOBJECT_DEFAULT = 20; 
    Integer CMSOBJECT_PRE = 10;

    Map<String,Object> getProcessorProperties();
}
