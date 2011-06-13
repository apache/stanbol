package org.apache.stanbol.cmsadapter.core.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.enhanced.UnsupportedPolymorphismException;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class PropertyProcesser {

    private static final Logger logger = LoggerFactory.getLogger(PropertyProcesser.class);

    public void processConceptPropertyBridgeCreate(OntClass subjectClass,
                                                   PropertyBridge propertyBridge,
                                                   DObject propertySubject,
                                                   MappingEngine engine) throws RepositoryAccessException {
        AnnotationType annotation = getAnnotation(propertyBridge.getPropertyAnnotation());
        String predicateName = propertyBridge.getPredicateName();

        for (DProperty prop : propertySubject.getProperties()) {
            DPropertyDefinition propDef = prop.getDefinition();
            // propDef returns null if a * named property comes
            // TODO after handling * named properties, remove the null check
            if (propDef == null) {
                logger.warn("Property definition could not be got for property {}", prop.getName());
                continue;
            }

            String propName = propDef.getName();
            String propFullName = propDef.getNamespace() + ":" + propDef.getName();
            if (propName.equals(predicateName) || propName.contains(predicateName)
                || propFullName.equals(predicateName)) {
                if (annotation != null) {
                    List<CMSObject> referencedNodes = resolveReferenceNodes(prop, engine);
                    processPropertyAnnotation(annotation, subjectClass, referencedNodes, engine);
                }
                // property found. break the loop
                break;
            }
        }
    }

    public void processClassificationObjectProperty(DProperty prop,
                                                    AnnotationType annotation,
                                                    OntClass subjectClass,
                                                    MappingEngine engine) throws RepositoryAccessException {

        List<CMSObject> referencedNodes = resolveReferenceNodes(prop, engine);
        processPropertyAnnotation(annotation, subjectClass, referencedNodes, engine);
    }

    public List<CMSObject> resolveReferenceNodes(DProperty prop, MappingEngine engine) throws RepositoryAccessException {
        RepositoryAccess accessor = engine.getRepositoryAccess();
        Object session = engine.getSession();

        // TODO consider other property types
        List<CMSObject> referencedNodes = new ArrayList<CMSObject>();
        PropType type = prop.getDefinition().getPropertyType();
        if (type.equals(PropType.REFERENCE)) {
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

        } else if (type.equals(PropType.NAME)) {
            for (String name : prop.getValue()) {
                try {
                    referencedNodes.addAll(accessor.getNodeByName(name, session));
                } catch (Exception e) {
                    logger.warn("Error at getting name nodes.", e);
                }
            }

        } else if (type.equals(PropType.PATH)) {
            for (String value : prop.getValue()) {
                try {
                    referencedNodes.addAll(accessor.getNodeByPath(value, session));
                } catch (RepositoryAccessException e) {
                    logger.warn("Error at getting node by path ", e);
                }
            }
        }

        return referencedNodes;
    }

    private void processPropertyAnnotation(AnnotationType annotation,
                                           OntClass subjectClass,
                                           List<CMSObject> referencedNodes,
                                           MappingEngine engine) {

        if (annotation != null) {
            OntologyResourceHelper orh = engine.getOntologyResourceHelper();

            List<OntClass> referencedClasses = new ArrayList<OntClass>();
            for (CMSObject referenceObject : referencedNodes) {
                OntClass c = orh.createOntClassByCMSObject(referenceObject);
                if (c == null) {
                    logger.warn("Failed create OntClass for CMS object {}", referenceObject.getLocalname());
                    continue;
                }
                referencedClasses.add(c);
            }

            // process the annotations
            if (annotation.equals(AnnotationType.SUBSUMPTION)) {
                for (OntClass c : referencedClasses) {
                    orh.addSubsumptionAssertion(subjectClass, c);
                }

            } else if (annotation.equals(AnnotationType.EQUIVALENT_CLASS)) {
                for (OntClass c : referencedClasses) {
                    orh.addEquivalentClassAssertion(subjectClass, c);
                }

            } else if (annotation.equals(AnnotationType.DISJOINT_WITH)) {
                for (OntClass c : referencedClasses) {
                    orh.addDisjointWithAssertion(subjectClass, c);
                }
            } else {
                logger.warn("{} annotation is not supported for this property", annotation.value());
                return;
            }
        }
    }

    public void processInstancePropertyBridgeCreate(Individual individual,
                                                    DObject contentObject,
                                                    PropertyBridge propertyBridge,
                                                    MappingEngine engine) throws RepositoryAccessException {
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
            String propFullName = propDef.getNamespace() + ":" + propDef.getName();
            if (propName.equals(predicateName) || propName.contains(predicateName) || propFullName.equals(predicateName)) {
                AnnotationType annotation = getAnnotation(propertyBridge.getPropertyAnnotation());
                processContentObjectProperty(property, propDef, contentObject, individual, annotation, engine);
                // property found break the loop
                break;
            }
        }
    }

    public void processContentObjectProperty(DProperty property,
                                             DPropertyDefinition propDef,
                                             DObject contentObject,
                                             Individual individual,
                                             AnnotationType annotation,
                                             MappingEngine engine) throws RepositoryAccessException {

        if (objectPropertyCheck(property)) {
            processObjectProperty(individual, contentObject, annotation, property, propDef, engine);
        } else if (datatypePropertyCheck(property)) {
            processDataTypeProperty(individual, property, propDef, engine);
        } else {
            logger.warn("{} property type is not supported yet", propDef.getPropertyType());
        }
    }

    private void processObjectProperty(Individual individual,
                                       DObject contentObject,
                                       AnnotationType annotation,
                                       DProperty property,
                                       DPropertyDefinition propDef,
                                       MappingEngine engine) {

        RepositoryAccess accessor = engine.getRepositoryAccess();
        Object session = engine.getSession();
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();

        // get referenced values
        List<CMSObject> referencedObjects = new ArrayList<CMSObject>();
        CMSObject parentObject = null;
        PropType type = propDef.getPropertyType();
        if (type == PropType.REFERENCE) {
            for (String referencedObject : property.getValue()) {
                try {
                    parentObject = accessor.getFirstNodeById(referencedObject, session);
                    if (parentObject != null) {
                        referencedObjects.add(parentObject);
                    }
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedObject, e);
                }
            }

        } else if (type == PropType.NAME) {
            for (String referencedName : property.getValue()) {
                List<CMSObject> names;
                try {
                    names = accessor.getNodeByName(referencedName, session);
                    referencedObjects.addAll(names);
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedName, e);
                }
            }
        } else if (type == PropType.PATH) {
            for (String referencedPath : property.getValue()) {
                try {
                    parentObject = accessor.getFirstNodeByPath(referencedPath, session);
                    if (parentObject != null) {
                        referencedObjects.add(parentObject);
                    }
                } catch (RepositoryAccessException e) {
                    logger.warn("Error while getting referenced value {} ", referencedPath, e);
                }
            }
        }

        ObjectProperty objectProperty = null;

        try {
            objectProperty = orh.getObjectPropertyByReference(propDef.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            logger.warn("Another type of resource has been created for the property definition: {}",
                propDef.getName());
        } catch (ConversionException e) {
            logger.warn("Another type of resource has been created for the property definition: {}",
                propDef.getName());
        }

        if (objectProperty != null) {
            if (annotation != null) {
                processInstancePropertyBridgeAnnotation(individual, contentObject, annotation,
                    objectProperty, referencedObjects, orh);
            } else {
                createReferencedIndividuals(individual, contentObject, objectProperty, referencedObjects, orh);
            }
        } else {
            logger.warn("There is no object property create property ref {}", propDef.getUniqueRef());
        }
    }

    private void createReferencedIndividuals(Individual individual,
                                             DObject contentObject,
                                             ObjectProperty objectProperty,
                                             List<CMSObject> referencedObjects,
                                             OntologyResourceHelper ontologyResourceHelper) {

        if (objectProperty == null) {
            logger.warn("There is no suitable object property for reference");
            return;
        }

        OntResource individualType = ontologyResourceHelper.createOntClassByReference(contentObject
                .getInstance().getObjectTypeRef());

        for (CMSObject referencedObject : referencedObjects) {
            Individual ind = ontologyResourceHelper.createIndividualByCMSObject(referencedObject,
                individualType);
            if (ind != null) {
                individual.addProperty(objectProperty, ind);
            } else {
                logger.warn(
                    "Failed to create individual for referenced value {} while creating referenced individuals for {}",
                    referencedObject.getLocalname(), contentObject.getName());
            }
        }
    }

    private void processDataTypeProperty(Individual individual,
                                         DProperty property,
                                         DPropertyDefinition propDef,
                                         MappingEngine engine) {
        OntologyResourceHelper orh = engine.getOntologyResourceHelper();
        OntModel ontModel = engine.getOntModel();

        DatatypeProperty datatypeProperty = null;

        try {
            datatypeProperty = orh.getDatatypePropertyByReference(propDef.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            logger.warn("Another type of resource has been created for the property definition: {}",
                propDef.getName());
            return;
        } catch (ConversionException e) {
            logger.warn("Another type of resource has been created for the property definition: {}",
                propDef.getName());
            return;
        }

        if (datatypeProperty != null) {
            for (String value : property.getValue()) {
                individual.setPropertyValue(datatypeProperty,
                    ontModel.createTypedLiteral(value, datatypeProperty.getRange().getURI()));
            }
        } else {
            logger.warn("There is no datatype property create property ref {}", propDef.getUniqueRef());
        }

    }

    private void processInstancePropertyBridgeAnnotation(Individual containerIndividual,
                                                         DObject contentObject,
                                                         AnnotationType annotation,
                                                         ObjectProperty property,
                                                         List<CMSObject> referencedObjects,
                                                         OntologyResourceHelper orh) {

        // process the annotations
        if (!annotation.equals(AnnotationType.INSTANCE_OF)) {
            if (annotation.equals(AnnotationType.FUNCTIONAL)) {
                property.convertToFunctionalProperty();
            } else if (annotation.equals(AnnotationType.INVERSE_FUNCTIONAL)) {
                property.convertToInverseFunctionalProperty();
            } else if (annotation.equals(AnnotationType.SYMMETRIC)) {
                property.convertToSymmetricProperty();
            } else if (annotation.equals(AnnotationType.TRANSITIVE)) {
                property.convertToTransitiveProperty();
            } else {
                logger.warn("{} annotation is not supported for this property", annotation.value());
                return;
            }
            createReferencedIndividuals(containerIndividual, contentObject, property, referencedObjects, orh);

        } else if (annotation.equals(AnnotationType.INSTANCE_OF)) {
            OntClass parentClass;
            for (CMSObject parent : referencedObjects) {
                parentClass = orh.createOntClassByCMSObject(parent);
                if (parentClass == null) {
                    logger.warn("Failed to create OntClass for CMS Object {}", parent.getLocalname());
                    continue;
                }
                containerIndividual.addOntClass(parentClass);
            }
        }
    }

    private static Boolean objectPropertyCheck(DProperty prop) throws RepositoryAccessException {
        PropType propType = prop.getDefinition().getPropertyType();
        // TODO consider all object properties
        if (propType == PropType.REFERENCE || propType == PropType.PATH || propType == PropType.NAME) {
            return true;
        }
        return false;
    }

    private static Boolean datatypePropertyCheck(DProperty prop) throws RepositoryAccessException {
        PropType propType = prop.getDefinition().getPropertyType();
        // TODO consider all data type properties
        if (propType == PropType.STRING || propType == PropType.BOOLEAN || propType == PropType.BINARY
            || propType == PropType.DATE || propType == PropType.DOUBLE || propType == PropType.LONG) {
            return true;
        }
        return false;
    }

    private static AnnotationType getAnnotation(org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyAnnotation propertyAnnotation) {
        if (propertyAnnotation == null || propertyAnnotation.getAnnotation() == null) {
            return null;
        } else {
            return AnnotationType.fromValue(propertyAnnotation.getAnnotation().value());
        }
    }
}
