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

@Path("/ontology/{ontologyPath:.+}/datatypeProperties/{datatypePropertyPath:.+}/domains")
public class ParticularDatatypePropertyDomains extends BaseStanbolResource{
    private static final Logger logger = LoggerFactory.getLogger(ParticularDatatypePropertyDomains.class);

    private PersistenceStore persistenceStore;

    public ParticularDatatypePropertyDomains(@Context ServletContext context) {
        this.persistenceStore = ContextHelper.getServiceFromContext(PersistenceStore.class, context);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addDomains(@PathParam("ontologyPath") String ontologyPath,
                               @PathParam("datatypePropertyPath") String datatypePropertyPath,
                               @FormParam("domainURIs") List<String> domainURIs) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String datatypePropertyURI = resourceManager.getResourceURIForPath(ontologyPath,
                datatypePropertyPath);
            if (datatypePropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                for (String domainURI : domainURIs) {
                    try {
                        persistenceStore.addDomain(datatypePropertyURI, domainURI);
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
    @Path("/{domainPath:.+}")
    public Response deleteDomain(@PathParam("ontologyPath") String ontologyPath,
                                 @PathParam("datatypePropertyPath") String datatypePropertyPath,
                                 @PathParam("domainPath") String domainPath) {
        LockManager lockManager = LockManagerImp.getInstance();
        lockManager.obtainWriteLockFor(ontologyPath);
        try {

            ResourceManagerImp resourceManager = ResourceManagerImp.getInstance();
            String datatypePropertyURI = resourceManager.getResourceURIForPath(ontologyPath,
                datatypePropertyPath);
            String domainURI = resourceManager.convertEntityRelativePathToURI(domainPath);
            if (datatypePropertyURI == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            } else {
                try {
                    persistenceStore.deleteDomain(datatypePropertyURI, domainURI);
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
