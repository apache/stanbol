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
package org.apache.stanbol.commons.jobs.api;

import java.util.concurrent.Future;
/**
 * This interface defines the executor of asynch processes.
 * 
 * @author enridaga
 *
 */
public interface JobManager {
    /**
     * Adds and runs an asynch process. Returns the String id to use for pinging its execution.
     * 
     * @param task
     * @return
     */
    public String execute(Job job);

    /**
     * Get the Future object to monitor the state of a job
     */
    public Future<?> ping(String id);
    
    /**
     * Get the location url of the result
     * 
     * @param id
     * @return
     */
    public String getResultLocation(String id);

    /**
     * If the executor is managing the given job
     * 
     * @param id
     * @return
     */
    public boolean hasJob(String id);

    /**
     * The currently managed jobs, in any state (running, complete, interrupted)
     * 
     * @return
     */
    public int size();

    /**
     * Interrupt the asynch process and remove it from the job list.
     * To interrupt the process and keeping it, use the Future object from the ping() method.
     * 
     * @param id
     */
    public void remove(String id);
    
    /**
     * Interrupt all asynch processes and remove them form the job list.
     */
    public void removeAll();
}
