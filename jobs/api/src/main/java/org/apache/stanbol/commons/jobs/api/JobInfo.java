package org.apache.stanbol.commons.jobs.api;

import java.util.List;

public interface JobInfo {

    public static final String FINISHED = "finished";
    public static final String RUNNING = "running";

    public abstract void setOutputLocation(String outputLocation);

    public abstract String getOutputLocation();

    public abstract void addMessage(String message);

    public abstract List<String> getMessages();

    public abstract void setMessages(List<String> messages);

    public abstract String getStatus();

    public abstract void setFinished();

    public abstract void setRunning();

    public abstract boolean isRunning();

    public abstract boolean isFinished();

}