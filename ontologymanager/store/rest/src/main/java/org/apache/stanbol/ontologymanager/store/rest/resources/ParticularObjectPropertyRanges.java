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

@Path("/ontologies/{ontologyPath:.+}/objectProperties/{objectPropertyPath:.+}/ranges")
public class ParticularObjectPropertyRanges {
    private static final Logger logger = LoggerFactory.getLogger(ParticularObjectPropertyRanges.class);

    private PersistenceStore persistenceStore;

    public ParticularObjectPropertyRanges(@Context ServletContext context) {
        this.persistenceStore = (PersistenceStore) context.getAttribute(PersistenceStore.class.getName());
    }

    @POST
    public Response addRanges(@PathParam("ontologyPath") String ontologyPath,
                              @PathParam("objectPropertyPath") String objectPropertyPath,
                              @FormParam("rangeURIs") List<String> rangeURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                for (String domainURI : rangeURIs) {
                    try {
                        persistenceStore.addRange(objectPropertyURI, domainURI);
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
    @Path("{rangePath:.+}")
    public Response deleteRange(@PathParam("ontologyPath") String ontologyPath,
                                @PathParam("objectPropertyPath") String objectPropertyPath,
                                @PathParam("rangePath") String rangePath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {
            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String objectPropertyURI = resourceManager
                    .getResourceURIForPath(ontologyPath, objectPropertyPath);
            String rangeURI = resourceManager.convertEntityRelativePathToURI(rangePath);
            if (objectPropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteRange(objectPropertyURI, rangeURI);
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
