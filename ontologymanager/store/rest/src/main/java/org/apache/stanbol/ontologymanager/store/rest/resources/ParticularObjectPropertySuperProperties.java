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

import org.apache.stanbol.ontologymanager.store.api.LockManager;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.rest.LockManagerImp;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ontologies/{ontologyPath:.+}/objectProperties/{objectPropertyPath:.+}/superProperties")
public class ParticularObjectPropertySuperProperties {
    private static final Logger logger = LoggerFactory
            .getLogger(ParticularObjectPropertySuperProperties.class);

    private PersistenceStore persistenceStore;

    public ParticularObjectPropertySuperProperties(@Context ServletContext context) {
        this.persistenceStore = (PersistenceStore) context.getAttribute(PersistenceStore.class.getName());
    }

    @POST
    public Response addSuperProperties(@PathParam("ontologyPath") String ontologyPath,
                                       @PathParam("objectPropertyPath") String objectPropertyPath,
                                       @FormParam("superPropertyURIs") List<String> superPropertyURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                for (String superPropertyURI : superPropertyURIs) {
                    try {
                        persistenceStore.makeSubPropertyOf(objectPropertyURI, superPropertyURI);
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
    @Path("{superPropertyPath:.+}")
    public Response deleteSuperProperty(@PathParam("ontologyPath") String ontologyPath,
                                        @PathParam("objectPropertyPath") String objectPropertyPath,
                                        @PathParam("superPropertyPath") String superPropertyPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            String superPropertyURI = resourceManager.convertEntityRelativePathToURI(superPropertyPath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteSuperPropertyAssertion(objectPropertyURI, superPropertyURI);
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
