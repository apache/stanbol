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
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.ProcessorProperties;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

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
    public Boolean canProcess(Object cmsObject) {
        return cmsObject instanceof ObjectTypeDefinition;
    }

    @Override
    public void createObjects(List<Object> objects, MappingEngine engine) {
        List<DObjectType> objectTypes = cmsObject2dobjectType(objects, engine);
        createDecoratedObjectTypes(objectTypes, engine);
    }

    private void createDecoratedObjectTypes(List<DObjectType> objectTypes, MappingEngine engine) {
        for (DObjectType objectType : objectTypes) {
            if (canProcess(objectType.getInstance())) {
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
            RepositoryAccess accessor = engine.getRepositoryAccess();
            Object session = engine.getSession();
            OntologyResourceHelper orh = engine.getOntologyResourceHelper();

            if ((propDef.getPropertyType() == PropType.NAME) || (propDef.getPropertyType() == PropType.PATH)
                || (propDef.getPropertyType() == PropType.REFERENCE)) {

                List<String> constraints = propDef.getValueConstraints();
                List<CMSObject> referencedObjects = new ArrayList<CMSObject>();

                if (propDef.getPropertyType() == PropType.NAME) {
                    for (String constraint : constraints) {
                        try {
                            referencedObjects.addAll(accessor.getNodeByName(constraint, session));
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error while getting referenced value {} ", constraint, e);
                        }
                    }

                } else if (propDef.getPropertyType() == PropType.PATH) {
                    for (String constraint : constraints) {
                        try {
                            referencedObjects.addAll(accessor.getNodeByPath(constraint, session));
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error while getting referenced value {} ", constraint, e);
                        }
                    }

                } else if (propDef.getPropertyType() == PropType.REFERENCE) {
                    for (String constraint : constraints) {
                        try {
                            referencedObjects.addAll(accessor.getNodeById(constraint, session));
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error while getting referenced value {} ", constraint, e);
                        }
                    }
                }

                Resource rangeClass = null;
                if (referencedObjects.size() == 0) {
                    rangeClass = OWL.Thing;

                } else if (referencedObjects.size() == 1) {
                    rangeClass = orh.createOntClassByCMSObject(referencedObjects.get(0));

                    if (rangeClass == null) {
                        logger.warn("Failed create class for range value {}", referencedObjects.get(0)
                                .getLocalname());
                    }

                } else {
                    RDFList rdfList = engine.getOntModel().createList();
                    for (CMSObject referencedObject : referencedObjects) {
                        rdfList = rdfList.cons(orh.createOntClassByCMSObject(referencedObject));
                    }
                    rangeClass = orh.createUnionClass(rdfList);
                }
                ObjectProperty op = orh.createObjectPropertyByPropertyDefinition(propDef.getInstance(),
                    Arrays.asList(new Resource[] {subjectClass}), Arrays.asList(new Resource[] {rangeClass}));

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
            if (canProcess(objectType.getInstance())) {
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
                if (canProcess(o)) {
                    dObjectTypes.add(adapter.wrapAsDObjectType((ObjectTypeDefinition) o));
                }
            }
        }
        return dObjectTypes;
    }
}
