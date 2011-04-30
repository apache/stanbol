package org.apache.stanbol.cmsadapter.jcr.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.MappingModelParser;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.InstanceBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;

public class InstanceBridgesProcesser extends JCRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(InstanceBridgesProcesser.class);

    public InstanceBridgesProcesser(MappingEngine mappingEngine) {
        super(mappingEngine);
    }

    public void processUpdates(List<CMSObject> cmsObjects) {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<InstanceBridge> instanceBridges = MappingModelParser.getInstanceBridges(bridgeDefinitions);

        for (InstanceBridge instanceBridge : instanceBridges) {
            try {
                for (CMSObject contentObject : cmsObjects) {
                    if (matches(contentObject.getPath(), instanceBridge.getQuery())) {
                        processInstanceBridge(instanceBridge, adapter.wrapAsDObject(contentObject));
                    }
                }
            } catch (RepositoryAccessException e) {
                // TODO Auto-generated catch block
                logger.warn("Error processing bridge", e);
            }
        }
    }

    public void processBridges() {
        BridgeDefinitions bridgeDefinitions = engine.getBridgeDefinitions();
        DObjectAdapter adapter = engine.getDObjectAdapter();
        List<InstanceBridge> instanceBridges = MappingModelParser.getInstanceBridges(bridgeDefinitions);

        for (InstanceBridge instanceBridge : instanceBridges) {
            try {
                List<CMSObject> cmsObjects = this.accessor.getNodeByPath(instanceBridge.getQuery(), session);
                for (CMSObject contentObject : cmsObjects) {
                    processInstanceBridge(instanceBridge, adapter.wrapAsDObject(contentObject));
                }
            } catch (RepositoryAccessException e) {
                // TODO Auto-generated catch block
                logger.warn("Error processing bridge", e);
            }
        }
    }

    private void processInstanceBridge(InstanceBridge instanceBridge, DObject instanceNode) throws RepositoryAccessException {
        Individual individual;
        List<OntClass> indTypes = new ArrayList<OntClass>();

        // currently object type reference of an object is set to primary node
        // type
        OntClass primaryNodeClass = ontologyResourceHelper.createOntClassByReference(instanceNode
                .getInstance().getObjectTypeRef());

        // create individual
        // TODO consider mixin types
        individual = ontologyResourceHelper.createIndividualByCMSObject(instanceNode.getInstance(),
            primaryNodeClass);
        indTypes.add(primaryNodeClass);

        // process propertyBridges
        List<PropertyBridge> instancePropertyBridges = instanceBridge.getPropertyBridge();
        // TODO property bridges containing instance of annotatins should be
        // handled first
        PropertyBridgesProcesser pbp = new PropertyBridgesProcesser(engine);
        for (PropertyBridge propBridge : instancePropertyBridges) {
            pbp.processInstancePropertyBridge(individual, instanceNode, propBridge, indTypes);
        }
    }

}
