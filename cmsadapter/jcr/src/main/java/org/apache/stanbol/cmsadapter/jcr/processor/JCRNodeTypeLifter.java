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
package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.stanbol.cmsadapter.jcr.repository.JCRModelMapper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

public class JCRNodeTypeLifter {
    private static final Logger logger = LoggerFactory.getLogger(JCRNodeTypeLifter.class);

    private MappingEngine engine;
    private Session session;
    private RepositoryAccess accessor;
    private OntologyResourceHelper ontologyResourceHelper;
    private OntModel jcrOntModel;
    
    public JCRNodeTypeLifter(MappingEngine mappingEngine) {
        this.engine = mappingEngine;
        this.session = (Session) engine.getSession();
        this.ontologyResourceHelper = this.engine.getOntologyResourceHelper();
        this.jcrOntModel = this.engine.getOntModel();
        this.accessor = this.engine.getRepositoryAccessManager()
                .getRepositoryAccess(this.engine.getSession());
        if (this.accessor == null) {
            throw new IllegalArgumentException("Can not find suitable accessor");
        }
    }

    public void lift() throws RepositoryException {
        // initializeDefaultResources();

        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nodeTypesItr = nodeTypeManager.getAllNodeTypes();
        while (nodeTypesItr.hasNext()) {
            NodeType curNodeType = nodeTypesItr.nextNodeType();
            // create the class
            ObjectTypeDefinition otd = JCRModelMapper.getObjectTypeDefinition(curNodeType);
            OntClass nodeTypeClass = ontologyResourceHelper.createOntClassByObjectTypeDefinition(otd);
            if (nodeTypeClass == null) {
                logger.warn("Failed to create OntClass for object type definition {}", otd.getLocalname());
                continue;
            }

            // create subsumption relationships
            NodeType[] supertypes = curNodeType.getDeclaredSupertypes();
            for (NodeType supertype : supertypes) {
                otd = JCRModelMapper.getObjectTypeDefinition(supertype);
                OntClass s = ontologyResourceHelper.createOntClassByObjectTypeDefinition(otd);
                if (s == null) {
                    logger.warn("Failed to create OntClass for object type definition {}", otd.getLocalname());
                    continue;
                }
                nodeTypeClass.addSuperClass(s);
            }

            // create properties
            PropertyDefinition[] propertyDefinitionList = curNodeType.getDeclaredPropertyDefinitions();
            for (PropertyDefinition jcrPropDef : propertyDefinitionList) {
                org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition propDef = JCRModelMapper
                        .getPropertyDefinition(jcrPropDef);
                if (jcrPropDef.getName().equals("*")) {
                    // TODO decide how to handle * named properties
                } else {
                    if ((propDef.getPropertyType() == PropType.NAME)
                        || (propDef.getPropertyType() == PropType.PATH)
                        || (propDef.getPropertyType() == PropType.REFERENCE)) {

                        List<String> constraints = propDef.getValueConstraint();
                        List<CMSObject> referencedObjects = new ArrayList<CMSObject>();

                        if (propDef.getPropertyType() == PropType.NAME) {
                            for (String constraint : constraints) {
                                try {
                                    referencedObjects
                                            .addAll(accessor.getNodeByName(constraint, this.session));
                                } catch (RepositoryAccessException e) {
                                    logger.warn("Error while getting referenced value {} ", constraint, e);
                                }
                            }

                        } else if (propDef.getPropertyType() == PropType.PATH) {
                            for (String constraint : constraints) {
                                try {
                                    referencedObjects
                                            .addAll(accessor.getNodeByPath(constraint, this.session));
                                } catch (RepositoryAccessException e) {
                                    logger.warn("Error while getting referenced value {} ", constraint, e);
                                }
                            }

                        } else if (propDef.getPropertyType() == PropType.REFERENCE) {
                            for (String constraint : constraints) {
                                try {
                                    referencedObjects.addAll(accessor.getNodeById(constraint, this.session));
                                } catch (RepositoryAccessException e) {
                                    logger.warn("Error while getting referenced value {} ", constraint, e);
                                }
                            }
                        }

                        Resource rangeClass = null;
                        if (referencedObjects.size() == 0) {
                            rangeClass = OWL.Thing;

                        } else if (referencedObjects.size() == 1) {
                            rangeClass = ontologyResourceHelper.createOntClassByCMSObject(referencedObjects
                                    .get(0));

                            if (rangeClass == null) {
                                logger.warn("Failed create class for range value {}", referencedObjects
                                        .get(0).getLocalname());
                            }

                        } else {
                            RDFList rdfList = jcrOntModel.createList();
                            for (CMSObject referencedObject : referencedObjects) {
                                rdfList = rdfList.cons(ontologyResourceHelper
                                        .createOntClassByCMSObject(referencedObject));
                            }
                            rangeClass = ontologyResourceHelper.createUnionClass(rdfList);
                        }
                        ObjectProperty op = ontologyResourceHelper.createObjectPropertyByPropertyDefinition(
                            propDef, Arrays.asList(new Resource[] {nodeTypeClass}),
                            Arrays.asList(new Resource[] {rangeClass}));

                        if (op == null) {
                            logger.warn("Failed to create ObjectProperty for property definition {}",
                                propDef.getLocalname());
                        }

                    } else {
                        DatatypeProperty dtp = ontologyResourceHelper
                                .createDatatypePropertyByPropertyDefinition(propDef,
                                    Arrays.asList(new Resource[] {nodeTypeClass}));

                        if (dtp == null) {
                            logger.warn("Failed to create DatatypeProperty for property definition {}",
                                propDef.getLocalname());
                        }
                    }
                }
            }
        }
    }

    private void initializeDefaultResources() {
        addPathProperty();
    }

    private void addPathProperty() {
        String propName = CMSAdapterVocabulary.CMS_ADAPTER_VOCABULARY_PREFIX + ":"
                          + CMSAdapterVocabulary.CMSAD_PATH_PROP_NAME;
        org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition propertyDefinition = new org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition();
        propertyDefinition.setLocalname(propName);
        propertyDefinition.setPropertyType(PropType.STRING);
        propertyDefinition.setUniqueRef(propName);
        ontologyResourceHelper.createDatatypePropertyByPropertyDefinition(propertyDefinition,
            Arrays.asList(new Resource[] {OWL.Thing}));
    }
}
