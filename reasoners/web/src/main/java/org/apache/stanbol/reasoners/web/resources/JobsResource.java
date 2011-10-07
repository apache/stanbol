package org.apache.stanbol.reasoners.web.resources;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.jobs.api.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To ping Jobs
 * 
 * @author mac
 * 
 */
@Path("/jobs")
public class JobsResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;

    public JobsResource(@Context ServletContext servletContext) {
        this.context = servletContext;
    }

    @GET
    @Path("ping")
    public Response get(@QueryParam("id") String id) {
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
                    return Response.ok("Job is done!").build();
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
        log.debug("(getServicesManager()) ");
        return (JobManager) ContextHelper.getServiceFromContext(JobManager.class, this.context);
    }
}
