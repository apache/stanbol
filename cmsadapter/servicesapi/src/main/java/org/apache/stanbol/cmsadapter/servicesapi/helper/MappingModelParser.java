package org.apache.stanbol.cmsadapter.servicesapi.helper;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ConceptBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.InstanceBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for serializing and deserializing bridge definitions
 * 
 */
public class MappingModelParser {
    private static final Logger logger = LoggerFactory.getLogger(MappingModelParser.class);

    public static BridgeDefinitions deserializeObject(String xmlContent) {
        BridgeDefinitions bridgeDefinitions = null;

        try {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
            Unmarshaller um = jc.createUnmarshaller();
            StringReader stringReader = new StringReader(xmlContent);
            bridgeDefinitions = (BridgeDefinitions) um.unmarshal(stringReader);

        } catch (JAXBException e) {
            logger.error("JAXB Exception when parsing serialized BridgeDefinitions");
        }
        return bridgeDefinitions;
    }

    public static String serializeObject(Object object) {
        String bridgeDefinitions = null;

        try {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
            Marshaller m = jc.createMarshaller();
            StringWriter stringWriter = new StringWriter();
            m.marshal(object, stringWriter);
            bridgeDefinitions = stringWriter.toString();

        } catch (JAXBException e) {
            logger.error("JAXB Exception when parsing serialized BridgeDefinitions");
        }
        return bridgeDefinitions;
    }

    /**
     * Gets {@link ConceptBridge}s from inside the given {@link BridgeDefinitions} object.
     * 
     * @param bridgeDefinitions
     * @return
     */
    public static List<ConceptBridge> getConceptBridges(BridgeDefinitions bridgeDefinitions) {
        List<ConceptBridge> cList = new ArrayList<ConceptBridge>();
        List<Object> aList = bridgeDefinitions.getConceptBridgeOrSubsumptionBridgeOrInstanceBridge();
        for (Object bridge : aList) {
            if (bridge instanceof ConceptBridge) {
                cList.add((ConceptBridge) bridge);
            }
        }
        return cList;
    }

    /**
     * Gets {@link SubsumptionBridge}s from inside the given {@link BridgeDefinitions} object.
     * 
     * @param bridgeDefinitions
     * @return
     */
    public static List<SubsumptionBridge> getSubsumptionBridges(BridgeDefinitions bridgeDefinitions) {

        List<SubsumptionBridge> sList = new ArrayList<SubsumptionBridge>();
        List<Object> aList = bridgeDefinitions.getConceptBridgeOrSubsumptionBridgeOrInstanceBridge();
        for (Object bridge : aList) {
            if (bridge instanceof SubsumptionBridge) {
                sList.add((SubsumptionBridge) bridge);
            }
        }
        return sList;
    }

    /**
     * Gets {@link InstanceBridge}s from inside the given {@link BridgeDefinitions} object.
     * 
     * @param bridgeDefinitions
     * @return
     */
    public static List<InstanceBridge> getInstanceBridges(BridgeDefinitions bridgeDefinitions) {

        List<InstanceBridge> sList = new ArrayList<InstanceBridge>();
        List<Object> aList = bridgeDefinitions.getConceptBridgeOrSubsumptionBridgeOrInstanceBridge();
        for (Object bridge : aList) {
            if (bridge instanceof InstanceBridge) {
                sList.add((InstanceBridge) bridge);
            }
        }
        return sList;
    }
}
