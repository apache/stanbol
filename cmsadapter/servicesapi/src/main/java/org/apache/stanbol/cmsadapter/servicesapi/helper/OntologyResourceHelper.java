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
package org.apache.stanbol.cmsadapter.servicesapi.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.NamingStrategy;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
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
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Helper class that contains methods for updating the context ontology model. This class should be
 * initialized by specifying an {@link OntModel} , an <b>ontologyURI</b> and a {@link NamingStrategy}
 * separately or simply specifying a {@link MappingEngine} instance in its constructors.
 * 
 * Non-static methods of this class work on specified ontology model. And static methods take the ontology
 * model as a parameter. Because it is not always possible or necessary to create a MappingEngine or the
 * others parameters to initialize the this class.
 * 
 */
public class OntologyResourceHelper {
    private static final Logger log = LoggerFactory.getLogger(OntologyResourceHelper.class);
    private OntModel ontModel;
    private String ontologyURI;
    private NamingStrategy namingStrategy;

    public OntologyResourceHelper(MappingEngine mappingEngine) {
        this.ontModel = mappingEngine.getOntModel();
        this.ontologyURI = mappingEngine.getOntologyURI();
        this.namingStrategy = mappingEngine.getNamingStrategy();

    }

    public OntologyResourceHelper(OntModel ontModel, String ontologyURI, NamingStrategy namingStrategy) throws RepositoryAccessException {
        this.ontModel = ontModel;
        this.ontologyURI = ontologyURI;
        this.namingStrategy = namingStrategy;

    }

    /**
     * @param reference
     *            Unique reference for which the {@link OntClass} is requested
     * @return {@link OntClass} if there is an already created class for cms object whose identifier got as a
     *         reference, otherwise <code>null</code>.
     */
    public OntClass getOntClassByReference(String reference) throws UnsupportedPolymorphismException,
                                                            ConversionException {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            return resource.as(OntClass.class);

        }
        return null;
    }

    /**
     * Creates an {@link OntClass} for a unique reference or returns the existing one.
     * 
     * @param reference
     *            Unique reference of object for which the {@link OntClass} is requested.
     * @return {@link OntClass} instance.
     */
    public OntClass createOntClassByReference(String reference) {
        log.debug("Creating OWL Class for reference {}", reference);
        OntClass klass = null;

        try {
            klass = getOntClassByReference(reference);
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the reference: {}", reference);
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the reference: {}", reference);
            return null;
        }

        if (klass == null) {
            String classURI = namingStrategy.getClassName(ontologyURI, reference);
            klass = ontModel.createClass(classURI);
            klass.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);
            log.debug("OWL Class {} not found for reference. Creating new one...", reference);
        }

        return klass;
    }

    /**
     * Creates an {@link OntClass} for a {@link CMSObject} object or returns the existing one.
     * 
     * @param cmsObject
     *            {@link CMSObject} object for which the {@link OntClass} is requested.
     * @return {@link OntClass} instance.
     */
    public OntClass createOntClassByCMSObject(CMSObject cmsObject) {
        log.debug("Getting OWL Class for cms object = {}", cmsObject);
        OntClass klass = null;
        try {
            klass = getOntClassByReference(cmsObject.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the CMS object: {}",
                cmsObject.getLocalname());
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the CMS object: {}",
                cmsObject.getLocalname());
            return null;
        }

        if (klass == null) {
            String classURI = namingStrategy.getClassName(ontologyURI, cmsObject);
            klass = ontModel.createClass(classURI);
            klass.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, cmsObject.getUniqueRef());
            log.debug("OWL Class {} not found creating...", classURI);
        }

        return klass;
    }

    /**
     * Creates an {@link OntClass} for a {@link ObjectTypeDefinition} object or returns the existing one.
     * 
     * @param objectTypeDefinition
     *            {@link ObjectTypeDefinition} object for which the {@link OntClass} is requested.
     * @return {@link OntClass} instance.
     */
    public OntClass createOntClassByObjectTypeDefinition(ObjectTypeDefinition objectTypeDefinition) {
        log.debug("Getting OWL Class for node type {}", objectTypeDefinition);
        OntClass klass = null;

        try {
            klass = getOntClassByReference(objectTypeDefinition.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the object type definition: {}",
                objectTypeDefinition.getLocalname());
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the object type definition: {}",
                objectTypeDefinition.getLocalname());
            return null;
        }

        if (klass == null) {
            String classURI = namingStrategy.getClassName(ontologyURI, objectTypeDefinition);
            klass = ontModel.createClass(classURI);
            klass.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                objectTypeDefinition.getUniqueRef());
            log.debug("OWL Class {} not found, creating new one...", classURI);
        }

        return klass;
    }

    /**
     * Gets an {@link OntProperty} for the unique reference specified.
     * 
     * @param reference
     *            Unique reference for which the {@link OntProperty} is requested.
     * @return {@link OntProperty} instance if there is a valid one, otherwise <code>null</code>.
     */
    public OntProperty getPropertyByReference(String reference) throws UnsupportedPolymorphismException,
                                                               ConversionException {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            return resource.as(OntProperty.class);
        }
        return null;
    }

    /**
     * Gets an {@link ObjectProperty} for the unique reference specified.
     * 
     * @param reference
     *            Unique reference for which the {@link ObjectProperty} is requested.
     * @return {@link ObjectProperty} instance if there is a valid one, otherwise <code>null</code>.
     */
    public ObjectProperty getObjectPropertyByReference(String reference) throws UnsupportedPolymorphismException,
                                                                        ConversionException {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            return resource.as(ObjectProperty.class);
        }
        return null;
    }

    /**
     * Gets an {@link DatatypeProperty} for the unique reference specified.
     * 
     * @param reference
     *            Unique reference for which the {@link DatatypeProperty} is requested.
     * @return {@link DatatypeProperty} instance if there is a valid one, otherwise <code>null</code>.
     */
    public DatatypeProperty getDatatypePropertyByReference(String reference) throws UnsupportedPolymorphismException,
                                                                            ConversionException {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            return resource.as(DatatypeProperty.class);
        }
        return null;
    }

    /**
     * Creates an {@link ObjectProperty} for a {@link PropertyDefinition} object or returns the existing one.
     * 
     * @param propertyDefinition
     *            source property definition
     * @param domains
     *            {@link Resource}s that will be set as domains of the resultant property
     * @param ranges
     *            {@link Resource}s that will be set as ranges of the resultant property
     * @return {@link ObjectProperty} instance.
     */
    public ObjectProperty createObjectPropertyByPropertyDefinition(PropertyDefinition propertyDefinition,
                                                                   List<Resource> domains,
                                                                   List<Resource> ranges) {
        log.debug("Creating Object property for property {}", propertyDefinition);
        ObjectProperty objectProperty = null;
        try {
            objectProperty = getObjectPropertyByReference(propertyDefinition.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the property definition: {}",
                propertyDefinition.getLocalname());
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the property definition: {}",
                propertyDefinition.getLocalname());
            return null;
        }

        if (objectProperty == null) {
            String propertyURI = namingStrategy.getObjectPropertyName(ontologyURI, propertyDefinition);
            objectProperty = ontModel.createObjectProperty(propertyURI);
            objectProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                propertyDefinition.getUniqueRef());
            if (propertyDefinition.getSourceObjectTypeRef() != null) {
                objectProperty.addProperty(CMSAdapterVocabulary.CMSAD_PROPERTY_SOURCE_OBJECT_PROP,
                    propertyDefinition.getSourceObjectTypeRef());
            } else {
                log.warn("Source object type reference not found on property definition {}",
                    propertyDefinition.getLocalname());
            }

            for (Resource domain : domains) {
                objectProperty.addDomain(domain);
            }
            for (Resource range : ranges) {
                objectProperty.addRange(range);
            }
            log.debug("ObjectProperty {} not found, creating new one...", propertyURI);
        } else {
            // Add domains to union class
            OntResource domain = objectProperty.getDomain();
            if (domain != null) {
                if (domain.isClass() && domain.asClass().isUnionClass()) {
                    UnionClass unclass = domain.asClass().asUnionClass();
                    for (Resource newDomain : domains) {
                        unclass.addOperand(newDomain);
                    }
                } else {
                    List<Resource> resources = new ArrayList<Resource>(domains);
                    resources.add(domain);
                    objectProperty.setDomain(createUnionClass(resources));
                }
            }

            // Add ranges to union class

            OntResource range = objectProperty.getRange();
            if (range != null) {
                if (range.isClass() && range.asClass().isUnionClass()) {
                    UnionClass unclass = range.asClass().asUnionClass();
                    for (Resource newRange : ranges) {
                        unclass.addOperand(newRange);
                    }
                } else {
                    List<Resource> resources = new ArrayList<Resource>(ranges);
                    resources.add(range);
                    objectProperty.setDomain(createUnionClass(resources));
                }
            }
        }
        return objectProperty;
    }

    /**
     * Creates an {@link DatatypeProperty} for a {@link PropertyDefinition} object or returns the existing
     * one.
     * 
     * @param propertyDefinition
     *            source property definition
     * @param domains
     *            {@link Resource}s that will be set as domains of the resultant property
     * @return {@link DatatypeProperty} instance.
     */
    public DatatypeProperty createDatatypePropertyByPropertyDefinition(PropertyDefinition propertyDefinition,
                                                                       List<Resource> domains) {
        DatatypeProperty datatypeProperty = null;

        try {
            datatypeProperty = getDatatypePropertyByReference(propertyDefinition.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the property definition: {}",
                propertyDefinition.getLocalname());
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the property definition: {}",
                propertyDefinition.getLocalname());
            return null;
        }

        if (datatypeProperty == null) {
            String propertyURI = namingStrategy.getDataPropertyName(ontologyURI, propertyDefinition);
            Resource range = getDatatypePropertyRange(propertyDefinition.getPropertyType());
            datatypeProperty = ontModel.createDatatypeProperty(propertyURI);
            datatypeProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                propertyDefinition.getUniqueRef());
            if (propertyDefinition.getSourceObjectTypeRef() != null) {
                datatypeProperty.addProperty(CMSAdapterVocabulary.CMSAD_PROPERTY_SOURCE_OBJECT_PROP,
                    propertyDefinition.getSourceObjectTypeRef());
            } else {
                log.info("Source object type reference not found on property definition {}",
                    propertyDefinition.getLocalname());
            }

            for (Resource domain : domains) {
                datatypeProperty.addDomain(domain);
            }
            datatypeProperty.addRange(range);
        } else {
            // Add domains to union class
            OntResource domain = datatypeProperty.getDomain();
            if (domain != null) {
                if (domain.isClass() && domain.asClass().isUnionClass()) {
                    UnionClass unclass = domain.asClass().asUnionClass();
                    for (Resource newDomain : domains) {
                        unclass.addOperand(newDomain);
                    }
                } else {
                    List<Resource> resources = new ArrayList<Resource>(domains);
                    resources.add(domain);
                    datatypeProperty.setDomain(createUnionClass(resources));
                }
            }
        }
        return datatypeProperty;
    }

    /**
     * Creates a union {@link OntClass} from specified classes. First makes an {@link RDFList} and gives it to
     * {@link OntologyResourceHelper#createUnionClass(RDFList)} as a parameter.
     * 
     * @param classes
     *            {@link OntClass}es from which union class will be created
     * @return {@link OntClass} instance.
     */
    public OntClass createUnionClass(List<Resource> classes) {
        RDFList list = ontModel.createList();
        for (Resource klass : classes) {
            list.cons(klass);
        }
        return createUnionClass(list);
    }

    /**
     * Creates a union {@link OntClass} from specified {@link RDFList}.
     * 
     * @param list
     *            for which the union class is requested
     * @return {@link OntClass} instance.
     */
    public OntClass createUnionClass(RDFList list) {
        String unionClassURI = namingStrategy.getUnionClassURI(ontologyURI, list);
        OntClass unionClass = ontModel.createUnionClass(unionClassURI, list);
        return unionClass;
    }

    /**
     * Gets a {@link PropType} and returns related range for datatype properties.
     * 
     * @param propType
     * @return
     */
    private Resource getDatatypePropertyRange(PropType propType) {
        Resource range;
        if (propType == PropType.STRING) {
            range = XSD.normalizedString;
        } else if (propType == PropType.BOOLEAN) {
            range = XSD.xboolean;
        } else if (propType == PropType.BINARY) {
            range = XSD.base64Binary;
        } else if (propType == PropType.DATE) {
            range = XSD.dateTime;
        } else if (propType == PropType.DOUBLE) {
            range = XSD.unsignedLong;
        } else if (propType == PropType.LONG) {
            range = XSD.unsignedLong;
        } else if (propType == PropType.INTEGER) {
            range = XSD.integer;
        } else {
            range = XSD.normalizedString;
            log.warn("{} property type is not supported yet. XSD.normalizedString is set as default range",
                propType);
        }
        return range;
    }

    /**
     * Gets an {@link Individual} for the unique reference specified. It first tries to fetch individual with
     * {@link OntologyResourceHelper#getLooseIndividualByReference(String)}. If result is not
     * <code>null</code> it checks the resultant result is really {@link Individual}. Because it is possible
     * for an {@link OntClass} resource to act as an {@link Individual} after {@link Resource#as(Individual)}.
     * 
     * @param reference
     *            Unique reference for which the {@link Individual} is requested
     * @return {@link Individual} if there is a valid already created individual for <i>reference</i>,
     *         otherwise <code>null</code>.
     */
    public Individual getIndividualByReference(String reference) {
        Individual ind = null;
        try {
            ind = getLooseIndividualByReference(reference);
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the reference: {}", reference);
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the reference: {}", reference);
            return null;
        }
        if (ind != null) {
            if (ind.isClass()) {
                log.debug("Resource {} is already a class", ind.getURI());
                return null;
            } else if (ind.isIndividual()) {
                return ind;
            }
        }
        return null;
    }

    /**
     * Gets an {@link Individual} for the unique reference specified.
     * 
     * @param reference
     *            Unique reference for which {@link Individual} is requested.
     * @return {@link Individual} instance if there is a valid one, otherwise <code>null</code>
     */
    public Individual getLooseIndividualByReference(String reference) throws UnsupportedPolymorphismException,
                                                                     ConversionException {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            return resource.as(Individual.class);
        }
        return null;
    }

    /**
     * Creates an {@link Individual} for the specified {@link CMSObject}. It first tries to fetch individual
     * with {@link OntologyResourceHelper#getLooseIndividualByReference(String)} by giving the reference of
     * CMSObject. If result is not <code>null</code> it checks the resultant result is really
     * {@link Individual}. Because it is possible for an {@link OntClass} resource to act as an
     * {@link Individual} after {@link Resource#as(Individual)}.
     * 
     * @param cmsObject
     *            {@link CMSObject} instance for which the {@link Individual} is requested
     * @param klass
     *            This {@link Resource} represents the ontology class which will be specified as type of the
     *            resultant individual
     * @return {@link Individual} instance if there is a real already created one or there is no resources
     *         created for the specified CMSObject. If there is an ontology class created for the CMSObject,
     *         it returns <code>null</code>.
     */
    public Individual createIndividualByCMSObject(CMSObject cmsObject, Resource klass) {
        Individual ind = null;
        try {
            ind = getLooseIndividualByReference(cmsObject.getUniqueRef());
        } catch (UnsupportedPolymorphismException e) {
            log.warn("Another type of resource has been created for the CMS Object: {}",
                cmsObject.getLocalname());
            return null;
        } catch (ConversionException e) {
            log.warn("Another type of resource has been created for the CMS Object: {}",
                cmsObject.getLocalname());
            return null;
        }

        if (ind == null) {
            String indURI = namingStrategy.getIndividualName(ontologyURI, cmsObject);
            ind = ontModel.createIndividual(indURI, klass);
            ind.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, cmsObject.getUniqueRef());
            return ind;
        } else if (ind.isClass()) {
            log.debug("Resource {} is already a class", ind.getURI());
            return null;
        } else {
            return ind;
        }

    }

    /**
     * First find the URI of actual resource represented with <i>reference</i> parameter. Then deletes all
     * statements where it is a subject or object
     * 
     * @param reference
     */
    public void deleteStatementsByReference(String reference) {
        List<Statement> refStatement = ontModel.listStatements(null,
            CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference).toList();

        if (refStatement.size() == 0) {
            log.warn("There is no resource having CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP {}", reference);
            return;
        }

        Resource subject = refStatement.get(0).getSubject();
        deleteStatementsByResource(subject);
    }

    public void deleteObjectTypeProperties(String objectTypeRef) {
        List<Statement> props = ontModel.listStatements(null,
            CMSAdapterVocabulary.CMSAD_PROPERTY_SOURCE_OBJECT_PROP, objectTypeRef).toList();
        for (Statement s : props) {
            deleteStatementsByResource(s.getSubject());
        }
    }

    public void deleteStatementsByResource(Resource res) {
        ontModel.remove(ontModel.listStatements(res, null, (RDFNode) null));
        ontModel.remove(ontModel.listStatements(null, null, res));
    }

    /**
     * Adds subsumption assertions to given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void addSubsumptionAssertion(OntClass parentClass, OntClass childClass) {
        parentClass.addSubClass(childClass);
        childClass.addSuperClass(parentClass);
    }

    /**
     * Removes subsumption assertions from given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void removeSubsumptionAssertion(OntClass parentClass, OntClass childClass) {
        parentClass.removeSubClass(childClass);
        childClass.removeSuperClass(parentClass);
    }

    /**
     * Adds equivalent class assertions to given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void addEquivalentClassAssertion(OntClass class1, OntClass class2) {
        class1.addEquivalentClass(class2);
        class2.addEquivalentClass(class1);
    }

    /**
     * Removes equivalent class assertions from given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void removeEquivalentClassAssertion(OntClass class1, OntClass class2) {
        class1.removeEquivalentClass(class2);
        class2.removeEquivalentClass(class1);
    }

    /**
     * Adds disjoint with assertions to given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void addDisjointWithAssertion(OntClass class1, OntClass class2) {
        class1.addDisjointWith(class2);
        class2.addDisjointWith(class1);
    }

    /**
     * Removes disjoint with assertions from given two {@link OntClass}es mutually.
     * 
     * @param parentClass
     * @param childClass
     */
    public void removeDisjointWithAssertion(OntClass class1, OntClass class2) {
        class1.removeDisjointWith(class2);
        class2.removeDisjointWith(class1);
    }

    /**
     * Saves the specified {@link ConnectionInfo} instance to the specified {@link OntModel}.
     * 
     * @param connectionInfo
     * @param ontModel
     */
    public static void saveConnectionInfo(ConnectionInfo connectionInfo, OntModel ontModel) {
        Resource r = CMSAdapterVocabulary.CONNECTION_INFO_RES;
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_TYPE_PROP, connectionInfo.getConnectionType());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_PASSWORD_PROP, connectionInfo.getPassword());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_USERNAME_PROP, connectionInfo.getUsername());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_WORKSPACE_PROP, connectionInfo.getWorkspaceName());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_WORKSPACE_URL_PROP, connectionInfo.getRepositoryURL());
    }

    /**
     * Gets {@link BridgeDefinitions} instance from specified {@link OntModel}.
     * 
     * @param ontModel
     * @return
     */
    public static ConnectionInfo getConnectionInfo(OntModel ontModel) {
        ObjectFactory of = new ObjectFactory();
        ConnectionInfo ci = null;
        Resource ciResource = ontModel.getResource(CMSAdapterVocabulary.CONNECTION_INFO_RES.getURI());

        try {
            ci = of.createConnectionInfo();
            ci.setConnectionType(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_TYPE_PROP)
                    .getString());
            ci.setPassword(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_PASSWORD_PROP).getString());
            ci.setUsername(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_USERNAME_PROP).getString());
            ci.setWorkspaceName(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_WORKSPACE_PROP)
                    .getString());
            ci.setRepositoryURL(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_WORKSPACE_URL_PROP)
                    .getString());
        } catch (Exception e) {
            log.warn("Failed to get connection info from ont model");
            return null;
        }
        return ci;

    }

    /**
     * Saves the specified {@link BridgeDefinitions} instance to the specified {@link OntModel}.
     * 
     * @param bridgeDefinitions
     * @param ontModel
     */
    public static void saveBridgeDefinitions(BridgeDefinitions bridgeDefinitions, OntModel ontModel) {
        Resource r = CMSAdapterVocabulary.BRIDGE_DEFINITIONS_RES;
        String bridges = MappingModelParser.serializeObject(bridgeDefinitions);
        ontModel.add(r, CMSAdapterVocabulary.BRIDGE_DEFINITIONS_CONTENT_PROP, bridges);
    }

    /**
     * Gets {@link BridgeDefinitions} instance from specified {@link OntModel}.
     * 
     * @param ontModel
     *            ontology model from which the bridge definitions will be extracted
     * @return
     */
    public static BridgeDefinitions getBridgeDefinitions(OntModel ontModel) {
        Resource r = ontModel.getResource(CMSAdapterVocabulary.BRIDGE_DEFINITIONS_RES.getURI());
        try {
            String bridgeStr = r.getProperty(CMSAdapterVocabulary.BRIDGE_DEFINITIONS_CONTENT_PROP)
                    .getString();
            return MappingModelParser.deserializeObject(bridgeStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Adds '#' character to at the end of the specified URI if there is not any.
     */
    public static final String addResourceDelimiter(String URI) {
        if (!URI.endsWith("#")) {
            URI += "#";
        }
        return URI;
    }
}
