package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.stanbol.cmsadapter.jcr.repository.JCRModelMapper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

public class JCRNodeTypeLifter extends JCRProcessor {
    private static final Logger logger = LoggerFactory.getLogger(JCRNodeTypeLifter.class);

    public JCRNodeTypeLifter(MappingEngine mappingEngine) {
        super(mappingEngine);
    }

    public void lift() throws RepositoryException {
        //initializeDefaultResources();

        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nodeTypesItr = nodeTypeManager.getAllNodeTypes();
        while (nodeTypesItr.hasNext()) {
            NodeType curNodeType = nodeTypesItr.nextNodeType();
            // create the class
            OntClass nodeTypeClass = ontologyResourceHelper
                    .createOntClassByObjectTypeDefinition(JCRModelMapper.getObjectTypeDefinition(curNodeType));

            // create subsumption relationships
            NodeType[] supertypes = curNodeType.getDeclaredSupertypes();
            for (NodeType supertype : supertypes) {
                OntClass s = ontologyResourceHelper.createOntClassByObjectTypeDefinition(JCRModelMapper
                        .getObjectTypeDefinition(supertype));
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
                            rangeClass = ontologyResourceHelper
                                    .createOntClassByCMSObject(referencedObjects.get(0));

                        } else {
                            RDFList rdfList = jcrOntModel.createList();
                            for (CMSObject referencedObject : referencedObjects) {
                                rdfList = rdfList.cons(ontologyResourceHelper
                                        .createOntClassByCMSObject(referencedObject));
                            }
                            rangeClass = ontologyResourceHelper.createUnionClass(rdfList);
                        }
                        ontologyResourceHelper.createObjectPropertyByPropertyDefinition(propDef,
                            Arrays.asList(new Resource[] {nodeTypeClass}),
                            Arrays.asList(new Resource[] {rangeClass}));

                    } else {
                        ontologyResourceHelper.createDatatypePropertyByPropertyDefinition(propDef,
                            Arrays.asList(new Resource[] {nodeTypeClass}));
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
