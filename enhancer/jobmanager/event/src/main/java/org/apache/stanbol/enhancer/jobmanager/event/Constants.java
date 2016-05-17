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
package org.apache.stanbol.enhancer.jobmanager.event;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.stanbol.enhancer.jobmanager.event.impl.EnhancementJob;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * Defines constants such as the used {@link EventConstants#EVENT_TOPIC} and the
 * properties used by the sent {@link Event}s.
 * @author Rupert Westenthaler
 *
 */
public interface Constants {

    /**
     * The topic used to report the completion the execution of an
     * EnhancementEngine back to the event job manager
     */
    String TOPIC_JOB_MANAGER = "stanbol/enhancer/jobmanager/event/topic";
    
    /**
     * Property used to provide the {@link EnhancementJob} instance
     */
    String PROPERTY_JOB_MANAGER = "stanbol.enhancer.jobmanager.event.job";
    /**
     * Property used to provide the {@link BlankNodeOrIRI} describing the
     * {@link ExecutionMetadata#EXECUTION} instance
     */
    String PROPERTY_EXECUTION = "stanbol.enhancer.jobmanager.event.execution";

}
