package org.apache.stanbol.cmsadapter.servicesapi.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.NamingStrategy;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClient;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.enhanced.UnsupportedPolymorphismException;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.XSD;

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
     *            Unique reference of object for which the {@link OntClass} is requested
     * @return {@link OntClass} if there is an already created class for cms object whose identifier got as a
     *         reference, otherwise <code>null</code>.
     */
    public OntClass getOntClassByReference(String reference) {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            try {
                return resource.as(OntClass.class);
            } catch (UnsupportedPolymorphismException e) {

            }
        }
        return null;
    }

    /**
     * @param cmsObject
     *            {@link CMSObject} object for which the {@link OntClass} is requested
     * @return {@link OntClass} if there is an already created class for the cms object, otherwise
     *         <code>null</code>.
     */
    public OntClass getOntClassByCMSObject(CMSObject cmsObject) {
        return getOntClassByReference(cmsObject.getUniqueRef());
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
        OntClass klass = getOntClassByReference(reference);
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
        OntClass klass = getOntClassByCMSObject(cmsObject);
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
        OntClass klass = getOntClassByReference(objectTypeDefinition.getUniqueRef());
        if (klass == null) {
            String classURI = namingStrategy.getClassName(ontologyURI, objectTypeDefinition);
            klass = ontModel.createClass(classURI);
            klass.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                objectTypeDefinition.getUniqueRef());
            log.debug("OWL Class {} not found, creating new one...", classURI);
        }

        return klass;
    }

    public OntProperty getPropertyByReference(String reference) {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            try {
                return resource.as(OntProperty.class);
            } catch (UnsupportedPolymorphismException e) {

            }
        }
        return null;
    }

    public ObjectProperty getObjectPropertyByReference(String reference) {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            try {
                return resource.as(ObjectProperty.class);
            } catch (UnsupportedPolymorphismException e) {

            }
        }
        return null;
    }

    public DatatypeProperty getDatatypePropertyByReference(String reference) {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            try {
                return resource.as(DatatypeProperty.class);
            } catch (UnsupportedPolymorphismException e) {

            }
        }
        return null;
    }

    public ObjectProperty createObjectPropertyByReference(String reference,
                                                          List<Resource> domains,
                                                          List<Resource> ranges) {
        log.debug("Creating Object property for reference {}", reference);
        ObjectProperty objectProperty = getObjectPropertyByReference(reference);
        if (objectProperty == null) {
            String propertyURI = namingStrategy.getObjectPropertyName(ontologyURI, reference);
            objectProperty = ontModel.createObjectProperty(propertyURI);
            objectProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);

            for (Resource domain : domains) {
                objectProperty.addDomain(domain);
            }
            for (Resource range : ranges) {
                objectProperty.addRange(range);
            }
            log.debug("ObjectProperty {} not found, creating new one...", propertyURI);
        }
        return objectProperty;
    }

    public ObjectProperty createObjectPropertyByPropertyDefinition(PropertyDefinition propertyDefinition,
                                                                   List<Resource> domains,
                                                                   List<Resource> ranges) {
        log.debug("Creating Object property for property {}", propertyDefinition);
        ObjectProperty objectProperty = getObjectPropertyByReference(propertyDefinition.getUniqueRef());
        if (objectProperty == null) {
            String propertyURI = namingStrategy.getObjectPropertyName(ontologyURI, propertyDefinition);
            objectProperty = ontModel.createObjectProperty(propertyURI);
            objectProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                propertyDefinition.getUniqueRef());

            for (Resource domain : domains) {
                objectProperty.addDomain(domain);
            }
            for (Resource range : ranges) {
                objectProperty.addRange(range);
            }
            log.debug("ObjectProperty {} not found, creating new one...", propertyURI);
        }
        return objectProperty;
    }

    public DatatypeProperty createDatatypePropertyByReference(String reference,
                                                              List<Resource> domains,
                                                              PropType propType) {
        log.debug("Creating Datatype property for reference {}", reference);
        DatatypeProperty datatypeProperty = getDatatypePropertyByReference(reference);
        if (datatypeProperty == null) {
            String propertyURI = namingStrategy.getDataPropertyName(ontologyURI, reference);
            Resource range = getDatatypePropertyRange(propType);
            datatypeProperty = ontModel.createDatatypeProperty(propertyURI);
            datatypeProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP, reference);

            for (Resource domain : domains) {
                datatypeProperty.addDomain(domain);
            }
            datatypeProperty.addRange(range);
            log.debug("Datatype property {} not found, creating new one...", propertyURI);
        }
        return datatypeProperty;
    }

    public DatatypeProperty createDatatypePropertyByPropertyDefinition(PropertyDefinition propertyDefinition,
                                                                       List<Resource> domains) {
        DatatypeProperty datatypeProperty = getDatatypePropertyByReference(propertyDefinition.getUniqueRef());
        if (datatypeProperty == null) {
            String propertyURI = namingStrategy.getDataPropertyName(ontologyURI, propertyDefinition);
            Resource range = getDatatypePropertyRange(propertyDefinition.getPropertyType());
            datatypeProperty = ontModel.createDatatypeProperty(propertyURI);
            datatypeProperty.addProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
                propertyDefinition.getUniqueRef());

            for (Resource domain : domains) {
                datatypeProperty.addDomain(domain);
            }
            datatypeProperty.addRange(range);
        }
        return datatypeProperty;
    }

    public OntClass createUnionClass(List<OntClass> classes) {
        RDFList list = ontModel.createList();
        for (OntClass klass : classes) {
            list.cons(klass);
        }
        return createUnionClass(list);
    }

    public OntClass createUnionClass(RDFList list) {
        String unionClassURI = namingStrategy.getUnionClassURI(ontologyURI, list);
        OntClass unionClass = ontModel.createUnionClass(unionClassURI, list);
        return unionClass;
    }

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
        } else {
            range = XSD.normalizedString;
            log.warn("{} property type is not supported yet. XSD.normalizedString is set as default range",
                propType);
        }
        return range;
    }

    public Individual getIndividualByReference(String reference) {
        Individual ind = getLooseIndividualByReference(reference);
        if (ind.isClass()) {
            log.debug("Resource {} is already a class", ind.getURI());
            return null;
        } else if (ind.isIndividual()) {
            return ind;
        } else {
            return null;
        }
    }

    public Individual getLooseIndividualByReference(String reference) {
        ResIterator it = ontModel.listResourcesWithProperty(CMSAdapterVocabulary.CMSAD_RESOURCE_REF_PROP,
            reference);
        Resource resource;
        if (it.hasNext()) {
            resource = it.next();
            try {
                return resource.as(Individual.class);
            } catch (UnsupportedPolymorphismException e) {
                log.debug("Can not cast resource {} to individual", resource.getURI());
            }
        }
        return null;
    }

    public Individual createIndividualByReference(String reference) {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param cmsObject
     * @param klass
     * @return
     */
    public Individual createIndividualByCMSObject(CMSObject cmsObject, Resource klass) {
        Individual ind = getLooseIndividualByReference(cmsObject.getUniqueRef());
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

    public static void saveConnectionInfo(ConnectionInfo connectionInfo, OntModel ontModel) {
        Resource r = CMSAdapterVocabulary.CONNECTION_INFO_RES;
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_TYPE_PROP, connectionInfo.getConnectionType());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_PASSWORD_PROP, connectionInfo.getPassword());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_USERNAME_PROP, connectionInfo.getUsername());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_WORKSPACE_PROP, connectionInfo.getWorkspaceName());
        ontModel.add(r, CMSAdapterVocabulary.CONNECTION_WORKSPACE_URL_PROP, connectionInfo.getRepositoryURL());
    }

    public static ConnectionInfo getConnectionInfo(OntModel ontModel) {
        ObjectFactory of = new ObjectFactory();
        ConnectionInfo ci = of.createConnectionInfo();
        Resource ciResource = ontModel.getResource(CMSAdapterVocabulary.CONNECTION_INFO_RES.getURI());
        ci.setConnectionType(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_TYPE_PROP).getString());
        ci.setPassword(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_PASSWORD_PROP).getString());
        ci.setUsername(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_USERNAME_PROP).getString());
        ci.setWorkspaceName(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_WORKSPACE_PROP)
                .getString());
        ci.setRepositoryURL(ciResource.getProperty(CMSAdapterVocabulary.CONNECTION_WORKSPACE_URL_PROP)
                .getString());
        return ci;

    }

    public static void saveBridgeDefinitions(BridgeDefinitions bridgeDefinitions, OntModel ontModel) {
        Resource r = CMSAdapterVocabulary.BRIDGE_DEFINITIONS_RES;
        String bridges = MappingModelParser.serializeObject(bridgeDefinitions);
        ontModel.add(r, CMSAdapterVocabulary.BRIDGE_DEFINITIONS_CONTENT_PROP, bridges);
    }

    public static BridgeDefinitions getBridgeDefinitions(OntModel ontModel) {
        Resource r = ontModel.getResource(CMSAdapterVocabulary.BRIDGE_DEFINITIONS_RES.getURI());
        String bridgeStr = r.getProperty(CMSAdapterVocabulary.BRIDGE_DEFINITIONS_CONTENT_PROP).getString();
        return MappingModelParser.deserializeObject(bridgeStr);
    }

    public static OntModel createOntModel() {
        return ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
    }

    public static OntModel getOntModel(RestClient storeClient, String ontologyURI, String ontologyHref) throws RestClientException,
                                                                                                       UnsupportedEncodingException {
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        String ontContent = storeClient.retrieveOntology(ontologyHref, "RDF/XML", false);
        InputStream is = new ByteArrayInputStream(ontContent.getBytes("UTF-8"));
        ontModel.read(is, ontologyURI, "RDF/XML");
        return ontModel;
    }

    public static final String addResourceDelimiter(String URI) {
        if (!URI.endsWith("#")) {
            URI += "#";
        }
        return URI;
    }
}
