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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link RDFBridge} interface. It provides annotation of raw RDF data and
 * generating RDF from the content repository based on the configurations described below:
 * 
 * <ul>
 * <li><b>Resource selector:</b></li> This property is used to filter resources from the RDF data. It should
 * have the syntax:<br>
 * <b> rdf:Type > skos:Concept </b>. This example states that triples having value <b>skos:Concept</b> of
 * <b>rdf:type</b> predicate will be filtered. And subject of selected triples indicates the resource to be
 * created/updated as node/object in the repository. It is also acceptable to pass full URIs such as <br>
 * <b> http://www.w3.org/1999/02/22-rdf-syntax-ns#type > http://www.w3.org/2004/02/skos/core#Concept</b><br>
 * <li><b>Name:</b></li> This configuration indicates the predicate which points to the name of node/object to
 * be created in the repository. It should indicate a single URI such as <b>rdfs:label</b> or
 * <b>http://www.w3.org/2000/01/rdf-schema#label</b>. Actually name value is obtained through the triple
 * (s,p,o) where <b>s</b> is one of the subjects filtered by the "Resource Selector" configuration, <b>p</b>
 * is this parameter. This configuration is optional. If an empty configuration is passed name of the CMS
 * objects will be set as the local name of the URI represented by <b>s</b><br>
 * <li><b>Children:</b></li> This property specifies the children of nodes/objects to be created in the
 * repository. Value of this configuration should be like <b>skos:narrower > narrowerObject</b> or
 * <b>skos:narrower > rdfs:label</b>. First option has same logic with the previous parameter. It determines
 * the name of the child CMS object to be created/updated. In the second case, value rdfs:label predicate of
 * resource representing child object will be set as the name of child object/node in the repository. This
 * option would be useful to create hierarchies.
 * <p>
 * It is also possible to set only predicate indicating the subsumption relations such as only
 * <b>skos:narrower</b>. In this case name of the child resource will be obtained from the local name of URI
 * representing this CMS object. This configuration is optional.
 * <li><b>Default child predicate:</b></li> First of all this configuration is used only when generating an
 * RDF from the repository. If there are more than one child selector in previous configuration, it is not
 * possible to detect the predicate that will be used as the child assertion. In that case, this configuration
 * is used to set child assertion between parent and child objects. This configuration is optional. But if
 * there is a case in which this configuration should be used and if it is not set, this causes missing
 * assertions in the generated RDF.
 * <li><b>Content repository path:</b></li> This property specifies the content repository path in which the
 * new CMS objects will be created or existing ones will be updated.
 * </ul>
 * 
 * @author suat
 * 
 */
@Component(configurationFactory = true, metatype = true, immediate = true)
@Service
@Properties(value = {
                     @Property(name = DefaultRDFBridgeImpl.PROP_RESOURCE_SELECTOR, value = "rdf:type > skos:Concept"),
                     @Property(name = DefaultRDFBridgeImpl.PROP_NAME, value = "rdfs:label"),
                     @Property(name = DefaultRDFBridgeImpl.PROP_CHILDREN, cardinality = 1000, value = {"skos:narrower"}),
                     @Property(name = DefaultRDFBridgeImpl.PROP_DEFAULT_CHILD_PREDICATE, value = "skos:narrower"),
                     @Property(name = DefaultRDFBridgeImpl.PROP_CMS_PATH, value = "/rdfmaptest")})
public class DefaultRDFBridgeImpl implements RDFBridge {
    public static final String PROP_RESOURCE_SELECTOR = "org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl.resourceSelector";
    public static final String PROP_NAME = "org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl.resourceNamePredicate";
    public static final String PROP_CHILDREN = "org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl.childrenPredicates";
    public static final String PROP_DEFAULT_CHILD_PREDICATE = "org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl.defaultChildPredicate";
    public static final String PROP_CMS_PATH = "org.apache.stanbol.cmsadapter.core.mapping.DefaultRDFBridgeImpl.contentRepositoryPath";

    private static final Logger log = LoggerFactory.getLogger(CMSAdapterVocabulary.class);

    private UriRef targetResourcePredicate;
    private UriRef targetResourceValue;
    private UriRef nameResource;
    private Map<UriRef,Object> targetChildrenMappings = new HashMap<UriRef,Object>();
    private UriRef defaultChildPredicate;
    private String cmsPath;

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        Dictionary<String,Object> properties = (Dictionary<String,Object>) context.getProperties();
        parseTargetResourceConfig(properties);
        parseURIConfig(PROP_NAME, properties);
        parseChildrenMappings(properties);
        parseURIConfig(PROP_DEFAULT_CHILD_PREDICATE, properties);
        this.cmsPath = (String) checkProperty(properties, PROP_CMS_PATH, true);
    }

    @Override
    public MGraph annotateGraph(Graph rawRDF) {
        MGraph graph = new SimpleMGraph(rawRDF);
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        Iterator<Triple> tripleIterator = graph.filter(null, targetResourcePredicate, targetResourceValue);
        List<NonLiteral> processedURIs = new ArrayList<NonLiteral>();

        // add cms object annotations
        while (tripleIterator.hasNext()) {
            Triple t = tripleIterator.next();
            NonLiteral subject = t.getSubject();
            String name = getObjectName(subject, nameResource, graph, false);

            // There should be a valid name for CMS Object
            if (!name.contentEquals("")) {
                graph.add(new TripleImpl(subject, RDFBridgeHelper.RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT));
                processedURIs.add(subject);

                /*
                 * if this object has already has name and path annotations, it means that it's already
                 * processed as child of another object. So, don't put new name annotations
                 */
                if (!graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, null).hasNext()) {
                    graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, literalFactory
                            .createTypedLiteral(name)));
                }

                // check children and add child and parent annotations
                checkChildren(subject, processedURIs, graph);
            }
        }
        RDFBridgeHelper.addPathAnnotations(cmsPath, processedURIs, graph);
        return graph;
    }

    private void checkChildren(NonLiteral objectURI, List<NonLiteral> processedURIs, MGraph graph) {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        for (UriRef childPropURI : targetChildrenMappings.keySet()) {
            Iterator<Triple> childrenIt = graph.filter(objectURI, childPropURI, null);
            Map<String,Integer> childNames = new HashMap<String,Integer>();
            while (childrenIt.hasNext()) {
                Triple child = childrenIt.next();
                NonLiteral childSubject = new UriRef(RDFBridgeHelper.removeEndCharacters(child.getObject()
                        .toString()));

                String childName = getChildName(childSubject, targetChildrenMappings.get(childPropURI), graph);
                if (!childName.contentEquals("")) {
                    RDFBridgeHelper.removeExistingTriple(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                        graph);
                    graph.add(new TripleImpl(childSubject, RDFBridgeHelper.RDF_TYPE,
                            CMSAdapterVocabulary.CMS_OBJECT));
                    graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF,
                            objectURI));
                    graph.add(new TripleImpl(childSubject, CMSAdapterVocabulary.CMS_OBJECT_NAME,
                            literalFactory.createTypedLiteral(checkDuplicateChildName(childName, childNames))));

                } else {
                    log.warn("Failed to obtain a name for child property: {}", childPropURI);
                }
            }
        }
    }

    private String getObjectName(NonLiteral subject,
                                 UriRef namePredicate,
                                 MGraph graph,
                                 boolean tryTargetResourceNamePredicate) {
        String name = "";
        // try to get name through default CMS Vocabulary predicate
        name = RDFBridgeHelper.getResourceStringValue(subject, CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        if (!name.contentEquals("")) {
            return name;
        }

        // if there is a configuration specifying the name try to obtain through it
        if (namePredicate != null) {
            name = RDFBridgeHelper.getResourceStringValue(subject, namePredicate, graph);
            if (!name.contentEquals("")) {
                return name;
            }
        }

        // if this method is called from a child node try to obtain name by target resource name predicate
        if (nameResource != null) {
            name = RDFBridgeHelper.getResourceStringValue(subject, nameResource, graph);
            if (!name.contentEquals("")) {
                return name;
            }
        }

        // failed to obtain name by the specified property assign local name from the URI
        name = RDFBridgeHelper.extractLocalNameFromURI(subject);
        return name;
    }

    private String getChildName(NonLiteral subject, Object nameProp, MGraph graph) {
        if (nameProp instanceof String) {
            if (((String) nameProp).contentEquals("")) {
                return getObjectName(subject, null, graph, true);
            } else {
                return (String) nameProp;
            }
        } else {
            return getObjectName(subject, (UriRef) nameProp, graph, true);
        }
    }

    private static String checkDuplicateChildName(String candidateName, Map<String,Integer> childNames) {
        Integer childNameCount = childNames.get(candidateName);
        if (childNameCount != null) {
            candidateName += (childNameCount + 1);
            childNames.put(candidateName, (childNameCount + 1));
        } else {
            childNames.put(candidateName, 1);
        }
        return candidateName;
    }

    @Override
    public void annotateCMSGraph(MGraph cmsGraph) {
        List<NonLiteral> roots = RDFBridgeHelper.getRootObjectsOfGraph(cmsPath, cmsGraph);
        for (NonLiteral rootObjectURI : roots) {
            applyReverseBridgeSettings(rootObjectURI, cmsGraph);
            if (defaultChildPredicate != null) {
                addChildrenAnnotations(rootObjectURI, cmsGraph);
            }
        }
    }

    private void addChildrenAnnotations(NonLiteral parentURI, MGraph graph) {
        Iterator<Triple> children = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, parentURI);
        while (children.hasNext()) {
            NonLiteral childURI = children.next().getSubject();
            UriRef childPredicate;
            if (targetChildrenMappings.size() == 1) {
                childPredicate = targetChildrenMappings.keySet().iterator().next();
            } else {
                childPredicate = defaultChildPredicate;
            }
            graph.add(new TripleImpl(parentURI, childPredicate, childURI));
            applyReverseBridgeSettings(childURI, graph);
            addChildrenAnnotations(childURI, graph);
        }
    }

    private void applyReverseBridgeSettings(NonLiteral subject, MGraph graph) {
        // add subsumption assertion
        graph.add(new TripleImpl(subject, targetResourcePredicate, targetResourceValue));
        // add name assertion
        revertObjectName(subject, graph);
    }

    private void revertObjectName(NonLiteral objectURI, MGraph graph) {
        if (nameResource != null) {
            Iterator<Triple> it = graph.filter(objectURI, CMSAdapterVocabulary.CMS_OBJECT_NAME, null);
            if (it.hasNext()) {
                Triple nameProp = it.next();
                Resource name = nameProp.getObject();
                graph.add(new TripleImpl(objectURI, nameResource, name));
            } else {
                log.warn("Failed to find name property for URI: {}", objectURI);
            }
        }
    }

    @Override
    public String getCMSPath() {
        return this.cmsPath;
    }

    /*
     * Methods to parse configurations
     */

    private void parseURIConfig(String config, Dictionary<String,Object> properties) {
        Object value = null;
        try {
            value = checkProperty(properties, config, false);
            if (value != null && !((String) value).trim().contentEquals("")) {
                if (config.contentEquals(PROP_NAME)) {
                    this.nameResource = parseUriRefFromConfig((String) value);
                } else if (config.contentEquals(PROP_DEFAULT_CHILD_PREDICATE)) {
                    this.defaultChildPredicate = parseUriRefFromConfig((String) value);
                }
            }
        } catch (ConfigurationException e) {
            log.warn("This configuration should be either empty or has one of the following formats:"
                     + "\nskos:narrower" + "\nhttp://www.w3.org/2004/02/skos/core#narrower");
        }
    }

    private void parseTargetResourceConfig(Dictionary<String,Object> properties) throws ConfigurationException {
        String targetResourceConfig = (String) checkProperty(properties, PROP_RESOURCE_SELECTOR, true);
        String[] configParts = parsePropertyConfig(targetResourceConfig);

        this.targetResourcePredicate = parseUriRefFromConfig(configParts[0]);
        this.targetResourceValue = parseUriRefFromConfig(configParts[1]);
    }

    private void parseChildrenMappings(Dictionary<String,Object> properties) {
        Object value = null;
        try {
            value = checkProperty(properties, PROP_CHILDREN, false);
        } catch (ConfigurationException e) {
            // not the case
        }
        if (value != null) {
            if (value instanceof String) {
                getChildConfiguration((String) value);

            } else if (value instanceof String[]) {
                for (String config : (String[]) value) {
                    getChildConfiguration(config);
                }
            }
        }
    }

    private UriRef parseUriRefFromConfig(String config) {
        return new UriRef(NamespaceEnum.getFullName(config));
    }

    private void getChildConfiguration(String config) {
        try {
            String[] configParts = parseChildrenConfig(config);
            int configLength = configParts.length;

            Object name = null;
            if (configLength == 2) {
                if (configParts[1].contains(":")) {
                    if (RDFBridgeHelper.isShortNameResolvable(configParts[1])) {
                        name = new UriRef(NamespaceEnum.getFullName(configParts[1]));
                    } else {
                        name = null;
                    }
                } else {
                    name = configParts[1];
                }
            }

            this.targetChildrenMappings.put(parseUriRefFromConfig(configParts[0]), name);

        } catch (ConfigurationException e) {
            log.warn("Failed to parse configuration value: {}", config);
            log.warn("Configuration value should be in the format e.g skos:Definition > definition");
        }
    }

    private Object checkProperty(Dictionary<String,Object> properties, String key, boolean required) throws ConfigurationException {
        Object value = properties.get(key);
        if (value == null) {
            if (required) {
                throw new ConfigurationException(key, "Failed to get value for this property");
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    private String[] parseChildrenConfig(String config) throws ConfigurationException {
        String[] configParts = config.split(">");
        int parts = configParts.length;

        if (parts == 1 || parts == 2) {
            for (int i = 0; i < parts; i++) {
                configParts[i] = configParts[i].trim();
            }
        } else {
            throw new ConfigurationException(PROP_CHILDREN,
                    "Children resource configuration should have a value like one of the following three alternatives: "
                            + "\nskos:narrower" + "\nskos:narrower > narrower"
                            + "\nskos:narrawer > rdfs:label");
        }
        return configParts;
    }

    private String[] parsePropertyConfig(String config) throws ConfigurationException {
        String[] configParts = config.split(">");
        int parts = configParts.length;

        if (parts == 1 || parts == 2) {
            for (int i = 0; i < parts; i++) {
                configParts[i] = configParts[i].trim();
            }
        } else {
            throw new ConfigurationException(PROP_RESOURCE_SELECTOR,
                    "Target resource and resource value should be seperated by a single'>' sign");
        }
        return configParts;
    }
}
