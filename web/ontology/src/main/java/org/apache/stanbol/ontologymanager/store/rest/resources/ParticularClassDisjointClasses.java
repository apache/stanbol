package org.apache.stanbol.ontologymanager.store.rest.resources;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontology/{ontologyPath:.+}/classes/{classPath:.+}/disjointClasses/")
public class ParticularClassDisjointClasses extends BaseStanbolResource{

    private static final Logger logger = LoggerFactory.getLogger(ParticularClassDisjointClasses.class);

    private PersistenceStore persistenceStore;

    public ParticularClassDisjointClasses(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    @POST
    public Response addDisjointClasses(@PathParam("ontologyPath") String ontologyPath,
                                       @PathParam("classPath") String classPath,
                                       @FormParam("disjointClassURIs") List<String> disjointClassURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String classURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            if (classURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                for (String disjointClassURI : disjointClassURIs) {
                    try {
                        persistenceStore.addDisjointClass(classURI, disjointClassURI);
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
    @Path("/ontologymanager/store{disjointClassPath:.+}")
    public Response removeDisjointClass(@PathParam("ontologyPath") String ontologyPath,
                                        @PathParam("classPath") String classPath,
                                        @PathParam("disjointClassPath") String disjointClassPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String classURI = resourceManager.getResourceURIForPath(ontologyPath, classPath);
            String disjointClassURI = resourceManager.convertEntityRelativePathToURI(disjointClassPath);
            if (classURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteDisjointClass(classURI, disjointClassURI);
                } catch (Exception e) {
                    throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
                }
            }
        } finally {
            lockManager.releaseWriteLockFor(ontologyPath);
        }
        return Response.ok().build();
    }
}
