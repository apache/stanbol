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
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
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

@Component(immediate = true)
@Service
public class SubsumptionBridgesProcesser extends BaseProcessor implements Processor, ProcessorProperties {

    private static final Logger logger = LoggerFactory.getLogger(SubsumptionBridgesProcesser.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, CMSOBJECT_PRE);
    }

    private PropertyProcesser propertyBridgeProcessor = new PropertyProcesser();

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        List<DObject> cmsObjects = object2dobject(objects, engine);
        if (engine.getBridgeDefinitions() != null) {
            DObjectAdapter adapter = engine.getDObjectAdapter();
            List<SubsumptionBridge> subsumptionBridges = MappingModelParser.getSubsumptionBridges(engine
                    .getBridgeDefinitions());
            Object session = engine.getSession();
            RepositoryAccess accessor = engine.getRepositoryAccess();
            boolean emptyList = (cmsObjects == null || cmsObjects.size() == 0);

            for (SubsumptionBridge sb : subsumptionBridges) {
                if (emptyList) {
                    try {
                        List<CMSObject> retrievedObjects = accessor.getNodeByPath(sb.getSubjectQuery(),
                            session);
                        cmsObjects = new ArrayList<DObject>();
                        for (CMSObject o : retrievedObjects) {
                            cmsObjects.add(adapter.wrapAsDObject(o));
                        }
                    } catch (RepositoryAccessException e) {
                        logger.warn("Failed to obtain CMS Objects for query {}", sb.getSubjectQuery());
                        continue;
                    }
                }

                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), sb.getSubjectQuery())) {
                        try {
                            processSubsumptionBridgeCreate(sb, cmsObject, engine);
                        } catch (RepositoryAccessException e) {
                            logger.warn("Failed to process CMS Object {}", cmsObject);
                        }
                    }
                }
            }
        }
    }

    private void processSubsumptionBridgeCreate(SubsumptionBridge s,
                                                DObject parentObject,
                                                MappingEngine engine) throws RepositoryAccessException {

        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        OntClass parentClass = orh.createOntClassByCMSObject(parentObject.getInstance());
        if (parentClass != null) {
            processSubsumptionBridgeCreate(s.getPredicateName(), parentObject, engine, parentClass);
        } else {
            logger.warn("Failed to create OntClass for CMS Object {} while processing bridges for creation",
                parentObject.getName());
        }
    }

    public void processSubsumptionBridgeCreate(String predicateName,
                                               DObject parentObject,
                                               MappingEngine engine,
                                               OntClass parentClass) throws RepositoryAccessException {

        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        if (predicateName.equals("child")) {
            // find all child nodes of the parentMode
            for (DObject childObject : parentObject.getChildren()) {
                OntClass childClass = orh.createOntClassByCMSObject(childObject.getInstance());
                if (childClass != null) {
                    orh.addSubsumptionAssertion(parentClass, childClass);
                } else {
                    logger.warn("Failed to create OntClass for child object {} while processing CMS Object",
                        childObject.getName(), parentObject.getName());
                }
            }

        } else {
            // find the ranges of the predicate whose subject is parentNode
            for (DProperty property : parentObject.getProperties()) {
                DPropertyDefinition propDef = property.getDefinition();
                // propDef returns null if a * named property comes
                // TODO after handling * named properties, remove the null check
                if (propDef == null) {
                    logger.warn("Property definition could not be got for property {}", property.getName());
                    continue;
                }
                String propName = propDef.getName();
                if (propName.equals(predicateName) || propName.contains(predicateName)) {
                    List<CMSObject> referencedObjects = propertyBridgeProcessor.resolveReferenceNodes(
                        property, engine);
                    for (CMSObject o : referencedObjects) {
                        OntClass childClass = orh.createOntClassByCMSObject(o);
                        if (childClass != null) {
                            orh.addSubsumptionAssertion(parentClass, childClass);
                        } else {
                            logger.warn(
                                "Failed to create OntClass for referenced object {} while processing {}",
                                o.getLocalname(), parentObject.getName());
                        }
                    }
                    break;
                } else if (propName.contentEquals("*")) {
                    logger.warn("Properties added to nt:unstructured types (* named properties) are not handled yet");
                }
            }
        }
    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        if (engine.getBridgeDefinitions() != null) {
            List<DObject> cmsObjects = object2dobject(objects, engine);
            List<SubsumptionBridge> subsumptionBridges = MappingModelParser.getSubsumptionBridges(engine
                    .getBridgeDefinitions());
            OntologyResourceHelper orh = engine.getOntologyResourceHelper();

            for (SubsumptionBridge sb : subsumptionBridges) {
                for (DObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), sb.getSubjectQuery())) {
                        orh.deleteStatementsByReference(cmsObject.getID());
                    }
                }
            }
        }
    }

    @Override
    public Boolean canProcess(Object object, Object session) {
        return object instanceof ClassificationObject;
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