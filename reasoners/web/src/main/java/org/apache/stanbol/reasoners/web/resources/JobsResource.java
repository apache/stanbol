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

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceResult;
import org.apache.stanbol.reasoners.web.utils.ResponseTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * Return the result of a reasoners background job
 * 
 * @author enridaga
 * 
 */
@Path("/reasoners/jobs")
public class JobsResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;
    private HttpHeaders headers;

    private String jobLocation = "";
    
    public JobsResource(@Context ServletContext servletContext,@Context HttpHeaders headers) {
        this.context = servletContext;
        this.headers = headers;
    }

    /**
     * To read a /reasoners job output.
     * 
     * @param id
     * @return
     */
    @GET
    @Path("/{jid}")
    public Response get(@PathParam("jid") String id,
                        @Context HttpHeaders headers) {
        
        
        log.info("Pinging job {}", id);

        // No id
        if(id == null || id.equals("")){
            ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            addCORSOrigin(servletContext, rb, headers);
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
                        return new ResponseTaskBuilder(uriInfo,context,headers).build(result);
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
                //return Response.status(404).header("Content-Location", jobService).header("Content-type","text/html").entity( viewable ).build();
                
                ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML+ "; charset=utf-8");
                addCORSOrigin(servletContext, rb, headers);
                rb.entity(viewable);
                return rb.build();
            }
        } else {
            log.info("No job found with id {}", id);
            //return Response.status(Response.Status.NOT_FOUND).build();
            ResponseBuilder rb = Response.status(Status.NOT_FOUND);
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML+ "; charset=utf-8");
            addCORSOrigin(servletContext, rb, headers);
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
        return (JobManager) ContextHelper.getServiceFromContext(JobManager.class, this.context);
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }
}
