package org.apache.stanbol.ontologymanager.store.rest.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.apache.stanbol.ontologymanager.store.rest.ResourceManagerImp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Component
//@Service(value = Object.class)
//@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontologies/{ontologyPath:.+}/dump")
public class OntologyDump {
    private static final Logger logger = LoggerFactory.getLogger(OntologyDump.class);
    private PersistenceStore persistenceStore;

    public OntologyDump(@Context ServletContext context) {
        this.persistenceStore = (PersistenceStore) context.getAttribute(PersistenceStore.class.getName());
    }

    @GET
    @Produces("application/xml")
    public String dumpOntology(@PathParam("ontologyPath") String ontologyPath,
                               @QueryParam(value = "infer") boolean infer) {
        String result = null;
        try {
            ResourceManager resourceManager = ResourceManagerImp.getInstance();
            String ontologyURI = resourceManager.getOntologyURIForPath(ontologyPath);
            result = persistenceStore.retrieveOntology(ontologyURI, "RDF/XML", infer);
        } catch (Exception e) {
            logger.error("Error ", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (result == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return result;
    }
}
