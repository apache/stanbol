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

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;

/**
 * An extractor which is responsible for creation and deletion of triples.
 * With the same list of CMS objects and mapping environment a processor is expected to
 * be able to delete all the triples it generated after successive calls of 
 * {@link #createObjects(List, MappingEngine)} and {@linkplain #deleteObjects(List, MappingEngine)} 
 * @author cihan
 *
 */
public interface Processor {
    /**
     * Method for determining if the processor can process the specified CMS object.
     * @param cmsObject
     * @param session a JCR or CMIS Session object 
     * @return true if the CMS object can be processed.
     */
    Boolean canProcess(Object cmsObject, Object session);
    
    /**
     * Creates extracted triples from the provided CMS objects. 
     * The ontology should be available through <b>engine</b> parameter. 
     * @param objects a list of CMS objects to process
     * @param engine 
     */
    void createObjects(List<Object> objects, MappingEngine engine);
    
    /**
     * Deletes previously extracted triples from the provided CMS objects, by this processor.
     * @param objects a list of CMS objects to process
     * @param engine
     */
    void deleteObjects(List<Object> objects, MappingEngine engine);
}
