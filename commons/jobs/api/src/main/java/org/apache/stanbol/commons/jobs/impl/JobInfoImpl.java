package org.apache.stanbol.commons.jobs.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.commons.jobs.api.JobInfo;

public class JobInfoImpl implements JobInfo {
    private String status = "undefined";
    private String outputLocation = "";
    private List<String> messages = new ArrayList<String>();
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#setOutputLocation(java.lang.String)
     */
    @Override
    public void setOutputLocation(String outputLocation){
        this.outputLocation = outputLocation;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#getOutputLocation()
     */
    @Override
    public String getOutputLocation(){
        return this.outputLocation;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#addMessage(java.lang.String)
     */
    @Override
    public void addMessage(String message){
        this.messages.add(message);
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#getMessages()
     */
    @Override
    public List<String> getMessages(){
        return messages;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#setMessages(java.util.List)
     */
    @Override
    public void setMessages(List<String> messages){
        this.messages = messages;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#getStatus()
     */
    @Override
    public String getStatus(){
        return this.status;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#setFinished()
     */
    @Override
    public void setFinished(){
        this.status = FINISHED;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#setRunning()
     */
    @Override
    public void setRunning(){
        this.status = RUNNING;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#isRunning()
     */
    @Override
    public boolean isRunning(){
        return this.status.equals(RUNNING);
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.jobs.web.utils.JobInfo#isFinished()
     */
    @Override
    public boolean isFinished(){
        return this.status.equals(FINISHED);
    }
}
