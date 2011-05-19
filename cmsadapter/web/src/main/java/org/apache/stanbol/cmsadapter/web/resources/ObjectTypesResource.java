package org.apache.stanbol.cmsadapter.web.resources;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.cmsadapter.core.mapping.MappingConfigurationImpl;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingConfiguration;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;
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

@Path("/cmsadapter/{ontologyURI:.+}/objectTypes")
public class ObjectTypesResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(ObjectTypesResource.class);

    private MappingEngine engine;
    private RestClient psi;

    public ObjectTypesResource(@Context ServletContext context) {
        this.psi = ContextHelper.getServiceFromContext(RestClient.class, context);
        try {
            BundleContext bundleContext = (BundleContext) context.getAttribute(BundleContext.class.getName());
            ServiceReference serviceReference = bundleContext.getServiceReferences(null,
                "(component.factory=org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngineFactory)")[0];
            ComponentFactory componentFactory = (ComponentFactory) bundleContext.getService(serviceReference);
            ComponentInstance componentInstance = componentFactory
                    .newInstance(new Hashtable<Object,Object>());
            this.engine = (MappingEngine) componentInstance.getInstance();

        } catch (InvalidSyntaxException e) {
            logger.warn("Mapping engine instance could not be instantiated", e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    @POST
    public Response liftObjectTypes(@PathParam("ontologyURI") String ontologyURI,
                                    @FormParam("objectTypeDefinitions") ObjectTypeDefinitions objectTypeDefinitions) {

        List<ObjectTypeDefinition> createdObjectList = objectTypeDefinitions.getObjectTypeDefinition();
        OntModel model;
        try {
            model = OntologyResourceHelper.createOntModel(psi, ontologyURI,
                RestURIHelper.getOntologyHref(ontologyURI));
            MappingConfiguration conf = new MappingConfigurationImpl();
            conf.setOntModel(model);
            conf.setOntologyURI(ontologyURI);
            conf.setObjects((List<Object>) (List<?>) createdObjectList);
            conf.setAdapterMode(AdapterMode.STRICT_OFFLINE);
            engine.createModel(conf);
            return Response.ok().build();

        } catch (UnsupportedEncodingException e) {
            logger.warn("Ontology content could not be transformed to bytes", e);
        } catch (RestClientException e) {
            logger.warn("Error occured while interacting with store", e);
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @SuppressWarnings("unchecked")
    @PUT
    public Response updateClassificationObjects(@PathParam("ontologyURI") String ontologyURI,
                                                @FormParam("objectTypeDefinitions") ObjectTypeDefinitions objectTypeDefinitions) {

        List<ObjectTypeDefinition> createdObjectList = objectTypeDefinitions.getObjectTypeDefinition();
        OntModel model;
        try {
            model = OntologyResourceHelper.getOntModel(psi, ontologyURI,
                RestURIHelper.getOntologyHref(ontologyURI));
            MappingConfiguration conf = new MappingConfigurationImpl();
            conf.setOntModel(model);
            conf.setOntologyURI(ontologyURI);
            conf.setObjects((List<Object>) (List<?>) createdObjectList);
            conf.setAdapterMode(AdapterMode.STRICT_OFFLINE);
            engine.updateModel(conf);
            return Response.ok().build();

        } catch (UnsupportedEncodingException e) {
            logger.warn("Ontology content could not be transformed to bytes", e);
        } catch (RestClientException e) {
            logger.warn("Error occured while interacting with store", e);
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @SuppressWarnings("unchecked")
    @DELETE
    public Response deleteClassificationObjects(@PathParam("ontologyURI") String ontologyURI,
                                                @FormParam("objectTypeDefinitions") ObjectTypeDefinitions objectTypeDefinitions) {

        List<ObjectTypeDefinition> createdObjectList = objectTypeDefinitions.getObjectTypeDefinition();
        OntModel model;
        try {
            model = OntologyResourceHelper.getOntModel(psi, ontologyURI,
                RestURIHelper.getOntologyHref(ontologyURI));
            MappingConfiguration conf = new MappingConfigurationImpl();
            conf.setOntModel(model);
            conf.setOntologyURI(ontologyURI);
            conf.setObjects((List<Object>) (List<?>) createdObjectList);
            conf.setAdapterMode(AdapterMode.STRICT_OFFLINE);
            engine.deleteModel(conf);
            return Response.ok().build();

        } catch (UnsupportedEncodingException e) {
            logger.warn("Ontology content could not be transformed to bytes", e);
        } catch (RestClientException e) {
            logger.warn("Error occured while interacting with store", e);
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}
