package org.apache.stanbol.cmsadapter.web.resources;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.web.utils.RestURIHelper;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClient;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClientException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;

@Path("/cmsadapter/bridgeDefinitions")
public class BridgeDefinitionsResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(BridgeDefinitionsResource.class);

    private static final String MAPPING_ENGINE_COMPONENT_FACTORY_FILTER = "(component.factory=org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngineFactory)";

    private RestClient storeClient;

    private MappingEngine engine;

    @SuppressWarnings("rawtypes")
    public BridgeDefinitionsResource(@Context ServletContext context) {
        this.storeClient = ContextHelper.getServiceFromContext(RestClient.class, context);
        try {
            BundleContext bundleContext = (BundleContext) context.getAttribute(BundleContext.class.getName());
            ServiceReference serviceReference = bundleContext.getServiceReferences(null,
                MAPPING_ENGINE_COMPONENT_FACTORY_FILTER)[0];
            ComponentFactory componentFactory = (ComponentFactory) bundleContext.getService(serviceReference);
            ComponentInstance componentInstance = componentFactory.newInstance(new Hashtable());
            this.engine = (MappingEngine) componentInstance.getInstance();

        } catch (InvalidSyntaxException e) {
            logger.warn("Mapping engine instance could not be instantiated", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Takes connection information to access the content managament system and executes the bridge
     * definitions. After completing processing of bridges, generated ontology is stored through <b>Store</b>
     * component.
     * 
     * @param connectionInfo
     *            Information to access content management system when needed. It also includes the URI of the
     *            ontology to be generated
     * @param bridgeDefinitions
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response registeBridgeDefinitions(@FormParam("connectionInfo") ConnectionInfo connectionInfo,
                                             @FormParam("bridgeDefinitions") BridgeDefinitions bridgeDefinitions) {

        String ontologyURI = connectionInfo.getOntologyURI();
        try {
            engine.mapCR(bridgeDefinitions, connectionInfo, ontologyURI);
            return Response.status(Status.OK).build();

        } catch (RepositoryAccessException e) {
            logger.warn("Cannot access to repository", e);
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Simply executes new bridge definitions and as a result new generated ontology overwrites the existing
     * one.
     * 
     * @param ontologyURI
     *            URI of the ontology for which the bridge definitions are defined
     * @param bridgeDefinitions
     *            New bridge definitions
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateBridgeDefinitions(@FormParam("ontologyURI") String ontologyURI,
                                            @FormParam("bridgeDefinitions") BridgeDefinitions bridgeDefinitions) {
        try {
            OntModel model = OntologyResourceHelper.getOntModel(storeClient, ontologyURI,
                RestURIHelper.getOntologyHref(ontologyURI));

            ConnectionInfo connectionInfo = OntologyResourceHelper.getConnectionInfo(model);
            engine.mapCR(bridgeDefinitions, connectionInfo, ontologyURI);
            return Response.status(Status.OK).build();

        } catch (UnsupportedEncodingException e) {
            logger.warn("Ontology content could not be transformed to bytes", e);
        } catch (RestClientException e) {
            logger.warn("Error occured while interacting with store", e);
        } catch (RepositoryAccessException e) {
            logger.warn("Cannot access to repository", e);
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}