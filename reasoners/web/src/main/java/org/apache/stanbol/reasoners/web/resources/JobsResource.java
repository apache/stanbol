package org.apache.stanbol.reasoners.web.resources;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.jobs.api.JobManager;
import org.apache.stanbol.reasoners.web.utils.ReasoningServiceResult;
import org.apache.stanbol.reasoners.web.utils.ResponseTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To ping Jobs
 * 
 * @author mac
 * 
 */
@Path("/reasoners/jobs")
public class JobsResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;
    private HttpHeaders headers;

    public JobsResource(@Context ServletContext servletContext,@Context HttpHeaders headers) {
        this.context = servletContext;
        this.headers = headers;
    }

    @GET
    @Path("ping/{jid}")
    public Response get(@PathParam("jid") String id) {
        log.info("Pinging job {}", id);

        // No id
        if(id == null || id.equals("")){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        JobManager m = getJobManager();

        // If the job exists
        if (m.hasJob(id)) {
            log.info("Found job with id {}", id);
            Future<?> f = m.ping(id);
            if(f.isDone()){
                if(f.isCancelled()){
                    // NOTE: In this case the job still remains in the JobManager list
                    return Response.ok("Job have been canceled!").build();                
                }else{
                    Object o;
                    try {
                        o = f.get();
                        if(o instanceof ReasoningServiceResult<?>){
                            ReasoningServiceResult<?> result = (ReasoningServiceResult<?>) f.get();
                            return new ResponseTaskBuilder(uriInfo,context,headers).build(result);
                        }else if(o instanceof String){
                            // FIXME We keep this for the moment, must remove later on
                            return Response.ok("Test Job is done!\n " + (String) o).build();                        
                        }else{
                            log.error("Unsupported job result type: {}",o.getClass());
                            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                        }
                    } catch (InterruptedException e) {
                        log.error("Error: ",e);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    } catch (ExecutionException e) {
                        log.error("Error: ",e);
                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                    }
                }
            }else{
                // FIXME Change the HTTP Status code here!
                return Response.ok("Job is still working").build();
            }
        } else {
            log.info("No job found with id {}", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Creates a new background job to be used to test
     * 
     * @return
     */
    @GET
    @Path("test")
    public Response test() {
        log.info("Starting test job");

        // No id
        JobManager m = getJobManager();
        String id = m.execute(new Callable<String>() {
            @Override
            public String call() throws Exception {
                for (int i = 0; i < 10; i++) {
                    try {
                        log.info("Test Process is working");
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {}
                }
                return "This is the test job";
            }
        });
        return Response.ok(id).build();
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
}
