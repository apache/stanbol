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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link RDFBridge} interface. It basically provides annotation of raw RDF data
 * using the {@link RDFBridgeConfiguration} instances available in the environment.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class DefaultRDFBridgeImpl implements RDFBridge {
    private static final Logger log = LoggerFactory.getLogger(CMSAdapterVocabulary.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = RDFBridgeConfiguration.class, policy = ReferencePolicy.DYNAMIC, bind = "bindRDFBridgeConfiguration", unbind = "unbindRDFBridgeConfiguration", strategy = ReferenceStrategy.EVENT)
    List<RDFBridgeConfiguration> defaultBridgeConfigs = new CopyOnWriteArrayList<RDFBridgeConfiguration>();

    @Override
    public MGraph annotateGraph(Graph rawRDF) {
        MGraph annotatedGraph = new SimpleMGraph(rawRDF);
        addAnnotationsToGraph(annotatedGraph);
        return annotatedGraph;
    }

    /**
     * Adds annotations according to available {@link RDFBridgeConfiguration} instances<br>
     * <br>
     * It first select target resources by using the configurations obtained from
     * {@link RDFBridgeConfiguration#getTargetPropertyResources()} and
     * {@link RDFBridgeConfiguration#getTargetResourceValue()}.<br>
     * <br>
     * In the next step, parent/child relations are set according to configuration values obtained from
     * {@link RDFBridgeConfiguration#getChildrenResources()}. In case of multiple children having same name,
     * an integer value added to the end of the name incrementally e.g
     * <b>name</b>,<b>name1</b>,<b>name2</b>,...<br>
     * <br>
     * Then property annotations are added to according configuration values obtained from
     * {@link RDFBridgeConfiguration#getTargetPropertyResources()}. Name of a property is kept for each bridge
     * to to make possible giving different names for the same property in different bridges.<br>
     * <br>
     * 
     * @param graph
     *            {@link MGraph} keeping the raw RDF data
     */
    private void addAnnotationsToGraph(MGraph graph) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        Map<UriRef,Object> children;
        Map<UriRef,Object> properties;

        for (RDFBridgeConfiguration config : defaultBridgeConfigs) {
            children = config.getChildrenResources();
            properties = config.getTargetPropertyResources();
            Iterator<Triple> tripleIterator = graph.filter(null, config.getTargetResourcePredicate(),
                config.getTargetResourceValue());
            UriRef nameProp = config.getNameResource();

            // add cms object annotations
            while (tripleIterator.hasNext()) {
                Triple t = tripleIterator.next();
                NonLiteral subject = t.getSubject();
                String name = RDFBridgeHelper.getResourceStringValue(subject, nameProp, graph);

                // There should be a valid name for CMS Object
                if (!name.contentEquals("")) {
                    graph.add(new TripleImpl(subject, RDFBridgeHelper.RDF_TYPE,
                            CMSAdapterVocabulary.CMS_OBJECT));

                    // if this object has already has name and path annotations, it means that it's already
                    // processed as child of another object. So, don't put new name and path annotations
                    if (!graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, null).hasNext()) {
                        graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                                literalFactory.createTypedLiteral(name)));
                    }

                    // check children and add child and parent annotations
                    checkChildren(children, subject, graph);

                    // check desired properties to be mapped
                    checkProperties(properties, subject, config, graph);
                }
            }
        }
    }

    private static void checkChildren(Map<UriRef,Object> children, NonLiteral objectURI, MGraph graph) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        for (UriRef childPropURI : children.keySet()) {
            Iterator<Triple> childrenIt = graph.filter(objectURI, childPropURI, null);
            Map<String,Integer> childNames = new HashMap<String,Integer>();
            while (childrenIt.hasNext()) {
                Triple child = childrenIt.next();
                NonLiteral childSubject = new UriRef(RDFBridgeHelper.removeEndCharacters(child.getObject()
                        .toString()));

                String childName = getNameOfProperty(childSubject, children.get(childPropURI), graph);
                if (!childName.contentEquals("")) {
                    RDFBridgeHelper.removeExistingTriple(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                        graph);
                    graph.add(new TripleImpl(childSubject, RDFBridgeHelper.RDF_TYPE,
                            CMSAdapterVocabulary.CMS_OBJECT));
                    graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF,
                            objectURI));
                    graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                            literalFactory.createTypedLiteral(getChildName(childName, childNames))));

                } else {
                    log.warn("Failed to obtain a name for child property: {}", childPropURI);
                }
            }
        }
    }

    private static void checkProperties(Map<UriRef,Object> properties,
                                        NonLiteral subject,
                                        RDFBridgeConfiguration bridge,
                                        MGraph graph) {

        LiteralFactory literalFactory = LiteralFactory.getInstance();
        Map<UriRef,UriRef> propertiesNamesInBridge = new HashMap<UriRef,UriRef>();
        for (UriRef propURI : properties.keySet()) {
            String propertyName = getNameOfProperty(subject, properties.get(propURI), graph);
            if (!propertyName.contentEquals("")) {
                if (!propertiesNamesInBridge.containsKey(propURI)) {

                    UriRef tempRef = new UriRef(propertyName + "Prop" + bridge.hashCode());
                    propertiesNamesInBridge.put(propURI, tempRef);

                    graph.add(new TripleImpl(tempRef, CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_NAME,
                            literalFactory.createTypedLiteral(propertyName)));
                    graph.add(new TripleImpl(tempRef, CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_URI, propURI));
                }
                graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_HAS_PROPERTY,
                        propertiesNamesInBridge.get(propURI)));
            } else {
                log.warn("Failed to obtain a name for property: {}", propURI);
            }
        }
    }

    private static String getNameOfProperty(NonLiteral subject, Object nameProp, MGraph graph) {
        if (nameProp instanceof String) {
            return (String) nameProp;
        } else if (nameProp instanceof UriRef) {
            return RDFBridgeHelper.getResourceStringValue(subject, (UriRef) nameProp, graph);
        } else {
            log.warn("Only String and UriRef instance can be passed to specify property name");
            return "";
        }
    }

    private static String getChildName(String candidateName, Map<String,Integer> childNames) {
        Integer childNameCount = childNames.get(candidateName);
        if (childNameCount != null) {
            candidateName += (childNameCount + 1);
            childNames.put(candidateName, (childNameCount + 1));
        } else {
            childNames.put(candidateName, 1);
        }
        return candidateName;
    }

    protected void bindRDFBridgeConfiguration(RDFBridgeConfiguration rdfBridgeConfiguration) {
        defaultBridgeConfigs.add(rdfBridgeConfiguration);
    }

    protected void unbindRDFBridgeConfiguration(RDFBridgeConfiguration rdfBridgeConfiguration) {
        defaultBridgeConfigs.remove(rdfBridgeConfiguration);
    }
}
