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
package org.apache.stanbol.reasoners.web.resources;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceResult;
import org.apache.stanbol.reasoners.web.utils.ResponseTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Return the result of a reasoners background job
 * 
 * @author enridaga
 * 
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/reasoners/jobs")
public class JobsResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private String jobLocation = "";

    @Reference
    private JobManager jobManager;
    
    public JobsResource() {
    }

    /**
     * To read a /reasoners job output.
     * 
     * @param id
     * @return
     */
    @GET
    @Path("/{jid}")
    public Response get(@PathParam("jid") String id, @Context HttpHeaders headers) {
        
        
        log.info("Pinging job {}", id);

        // No id
        if(id == null || id.equals("")){
            ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            return rb.build();
        }
        
        JobManager m = getJobManager();

        // If the job exists
        if (m.hasJob(id)) {
            log.info("Found job with id {}", id);
            Future<?> f = m.ping(id);
            if(f.isDone() && (!f.isCancelled())){
                /**
                 * We return OK with the result
                 */
                Object o;
                try {
                    o = f.get();
                    if(o instanceof ReasoningServiceResult){
                        log.debug("Is a ReasoningServiceResult");
                        ReasoningServiceResult<?> result = (ReasoningServiceResult<?>) o;
                        return new ResponseTaskBuilder(new JobResultResource(uriInfo, headers)).build(result);
                    }else{
                        log.error("Job {} does not belong to reasoners", id);
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
                 * We return 404 with additional info
                 */
                String jobService = new StringBuilder().append(getPublicBaseUri()).append("jobs/").append(id).toString();
                this.jobLocation = jobService;
                Viewable viewable = new Viewable("404.ftl",this);
                
                ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML+ "; charset=utf-8");
                rb.entity(viewable);
                return rb.build();
            }
        } else {
            log.info("No job found with id {}", id);
            ResponseBuilder rb = Response.status(Status.NOT_FOUND);
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML+ "; charset=utf-8");
            return rb.build();
            
        }
    }

    /**
     * If the output is not ready, this field contains the location of the job to be rendered in the viewable.
     * 
     * @return
     */
    public String getJobLocation(){
        return this.jobLocation;
    }
    
    /**
     * Gets the job manager
     * 
     * @return
     */
    private JobManager getJobManager() {
        log.debug("(getJobManager()) ");
        return jobManager;
    }
    
    @OPTIONS
    public Response handleCorsPreflight() {
        ResponseBuilder rb = Response.ok();
        return rb.build();
    }
    public class JobResultResource extends ResultData implements ReasoningResult {
        private Object result;
        private UriInfo uriInfo;
        private HttpHeaders headers;
        public JobResultResource(UriInfo uriInfo, HttpHeaders headers) {
            this.headers = headers;
            this.uriInfo = uriInfo;
        }

        public void setResult(Object result){
            this.result = result;
        }
        public Object getResult() {
            return this.result;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }
        
        public UriInfo getUriInfo(){
            return uriInfo;
        }
    }
}
