package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyAnnotation;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;

public class PropertyBridgesProcesser extends JCRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PropertyBridgesProcesser.class);

    public PropertyBridgesProcesser(MappingEngine mappingEngine) {
        super(mappingEngine);
    }

    public void processUpdates(List<CMSObject> cmsObjects) {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        OntologyResourceHelper ontologyResourceHelper = engine.getOntologyResourceHelper();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<PropertyBridge> propertyBridges = MappingModelParser.getPropertyBridges(bridgeDefinitions);

        for (PropertyBridge bridge : propertyBridges) {
            try {
                for (CMSObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), bridge.getSubjectQuery())) {
                        OntClass c = ontologyResourceHelper.createOntClassByCMSObject(cmsObject);
                        processConceptPropertyBridge(c, bridge, adapter.wrapAsDObject(cmsObject));
                    }
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Error processing bridge", e);
            }
        }

    }

    public void processBridges() {

        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        OntologyResourceHelper ontologyResourceHelper = engine.getOntologyResourceHelper();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<PropertyBridge> propertyBridges = MappingModelParser.getPropertyBridges(bridgeDefinitions);

        for (PropertyBridge bridge : propertyBridges) {
            try {
                List<CMSObject> cmsObjects = this.accessor.getNodeByPath(bridge.getSubjectQuery(), session);

                for (CMSObject cmsObject : cmsObjects) {
                    OntClass c = ontologyResourceHelper.createOntClassByCMSObject(cmsObject);
                    processConceptPropertyBridge(c, bridge, adapter.wrapAsDObject(cmsObject));
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Error processing bridge", e);
            }
        }
    }

    public void processConceptPropertyBridge(OntClass subjectClass,
                                             PropertyBridge propertyBridge,
                                             DObject propertySubject) throws RepositoryAccessException {

        String predicateName = propertyBridge.getPredicateName();
        PropertyAnnotation propertyAnnotation = propertyBridge.getPropertyAnnotation();

        for (DProperty prop : propertySubject.getProperties()) {
            DPropertyDefinition propDef = prop.getDefinition();
            // propDef returns null if a * named property comes
            // TODO after handling * named properties, remove the null check
            if (propDef == null) {
                logger.warn("Property definition could not be got for property {}", prop.getName());
                continue;
            }

            String propName = propDef.getName();
            if (propName.equals(predicateName) || propName.contains(predicateName)) {
                // TODO consider other property types
                List<CMSObject> referencedNodes = new ArrayList<CMSObject>();
                if (prop.getType().equals(PropType.REFERENCE)) {
                    // Resolve references
                    // TODO need a better value representation than string
                    // For example reference types may be resolved an put as an
                    // objecttype
                    for (String value : prop.getValue()) {
                        try {
                            referencedNodes.addAll(accessor.getNodeById(value, session));
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error resolving reference value {}", value, e);
                        }
                    }

                } else if (prop.getType().equals(PropType.NAME)) {
                    for (String name : prop.getValue()) {
                        try {
                            referencedNodes.addAll(accessor.getNodeByName(name, session));
                        } catch (Exception e) {
                            logger.warn("Error at getting name nodes.", e);
                        }
                    }

                } else if (prop.getType().equals(PropType.PATH)) {
                    for (String value : prop.getValue()) {
                        try {
                            referencedNodes.addAll(accessor.getNodeByPath(value, session));
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error at getting node by path ", e);
                        }
                    }
                }

                if (propertyAnnotation != null) {
                    processConceptPropertyBridgeAnnotation(propertyAnnotation, subjectClass, referencedNodes);
                } else {
                    logger.warn("One of the following annotations is expected: 'subsumption', 'disjointWith', 'equivalentClass'");
                }
                // property found. break the loop
                break;
            }
        }
    }

    public void processConceptPropertyBridgeAnnotation(PropertyAnnotation propertyAnnotation,
                                                       OntClass subjectClass,
                                                       List<CMSObject> referencedNodes) {
        AnnotationType annotation = propertyAnnotation.getAnnotation();
        List<OntClass> referencedClasses = new ArrayList<OntClass>();
        for (CMSObject referenceObject : referencedNodes) {
            referencedClasses.add(ontologyResourceHelper.createOntClassByCMSObject(referenceObject));
        }

        // process the annotations
        if (annotation.equals(AnnotationType.SUBSUMPTION)) {
            for (OntClass c : referencedClasses) {
                subjectClass.addSubClass(c);
            }

        } else if (annotation.equals(AnnotationType.EQUIVALENT_CLASS)) {
            for (OntClass c : referencedClasses) {
                subjectClass.addEquivalentClass(c);
            }

        } else if (annotation.equals(AnnotationType.DISJOINT_WITH)) {
            for (OntClass c : referencedClasses) {
                subjectClass.addDisjointWith(c);
            }
        } else {
            logger.warn("{} annotation is not supported for this property", annotation.value());
            return;
        }
    }

    public void processInstancePropertyBridge(Individual individual,
                                              DObject contentObject,
                                              PropertyBridge propertyBridge,
                                              List<OntClass> indTypes) throws RepositoryAccessException {
        String predicateName = propertyBridge.getPredicateName();

        for (DProperty property : contentObject.getProperties()) {
            DPropertyDefinition propDef = property.getDefinition();
            // propDef returns null if a * named property comes
            // TODO after handling * named properties, remove the null check
            if (propDef == null) {
                logger.warn("Property definition could not be got for property {}", property.getName());
                continue;
            }
            String propName = propDef.getName();
            if (propName.equals(predicateName) || propName.contains(predicateName)) {
                if (objectPropertyCheck(property)) {
                    processObjectProperty(individual, contentObject, propertyBridge, property, propDef,
                        indTypes);
                } else if (datatypePropertyCheck(property)) {
                    processDataTypeProperty(individual, property, propDef);
                } else {
                    logger.warn("{} property type is not supported yet", property.getType());
                }
                // property found break the loop
                break;
            }
        }
    }

    private void processObjectProperty(Individual individual,
                                       DObject contentObject,
                                       PropertyBridge propBridge,
                                       DProperty property,
                                       DPropertyDefinition propDef,
                                       List<OntClass> indTypes) {

        // get referenced values
        List<CMSObject> referencedObjects = new ArrayList<CMSObject>();
        CMSObject parentObject = null;
        if (property.getType() == PropType.REFERENCE) {
            for (String referencedObject : property.getValue()) {
                try {
                    parentObject = this.accessor.getFirstNodeById(referencedObject, this.session);
                    referencedObjects.add(parentObject);
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedObject, e);
                }
            }

        } else if (property.getType() == PropType.NAME) {
            for (String referencedName : property.getValue()) {
                List<CMSObject> names;
                try {
                    names = this.accessor.getNodeByName(referencedName, this.session);
                    referencedObjects.addAll(names);
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedName, e);
                }
            }
        } else if (property.getType() == PropType.PATH) {
            for (String referencedPath : property.getValue()) {
                try {
                    parentObject = this.accessor.getFirstNodeByPath(referencedPath, this.session);
                    referencedObjects.add(parentObject);
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedPath, e);
                }
            }
        }

        ObjectProperty objectProperty = ontologyResourceHelper.getObjectPropertyByReference(propDef
                .getUniqueRef());
        PropertyAnnotation propertyAnnotation = propBridge.getPropertyAnnotation();
        if (propertyAnnotation != null) {

            // process instanceOf Annotation as it needs a special consideration
            if (propertyAnnotation.getAnnotation().value().equals("instanceOf")) {

                // Referenced items are added to the ontology and set as type of
                // individual
                OntClass parentClass;
                for (CMSObject parent : referencedObjects) {
                    parentClass = ontologyResourceHelper.createOntClassByCMSObject(parent);
                    individual.addOntClass(parentClass);
                    indTypes.add(parentClass);
                }
            } else {
                createReferencedIndividuals(individual, contentObject, objectProperty, referencedObjects);
            }
            processInstancePropertyBridgeAnnotation(propertyAnnotation, objectProperty);

        } else {
            createReferencedIndividuals(individual, contentObject, objectProperty, referencedObjects);
        }
    }

    private void createReferencedIndividuals(Individual individual,
                                             DObject contentObject,
                                             ObjectProperty objectProperty,
                                             List<CMSObject> referencedObjects) {

        OntResource individualType = ontologyResourceHelper.createOntClassByReference(contentObject
                .getInstance().getObjectTypeRef());

        for (CMSObject referencedObject : referencedObjects) {
            Individual ind = ontologyResourceHelper.createIndividualByCMSObject(referencedObject, individualType);
            if (ind != null) {
                individual.setPropertyValue(objectProperty, ind);
            } else {
                logger.info("Resource {} for node is already a class. Property value is not set",
                    referencedObject.getPath());
            }
        }
    }

    private void processDataTypeProperty(Individual individual,
                                         DProperty property,
                                         DPropertyDefinition propDef) {

        DatatypeProperty datatypeProperty = ontologyResourceHelper.getDatatypePropertyByReference(propDef
                .getUniqueRef());

        for (String value : property.getValue()) {
            individual.setPropertyValue(datatypeProperty,
                jcrOntModel.createTypedLiteral(value, datatypeProperty.getRange().getURI()));
        }
    }

    private void processInstancePropertyBridgeAnnotation(PropertyAnnotation propertyAnnotation,
                                                         OntProperty property) {
        AnnotationType annotation = propertyAnnotation.getAnnotation();

        // process the annotations
        if (annotation.equals(AnnotationType.FUNCTIONAL)) {
            property.convertToFunctionalProperty();
        } else if (annotation.equals(AnnotationType.INVERSE_FUNCTIONAL)) {
            property.convertToInverseFunctionalProperty();
        } else if (annotation.equals(AnnotationType.SYMMETRIC)) {
            property.convertToSymmetricProperty();
        } else if (annotation.equals(AnnotationType.TRANSITIVE)) {
            property.convertToTransitiveProperty();
        } else if (annotation.equals(AnnotationType.INSTANCE_OF)) {

        } else {
            logger.warn("{} annotation is not supported for this property", annotation.value());
        }
    }

    private static Boolean objectPropertyCheck(DProperty prop) {
        PropType propType = prop.getType();
        // TODO consider all object properties
        if (propType == PropType.REFERENCE || propType == PropType.PATH || propType == PropType.NAME) {
            return true;
        }
        return false;
    }

    private static Boolean datatypePropertyCheck(DProperty prop) {
        PropType propType = prop.getType();
        // TODO consider all data type properties
        if (propType == PropType.STRING || propType == PropType.BOOLEAN || propType == PropType.BINARY
            || propType == PropType.DATE || propType == PropType.DOUBLE || propType == PropType.LONG) {
            return true;
        }
        return false;
    }
}
