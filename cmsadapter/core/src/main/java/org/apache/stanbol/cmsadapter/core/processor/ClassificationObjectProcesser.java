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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ConceptBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ClassificationObject;
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

import com.hp.hpl.jena.ontology.OntClass;

/**
 * This processor can process {@link ClassificationObject}s. On
 * {@link #createDecoratedObjects(List, MappingEngine)} call, for each CMS Object of type
 * {@link ClassificationObject} an OWL Class is created. Also if there is a property mapping defined in
 * {@link BridgeDefinitions} then an instance of {@link PropertyProcesser} will be used to process these
 * definitions. <br/>
 * If there is a {@link SubsumptionBridge} service available, children of any {@link ClassificationObject}
 * will be processed by subsumption processer.
 * 
 * On {@link #deleteObjects(List, MappingEngine)} call, for each CMS Object of the type
 * {@link ClassificationObject} the previously created resource is found and all the triples of which the
 * resource is the subject is deleted.
 * 
 * @author Suat
 * 
 */
@Component(immediate = true)
@Service
@Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = Processor.class, policy = ReferencePolicy.DYNAMIC, bind = "bindProcessor", unbind = "unbindProcessor", name = "processor")
public class ClassificationObjectProcesser extends BaseProcessor implements Processor, ProcessorProperties {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationObjectProcesser.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, CMSOBJECT_PRE);
    }

    private SubsumptionBridgesProcesser subsumptionBridgeProcessor;

    private PropertyProcesser propertyBridgeProcessor = new PropertyProcesser();

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = cmsObject2dobject(objects, engine);
        createDecoratedObjects(cmsObjects, engine);
    }

    private void createDecoratedObjects(List<DObject> cmsObjects, MappingEngine engine) {
        // if there is bridge definitions try to fetch concept bridges
        if (engine.getBridgeDefinitions() != null) {
            List<ConceptBridge> conceptBridges = MappingModelParser.getConceptBridges(engine
                    .getBridgeDefinitions());
            RepositoryAccess accessor = engine.getRepositoryAccess();
            Object session = engine.getSession();
            DObjectAdapter adapter = engine.getDObjectAdapter();
            boolean emptyList = (cmsObjects == null || cmsObjects.size() == 0);

            for (ConceptBridge cb : conceptBridges) {
                // cms objects will be null in the case of initial bridge execution or update of bridge
                // definitions i.e when a BridgeDefinitionsResource service is called
                if (emptyList) {
                    try {
                        List<CMSObject> retrievedObjects = accessor.getNodeByPath(cb.getQuery(), session);
                        cmsObjects = new ArrayList<DObject>();
                        for (CMSObject o : retrievedObjects) {
                            cmsObjects.add(adapter.wrapAsDObject(o));
                        }
                    } catch (RepositoryAccessException e) {
                        logger.warn("Error at processor", e);
                        logger.warn("Failed to obtain CMS Objects for query {}", cb.getQuery());
                        continue;
                    }
                }

                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), cb.getQuery())) {
                        try {
                            processConceptBridgeCreate(cb, cmsObject, engine);
                        } catch (RepositoryAccessException e) {
                            logger.warn("Failed to process CMS Object {}", cmsObject.getName());
                        }
                    }
                }
            }
        } else {
            // work without bridge definitions
            for (DObject cmsObject : cmsObjects) {
                if (canProcess(cmsObject.getInstance(), null)) {
                    try {
                        OntClass parentClass = processObject(cmsObject, engine);
                        if (parentClass == null) {
                            continue;
                        }
                        processProperties(cmsObject, parentClass, engine);

                        // process children
                        List<DObject> children = cmsObject.getChildren();
                        createDecoratedObjects(children, engine);
                    } catch (RepositoryAccessException e) {
                        logger.warn("Failed to process CMS Object {}", cmsObject.getName());
                        logger.warn("Message: " + e.getMessage());
                    }
                }
            }
        }
    }

    private OntClass processObject(DObject cmsObject, MappingEngine engine) throws RepositoryAccessException {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        OntClass parentClass = orh.createOntClassByCMSObject(cmsObject.getInstance());
        if (parentClass == null) {
            logger.warn("Failed create class from CMS Object {}", cmsObject.getName());
            return null;
        }

        List<DObject> children = cmsObject.getChildren();
        if (children != null) {
            for (DObject child : children) {
                OntClass childClass = orh.createOntClassByCMSObject(child.getInstance());
                if (childClass != null) {
                    orh.addSubsumptionAssertion(parentClass, childClass);
                } else {
                    logger.warn("Failed to create OntClass for child object {} while processing object {}",
                        child.getName(), cmsObject.getName());
                }
            }
        }
        return parentClass;
    }

    private void processProperties(DObject cmsObject, OntClass subjectClass, MappingEngine engine) throws RepositoryAccessException {
        for (DProperty prop : cmsObject.getProperties()) {
            DPropertyDefinition propDef = prop.getDefinition();
            // propDef returns null if a * named property comes
            // TODO after handling * named properties, remove the null check
            if (propDef == null) {
                logger.warn("Failed to get property definition for property {}", prop.getName());
                continue;
            }

            AnnotationType annotationType = propDef.getAnnotations();
            propertyBridgeProcessor.processClassificationObjectProperty(prop, annotationType, subjectClass,
                engine);
        }
    }

    private void processConceptBridgeCreate(ConceptBridge conceptBridge,
                                            DObject cmsObject,
                                            MappingEngine engine) throws RepositoryAccessException {

        OntClass parentClass = processObject(cmsObject, engine);
        if (parentClass == null) {
            return;
        }
        processInnerBridges(conceptBridge, cmsObject, parentClass, engine);
    }

    private void processInnerBridges(ConceptBridge conceptBridge,
                                     DObject cmsObject,
                                     OntClass parentClass,
                                     MappingEngine engine) throws RepositoryAccessException {

        // If SubsumptionBridgeExists....
        if (subsumptionBridgeProcessor != null) {
            List<SubsumptionBridge> subsumptionBridgeList = conceptBridge.getSubsumptionBridge();
            for (SubsumptionBridge bridge : subsumptionBridgeList) {
                /*
                 * as subsumption assertions are already deleted before, we directly call create method to
                 * prevent deletion of newly added assertions
                 */
                subsumptionBridgeProcessor.processSubsumptionBridgeCreate(bridge.getPredicateName(),
                    cmsObject, engine, parentClass);
            }
        } else {
            logger.warn("There is no valid subsumption bridge processor");
        }

        // If PropertyBridges Exist.....
        if (propertyBridgeProcessor != null) {
            List<PropertyBridge> propertyBridgeList = conceptBridge.getPropertyBridge();
            for (PropertyBridge bridge : propertyBridgeList) {
                // create subsumptionrelationships....
                propertyBridgeProcessor.processConceptPropertyBridgeCreate(parentClass, bridge, cmsObject,
                    engine);
            }
        } else {
            logger.warn("There is no valid property bridge processor");
        }
    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = cmsObject2dobject(objects, engine);
        deleteDecoratedObjects(cmsObjects, engine);
    }

    private void deleteDecoratedObjects(List<DObject> cmsObjects, MappingEngine engine) {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();

        // if there is bridge definitions try to fetch concept bridges
        if (engine.getBridgeDefinitions() != null) {
            List<ConceptBridge> conceptBridges = MappingModelParser.getConceptBridges(engine
                    .getBridgeDefinitions());

            for (ConceptBridge cb : conceptBridges) {
                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), cb.getQuery())) {
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

    @Override
    public Boolean canProcess(Object cmsObject, Object session) {
        return cmsObject instanceof ClassificationObject;
    }

    protected void bindProcessor(Processor processor) {
        String processorName = processor.getClass().getName();
        if (processorName.contentEquals(SubsumptionBridgesProcesser.class.getName())) {
            this.subsumptionBridgeProcessor = (SubsumptionBridgesProcesser) processor;
        }
    }

    protected void unbindProcessor(Processor processor) {
        this.subsumptionBridgeProcessor = null;
    }

    @Override
    public Map<String,Object> getProcessorProperties() {
        return properties;
    }

    private List<DObject> cmsObject2dobject(List<Object> objects, MappingEngine engine) {
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
