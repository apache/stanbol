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
package org.apache.stanbol.cmsadapter.cmis.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.cmis.repository.CMISObjectId;
import org.apache.stanbol.cmsadapter.cmis.utils.CMISUtils;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.core.repository.SessionManager;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;
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
 * This an implementation of {@link ContenthubFeeder} interface for CMIS repositories. It basically transforms
 * {@link Document}s in the CMIS content repository into content items in Contenthub based on the paths or IDs
 * of documents. Properties (e.g <i>cmis:createdBy</i>) of the documents that are not included in the
 * <code>excludedProperties</code> list below, are indexed as metadata of the content item. Furthermore,
 * triples extracted from the metadata documents whose names are formed by <b>"actual_document_name" +
 * "_metadata"</b> are also considered as metadata of the actual document in a similar way as default CMIS
 * properties. Metadata of the the content items provides faceted search feature in the Contenthub.
 * 
 * @author suat
 * 
 */
@Component
@Service(value = ContenthubFeeder.class)
public class CMISContenthubFeeder implements ContenthubFeeder {

    private static final Logger log = LoggerFactory.getLogger(CMISContenthubFeeder.class);

    @Reference
    private SolrStore solrStore;

    @Reference
    Parser parser;

    @Reference
    SessionManager sessionManager;

    private Session session = null;

    /*
     * These properties will not be indexed
     */
    private static List<String> excludedProperties;
    static {
        excludedProperties = new ArrayList<String>();
        excludedProperties.add("cmis:objectId");
        excludedProperties.add("cmis:changeToken");
        excludedProperties.add("cmis:versionSeriesId");
        excludedProperties.add("cmis:versionSeriesCheckedOutId");
        excludedProperties.add("cmis:versionSeriesCheckedOutBy");
        excludedProperties.add("cmis:contentStreamId");
        excludedProperties.add("cmis:contentStreamLength");
        excludedProperties.add("cmis:isImmutable");
        excludedProperties.add("cmis:isMajorVersion");
        excludedProperties.add("cmis:isLatestMajorVersion");
        excludedProperties.add("cmis:isVersionSeriesCheckedOut");
        excludedProperties.add("cmis:contentStreamFileName");
    }

    @Override
    public void submitContentItemByCMSObject(Object o, String id) {
        submitContentItemByCMSObject(o, id, null);
    }

    @Override
    public void submitContentItemByCMSObject(Object o, String id, String indexName) {
        CmisObject cmisObject = (CmisObject) o;
        if (hasType(cmisObject, BaseTypeId.CMIS_DOCUMENT)) {
            processDocumentAndSubmitToContenthub((Document) cmisObject, id, indexName);
        }
    }

    @Override
    public void submitContentItemByID(String contentItemID) {
        submitContentItemByID(contentItemID, null);
    }

    @Override
    public void submitContentItemByID(String contentItemID, String indexName) {
        CmisObject o;
        try {
            o = session.getObject(CMISObjectId.getObjectId(contentItemID));
        } catch (CmisBaseException e) {
            log.warn("Failed to retrieve document having id: {}", contentItemID);
            return;
        }

        if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
            processDocumentAndSubmitToContenthub((Document) o, indexName);
        }
    }

    @Override
    public void submitContentItemByPath(String contentItemPath) {
        submitContentItemByPath(contentItemPath, null);
    }

    @Override
    public void submitContentItemByPath(String contentItemPath, String indexName) {
        CmisObject o;
        try {
            o = session.getObjectByPath(contentItemPath);
        } catch (CmisBaseException e) {
            log.warn("Failed to retrieve document having path: {}", contentItemPath);
            return;
        }

        if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
            processDocumentAndSubmitToContenthub((Document) o, indexName);
        }
    }

    @Override
    public void submitContentItemsUnderPath(String rootPath) {
        submitContentItemsUnderPath(rootPath, null);
    }

    @Override
    public void submitContentItemsUnderPath(String rootPath, String indexName) {
        CmisObject o;
        try {
            o = session.getObjectByPath(rootPath);
        } catch (CmisBaseException e) {
            log.warn("Failed to retrieve object having path: {}", rootPath);
            return;
        }

        if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
            processDocumentAndSubmitToContenthub((Document) o, indexName);
        } else {
            List<Document> documents = new ArrayList<Document>();
            getDocumentsUnderFolder((Folder) o, documents);
            for (Document d : documents) {
                processDocumentAndSubmitToContenthub(d, indexName);
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
        CmisObject o;
        try {
            o = session.getObjectByPath(contentItemPath);
        } catch (CmisBaseException e) {
            log.warn("Failed to retrieve document having path: {}", contentItemPath);
            return;
        }

        try {
            solrStore.deleteById(o.getId(), indexName);
        } catch (StoreException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteContentItemsUnderPath(String rootPath) {
        deleteContentItemsUnderPath(rootPath, null);
    }

    @Override
    public void deleteContentItemsUnderPath(String rootPath, String indexName) {
        CmisObject o;
        try {
            o = session.getObjectByPath(rootPath);
        } catch (CmisBaseException e) {
            log.warn("Failed to retrieve document having path: {}", rootPath);
            return;
        }

        List<Document> documents = new ArrayList<Document>();
        if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
            documents.add((Document) o);
        } else {
            getDocumentsUnderFolder((Folder) o, documents);
        }
        for (Document d : documents) {
            try {
                solrStore.deleteById(d.getId(), indexName);
            } catch (StoreException e) {
                log.error(e.getMessage(), e);
            }
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

    @Override
    public void setConfigs(Dictionary<String,Object> configs) throws ContenthubFeederException {
        try {
            checkSession(configs);
        } catch (ConfigurationException e) {
            throw new ContenthubFeederException("Failed to set a session for CMISContenthubFeeder", e);
        } catch (RepositoryAccessException e) {
            throw new ContenthubFeederException("Failed to set a session for CMISContenthubFeeder", e);
        }
    }

    private void checkSession(Dictionary<String,Object> properties) throws ConfigurationException,
                                                                   RepositoryAccessException {
        Object value = properties.get(ContenthubFeeder.PROP_SESSION);
        if (value == null) {
            throw new ConfigurationException(PROP_SESSION,
                    "A valid CMIS Session or session key should be provided to activate this component.");
        }
        if (value instanceof String) {
            this.session = ((Session) sessionManager.getSession((String) value));
        } else if (value instanceof Session) {
            this.session = (Session) value;
        } else {
            throw new ConfigurationException(PROP_SESSION,
                    "A valid CMIS Session or session key should be provided to activate this component.");
        }
    }

    private void processDocumentAndSubmitToContenthub(Document d, String indexName) {
        processDocumentAndSubmitToContenthub(d, null, indexName);
    }

    private void processDocumentAndSubmitToContenthub(Document d, String id, String indexName) {
        byte[] content;
        try {
            content = IOUtils.toByteArray(d.getContentStream().getStream());
            if (content == null || content.length == 0) {
                log.warn("Failed to retrieve content for node: {}", d.getName());
                return;
            }
        } catch (IOException e) {
            log.warn("Failed to get bytes from binary content of document: {}", d.getName(), e);
            return;
        }
        String mimeType = d.getContentStreamMimeType();
        Map<String,List<Object>> constraints = getConstraintsFromDocument(d);
        id = (id == null || id.equals("")) ? d.getId() : id;
        SolrContentItem sci = solrStore.create(content, d.getId(), d.getName(), mimeType, constraints);
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

    private Map<String,List<Object>> getConstraintsFromDocument(Document d) {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        List<org.apache.chemistry.opencmis.client.api.Property<?>> docProps = d.getProperties();
        for (org.apache.chemistry.opencmis.client.api.Property<?> p : docProps) {
            if (!excludedProperties.contains(p.getQueryName())) {
                PropertyType t = p.getType();
                List<Object> values = new ArrayList<Object>();
                if (p.isMultiValued()) {
                    values.addAll(CMISUtils.getTypedPropertyValues(t, p.getValues()));
                } else {
                    values.add(CMISUtils.getTypedPropertyValue(t, p.getValue()));
                }
                constraints.put(p.getQueryName(), values);
            }
        }
        checkMetadataDocument(d, constraints);
        return constraints;
    }

    private void checkMetadataDocument(Document d, Map<String,List<Object>> constraints) {
        List<Folder> parents = d.getParents();
        for (Folder parent : parents) {
            Iterator<CmisObject> children = parent.getChildren().iterator();
            while (children.hasNext()) {
                CmisObject child = children.next();
                if (hasType(child, BaseTypeId.CMIS_DOCUMENT)
                    && child.getName().equals(d.getName() + CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION)) {

                    MGraph metadata = new SimpleMGraph();
                    ContentStream cs = ((Document) child).getContentStream();
                    parser.parse(metadata, cs.getStream(), SupportedFormat.RDF_XML);

                    Iterator<Triple> triples = metadata.filter(null, null, null);
                    while (triples.hasNext()) {
                        Triple t = triples.next();
                        String predicate = t.getPredicate().getUnicodeString();
                        String shortPredicate = NamespaceEnum.getShortName(predicate);
                        if (shortPredicate.equals(predicate)) {
                            log.warn("Failed to obtain short name for URI: {}", predicate);
                            continue;
                        }
                        Resource resource = t.getObject();

                        String propValue = "";
                        if (resource instanceof Literal) {
                            propValue = RDFBridgeHelper.getResourceStringValue(resource);
                        } else if (resource instanceof UriRef) {
                            propValue = ((UriRef) resource).getUnicodeString();
                        } else {
                            propValue = resource.toString();
                        }

                        if (constraints.containsKey(shortPredicate)) {
                            constraints.get(shortPredicate).add(propValue);
                        } else {
                            List<Object> valueList = new ArrayList<Object>();
                            valueList.add(propValue);
                            constraints.put(shortPredicate, valueList);
                        }
                    }
                }
            }
        }
    }

    private void getDocumentsUnderFolder(Folder f, List<Document> documentList) {
        Iterator<CmisObject> childs = f.getChildren().iterator();
        while (childs.hasNext()) {
            CmisObject child = childs.next();
            if (hasType(child, BaseTypeId.CMIS_FOLDER)) {
                getDocumentsUnderFolder((Folder) child, documentList);
            } else if (hasType(child, BaseTypeId.CMIS_DOCUMENT)) {
                String documentName = child.getName();
                if (!documentName.endsWith(CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION)) {
                    documentList.add((Document) child);
                }
            }
        }
    }

    private boolean hasType(CmisObject o, BaseTypeId type) {
        return o.getBaseTypeId().equals(type);
    }
}