package org.apache.stanbol.cmsadapter.jcr.repository;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
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
public class JCRRDFMapper implements RDFMapper {
    private static final Logger log = LoggerFactory.getLogger(JCRRDFMapper.class);

    @Override
    public void storeRDFinRepository(Object session, MGraph annotatedGraph) {
        List<NonLiteral> rootObjects = RDFBridgeHelper.getRootObjetsOfGraph(annotatedGraph);
        for (NonLiteral root : rootObjects) {
            String nodePath = RDFBridgeHelper.getResourceStringValue(root,
                CMSAdapterVocabulary.CMS_OBJECT_PATH, annotatedGraph);
            String nodeName = RDFBridgeHelper.getResourceStringValue(root,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, annotatedGraph);
            Node parent = checkCreateParentNodes(nodePath, (Session) session);
            createNode(parent, root, nodeName, annotatedGraph, (Session) session);
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
            if (parent.hasNode(nodeName)) {
                n = parent.getNode(nodeName);
            } else {
                n = parent.addNode(nodeName);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to create node {} for parent {}. ", new Object[] {parentPath, nodeName}, e);
        }

        // create properties
        createNodeProperties(n, nodeSubject, graph);

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
     * This function creates specified properties for the node given as parameter. If a property already
     * exists, it first is nullified then new value is assigned. Properties are accessed through
     * {@code CMSAdapterVocabulary#CMS_OBJECT_HAS_PROPERTY} property.
     * 
     * @param n
     *            {@link Node} of which properties will be updated/created
     * @param subject
     *            corresponding URI of the node
     * @param graph
     *            annotated {@link MGraph}
     */
    private void createNodeProperties(Node n, NonLiteral subject, MGraph graph) {
        Iterator<Triple> it = graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_HAS_PROPERTY, null);
        while (it.hasNext()) {
            UriRef tempPropURI = new UriRef(RDFBridgeHelper.removeEndCharacters(it.next().getObject()
                    .toString()));
            String propName = RDFBridgeHelper.getResourceStringValue(tempPropURI,
                CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_NAME, graph);
            UriRef propURI = RDFBridgeHelper.getResourceURIValue(tempPropURI,
                CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_URI, graph);
            Resource resource = RDFBridgeHelper.getResource(subject, propURI, graph);

            if (resource == null) {
                continue;
            }

            String propValue = "";
            if (resource instanceof Literal) {
                propValue = RDFBridgeHelper.getResourceStringValue(subject, propURI, graph);
            } else if (resource instanceof UriRef) {
                propValue = RDFBridgeHelper.removeEndCharacters(resource.toString());
            } else {
                propValue = resource.toString();
            }

            int propType = RDFBridgeHelper.getPropertyType(resource);
            try {
                n.setProperty(propName, (Value) null);
                n.setProperty(propName, propValue, propType);
                log.debug("{} property of updated/created with {}", propName, propValue);
            } catch (Exception e) {
                log.warn("Failed to update property: {} for node {}: ", propName, e);
            }
        }
    }

    /**
     * Takes path of a root object in the annotated RDF and tries to check parent nodes. If parent nodes do
     * not exist, they are created.
     * 
     * @param nodePath
     *            path of a root object
     * @param session
     *            session to access repository
     * @return the first level parent {@link Node} of the node specified with <code>nodePath</code> if there
     *         is not any exception, otherwise returns <code>null</code>.
     */
    private Node checkCreateParentNodes(String nodePath, Session session) {
        Node n;
        String currentPath;
        try {
            n = session.getRootNode();
            currentPath = n.getPath();
        } catch (RepositoryException e) {
            log.warn("Failed to get root node while trying to get Node for path: {}", nodePath, e);
            return null;
        }

        String[] pathSections = nodePath.split("/");
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
}
