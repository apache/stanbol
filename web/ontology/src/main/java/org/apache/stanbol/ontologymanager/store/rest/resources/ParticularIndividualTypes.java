package org.apache.stanbol.ontologymanager.store.rest.resources;

import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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

@Path("/ontology/{ontologyPath:.+}/individuals/{individualPath:.+}/types")
public class ParticularIndividualTypes extends BaseStanbolResource{
    private static final Logger logger = LoggerFactory.getLogger(ParticularIndividualTypes.class);

    private PersistenceStore persistenceStore;

    public ParticularIndividualTypes(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    // FIXME Handle PS errors properly, correct return values

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addType(@PathParam("ontologyPath") String ontologyPath,
                            @PathParam("individualPath") String individualPath,
                            @FormParam("containerClassURIs") List<String> containerClassURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String individualURI = resourceManager.getResourceURIForPath(ontologyPath, individualPath);
            if (individualURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            } else {
                for (String containerClassURI : containerClassURIs) {
                    try {
                        persistenceStore.addContainerClassForIndividual(individualURI, containerClassURI);
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
    @Path("/{containerClassPath:.+}")
    public Response addType(@PathParam("ontologyPath") String ontologyPath,
                            @PathParam("individualPath") String individualPath,
                            @PathParam("containerClassPath") String containerClassPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String individualURI = resourceManager.getResourceURIForPath(ontologyPath, individualPath);
            String containerClassURI = resourceManager.convertEntityRelativePathToURI(containerClassPath);
            if (individualURI == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteContainerClassForIndividual(individualURI, containerClassURI);
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
