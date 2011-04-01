package org.apache.stanbol.ontologymanager.store.rest.resources;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;

//@Component
//@Service(value = Object.class)
//@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontologies/{ontologyPath:.+}/classes/{classPath:.+}/")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2.0")
public class ParticularClass {
    private static final Logger logger = LoggerFactory.getLogger(ParticularClass.class);

    private static final String VIEWABLE_PATH = "/org/apache/stanbol/ontologymanager/store/rest/resources/particularClass";

    private PersistenceStore persistenceStore;

    // View Variables
    private ClassContext metadata;

    public ParticularClass(@Context ServletContext context) {
        this.persistenceStore = (PersistenceStore) context.getAttribute(PersistenceStore.class.getName());
    }

    // The Java method will process HTTP GET requests
    @GET
    // The Java method will produce content identified by the MIME Media
    // type "application/xml"
    @Produces("application/xml")
    public Response retrieveClassContext(@PathParam("ontologyPath") String ontologyPath,
                                         @PathParam("classPath") String classPath,
                                         @DefaultValue("false") @QueryParam("withInferredAxioms") boolean withInferredAxioms) {
        Response response = null;
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainReadLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String classURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            if (classURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            try {
                ClassContext classContext = persistenceStore.generateClassContext(classURI,
                    withInferredAxioms);
                response = Response.ok(classContext, MediaType.APPLICATION_XML).build();
            } catch (Exception e) {
                logger.error("Error ", e);
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        } finally {
            lockManager.releaseReadLockFor(ontologyPath);
        }
        return response;
    }

    // The Java method will process HTTP DELETE requests
    @DELETE
    public void delete(@PathParam("ontologyPath") String ontologyPath,
                       @PathParam("classPath") String classPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String resourceURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            persistenceStore.deleteResource(resourceURI);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
    }

    @POST
    @Path("/unionClasses")
    public Response addUnionClasses(@PathParam("ontologyPath") String ontologyPath,
                                    @PathParam("classPath") String classPath,
                                    @FormParam("unionClassURIs") List<String> unionClassURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String classURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            if (classURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                for (String unionClassURI : unionClassURIs) {
                    try {
                        persistenceStore.addUnionClass(classURI, unionClassURI);
                    } catch (Exception e) {
                        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/unionClasses/{unionClassPath:.+}")
    public Response removeUnionClass(@PathParam("ontologyPath") String ontologyPath,
                                     @PathParam("classPath") String classPath,
                                     @PathParam("unionClassPath") String unionClassPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String classURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            String unionClassURI = resourceManager.convertEntityRelativePathToURI(unionClassPath);
            if (classURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteUnionClass(classURI, unionClassURI);
                } catch (Exception e) {
                    throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
                }
            }
        } finally {

        }
        return Response.ok().build();
    }

    // HTML View Methods
    public ClassContext getMetadata() {
        return metadata;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getViewable(@PathParam("ontologyPath") String ontologyPath,
                                @PathParam("classPath") String classPath,
                                @DefaultValue("false") @QueryParam("withInferredAxioms") boolean withInferredAxioms) {
        metadata = (ClassContext) retrieveClassContext(ontologyPath, classPath, withInferredAxioms)
                .getEntity();
        return new Viewable(VIEWABLE_PATH, this);
    }
}
