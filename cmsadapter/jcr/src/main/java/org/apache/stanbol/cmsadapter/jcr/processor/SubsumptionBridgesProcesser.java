package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntClass;

public class SubsumptionBridgesProcesser extends JCRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SubsumptionBridgesProcesser.class);

    public SubsumptionBridgesProcesser(MappingEngine mappingEngine) {
        super(mappingEngine);
    }

    public void processUpdates(List<CMSObject> cmsObjects) {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        OntologyResourceHelper ontologyResourceHelper = engine.getOntologyResourceHelper();
        List<SubsumptionBridge> subsumptionBridges = MappingModelParser
                .getSubsumptionBridges(bridgeDefinitions);

        for (SubsumptionBridge bridge : subsumptionBridges) {
            try {
                for (CMSObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), bridge.getSubjectQuery())) {
                        OntClass c = ontologyResourceHelper.createOntClassByCMSObject(cmsObject);
                        processSubsumptionBridge(c, bridge, adapter.wrapAsDObject(cmsObject));
                    }
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Error at retrieving nodes on subsumption bridge path", e);
            }
        }

    }

    public void processBridges() {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        OntologyResourceHelper ontologyResourceHelper = engine.getOntologyResourceHelper();
        List<SubsumptionBridge> subsumptionBridges = MappingModelParser
                .getSubsumptionBridges(bridgeDefinitions);

        for (SubsumptionBridge bridge : subsumptionBridges) {
            try {
                List<CMSObject> cmsObjects = accessor.getNodeByPath(bridge.getSubjectQuery(), session);
                for (CMSObject cmsObject : cmsObjects) {
                    OntClass c = ontologyResourceHelper.createOntClassByCMSObject(cmsObject);
                    processSubsumptionBridge(c, bridge, adapter.wrapAsDObject(cmsObject));
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Error at retrieving nodes on subsumption bridge path", e);
            }
        }
    }

    public void processSubsumptionBridge(OntClass parentClass, SubsumptionBridge s, DObject parentObject) throws RepositoryAccessException {

        String predicateName = s.getPredicateName();
        if (predicateName.equals("child")) {
            // find all child nodes of the parentMode

            for (DObject childObject : parentObject.getChildren()) {
                OntClass childClass = ontologyResourceHelper.createOntClassByCMSObject(childObject.getInstance());
                parentClass.addSubClass(childClass);
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
                    List<String> propValues = property.getValue();
                    for (String refValue : propValues) {
                        try {
                            // TODO check other object property types such WEAKREFERENCE
                            if (property.getType() == PropType.REFERENCE) {

                                CMSObject childObject = accessor.getFirstNodeById(refValue, session);
                                OntClass childClass = ontologyResourceHelper
                                        .createOntClassByCMSObject(childObject);
                                parentClass.addSubClass(childClass);

                            } else if (property.getType() == PropType.NAME) {
                                List<CMSObject> childObjects = accessor.getNodeByName(refValue, session);
                                for (CMSObject child : childObjects) {
                                    OntClass childClass = ontologyResourceHelper.createOntClassByCMSObject(child);
                                    parentClass.addSubClass(childClass);
                                }

                            } else if (property.getType() == PropType.PATH) {
                                CMSObject childObject = accessor.getFirstNodeByPath(refValue, session);
                                OntClass childClass = ontologyResourceHelper
                                        .createOntClassByCMSObject(childObject);
                                parentClass.addSubClass(childClass);

                            } else {
                                logger.warn("ERROR: Predicate path is wrong, the range is not a node");
                            }
                        } catch (RepositoryAccessException e) {
                            logger.warn("Error at processing subsumption bridge. Skipping...", e);
                        }
                    }
                    break;
                } else if (propName.contentEquals("*")) {
                    logger.warn("Properties added to nt:unstructured types (* named properties) are not handled yet");
                }
            }
        }
    }
}