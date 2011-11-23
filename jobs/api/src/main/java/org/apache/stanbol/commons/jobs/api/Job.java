package org.apache.stanbol.commons.jobs.api;

import java.util.concurrent.Callable;

public interface Job extends Callable<JobResult> {
    
    public String buildResultLocation(String jobId);
}
