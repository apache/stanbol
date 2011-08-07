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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes an RDF in {@link MGraph} and annotates it according to specified {@link RDFBridge}s.
 * 
 * @author suat
 * 
 */
public class CMSVocabularyAnnotator {
    private static final Logger log = LoggerFactory.getLogger(CMSAdapterVocabulary.class);

    /**
     * It is the single method of this class that add CMS vocabulary annotations to the {@link MGraph}
     * specified.<br>
     * <br>
     * It first select target resources by using the configurations obtained from
     * {@link RDFBridge#getTargetPropertyResources()} and {@link RDFBridge#getTargetResourceValue()}.<br>
     * <br>
     * In the next step, parent/child relations are set according to configuration values obtained from
     * {@link RDFBridge#getChildrenResources()}. In case of multiple children having same name, an integer
     * value added to the end of the name incrementally e.g <b>name</b>,<b>name1</b>,<b>name2</b>,...<br>
     * <br>
     * Then property annotations are added to according configuration values obtained from
     * {@link RDFBridge#getTargetPropertyResources()}. Name of a property is kept for each bridge to to make
     * possible giving different names for the same property in different bridges.<br>
     * <br>
     * In the last step parent annotations are added according hierarchy that was formed previously.
     * 
     * @param rdfBridges
     *            {@link RDFBridge} instances keeping configurations to annotate raw RDF data
     * @param graph
     *            {@link MGraph} keeping the raw RDF data
     */
    public void addAnnotationsToGraph(List<RDFBridge> rdfBridges, MGraph graph) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        Map<UriRef,Object> children;
        Map<UriRef,Object> properties;

        for (RDFBridge bridge : rdfBridges) {
            children = bridge.getChildrenResources();
            properties = bridge.getTargetPropertyResources();
            Iterator<Triple> tripleIterator = graph.filter(null, bridge.getTargetResourcePredicate(),
                bridge.getTargetResourceValue());
            UriRef nameProp = bridge.getNameResource();
            String targetRootPath = bridge.getTargetCMSPath();
            List<NonLiteral> processedURIs = new ArrayList<NonLiteral>();

            // add cms object annotations
            while (tripleIterator.hasNext()) {
                Triple t = tripleIterator.next();
                NonLiteral subject = t.getSubject();
                String name = getResourceStringValue(subject, nameProp, graph);

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
                    checkProperties(properties, subject, bridge, graph);
                    
                    processedURIs.add(subject);
                }
            }

            /*
             * it is assumed that any two object to be created from different bridges will not be related with
             * each other. Otherwise, it is necessary to assign target cms path for each CMS Object
             */
            annotatePaths(processedURIs, targetRootPath, graph);
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
                                        RDFBridge bridge,
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

    private static void annotatePaths(List<NonLiteral> candidates, String targetRootPath, MGraph graph) {
        // first detect root objects
        List<NonLiteral> roots = RDFBridgeHelper.getRootObjectsOfGraph(graph, candidates);

        // assign paths to children recursively
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        for (NonLiteral root : roots) {
            assignChildrenPaths(targetRootPath, root, graph, literalFactory, true);
        }
    }

    private static void assignChildrenPaths(String cmsRootPath,
                                            NonLiteral root,
                                            MGraph graph,
                                            LiteralFactory literalFactory,
                                            boolean firstLevel) {
        String rootName = getResourceStringValue(root, CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        String rootPath = cmsRootPath;
        if (firstLevel) {
            rootPath = formRootPath(cmsRootPath, rootName);
            graph.add(new TripleImpl(root, CMSAdapterVocabulary.CMS_OBJECT_PATH, literalFactory
                    .createTypedLiteral(rootPath)));
        }

        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, root);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = getResourceStringValue(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                graph);
            String childPath = formRootPath(rootPath, childName);
            graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_PATH, literalFactory
                    .createTypedLiteral(childPath)));
            assignChildrenPaths(childPath, childSubject, graph, literalFactory, false);
        }
    }

    private static String formRootPath(String targetRootPath, String objectName) {
        if (!targetRootPath.endsWith("/")) {
            targetRootPath += "/";
        }
        return targetRootPath + objectName;
    }

    private static String getNameOfProperty(NonLiteral subject, Object nameProp, MGraph graph) {
        if (nameProp instanceof String) {
            return (String) nameProp;
        } else if (nameProp instanceof UriRef) {
            return getResourceStringValue(subject, (UriRef) nameProp, graph);
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

    private static String getResourceStringValue(NonLiteral subject, UriRef nameProp, MGraph graph) {
        Iterator<Triple> it = graph.filter(subject, nameProp, null);
        if (it.hasNext()) {
            Triple t = it.next();
            Resource r = t.getObject();
            if (r instanceof Literal) {
                return ((Literal) r).getLexicalForm();
            } else {
                log.warn("Resource value is not a Literal for triple: {}", t);
                return "";
            }
        } else {
            log.warn("Failed to get name from subject: {} and name property: {}", subject, nameProp);
            return "";
        }
    }
}
