package org.apache.stanbol.reasoners.web.utils;

public class ReasoningServiceResult<T extends Object> {
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

    public Object get() {
        return this.resultObj;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTask() {
        return task;
    }
}
