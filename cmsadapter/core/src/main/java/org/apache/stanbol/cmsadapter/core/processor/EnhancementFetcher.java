/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.cmsadapter.core.processor;

import java.util.ArrayList;
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
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * This processer can process CMS Objects of type {@link ContentObject}. If provided ContentObject has a
 * property with name equal to configured property
 * <b>org.apache.stanbol.cmsadapter.core.processor.EnhancementFetcher.contentProperty</b> then the content
 * submitted to content hub, and the individual generated for ContentObject is related to enhancement graph
 * with {@link CMSAdapterVocabulary#CMSAD_PROPERTY_CONTENT_ITEM_REF}
 * 
 * @author cihan
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class EnhancementFetcher extends BaseProcessor implements Processor, ProcessorProperties {
    private static final Logger logger = LoggerFactory.getLogger(EnhancementFetcher.class);

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
    public Boolean canProcess(Object cmsObject, Object session) {
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
                if (canProcess(cmsObject.getInstance(), null)) {
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
                addContentItemRelation(cmsObject.getID(),
                    engineRootResource + "metadata/" + cmsObject.getID(), engine);
            } catch (Exception e) {
                logger.warn("Failed to create content item for cms object: {}", cmsObject.getName());
                return;
            }
        } else {
            logger.warn("Empty content for object {}", cmsObject.getName());
        }
    }

    private void addContentItemRelation(String cmsObjectId, String contentItemReference, MappingEngine engine) {

        List<Statement> resources = engine
                .getOntModel()
                .listStatements(null, CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                    ResourceFactory.createPlainLiteral(cmsObjectId)).toList();
        for (Statement stmt : resources) {
            Resource resource = stmt.getSubject();
            resource.addProperty(CMSAdapterVocabulary.CMSAD_PROPERTY_CONTENT_ITEM_REF,
                ResourceFactory.createResource(contentItemReference));
            logger.debug("Added {} as content item ref to cms object with id {}", contentItemReference,
                cmsObjectId);
        }
    }

    private void deleteContentItemRelation(String cmsObjectId, MappingEngine engine) {
        List<Statement> resources = engine
                .getOntModel()
                .listStatements(null, CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                    ResourceFactory.createPlainLiteral(cmsObjectId)).toList();
        for (Statement stmt : resources) {
            engine.getOntModel().removeAll(stmt.getSubject(),
                CMSAdapterVocabulary.CMSAD_PROPERTY_CONTENT_ITEM_REF, null);
        }
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
                if (canProcess(cmsObject.getInstance(), null)) {
                    processableObjects.add(cmsObject);
                }
            }
            deleteEnhancementsOfCMSObject(processableObjects, engine);
        }
    }

    private void deleteEnhancementsOfCMSObject(List<DObject> cmsObjects, MappingEngine engine) {
        for (DObject cmsObject : cmsObjects) {
            deleteContentItemRelation(cmsObject.getID(), engine);
        }
    }

    private List<DObject> object2dobject(List<Object> objects, MappingEngine engine) {
        List<DObject> dObjects = new ArrayList<DObject>();
        if (objects != null) {
            DObjectAdapter adapter = engine.getDObjectAdapter();
            for (Object o : objects) {
                if (canProcess(o, null)) {
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
