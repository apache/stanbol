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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class CMSVocabularyAnnotator {
    private static final Logger log = LoggerFactory.getLogger(CMSAdapterVocabulary.class);

    private static final UriRef RDF_TYPE = new UriRef(NamespaceEnum.rdf + "Type");

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

            // add cms object annotations
            while (tripleIterator.hasNext()) {
                Triple t = tripleIterator.next();
                NonLiteral subject = t.getSubject();
                String name = getResourceStringValue(subject, nameProp, graph);

                // There should be a valid name for CMS Object
                if (!name.contentEquals("")) {
                    graph.add(new TripleImpl(subject, RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT));

                    // if this object has already has name and path annotations, it means that it's already
                    // processed as child of another object. So, don't put new name and path annotations
                    if (!graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, null).hasNext()) {
                        graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                                literalFactory.createTypedLiteral(name)));
                    }

                    // check children and add child and parent annotations
                    for (UriRef childPropURI : children.keySet()) {
                        Iterator<Triple> childrenIt = graph.filter(subject, childPropURI, null);
                        Map<String,Integer> childNames = new HashMap<String,Integer>();
                        while (childrenIt.hasNext()) {
                            Triple child = childrenIt.next();
                            NonLiteral childSubject = new UriRef(replaceEndCharacters(child.getObject()
                                    .toString()));

                            String childName = getNameOfProperty(childSubject, children.get(childPropURI),
                                graph);
                            if (!childName.contentEquals("")) {
                                removeExistingTriple(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                                    graph);
                                graph.add(new TripleImpl(childSubject, RDF_TYPE,
                                        CMSAdapterVocabulary.CMS_OBJECT));
                                graph.add(new TripleImpl(childSubject,
                                        CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, subject));
                                graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                                        literalFactory
                                                .createTypedLiteral(getChildName(childName, childNames))));

                            } else {
                                log.warn("Failed to obtain a name for child property: {}", childPropURI);
                            }
                        }
                    }

                    // check desired properties to be mapped
                    Map<UriRef,UriRef> propertiesNamesInBridge = new HashMap<UriRef,UriRef>();
                    for (UriRef propURI : properties.keySet()) {
                        String propertyName = getNameOfProperty(subject, properties.get(propURI), graph);
                        if (!propertyName.contentEquals("")) {
                            if (!propertiesNamesInBridge.containsKey(propURI)) {

                                UriRef tempRef = new UriRef(propertyName + "Prop" + bridge.hashCode());
                                propertiesNamesInBridge.put(propURI, tempRef);

                                graph.add(new TripleImpl(tempRef,
                                        CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_NAME, literalFactory
                                                .createTypedLiteral(propertyName)));
                                graph.add(new TripleImpl(tempRef,
                                        CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_URI, propURI));
                            }
                            graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_HAS_PROPERTY,
                                    propertiesNamesInBridge.get(propURI)));
                        } else {
                            log.warn("Failed to obtain a name for property: {}", propURI);
                        }
                    }
                }
            }

            /*
             * it is assumed that any two object to be created from different bridges will not be related with
             * each other. Otherwise, it is necessary to assign target cms path for each CMS Object
             */
            annotatePaths(targetRootPath, graph);
        }

        // remove code
        try {
            saveOntology(graph, ontologyURI, true);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void annotatePaths(String targetRootPath, MGraph graph) {
        // first detect root objects
        List<NonLiteral> roots = new ArrayList<NonLiteral>();
        Iterator<Triple> it = graph.filter(null, RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT);
        while (it.hasNext()) {
            Triple t = it.next();
            if (isRoot(t, graph)) {
                roots.add(t.getSubject());
            }
        }

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

    private static boolean isRoot(Triple cmsObjectTriple, MGraph graph) {
        NonLiteral subject = cmsObjectTriple.getSubject();
        if (graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, null).hasNext()) {
            return false;
        } else {
            return true;
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

    private static String replaceEndCharacters(String resource) {
        return resource.replace("<", "").replace(">", "");
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

    private static void removeExistingTriple(NonLiteral subject, UriRef predicate, MGraph mGraph) {
        Iterator<Triple> it = mGraph.filter(subject, predicate, null);
        if (it.hasNext()) {
            mGraph.remove(it.next());
        }
    }

    // /////////////////////////////////////

    private static String ontologyURI = "http://deneme#";

    public static void main(String[] args) throws FileNotFoundException {
        CMSVocabularyAnnotator cmsVocabularyAnnotator = new CMSVocabularyAnnotator();
        cmsVocabularyAnnotator.fillCMSObjects();
    }

    private MGraph fillCMSObjects() throws FileNotFoundException {

        MGraph mGraph = new SimpleMGraph();
        UriRef cmsObject1 = new UriRef(ontologyURI + "Concept1");
        UriRef cmsObject2 = new UriRef(ontologyURI + "Concept2");
        UriRef cmsObject3 = new UriRef(ontologyURI + "Concept3");
        UriRef populatedPlace2 = new UriRef(ontologyURI + "PopulatedPlace2");
        UriRef populatedPlace3 = new UriRef(ontologyURI + "PopulatedPlace3");
        UriRef city1 = new UriRef(ontologyURI + "City1");
        UriRef city2 = new UriRef(ontologyURI + "City2");

        // types
        addProperty(cmsObject1, NamespaceEnum.rdf + "Type", new UriRef(NamespaceEnum.skos + "Concept"), null,
            mGraph);
        addProperty(cmsObject2, NamespaceEnum.rdf + "Type", new UriRef(NamespaceEnum.skos + "Concept"), null,
            mGraph);
        addProperty(cmsObject3, NamespaceEnum.rdf + "Type", new UriRef(NamespaceEnum.skos + "Concept"), null,
            mGraph);
        addProperty(populatedPlace2, NamespaceEnum.rdf + "Type", new UriRef(NamespaceEnum.dbpediaOnt
                                                                            + "PopulatedPlace"), null, mGraph);
        addProperty(populatedPlace3, NamespaceEnum.rdf + "Type", new UriRef(NamespaceEnum.dbpediaOnt
                                                                            + "PopulatedPlace"), null, mGraph);

        // labels
        addProperty(cmsObject1, NamespaceEnum.rdfs + "label", null, "CMSObject1", mGraph);
        addProperty(cmsObject2, NamespaceEnum.rdfs + "label", null, "CMSObject2", mGraph);
        addProperty(cmsObject3, NamespaceEnum.rdfs + "label", null, "CMSObject3", mGraph);

        // children
        addProperty(cmsObject1, NamespaceEnum.skos + "narrower", cmsObject2, null, mGraph);
        addProperty(cmsObject3, NamespaceEnum.skos + "narrower", cmsObject3, null, mGraph);
        addProperty(cmsObject1, NamespaceEnum.dbpediaProp + "city", city1, null, mGraph);
        addProperty(cmsObject2, NamespaceEnum.dbpediaProp + "city", city2, null, mGraph);

        // prop
        addProperty(cmsObject2, NamespaceEnum.dbpediaProp + "place", populatedPlace2, null, mGraph);
        addProperty(cmsObject3, NamespaceEnum.dbpediaProp + "place", populatedPlace3, null, mGraph);
        addProperty(cmsObject1, NamespaceEnum.skos + "definition", null, "CMSObject1Def", mGraph);
        addProperty(cmsObject2, NamespaceEnum.skos + "definition", null, "CMSObject2Def", mGraph);
        addProperty(cmsObject3, NamespaceEnum.skos + "definition", null, "CMSObject3Def", mGraph);

        saveOntology(mGraph, ontologyURI, false);

        return mGraph;
    }

    private void addProperty(UriRef subject, String predicate, UriRef object, Object litObject, MGraph mGraph) {
        UriRef prop = new UriRef(predicate);
        if (object != null) {
            mGraph.add(new TripleImpl(subject, prop, object));
        } else if (litObject != null) {
            mGraph.add(new TripleImpl(subject, prop, LiteralFactory.getInstance().createTypedLiteral(
                litObject)));
        }
    }

    private static void saveOntology(TripleCollection tc, String ontologyURI, boolean output) throws FileNotFoundException {
        JenaGraph jenaGraph = new JenaGraph(tc);
        Model model = ModelFactory.createModelForGraph(jenaGraph);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);

        FileOutputStream fos;
        if (output) {
            fos = new FileOutputStream("/home/srdc/Desktop/cmsAdapterTest/rdfmap/Outmgraph");
        } else {
            fos = new FileOutputStream("/home/srdc/Desktop/cmsAdapterTest/rdfmap/mgraph");
        }
        RDFWriter rdfWriter = ontModel.getWriter("RDF/XML");
        rdfWriter.setProperty("xmlbase", ontologyURI);
        rdfWriter.write(ontModel, fos, ontologyURI);
    }

    // /////////////////////////////////////
}
