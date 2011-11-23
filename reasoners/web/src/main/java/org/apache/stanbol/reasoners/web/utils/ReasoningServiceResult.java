package org.apache.stanbol.reasoners.web.utils;

import org.apache.stanbol.commons.jobs.api.JobResult;

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
