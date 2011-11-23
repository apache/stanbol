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
package org.apache.stanbol.reasoners.web.utils;

import org.apache.stanbol.commons.jobs.api.JobResult;

/**
 * To represent a result of a reasoning service.
 * 
 * @author enridaga
 *
 * @param <T>
 */
public class ReasoningServiceResult<T extends Object> implements JobResult{
    private T resultObj;
    private boolean success;
    private String task;

    public ReasoningServiceResult(String task, boolean success, T resultObj) {
        this.task = task;
        this.resultObj = resultObj;
        this.success = success;
    }

    public ReasoningServiceResult(String task, boolean success) {
        this.task = task;
        this.resultObj = null;
        this.success = success;
    }

    public ReasoningServiceResult(String task, T resultObj) {
        this.task = task;
        this.resultObj = resultObj;
        this.success = true;
    }

    public ReasoningServiceResult(String task) {
        this.task = task;
        this.resultObj = null;
        this.success = true;
    }

    public T get() {
        return this.resultObj;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    public String getTask() {
        return task;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(task).append(". Result: ").append(success).append(". ");
        if(resultObj!=null){
            sb.append("Result type is ").append(resultObj.getClass().getCanonicalName());
        }
        return sb.toString();
    }
}
