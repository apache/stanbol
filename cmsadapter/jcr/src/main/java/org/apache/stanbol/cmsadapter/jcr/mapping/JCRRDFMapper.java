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
package org.apache.stanbol.cmsadapter.jcr.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.mapping.BaseRDFMapper;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.jcr.utils.JCRUtils;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RDFMapper} for JCR repositories.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class JCRRDFMapper extends BaseRDFMapper implements RDFMapper {
    private static final Logger log = LoggerFactory.getLogger(JCRRDFMapper.class);

    /**
     * This list contains properties that will not be included in the RDF which is generated from content
     * repository
     */
    private static final List<UriRef> excludedProperties;

    static {
        excludedProperties = new ArrayList<UriRef>();
        excludedProperties.add(CMSAdapterVocabulary.CMS_OBJECT_HAS_URI);
        excludedProperties.add(new UriRef(NamespaceEnum.jcr.getNamespace() + "data"));
    }

    @Override
    public void storeRDFinRepository(Object session, MGraph annotatedGraph) {
        List<NonLiteral> rootObjects = RDFBridgeHelper.getRootObjectsOfGraph(annotatedGraph);
        for (NonLiteral root : rootObjects) {
            String nodeName = getObjectName(root, annotatedGraph);
            String nodePath = getObjectPath(root, nodeName, annotatedGraph);
            Node parent = checkCreateParentNodes(nodePath, (Session) session);
            if (parent != null) {
                createNode(parent, root, nodeName, annotatedGraph, (Session) session);
            }
        }
        try {
            ((Session) session).save();
        } catch (RepositoryException e) {
            log.warn("Failed to save JCR session", e);
        }
    }

    /**
     * Recursively creates the node itself and its children. If processed node already exists, it is updated
     * with the new properties, a new node is created in the repository.
     * 
     * @param parent
     *            parent {@link Node} of the the node to be created
     * @param nodeSubject
     *            URI of the node to be created
     * @param nodeName
     *            name of the node to be created
     * @param graph
     *            annotated {@link MGraph}
     * @param session
     *            {@link Session} object to access repository
     */
    private void createNode(Node parent,
                            NonLiteral nodeSubject,
                            String nodeName,
                            MGraph graph,
                            Session session) {
        Node n = null;
        String parentPath = "";
        try {
            parentPath = parent.getPath();
            n = createActualNode(parent, nodeSubject, nodeName, graph);
        } catch (RepositoryException e) {
            log.warn(String.format("Failed to create node %s for parent %s. ", nodeName, parentPath), e);
            return;
        }

        // create properties
        createNodeProperties(n, nodeSubject, graph, session);

        // create children
        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, nodeSubject);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = RDFBridgeHelper.getResourceStringValue(childSubject,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
            createNode(n, childSubject, childName, graph, session);
        }
    }

    /**
     * This method creates a node by using the specified parameters. It first check RDF primary type and tries
     * to create the node with the type specified it the type is set. In the next step it checks mixin types
     * and tries to set if there is any.
     * 
     * @param parent
     * @param nodeSubject
     * @param nodeName
     * @param graph
     * @return
     * @throws RepositoryException
     */
    private Node createActualNode(Node parent, NonLiteral nodeSubject, String nodeName, MGraph graph) throws RepositoryException {
        Node n;
        if (parent.hasNode(nodeName)) {
            n = parent.getNode(nodeName);
        } else {
            String nodeType = RDFBridgeHelper.getResourceStringValue(nodeSubject,
                CMSAdapterVocabulary.JCR_PRIMARY_TYPE, graph);

            if (nodeType.contentEquals("")) {
                n = parent.addNode(nodeName);
            } else {
                try {
                    n = parent.addNode(nodeName, nodeType);
                } catch (RepositoryException e) {
                    log.warn(String.format("Failed to create node %s with the type %s", nodeName, nodeType),
                        e);
                    n = parent.addNode(nodeName);
                }
            }

            // check mixin types
            Iterator<Triple> mixins = graph.filter(nodeSubject, CMSAdapterVocabulary.JCR_MIXIN_TYPES, null);
            String mixinType = "";
            while (mixins.hasNext()) {
                Resource r = mixins.next().getObject();
                try {
                    mixinType = RDFBridgeHelper.getShortURIFromResource(r);
                    if (!mixinType.contentEquals("")) {
                        n.addMixin(mixinType);
                    }
                } catch (Exception e) {
                    log.warn("Failed to set mixin type: {}", mixinType);
                }
            }
        }
        return n;
    }

    /**
     * Transforms the assertions belonging to a resource in the RDF data as properties in the content
     * repository.
     * 
     * @param n
     * @param subject
     * @param graph
     * @param session
     */
    private void createNodeProperties(Node n, NonLiteral subject, MGraph graph, Session session) {
        Iterator<Triple> it = graph.filter(subject, null, null);
        Map<String,PropertyInfo> propVals = new HashMap<String,PropertyInfo>();

        createDefaultPropertiesForCMS(n, subject);

        while (it.hasNext()) {
            Triple t = it.next();
            String propURI = t.getPredicate().getUnicodeString();
            Resource resource = t.getObject();

            String propValue = "";
            if (resource instanceof Literal) {
                propValue = RDFBridgeHelper.getResourceStringValue(subject, t.getPredicate(), graph);
            } else if (resource instanceof UriRef) {
                propValue = RDFBridgeHelper.removeEndCharacters(resource.toString());
            } else {
                propValue = resource.toString();
            }

            if (propVals.containsKey(propURI)) {
                PropertyInfo pInfo = propVals.get(propURI);
                pInfo.addPropertyValue(propValue);
            } else {
                PropertyInfo pInfo = new PropertyInfo();
                pInfo.setPropertyType(resource);
                pInfo.addPropertyValue(propValue);
                propVals.put(propURI, pInfo);
            }
        }
        for (String propURI : propVals.keySet()) {
            PropertyInfo pInfo = propVals.get(propURI);
            List<String> singlePropValList = pInfo.getPropertyValues();
            String[] singlePropVals = new String[singlePropValList.size()];
            singlePropValList.toArray(singlePropVals);
            String propName = NamespaceEnum.getShortName(propURI);

            if (propName.equals(propURI)) {
                // we couldn't obtain the short name from the property URI and skipping this property
                log.warn("Failed to obtain short name for the propertyURI: {}. Skipping it...", propURI);
                continue;
            }

            // check whether the namespace prefix is registered in the JCR
            // repository
            try {
                checkNamespaceForShortURI(session, propName);
            } catch (Exception e) {
                log.warn("Failed to check namespace for property: {}", propURI, e);
                continue;
            }

            try {
                boolean isMultiple = false;
                try {
                    Property p = n.getProperty(propName);
                    isMultiple = p.isMultiple();
                    if (isMultiple) {
                        n.setProperty(propName, (Value[]) null);
                    } else {
                        n.setProperty(propName, (Value) null);
                    }
                } catch (Exception e) {
                    // assume property not found
                    n.setProperty(propName, (Value) null);
                }

                if (singlePropVals.length == 1) {
                    n.setProperty(propName, singlePropVals[0], pInfo.getPropertyType());
                } else {
                    n.setProperty(propName, singlePropVals, pInfo.getPropertyType());
                }
                log.debug("{} property of updated/created with {}", propName);
            } catch (Exception e) {
                log.warn("Failed to update property: {} for node {}: ", propName, e);
            }
        }
    }

    private void createDefaultPropertiesForCMS(Node n, NonLiteral subject) {
        String uriPropShortURI = NamespaceEnum.getShortName(CMSAdapterVocabulary.CMS_OBJECT_HAS_URI
                .getUnicodeString());
        try {
            n.setProperty(uriPropShortURI, RDFBridgeHelper.removeEndCharacters(subject.toString()));
        } catch (RepositoryException e) {
            log.warn("Failed to set URI property of node", e);
        }
    }

    /**
     * Takes a path and tries to check nodes that forms that path. If nodes do not exist, they are created.
     * 
     * @param rootPath
     *            path in which root objects will be created or existing one will be searched
     * @param session
     *            session to access repository
     * @return the first level parent {@link Node} of the node specified with <code>nodePath</code> if there
     *         is not any exception, otherwise returns <code>null</code>.
     */
    private Node checkCreateParentNodes(String rootPath, Session session) {
        Node n;
        String currentPath;
        try {
            n = session.getRootNode();
            currentPath = n.getPath();
        } catch (RepositoryException e) {
            log.warn("Failed to get Node for path: {}", rootPath, e);
            return null;
        }

        String[] pathSections = rootPath.split("/");
        for (int i = 1; i < pathSections.length - 1; i++) {
            try {
                if (!n.hasNode(pathSections[i])) {
                    n = n.addNode(pathSections[i]);
                } else {
                    n = n.getNode(pathSections[i]);
                }
                currentPath = n.getPath();
            } catch (RepositoryException e) {
                log.warn("Failed to get child node for name: {} of node: {}", new Object[] {pathSections[i],
                                                                                            currentPath}, e);
                return null;
            }
        }

        return n;
    }

    @Override
    public MGraph generateRDFFromRepository(String baseURI, Object session, String rootPath) throws RDFBridgeException {
        MGraph cmsGraph = new SimpleMGraph();
        Session jcrSession = (Session) session;

        Node rootNode = null;
        try {
            rootNode = jcrSession.getNode(rootPath);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve node having path: {} or its children", rootPath);
            throw new RDFBridgeException("Failed to node having path: " + rootPath + " or its children", e);
        }

        String name = "";
        try {
            name = rootNode.getName();
            cmsGraph.addAll(getGraphForNode(baseURI, rootNode));
        } catch (RepositoryException e) {
            log.warn("Repository exception while processing node having name: {}", name, e);
        }
        return cmsGraph;
    }

    /**
     * Generates an RDF from the specified {@link Node}. It annotates the resource representing the Node with
     * {@link CMSAdapterVocabulary#CMS_OBJECT}. In the next step, it transforms the properties of the Node to
     * the RDF. In the last step, it checks the child Nodes of the processed Node and executes same operations
     * for the child.
     * 
     * @param n
     * @return
     */
    MGraph getGraphForNode(String baseURI, Node n) {
        MGraph graph = new SimpleMGraph();

        // create CMS Object annotation
        NonLiteral subject = getNodeURI(baseURI, n);
        if (subject == null) {
            return graph;
        }
        graph.add(new TripleImpl(subject, RDFBridgeHelper.RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT));

        String nodeName = "";
        try {
            nodeName = n.getName();
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve name of node", e);
        }

        // properties
        try {
            createPropertiesAsRDF(n, subject, graph);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve properties of node: {}", nodeName, e);
        }

        // children
        NodeIterator nit;
        try {
            nit = n.getNodes();
            while (nit.hasNext()) {
                Node child = nit.nextNode();
                NonLiteral childURI = getNodeURI(baseURI, child);
                if (childURI == null) {
                    continue;
                }
                graph.add(new TripleImpl(childURI, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, subject));
                graph.addAll(getGraphForNode(baseURI, child));
            }
        } catch (RepositoryException e) {
            log.warn("Error while processing children of node: {}", nodeName, e);
        }
        return graph;
    }

    private void createPropertiesAsRDF(Node n, NonLiteral subject, MGraph graph) throws RepositoryException {
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        PropertyIterator pit = n.getProperties();
        while (pit.hasNext()) {
            try {
                Property p = pit.nextProperty();
                UriRef pURI = getPropertyURI(p.getName());
                if (pURI == null || excludedProperties.contains(pURI)) {
                    continue;
                }
                List<Object> values = new ArrayList<Object>();
                if (p.isMultiple()) {
                    values.addAll(JCRUtils.getTypedPropertyValues(p.getType(), p.getValues()));
                } else {
                    values.add(JCRUtils.getTypedPropertyValue(p.getType(), p.getValue()));
                }
                for (Object val : values) {
                    /*
                     * As JCR does not support retrieval of values of URI typed properties, object properties
                     * are reflected as String properties in JCR. So, when creating RDF from JCR repository,
                     * currently just look at the value of property starts with "http" prefix.
                     * 
                     * TODO: Other dirty workaround may be including some prefixes to the object properties to
                     * identify them
                     * 
                     * TODO: Fix this when JCR supports retrieval of URI typed property values
                     */
                    try {
                        String valStr = (String) val;
                        if (valStr.startsWith("http")) {
                            graph.add(new TripleImpl(subject, pURI, new UriRef(valStr)));
                            continue;
                        }
                    } catch (Exception e) {
                        // ignore the exception
                    }
                    graph.add(new TripleImpl(subject, pURI, literalFactory.createTypedLiteral(val)));
                }
            } catch (RepositoryException e) {
                log.warn("Failed to process property of node", e);
            }
        }

        String path = "";
        try {
            path = n.getPath();
        } catch (RepositoryException e) {
            log.warn("Failed to get path of node", e);
        }
        String name = "";
        try {
            name = n.getName();
        } catch (RepositoryException e) {
            log.warn("Failed to get name of the node", e);
        }

        RDFBridgeHelper.createDefaultPropertiesForRDF(subject, graph, path, name);
    }

    private UriRef getNodeURI(String baseURI, Node n) {
        String uriPropShortURI = NamespaceEnum.getShortName(CMSAdapterVocabulary.CMS_OBJECT_HAS_URI
                .getUnicodeString());
        String nodeName = "";
        String nodeURI = null;
        try {
            nodeName = n.getName();
            Property p = n.getProperty(uriPropShortURI);
            nodeURI = p.getString();
            return new UriRef(RDFBridgeHelper.removeEndCharacters(nodeURI));
        } catch (Exception e) {
            log.warn("Failed to retrieve URI from property for node {}", nodeName);
        }

        try {
            nodeURI = RDFBridgeHelper.appendLocalName(baseURI, n.getIdentifier());
            return new UriRef(nodeURI);
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve identifer to be used as URI for node {}", nodeName, e);
        }
        return null;
    }

    private static UriRef getPropertyURI(String shortName) throws RepositoryException {
        String prefix = "";
        String resourceName = "";
        int index = shortName.indexOf(':');
        if (index == -1) {
            prefix = NamespaceEnum.cms.getPrefix();
            resourceName = shortName;
            shortName = prefix + ":" + shortName;
        } else {
            resourceName = shortName.substring(index + 1);
            prefix = shortName.substring(0, shortName.indexOf(':'));
        }
        if (RDFBridgeHelper.isShortNameResolvable(shortName)) {
            return new UriRef(NamespaceEnum.getFullName(shortName));
        } else {
            // TODO it's a better solution to makes possible to add new prefix-uri tuples to NamespaceEnum
            log.debug("Auto generated URI is being assigned for namespace prefix: {}", prefix);
            return new UriRef(CMSAdapterVocabulary.CMS_ADAPTER_VOCABULARY_URI + "/ext_" + prefix + "/"
                              + resourceName);
        }
    }

    private void checkNamespaceForShortURI(Session session, String shortURI) throws NamespaceException,
                                                                            RepositoryException {
        String prefix = shortURI.split(":")[0];
        try {
            session.getNamespaceURI(prefix);
        } catch (Exception e) {
            String namespaceURI = NamespaceEnum.forPrefix(prefix).getNamespace();
            session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, namespaceURI);
        }
    }

    @Override
    public boolean canMapWith(Object session) {
        return session instanceof Session;
    }

    private class PropertyInfo {
        private int type;
        private List<String> propVals;

        public void addPropertyValue(String value) {
            if (propVals == null) {
                propVals = new ArrayList<String>();
            }
            propVals.add(value);
        }

        public List<String> getPropertyValues() {
            return propVals;
        }

        public void setPropertyType(Resource r) {
            type = JCRUtils.getPropertyTypeByResource(r);
        }

        public int getPropertyType() {
            return type;
        }
    }
}