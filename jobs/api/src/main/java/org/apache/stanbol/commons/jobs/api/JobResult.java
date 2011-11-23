package org.apache.stanbol.commons.jobs.api;

/**
 * Interface for Job results.
 * It only provides a report message and a boolean success/failure.
 * 
 * @author enridaga
 *
 */
public interface JobResult {
    
    /**
     * A report message.
     * 
     * @return
     */
    public String getMessage();
    
    /**
     * True if the job execution succeeded, false otherwise
     * 
     * @return
     */
    public boolean isSuccess();
}
