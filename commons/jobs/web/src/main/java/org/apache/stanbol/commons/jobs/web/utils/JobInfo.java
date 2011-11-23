package org.apache.stanbol.commons.jobs.web.utils;

import java.util.ArrayList;
import java.util.List;

public class JobInfo {
    public static final String FINISHED = "finished";
    public static final String RUNNING = "running";
    
    private String status = "undefined";
    private String outputLocation = "";
    private List<String> messages = new ArrayList<String>();
    
    public void setOutputLocation(String outputLocation){
        this.outputLocation = outputLocation;
    }
    
    public String getOutputLocation(){
        return this.outputLocation;
    }
    
    public void addMessage(String message){
        this.messages.add(message);
    }
    
    public List<String> getMessages(){
        return messages;
    }
    
    public void setMessages(List<String> messages){
        this.messages = messages;
    }
    
    public String getStatus(){
        return this.status;
    }
    
    public void setFinished(){
        this.status = FINISHED;
    }

    public void setRunning(){
        this.status = RUNNING;
    }

    public boolean isRunning(){
        return this.status.equals(RUNNING);
    }
    
    public boolean isFinished(){
        return this.status.equals(FINISHED);
    }
}
