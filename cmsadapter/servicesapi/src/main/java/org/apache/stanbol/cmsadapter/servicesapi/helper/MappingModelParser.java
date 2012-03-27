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

    /**
     * Parses an XML document and returns corresponding {@link BridgeDefinitions} instance.
     * @param xmlContent String representation of XML Document.
     * @return {@link BridgeDefinitions} instance or null if unsuccessful.
     */
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

    /**
     * Converts an object to its XML form.
     * @param object Any object that can be created by {@link ObjectFactory} 
     * @return XML Document as a string.
     */
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
