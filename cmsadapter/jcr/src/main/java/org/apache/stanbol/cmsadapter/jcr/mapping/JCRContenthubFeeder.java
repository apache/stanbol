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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.repository.SessionManager;
import org.apache.stanbol.cmsadapter.jcr.utils.JCRUtils;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContentItemFilter;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeederException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is default implementation of {@link ContenthubFeeder} interface for JCR content repositories. It
 * basically transforms the {@link Node}s in the content repository to content items in the Contenthub based
 * on the paths or IDs of the nodes.
 * 
 * <p>
 * Unless the node type of the processed node is <b>nt:file</b> or <b>nt:resource</b>, the content of the node
 * is obtained by checking the properties specified in <code>contentProperties</code> list below.
 * 
 * <p>
 * If the node type of the processed node is <i>nt:resource</i>, the content of the node is obtained from the
 * <b>jcr:data</b> property of the node. Also, if the mime type of the content is specified through the
 * <b>jcr:mimeType</b> property, it is set as the mime type of the content item, otherwise
 * <i>application/octet-stream</i> is set.
 * 
 * <p>
 * If the node type of processed node is <i>nt:file</i>, the content of the node is obtained from the
 * <i>nt:resource</i> property of the processed content. Mime type of the the content item is set in the same
 * manner explained for <i>nt:resource</i> nodes in previous paragraph.
 * 
 * @author suat
 * 
 */
@Component(metatype = true)
@Service(value = ContenthubFeeder.class)
public class JCRContenthubFeeder implements ContenthubFeeder {
    private static final String JCR_NT_FILE = "nt:file";

    private static final String JCR_NT_RESOURCE = "nt:resource";

    private static final String JCR_CONTENT = "jcr:content";

    private static final String JCR_DATA = "jcr:data";

    private static final String JCR_MIME_TYPE = "jcr:mimeType";

    private static final String JCR_ITEM_BY_PATH = "SELECT * from nt:base WHERE jcr:path = '%s'";

    private static final Logger log = LoggerFactory.getLogger(JCRContenthubFeeder.class);

    /*
     * These properties will not be indexed
     */
    private static List<String> excludedProperties;
    static {
        excludedProperties = new ArrayList<String>();
        excludedProperties.add("jcr:data");
        excludedProperties.add("jcr:uuid");
    }

    @Reference
    private SolrStore solrStore;

    @Reference
    private SessionManager sessionManager;

    private Session session = null;

    private List<String> contentProperties;

    @Override
    public void submitContentItemByCMSObject(Object o, String id) {
        submitContentItemByCMSObject(o, id, null);
    }

    @Override
    public void submitContentItemByCMSObject(Object o, String id, String indexName) {
        Node n = (Node) o;
        String actualNodeId = "";
        try {
            actualNodeId = n.getIdentifier();
            ContentContext contentContext = getContentContextWithBasicInfo(n, id);
            processContextAndSubmitToContenthub(contentContext, indexName);
        } catch (RepositoryException e) {
            log.warn("Failed to get basic information of node having id: {}", actualNodeId);
        }
    }

    @Override
    public void submitContentItemByID(String contentItemID) {
        submitContentItemByID(contentItemID, null);
    }

    @Override
    public void submitContentItemByID(String contentItemID, String indexName) {
        Node n;
        try {
            n = getNodeByID(contentItemID);
        } catch (RepositoryException e) {
            log.warn("Failed to obtain the item specified by the id: {}", contentItemID, e);
            return;
        }

        try {
            ContentContext contentContext = getContentContextWithBasicInfo(n);
            processContextAndSubmitToContenthub(contentContext, indexName);
        } catch (RepositoryException e) {
            log.warn("Failed to get basic information of node having id: {}", contentItemID);
        }
    }

    @Override
    public void submitContentItemByPath(String contentItemPath) {
        submitContentItemByPath(contentItemPath, null);
    }

    @Override
    public void submitContentItemByPath(String contentItemPath, String indexName) {
        Node n;
        try {
            n = getNodeByPath(contentItemPath);
        } catch (RepositoryException e) {
            log.warn("Failed to obtain the item specified by the path: {}", contentItemPath, e);
            return;
        }

        try {
            ContentContext contentContext = getContentContextWithBasicInfo(n);
            processContextAndSubmitToContenthub(contentContext, indexName);
        } catch (RepositoryException e) {
            log.warn("Failed to get basic information of node having path: {}", contentItemPath);
        }
    }

    @Override
    public void submitContentItemsUnderPath(String rootPath) {
        submitContentItemsUnderPath(rootPath, null);
    }

    @Override
    public void submitContentItemsUnderPath(String rootPath, String indexName) {
        List<Node> nodes;
        try {
            nodes = getNodesUnderPath(rootPath);
        } catch (RepositoryException e) {
            log.warn("Failed to obtain the item specified by the path: {}", rootPath, e);
            return;
        }

        for (Node n : nodes) {
            String path;
            try {
                path = n.getPath();
            } catch (RepositoryException e) {
                log.warn("Failed to obtain path of node", e);
                continue;
            }

            try {
                ContentContext contentContext = getContentContextWithBasicInfo(n);
                processContextAndSubmitToContenthub(contentContext, indexName);
            } catch (RepositoryException e) {
                log.warn("Failed to get basic information of node having path: {}", path);
            }
        }
    }

    @Override
    public void submitContentItemsByCustomFilter(ContentItemFilter customContentItemFilter) {
        throw new UnsupportedOperationException("This operation is not supported in this implementation");
    }

    @Override
    public void submitContentItemsByCustomFilter(ContentItemFilter customContentItemFilter, String indexName) {
        throw new UnsupportedOperationException("This operation is not supported in this implementation");
    }

    @Override
    public void deleteContentItemByID(String contentItemID) {
        deleteContentItemByID(contentItemID, null);
    }

    @Override
    public void deleteContentItemByID(String contentItemID, String indexName) {
        try {
            solrStore.deleteById(contentItemID, indexName);
        } catch (StoreException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteContentItemByPath(String contentItemPath) {
        deleteContentItemByPath(contentItemPath, null);
    }

    @Override
    public void deleteContentItemByPath(String contentItemPath, String indexName) {
        Node n;
        try {
            n = getNodeByPath(contentItemPath);
            try {
                solrStore.deleteById(n.getIdentifier(), indexName);
            } catch (StoreException e) {
                log.error(e.getMessage(), e);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to obtain the item specified by the path: {}", contentItemPath, e);
        }
    }

    @Override
    public void deleteContentItemsUnderPath(String rootPath) {
        deleteContentItemsUnderPath(rootPath, null);
    }

    @Override
    public void deleteContentItemsUnderPath(String rootPath, String indexName) {
        List<Node> nodes;
        try {
            nodes = getNodesUnderPath(rootPath);
            for (Node n : nodes) {
                try {
                    solrStore.deleteById(n.getIdentifier(), indexName);
                } catch (StoreException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to obtain the item specified by the path: {}", rootPath, e);
            return;
        }
    }

    @Override
    public void deleteContentItemsByCustomFilter(ContentItemFilter customContentItemFilter) {
        throw new UnsupportedOperationException("This operation is not supported in this implementation");
    }

    @Override
    public void deleteContentItemsByCustomFilter(ContentItemFilter customContentItemFilter, String indexName) {
        throw new UnsupportedOperationException("This operation is not supported in this implementation");
    }

    @Override
    public boolean canFeedWith(Object session) {
        return session instanceof Session;
    }

    private void processContextAndSubmitToContenthub(ContentContext contentContext, String indexName) {
        String id = contentContext.getIdentifier();
        populateContentContext(contentContext);
        if (contentContext.getContent() == null || contentContext.getContent().length == 0) {
            log.warn("Failed to get content for node having id: {}", id);
            return;
        }

        Map<String,List<Object>> constraints = getConstraintsFromNode(contentContext);
        if (constraints.isEmpty()) {
            log.debug("There is no constraint for the node having id: {}", id);
        }

        SolrContentItem sci = solrStore.create(contentContext.getContent(), id, contentContext.getNodeName(),
            contentContext.getContentType(), constraints);
        try {
            solrStore.enhanceAndPut(sci, indexName);
        } catch (StoreException e) {
            log.error(e.getMessage(), e);
        }
        log.info("Document submitted to Contenthub.");
        log.info("Id: {}", sci.getUri().getUnicodeString());
        log.info("Mime type: {}", sci.getMimeType());
        log.info("Constraints: {}", sci.getConstraints().toString());
    }

    private Map<String,List<Object>> getConstraintsFromNode(ContentContext contentContext) {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        try {
            PropertyIterator it = contentContext.getNode().getProperties();
            while (it.hasNext()) {
                javax.jcr.Property p = it.nextProperty();
                List<Object> propertyVals = new ArrayList<Object>();

                if (!skipProperty(p)) {
                    if (!p.isMultiple()) {
                        propertyVals.add(JCRUtils.getTypedPropertyValue(p.getType(), p.getValue()));
                    } else {
                        propertyVals.addAll(JCRUtils.getTypedPropertyValues(p.getType(), p.getValues()));
                    }
                    constraints.put(p.getName(), propertyVals);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failed to process properties of node having: {}", contentContext.getIdentifier());
        }
        return constraints;
    }

    private boolean skipProperty(javax.jcr.Property p) throws RepositoryException {
        if (excludedProperties.contains(p.getName())) {
            return true;
        }
        if (contentProperties != null) {
            for (String cProp : contentProperties) {
                if (p.getName().equals(cProp)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void populateContentContext(ContentContext contentContext) {
        String nodeName = contentContext.getNodeName();
        try {
            String nt = contentContext.getNodeType();
            if (nt.equals(JCR_NT_FILE)) {
                Node content = contentContext.getNode().getNode(JCR_CONTENT);
                if (content.getPrimaryNodeType().getName().equals(JCR_NT_RESOURCE)) {
                    getContentInfoFromNTResource(contentContext, content);
                } else {
                    getContentInfoFromCustomProperty(contentContext, content);
                }

            } else if (nt.equals(JCR_NT_RESOURCE)) {
                getContentInfoFromNTResource(contentContext, contentContext.getNode());

            } else {
                getContentInfoFromCustomProperty(contentContext, contentContext.getNode());
            }
        } catch (RepositoryException e) {
            log.warn("Failed to retrieve content for node: {}", nodeName, e);
        } catch (IOException e) {
            log.warn("Failed to get bytes from binary content of node: {}", nodeName, e);
        }
    }

    private void getContentInfoFromNTResource(ContentContext contentContext, Node dataNode) throws RepositoryException,
                                                                                           IOException {
        if (dataNode.hasProperty(JCR_DATA)) {
            javax.jcr.Property jcrData = dataNode.getProperty(JCR_DATA);
            Binary val = jcrData.getBinary();
            String encoding = ContentContext.MEDIA_TYPE_APPLICAION_OCTET_STREAM;
            if (dataNode.hasProperty(JCR_MIME_TYPE)) {
                encoding = dataNode.getProperty(JCR_MIME_TYPE).getString();
            }
            contentContext.setContentType(encoding);
            contentContext.setContent(IOUtils.toByteArray(val.getStream()));
        }
    }

    private void getContentInfoFromCustomProperty(ContentContext contentContext, Node dataNode) throws RepositoryException,
                                                                                               IOException {

        Node n = contentContext.getNode();
        if (contentProperties != null) {
            for (String prop : contentProperties) {
                if (n.hasProperty(prop)) {
                    javax.jcr.Property p = n.getProperty(prop);
                    if (p.getType() == PropertyType.STRING) {
                        String val = p.getString();
                        contentContext.setContent(val.getBytes());
                        contentContext.setContentType(ContentContext.MEDIA_TYPE_TEXT_PLAIN);

                    } else {
                        Binary val = p.getBinary();
                        contentContext.setContent(IOUtils.toByteArray(val.getStream()));
                        contentContext.setContentType(ContentContext.MEDIA_TYPE_APPLICAION_OCTET_STREAM);
                    }
                }
            }
        } else {
            log.warn("There is no content property specified for node:{}", contentContext.getNodeName());
        }
    }

    private ContentContext getContentContextWithBasicInfo(Node n) throws RepositoryException {
        return getContentContextWithBasicInfo(n, null);
    }

    private ContentContext getContentContextWithBasicInfo(Node n, String id) throws RepositoryException {
        ContentContext contentContext = new ContentContext();
        contentContext.setIdentifier((id == null || id.equals("")) ? n.getIdentifier() : id);
        contentContext.setNode(n);
        contentContext.setNodeType(n.getPrimaryNodeType().getName());
        contentContext.setNodeName(n.getName());
        return contentContext;
    }

    private Node getNodeByID(String id) throws ItemNotFoundException, RepositoryException {
        return session.getNodeByIdentifier(id);
    }

    private Node getNodeByPath(String path) throws PathNotFoundException, RepositoryException {
        return session.getNode(path);
    }

    private List<Node> getNodesUnderPath(String path) throws RepositoryException {
        List<Node> results = new ArrayList<Node>();
        // get root node
        Node root = getNodeByPath(path);
        results.add(root);

        // get child nodes
        // TODO use JCR-JOQM or JCR-SQL2 instead of deprecated SQL query type
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += "%";

        QueryManager qm = session.getWorkspace().getQueryManager();
        @SuppressWarnings("deprecation")
        Query query = qm.createQuery(String.format(JCR_ITEM_BY_PATH, path), Query.SQL);
        QueryResult queryResult = query.execute();
        NodeIterator nodes = queryResult.getNodes();
        while (nodes.hasNext()) {
            Node n = nodes.nextNode();
            if (n.getName().equals(JCR_CONTENT)
                && n.getParent().getPrimaryNodeType().getName().equals(JCR_NT_FILE)) {
                // skip the jcr:content child of nt:file nodes
                continue;
            }
            results.add(n);
        }
        return results;
    }

    @Override
    public void setConfigs(Dictionary<String,Object> configs) throws ContenthubFeederException {
        try {
            checkSession(configs);
        } catch (ConfigurationException e) {
            throw new ContenthubFeederException("Failed to set a session for JCRContenthubFeeder", e);
        } catch (RepositoryAccessException e) {
            throw new ContenthubFeederException("Failed to set a session for JCRContenthubFeeder", e);
        }
        checkContentProp(configs);
    }

    private void checkSession(Dictionary<String,Object> properties) throws ConfigurationException,
                                                                   RepositoryAccessException {
        Object value = properties.get(PROP_SESSION);
        if (value == null) {
            throw new ConfigurationException(PROP_SESSION,
                    "A valid JCR Session should be provided to activate this component.");
        }
        if (value instanceof String) {
            this.session = (Session) sessionManager.getSession((String) value);
        } else if (value instanceof Session) {
            this.session = (Session) value;
        } else {
            throw new ConfigurationException(PROP_SESSION,
                    "A valid JCR Session should be provided to activate this component.");
        }
    }

    @SuppressWarnings("unchecked")
    private void checkContentProp(Dictionary<String,Object> properties) {
        Object cProps = properties.get(PROP_CONTENT_PROPERTIES);
        if (cProps == null) {
            log.debug("No content properties specified for JCRContenthubFeeder");
        } else {
            this.contentProperties = (List<String>) cProps;
        }
    }

    /**
     * This context class holding the node itself and information related it exists to prevent repetitive
     * request for name, identifier, node type of actual node
     */
    private class ContentContext {
        public static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";
        public static final String MEDIA_TYPE_APPLICAION_OCTET_STREAM = "application/octet-stream";

        private Node node;
        private String nodeName;
        private String nodeType;
        private String identifier;
        private String contentType;
        private byte[] content;

        public Node getNode() {
            return node;
        }

        public void setNode(Node node) {
            this.node = node;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public byte[] getContent() {
            return content;
        }

        public void setContent(byte[] content) {
            this.content = content;
        }
    }
}