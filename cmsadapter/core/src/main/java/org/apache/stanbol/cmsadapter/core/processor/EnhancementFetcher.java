package org.apache.stanbol.cmsadapter.core.processor;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.InstanceBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ContentObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.processor.BaseProcessor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.ProcessorProperties;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@Component(immediate = true, metatype = true)
@Service
public class EnhancementFetcher extends BaseProcessor implements Processor, ProcessorProperties {
    private static final Logger logger = LoggerFactory.getLogger(EnhancementFetcher.class);

    /**
     * Prefix to identify assignment of an enhancement to a cms object.
     */
    private static final String ENHANCEMENT_PREFIX = "Enh-";

    private static final String PROP_CONTENT_PROPERTY = "org.apache.stanbol.cmsadapter.core.processor.EnhancementFetcher.contentProperty";
    @Property(name = PROP_CONTENT_PROPERTY, cardinality = 1000, value = {"content"})
    private String[] contentProperties;

    private static final String PROP_CONTENTHUB_RESOURCE = "org.apache.stanbol.cmsadapter.core.processort.EnhancementFetcher.contentHubResource";
    @Property(name = PROP_CONTENTHUB_RESOURCE, value = "http://localhost:8080/contenthub/")
    private String engineRootResource;

    @Reference
    Store store;

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, CMSOBJECT_POST + 10);
    }

    private Client client;

    @Override
    public Map<String,Object> getProcessorProperties() {
        return properties;
    }

    @Override
    public Boolean canProcess(Object cmsObject) {
        return cmsObject instanceof ContentObject;
    }

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = object2dobject(objects, engine);
        if (engine.getBridgeDefinitions() != null) {
            BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
            DObjectAdapter adapter = engine.getDObjectAdapter();

            List<InstanceBridge> instanceBridges = MappingModelParser.getInstanceBridges(bridgeDefinitions);
            RepositoryAccess accessor = engine.getRepositoryAccess();
            Object session = engine.getSession();
            boolean emptyList = (cmsObjects == null || cmsObjects.size() == 0);

            for (InstanceBridge ib : instanceBridges) {
                // cms objects will be null in the case of initial bridge execution or update of bridge
                // definitions
                if (emptyList) {
                    try {
                        List<CMSObject> retrievedObjects = accessor.getNodeByPath(ib.getQuery(), session);
                        cmsObjects = new ArrayList<DObject>();
                        for (CMSObject o : retrievedObjects) {
                            cmsObjects.add(adapter.wrapAsDObject(o));
                        }
                    } catch (RepositoryAccessException e) {
                        logger.warn("Failed to obtain CMS Objects for query {}", ib.getQuery());
                        continue;
                    }
                }

                for (DObject contentObject : cmsObjects) {
                    if (matches(contentObject.getPath(), ib.getQuery())
                        && !isRootNode(ib.getQuery(), contentObject.getPath())) {
                        getEnhancements(contentObject, engine);
                    }
                }
            }
        } else {
            // work without bridge definitions
            for (DObject cmsObject : cmsObjects) {
                if (canProcess(cmsObject.getInstance())) {
                    getEnhancements(cmsObject, engine);
                }
            }
        }
    }

    private void getEnhancements(DObject cmsObject, MappingEngine engine) {
        WebResource webResource = client.resource(engineRootResource + "content/" + cmsObject.getID());
        String content = getTextContent(cmsObject);

        if (!content.contentEquals("")) {
            try {
                webResource.type(MediaType.TEXT_PLAIN_TYPE).put(content.getBytes());
            } catch (Exception e) {
                logger.warn("Failed to create content item for cms object: {}", cmsObject.getName());
                return;
            }
            webResource = client.resource(engineRootResource + "metadata/" + cmsObject.getID());
            String enh = webResource.accept("application/rdf+xml").get(String.class);
            mergeEnhancements(cmsObject, enh, engine);

        } else {
            logger.warn("Empty content for object {}", cmsObject.getName());
        }
    }

    private void mergeEnhancements(DObject cmsObject, String enhancements, MappingEngine engine) {
        Model enhModel = ModelFactory.createDefaultModel();
        enhModel.read(new ByteArrayInputStream(enhancements.getBytes(Charset.forName("UTF-8"))), "");
        // first remove previously added enhancements from ontology
        deleteEnhancementsOfCMSObject(Arrays.asList(new DObject[] {cmsObject}), engine);
        engine.getOntModel().add(assignCMSObjectReferencesToEnhancements(enhModel, cmsObject.getID()));
    }

    /**
     * Add unique reference of cms objects to each enhancement of the types <b>Enhancement,
     * EntityAnnotation</b> and <b>Text Annotation</b> to be able to delete the annotations in delete
     * operation.
     * 
     * @param enhModel
     * @param reference
     * @return {@link OntModel} which contains enhancements having
     *         {@code CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP}
     */
    private OntModel assignCMSObjectReferencesToEnhancements(Model enhModel, String reference) {
        OntModel enhOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        enhOntModel.add(enhModel);
        reference = ENHANCEMENT_PREFIX + reference;

        String URI;
        List<String> processedURIs = new ArrayList<String>();
        List<Statement> enhs = enhOntModel.listStatements(null, RDF.type,
            ResourceFactory.createResource(NamespaceEnum.enhancer + "Enhancement")).toList();
        for (Statement stmt : enhs) {
            URI = stmt.getSubject().getURI();
            if (!processedURIs.contains(URI)) {
                stmt.getSubject().addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);
                processedURIs.add(URI);
            }
        }

        enhs = enhOntModel.listStatements(null, RDF.type, NamespaceEnum.enhancer + "EntityAnnotation")
                .toList();
        for (Statement stmt : enhs) {
            URI = stmt.getSubject().getURI();
            if (!processedURIs.contains(URI)) {
                stmt.getSubject().addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);
                processedURIs.add(URI);
            }
        }

        enhs = enhOntModel.listStatements(null, RDF.type, NamespaceEnum.enhancer + "TextAnnotation").toList();
        for (Statement stmt : enhs) {
            URI = stmt.getSubject().getURI();
            if (!processedURIs.contains(URI)) {
                stmt.getSubject().addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);
                processedURIs.add(URI);
            }
        }
        return enhOntModel;
    }

    private String getTextContent(DObject cmsObject) {
        List<DProperty> properties = null;
        try {
            properties = cmsObject.getProperties();
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to retrieve properties for object {}", cmsObject.getName());
            return "";
        }
        for (String propertyName : (String[]) contentProperties) {
            for (DProperty property : properties) {
                if (property.getName().contentEquals(propertyName)) {
                    // assumed content property is single valued
                    return property.getValue().get(0);
                }
            }
        }
        return "";
    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = object2dobject(objects, engine);

        // if there is bridge definitions try to fetch concept bridges
        if (engine.getBridgeDefinitions() != null) {
            List<InstanceBridge> instanceBridges = MappingModelParser.getInstanceBridges(engine
                    .getBridgeDefinitions());

            for (InstanceBridge ib : instanceBridges) {
                List<DObject> processableObjects = new ArrayList<DObject>();
                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), ib.getQuery())) {
                        processableObjects.add(cmsObject);
                    }
                }
                deleteEnhancementsOfCMSObject(processableObjects, engine);
            }
        } else {
            List<DObject> processableObjects = new ArrayList<DObject>();
            for (DObject cmsObject : cmsObjects) {
                if (canProcess(cmsObject.getInstance())) {
                    processableObjects.add(cmsObject);
                }
            }
            deleteEnhancementsOfCMSObject(processableObjects, engine);
        }
    }

    private void deleteEnhancementsOfCMSObject(List<DObject> cmsObjects, MappingEngine engine) {
        OntModel model = engine.getOntModel();
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();

        List<Statement> enhs = model.listStatements(null, RDF.type,
            ResourceFactory.createResource(NamespaceEnum.enhancer + "Enhancement")).toList();
        deleteEnhancements(cmsObjects, enhs, orh);

        enhs = model.listStatements(null, RDF.type, NamespaceEnum.enhancer + "EntityAnnotation").toList();
        deleteEnhancements(cmsObjects, enhs, orh);

        enhs = model.listStatements(null, RDF.type, NamespaceEnum.enhancer + "TextAnnotation").toList();
        deleteEnhancements(cmsObjects, enhs, orh);
    }

    private void deleteEnhancements(List<DObject> cmsObjects, List<Statement> enhs, OntologyResourceHelper orh) {
        String enhOwner;
        String reference;

        for (Statement stmt : enhs) {
            Statement refStmt = stmt.getSubject().getProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP);
            enhOwner = refStmt.getObject().asLiteral().getString();
            if (refStmt != null) {
                for (DObject cmsObject : cmsObjects) {
                    reference = ENHANCEMENT_PREFIX + cmsObject.getID();
                    if (enhOwner.contentEquals(reference)) {
                        orh.deleteStatementsByResource(refStmt.getSubject());
                    }
                }
            }
        }
    }

    private List<DObject> object2dobject(List<Object> objects, MappingEngine engine) {
        List<DObject> dObjects = new ArrayList<DObject>();
        if (objects != null) {
            DObjectAdapter adapter = engine.getDObjectAdapter();
            for (Object o : objects) {
                if (canProcess(o)) {
                    dObjects.add(adapter.wrapAsDObject((CMSObject) o));
                }
            }
        }
        return dObjects;
    }

    private Boolean isRootNode(String query, String objectPath) {
        if (query.substring(0, query.length() - 2).contentEquals(objectPath)) {
            return true;
        }
        return false;
    }

    @Activate
    public void activate(ComponentContext context) {
        client = Client.create();
        Object contentPropertiesObject = context.getProperties().get(PROP_CONTENT_PROPERTY);
        if (contentPropertiesObject instanceof String[]) {
            contentProperties = (String[]) contentPropertiesObject;
        } else {
            if (contentPropertiesObject != null) {
                contentProperties = new String[1];
                contentProperties[0] = (String) contentPropertiesObject;
            }
        }

        engineRootResource = (String) context.getProperties().get(PROP_CONTENTHUB_RESOURCE);
    }

    @Deactivate
    public void deactivate() {
        client = null;
    }

    public void bindStore(Store store) {
        this.store = store;
    }

    public void unbindStore(Store store) {
        this.store = null;
    }
}
