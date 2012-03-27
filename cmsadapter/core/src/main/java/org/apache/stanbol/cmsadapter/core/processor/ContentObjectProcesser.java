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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.InstanceBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ContentObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.processor.BaseProcessor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.ProcessorProperties;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;

/**
 * This processor can process {@link ContentObject}s. On {@link #createDecoratedObjects(List, MappingEngine)}
 * call, for each CMS Object of type {@link ContentObject} an OWL Individual is created. Also if there is a
 * property mapping defined in {@link BridgeDefinitions} then an instance of {@link PropertyProcesser} will be
 * used to process these definitions. <br/>
 * 
 * On {@link #deleteObjects(List, MappingEngine)} call, for each CMS Object of the type {@link ContentObject}
 * the previously created resource is found and all the triples of which the resource is the subject is
 * deleted.
 * 
 * @author Suat
 * 
 */
@Component(immediate = true)
@Service
public class ContentObjectProcesser extends BaseProcessor implements Processor, ProcessorProperties {

    private static final Logger logger = LoggerFactory.getLogger(ContentObjectProcesser.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, CMSOBJECT_POST);
    }

    private PropertyProcesser propertyBridgeProcessor = new PropertyProcesser();

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

                        try {
                            processInstanceBridgeCreate(ib, contentObject, engine);
                        } catch (RepositoryAccessException e) {
                            logger.warn("Failed to process CMS Object {}", contentObject);
                        }
                    }
                }
            }
        } else {
            // work without bridge definitions
            for (DObject cmsObject : cmsObjects) {
                if (canProcess(cmsObject.getInstance(), null)) {
                    try {
                        Individual individual = processObject(cmsObject, engine);
                        if (individual == null) {
                            continue;
                        }
                        processProperties(cmsObject, individual, engine);
                    } catch (RepositoryAccessException e) {
                        logger.warn("Failed to process CMS Object {}", cmsObject);
                    }
                }
            }
        }
    }

    private Individual processObject(DObject contentObject, MappingEngine engine) {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        OntClass primaryNodeClass = orh.createOntClassByReference(contentObject.getInstance()
                .getObjectTypeRef());
        if (primaryNodeClass == null) {
            logger.warn("Failed to create OntClass for reference {}", contentObject.getInstance()
                    .getObjectTypeRef());
            return null;
        }

        // create individual
        Individual individual = orh
                .createIndividualByCMSObject(contentObject.getInstance(), primaryNodeClass);
        if (individual == null) {
            logger.warn("Failed to create Individual for CMS object {}", contentObject.getName());
            return null;
        }
        return individual;
    }

    private void processProperties(DObject cmsObject, Individual individual, MappingEngine engine) throws RepositoryAccessException {
        for (DProperty prop : cmsObject.getProperties()) {
            DPropertyDefinition propDef = prop.getDefinition();
            // propDef returns null if a * named property comes
            // TODO after handling * named properties, remove the null check
            if (propDef == null) {
                logger.warn("Property definition could not be got for property {}", prop.getName());
                continue;
            }

            propertyBridgeProcessor.processContentObjectProperty(prop, propDef, cmsObject, individual,
                propDef.getAnnotations(), engine);
        }
    }

    private void processInstanceBridgeCreate(InstanceBridge instanceBridge,
                                             DObject contentObject,
                                             MappingEngine engine) throws RepositoryAccessException {

        Individual individual = processObject(contentObject, engine);
        if (individual == null) {
            return;
        }
        processInnerBridges(instanceBridge, contentObject, individual, engine);
    }

    @Override
    public Boolean canProcess(Object object, Object session) {
        return object instanceof ContentObject;
    }

    private void processInnerBridges(InstanceBridge instanceBridge,
                                     DObject contentObject,
                                     Individual individual,
                                     MappingEngine engine) throws RepositoryAccessException {

        // process propertyBridges
        List<PropertyBridge> instancePropertyBridges = instanceBridge.getPropertyBridge();
        // TODO property bridges containing instance of annotations should be
        // handled first
        if (propertyBridgeProcessor != null) {
            for (PropertyBridge propBridge : instancePropertyBridges) {
                propertyBridgeProcessor.processInstancePropertyBridgeCreate(individual, contentObject,
                    propBridge, engine);
            }
        }
    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = object2dobject(objects, engine);
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();

        // if there is bridge definitions try to fetch concept bridges
        if (engine.getBridgeDefinitions() != null) {
            List<InstanceBridge> instanceBridges = MappingModelParser.getInstanceBridges(engine
                    .getBridgeDefinitions());

            for (InstanceBridge ib : instanceBridges) {
                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), ib.getQuery())) {
                        orh.deleteStatementsByReference(cmsObject.getID());
                    }
                }
            }
        } else {
            for (DObject cmsObject : cmsObjects) {
                if (canProcess(cmsObject.getInstance(), null)) {
                    orh.deleteStatementsByReference(cmsObject.getID());
                }
            }
        }
    }

    private Boolean isRootNode(String query, String objectPath) {
        if (query.substring(0, query.length() - 2).contentEquals(objectPath)) {
            return true;
        }
        return false;
    }

    @Override
    public Map<String,Object> getProcessorProperties() {
        return properties;
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
}
