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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to be used by Reasoning services. This interface defines also default task to be implemented by
 * all reasoning services: * CLASSIFY : Should return only rdfs:subClassOf and rdf:type statements * ENRICH :
 * Should return all inferences
 * 
 * The consistency check task must be managed separately, since it has a different return type (boolean).
 * 
 * TODO In the future we may want to extend the consistency check concept, by supporting tasks which can be of
 * this type. For example, we would need a service to provide several validity check as additional tasks.
 * 
 * M => Model type; R => Rule type; S => Statement type
 * 
 */
public interface ReasoningService<M,R,S> {
    /**
     * Default tasks, to be supported by all implementations
     */
    public interface Tasks {
        public final static String CLASSIFY = "classify";
        public final static String ENRICH = "enrich";
        final static String[] _TASKS = {CLASSIFY, ENRICH};
        public final static List<String> DEFAULT_TASKS = Arrays.asList(_TASKS);
    }

    public abstract Class<M> getModelType();

    public abstract Class<R> getRuleType();

    public abstract Class<S> getStatementType();

    public static final String SERVICE_PATH = "org.apache.stanbol.reasoners.servicesapi.path";

    /**
     * The path that must be bound to this service
     * 
     * @return
     */
    public String getPath();

    /**
     * The implementation should check whether the data is consistent or not. The meaning of 'consistency'
     * depends on the implementation.
     * 
     * @param data
     * @param rules
     * @return
     * @throws ReasoningServiceException
     */
    public abstract boolean isConsistent(M data, List<R> rules) throws ReasoningServiceException;

    /**
     * The implementation should check whether the data is consistent or not. The meaning of 'consistency'
     * depends on the implementation.
     * 
     * @param data
     * @return
     * @throws ReasoningServiceException
     */
    public abstract boolean isConsistent(M data) throws ReasoningServiceException;

    /**
     * Execute the specified task.
     * 
     * @param taskID
     *            // The identifier of the task to execute
     * @param data
     *            // The input data
     * @param rules
     *            // The rules to add to the reasoner (can be null)
     * @param filtered
     *            // Whether to return only inferences (default should be 'false')
     * @param parameters
     *            // Additional parameters, for custom implementations (can be null)
     * @return
     * @throws UnsupportedTaskException
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    public abstract Set<S> runTask(String taskID,
                                   M data,
                                   List<R> rules,
                                   boolean filtered,
                                   Map<String,List<String>> parameters) throws UnsupportedTaskException,
                                                                       ReasoningServiceException,
                                                                       InconsistentInputException;

    /**
     * Execute the specified task with no additional configuration (keep defaults)
     * 
     * @param taskID
     *            // The identifier of the task to execute
     * @param data
     *            // The input data
     * 
     * @return
     * @throws UnsupportedTaskException
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    public abstract Set<S> runTask(String taskID, M data) throws UnsupportedTaskException,
                                                         ReasoningServiceException,
                                                         InconsistentInputException;

    public abstract List<String> getSupportedTasks();

    public abstract boolean supportsTask(String taskID);
}
