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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.ProcessorProperties;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This processer can process CMS Objects of type {@link ObjectTypeDefinition}. A type definition corresponds
 * to an OWL Class and properties defined on the type corresponds to OWL datatype or OWL object properties. <br/>
 * A property is converted to an OWL object property if it is type is amongst following:
 * <ul>
 * <li>{@link PropType#NAME}</li>
 * <li>{@link PropType#PATH}</li>
 * <li>{@link PropType#REFERENCE}</li>
 * </ul>
 * Otherwise the property is converted to an OWL datatype property. <br/>
 * 
 * @author Suat
 * 
 */
@Component(immediate = true)
@Service
public class ObjectTypeProcessor implements Processor, ProcessorProperties {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationObjectProcesser.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, OBJECT_TYPE);
    }

    @Override
    public Boolean canProcess(Object cmsObject, Object session) {
        return cmsObject instanceof ObjectTypeDefinition;
    }

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        List<DObjectType> objectTypes = cmsObject2dobjectType(objects, engine);
        createDecoratedObjectTypes(objectTypes, engine);
    }

    private void createDecoratedObjectTypes(List<DObjectType> objectTypes, MappingEngine engine) {
        for (DObjectType objectType : objectTypes) {
            if (canProcess(objectType.getInstance(), null)) {
                try {
                    OntClass parentClass = processType(objectType, engine);
                    if (parentClass == null) {
                        continue;
                    }
                    processProperties(objectType, parentClass, engine);

                    // process children
                    List<DObjectType> children = objectType.getChildDefinitions();
                    createDecoratedObjectTypes(children, engine);
                } catch (RepositoryAccessException e) {
                    logger.warn("Failed to process ObjectTypeDefinition {}", objectType.getName());
                    logger.warn("Message: " + e.getMessage());
                }
            }
        }
    }

    private OntClass processType(DObjectType objectType, MappingEngine engine) throws RepositoryAccessException {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        OntClass parentClass = orh.createOntClassByObjectTypeDefinition(objectType.getInstance());
        if (parentClass == null) {
            logger.warn("Failed create class from CMS Object {}", objectType.getName());
            return null;
        }
        List<DObjectType> children = objectType.getChildDefinitions();
        if (children != null) {
            for (DObjectType child : children) {
                OntClass childClass = orh.createOntClassByObjectTypeDefinition(child.getInstance());
                if (childClass != null) {
                    orh.addSubsumptionAssertion(parentClass, childClass);
                } else {
                    logger.warn("Failed to create OntClass for child object {} while processing type {}",
                        child.getName(), objectType.getName());
                }
            }
        }
        return parentClass;
    }

    private void processProperties(DObjectType objectType, OntClass subjectClass, MappingEngine engine) throws RepositoryAccessException {
        for (DPropertyDefinition propDef : objectType.getPropertyDefinitions()) {
            OntologyResourceHelper orh = engine.getOntologyResourceHelper();

            if ((propDef.getPropertyType() == PropType.NAME) || (propDef.getPropertyType() == PropType.PATH)
                || (propDef.getPropertyType() == PropType.REFERENCE)) {

                ObjectProperty op = orh.createObjectPropertyByPropertyDefinition(propDef.getInstance(),
                    Arrays.asList(new Resource[] {subjectClass}), new ArrayList<Resource>());

                if (op == null) {
                    logger.warn("Failed to create ObjectProperty for property definition {}",
                        propDef.getName());
                }

            } else {
                DatatypeProperty dtp = orh.createDatatypePropertyByPropertyDefinition(propDef.getInstance(),
                    Arrays.asList(new Resource[] {subjectClass}));

                if (dtp == null) {
                    logger.warn("Failed to create DatatypeProperty for property definition {}",
                        propDef.getName());
                }
            }
        }
    }

    @Override
    public void deleteObjects(List<Object> objects, MappingEngine engine) {
        List<DObjectType> objectTypes = cmsObject2dobjectType(objects, engine);
        deleteDecoratedObjectTypes(objectTypes, engine);
    }

    private void deleteDecoratedObjectTypes(List<DObjectType> objectTypes, MappingEngine engine) {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        for (DObjectType objectType : objectTypes) {
            if (canProcess(objectType.getInstance(), null)) {
                orh.deleteStatementsByReference(objectType.getID());
                deletePropertyDefinitions(objectType.getID(), orh);

                List<DObjectType> children = new ArrayList<DObjectType>();
                try {
                    children = objectType.getChildDefinitions();
                } catch (RepositoryAccessException e) {
                    // Not expected
                }
                deleteDecoratedObjectTypes(children, engine);
            }
        }
    }

    private void deletePropertyDefinitions(String objectTypeRef, OntologyResourceHelper orh) {
        orh.deleteObjectTypeProperties(objectTypeRef);
    }

    @Override
    public Map<String,Object> getProcessorProperties() {
        return properties;
    }

    private List<DObjectType> cmsObject2dobjectType(List<Object> objects, MappingEngine engine) {
        List<DObjectType> dObjectTypes = new ArrayList<DObjectType>();

        if (objects != null) {
            DObjectAdapter adapter = engine.getDObjectAdapter();
            for (Object o : objects) {
                if (canProcess(o, null)) {
                    dObjectTypes.add(adapter.wrapAsDObjectType((ObjectTypeDefinition) o));
                }
            }
        }
        return dObjectTypes;
    }
}
