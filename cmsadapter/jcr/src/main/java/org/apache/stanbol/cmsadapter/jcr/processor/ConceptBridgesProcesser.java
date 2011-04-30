package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ConceptBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntClass;

public class ConceptBridgesProcesser extends JCRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ConceptBridgesProcesser.class);

    public ConceptBridgesProcesser(MappingEngine mappingEngine) {
        super(mappingEngine);
    }

    public void processUpdates(List<CMSObject> cmsObjects) {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<ConceptBridge> conceptBridges = MappingModelParser.getConceptBridges(bridgeDefinitions);

        for (ConceptBridge bridge : conceptBridges) {
            try {
                boolean subsumptionExists = bridge.getQuery().endsWith("/%");
                for (CMSObject cmsObject : cmsObjects) {
                    if (matches(cmsObject.getPath(), bridge.getQuery())) {
                        processConceptBridge(engine, subsumptionExists, adapter.wrapAsDObject(cmsObject),
                            bridge);
                    }
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Can not process one of the bridges", e);
            }
        }
    }

    public void processBridges() {

        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<ConceptBridge> conceptBridges = MappingModelParser.getConceptBridges(bridgeDefinitions);

        for (ConceptBridge bridge : conceptBridges) {
            try {
                List<CMSObject> cmsObjects = this.accessor.getNodeByPath(bridge.getQuery(), session);
                boolean subsumptionExists = bridge.getQuery().endsWith("/%");
                for (CMSObject classificationObject : cmsObjects) {
                    processConceptBridge(engine, subsumptionExists,
                        adapter.wrapAsDObject(classificationObject), bridge);
                }
            } catch (RepositoryAccessException e) {
                logger.warn("Can not process one of the bridges", e);
            }
        }
    }

    public void processConceptBridge(MappingEngine mappingEngine,
                                     boolean subsumptionExists,
                                     DObject cmsObject,
                                     ConceptBridge conceptBridge) throws RepositoryAccessException {

        OntClass c = ontologyResourceHelper.createOntClassByCMSObject(cmsObject.getInstance());

        if (subsumptionExists) {
            // if the processed node is located in sublevels add it as a subclass to parent
            DObject parentObject = cmsObject.getParent();
            if (parentObject != null) {
                OntClass parentClass = ontologyResourceHelper.createOntClassByCMSObject(parentObject
                        .getInstance());
                parentClass.addSubClass(c);
            }
        }

        // If SubsumptionBridgeExists....
        List<SubsumptionBridge> subsumptionBridgeList = conceptBridge.getSubsumptionBridge();
        SubsumptionBridgesProcesser sbp = new SubsumptionBridgesProcesser(mappingEngine);
        for (SubsumptionBridge bridge : subsumptionBridgeList) {
            // create subsumptionrelationships....
            sbp.processSubsumptionBridge(c, bridge, cmsObject);
        }

        // If PropertyBridges Exist.....
        List<PropertyBridge> propertyBridgeList = conceptBridge.getPropertyBridge();
        PropertyBridgesProcesser pbp = new PropertyBridgesProcesser(mappingEngine);
        for (PropertyBridge bridge : propertyBridgeList) {
            // create subsumptionrelationships....
            pbp.processConceptPropertyBridge(c, bridge, cmsObject);
        }
    }
}
