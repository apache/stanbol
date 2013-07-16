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
package org.apache.stanbol.commons.jobs.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;


import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.jobs.api.Job;
import org.apache.stanbol.commons.jobs.api.JobInfo;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.jobs.api.JobResult;
import org.apache.stanbol.commons.jobs.impl.JobInfoImpl;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages Background Jobs
 * 
 * @author enridaga
 * 
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/jobs")
public class JobsResource extends BaseStanbolResource {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    @Context
    protected HttpHeaders headers;
    
    //private JobInfo info = null;
    
    @Reference
    private JobManager jobManager;

    
    @GET
    public Response get(){
        return Response.ok(new Viewable("index",new ResultData() {})).build();
    }
    
    /**
     * GET info about a Background Job
     * 
     * @param id
     * @return Response
     */
    @GET
    @Path("/{jid}")
    public Response get(@PathParam("jid") String id) {
        log.info("Called get() with id {}", id);

        // No id
        if(id == null || id.equals("")){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        JobManager m = jobManager;

        // If the job exists
        if (m.hasJob(id)) {
            log.info("Found job with id {}", id);
            Future<?> f = m.ping(id);
            //this.info = new JobInfoImpl();
            final JobInfo info = new JobInfoImpl();
            if(f.isDone()){
                // The job is finished
                if(f.isCancelled()){
                    // NOTE: Canceled jobs should never exist. 
                    // The web service remove any deleted process from the manager
                    // If a process have been canceled programmatically, it cannot be managed by the service anymore 
                    // (except for DELETE)
                    log.warn("Job with id {} have been canceled. Returning 404 Not found.", id);
                    return Response.status(Response.Status.NOT_FOUND).build();
                }else{
                    // Job is complete
                    info.setFinished();
                    info.addMessage("You can remove this job using DELETE");
                }
            }else{
                // the job exists but it is not complete
                info.setRunning();
                info.addMessage("You can interrupt this job using DELETE");
            }
            // Returns 200, the job exists
            info.setOutputLocation(getPublicBaseUri() + m.getResultLocation(id));

            if(isHTML()){
                // Result as HTML
                return Response.ok(new Viewable("info", new JobsResultData(info))).build();
            }else{
                // Result as application/json, text/plain
                return Response.ok(info).build();
            }
        } else {
            log.info("No job found with id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    public class JobsResultData extends ResultData{
        private JobInfo ji;
        public JobsResultData(JobInfo jinfo){
            this.ji = jinfo;
        }
        public JobInfo getJobInfo(){
            return ji;
        }
    }
    
    private boolean isHTML() {
        List<MediaType> mediaTypes = headers.getAcceptableMediaTypes();
        Set<String> htmlformats = new HashSet<String>();
        htmlformats.add(TEXT_HTML);
        for (MediaType t : mediaTypes) {
            String strty = t.toString();
            log.info("Acceptable is {}", t);
            if (htmlformats.contains(strty)) {
                log.debug("Requested format is HTML {}", t);
                return true;
            }
        }
        return false;
    }

    /**
     * DELETE a background job. This method will find a job, 
     * interrupt it if it is running, and removing it
     * from the {@see JobManager}.
     * 
     * @param jid
     * @return
     */
    @DELETE
    @Path("/{jid}")
    public Response delete(@PathParam(value = "jid") String jid){
        log.info("Called DELETE ({})", jid);
        if(!jid.equals("")){
            log.info("Looking for test job {}", jid);
            JobManager m = jobManager;

            // If the job exists
            if (m.hasJob(jid)){
                log.info("Deleting Job id {}", jid);
                m.remove(jid);
                return Response.ok("Job deleted.").build();
            }else {
                log.info("No job found with id {}", jid);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }else{
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
    
    /**
     * DELETE all background jobs.
     * 
     * @return
     */
    @DELETE
    public Response delete(){
        log.info("Called DELETE all jobs");
        JobManager manager = jobManager;
        manager.removeAll();
        return Response.ok("All jobs have been deleted.").build();
    }
    
    /**
     * Creates a new background job to be used to test the service.
     * This method is for testing the service and to provide a sample implementation
     * of a long term operation started form a rest service.
     * 
     * @return
     */
    @GET
    @Path("/test{jid: (/.*)?}")
    public Response test(@PathParam(value = "jid") String jid) {
        log.info("Called GET (create test job)");

        // If an Id have been provided, check whether the job has finished and return the result
        if(!jid.equals("")){
            log.info("Looking for test job {}", jid);
            JobManager m = jobManager;
            // Remove first slash from param value
            jid = jid.substring(1);
            
            // If the job exists
            if (m.hasJob(jid)){
                log.info("Found job with id {}", jid);
                Future<?> f = m.ping(jid);
                if(f.isDone() && (!f.isCancelled())){
                    /**
                     * We return OK with the result
                     */
                    Object o;
                    try {
                        o = f.get();
                        if(o instanceof JobResult){
                            JobResult result = (JobResult) o;
                            return Response.ok(result.getMessage()).build();
                        }else{
                            log.error("Job {} is not a test job", jid);
                            throw new WebApplicationException(Response.Status.NOT_FOUND);
                        }
                    } catch (InterruptedException e) {
                        log.error("Error: ",e);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    } catch (ExecutionException e) {
                        log.error("Error: ",e);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    }
                    
                }else{
                    /**
                     * We return 404 with additional info (Content-Location, the related job resource)
                     * 
                     * TODO
                     * Change into json representations
                     */
                    String location = getPublicBaseUri() + "jobs/" + jid;
                    String info = new StringBuilder().append("Result not ready.\n").append("Job Location: ").append(location).toString();
                    return Response.status(404).header("Content-Location", location).header("Content-type","text/plain").entity(info).build();
                }
            }else {
                log.info("No job found with id {}", jid);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }else{
            // No id have been provided, we create a new test job
            JobManager m = jobManager;
            String id = m.execute(new Job() {
                @Override
                public JobResult call() throws Exception {
                    for (int i = 0; i < 30; i++) {
                        try {
                            log.info("Test Process is working");
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {}
                    }
                    return new JobResult(){
    
                        @Override
                        public String getMessage() {
                            return "This is a test job";
                        }
    
                        @Override
                        public boolean isSuccess() {
                            return true;
                        }
                    };
                }
    
                @Override
                public String buildResultLocation(String jobId) {
                    return "jobs/test/" + jobId;
                }
            });
            // This service returns 201 Created on success
            String location = getPublicBaseUri() + "jobs/" + id;
            String info = new StringBuilder().append("Job started.\n")
                    .append("Location: ").append(location).toString();
            return Response.created(URI.create(location)).header("Content-type","text/plain").entity(info).build();
        }
    }


}
