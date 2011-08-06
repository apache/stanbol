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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.jcr.repository.JCRModelMapper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.processor.TypeLifter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.Resource;

@Component(immediate = true)
@Service
public class JCRNodeTypeLifter implements TypeLifter {
    private static final Logger logger = LoggerFactory.getLogger(JCRNodeTypeLifter.class);

    private MappingEngine engine;
    private Session session;
    private RepositoryAccess accessor;
    private OntologyResourceHelper ontologyResourceHelper;

    @Override
    public void liftNodeTypes(MappingEngine engine) throws RepositoryAccessException {
        initialize(engine);
        try {
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
                        logger.warn("Failed to create OntClass for object type definition {}",
                            otd.getLocalname());
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

                            ObjectProperty op = ontologyResourceHelper
                                    .createObjectPropertyByPropertyDefinition(propDef,
                                        Arrays.asList(new Resource[] {nodeTypeClass}),
                                        new ArrayList<Resource>());

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
        } catch (RepositoryException e) {
            throw new RepositoryAccessException("Repository exception while lifting node type definitions ",
                    e);
        }
    }

    @Override
    public boolean canLift(String type) {
        return type.contentEquals("JCR");
    }

    private void initialize(MappingEngine engine) {
        this.engine = engine;
        this.session = (Session) engine.getSession();
        this.ontologyResourceHelper = this.engine.getOntologyResourceHelper();
        this.accessor = this.engine.getRepositoryAccessManager()
                .getRepositoryAccess(this.engine.getSession());
        if (this.accessor == null) {
            throw new IllegalArgumentException("Can not find suitable accessor");
        }
    }
}