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
package org.apache.stanbol.cmsadapter.cmis.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.mapping.BaseRDFMapper;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RDFMapper} for CMIS repositories.
 * <p>
 * As CMIS specification does not allow hierarchy in the documents and custom properties for documents and
 * folders, a folder-document mix workaround is applied to represent hierarchical structures. See the
 * explanations in {@link #storeRDFinRepository(Object, MGraph)} and
 * {@link #generateRDFFromRepository(Object, String)}.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class CMISRDFMapper extends BaseRDFMapper implements RDFMapper {
    private static final Logger log = LoggerFactory.getLogger(CMISRDFMapper.class);

    private static final String DOCUMENT_RDF = "_metadata";

    private static final String DOCUMENT_RDF_MIME_TYPE = "text/plain";

    @Reference
    Serializer serializer;

    /**
     * This implementation of {@link RDFMapper#storeRDFinRepository(Object, MGraph)} realizes a workaround to
     * come up with the restriction of not being able to create hierarchical documents and set custom
     * properties to documents.
     * <p>
     * The workaround is to create 3 object in the content repository for each object that will normally be
     * created from the RDF data. For example if a single object named <b>MyObject</b> is expected to be
     * created from the annotated RDF data in the content repository, first of all a {@link Folder} named
     * <b>MyObject</b> will be created. In this folder a {@link Document} named <b>MyObject</b> representing
     * the actual object and another document named <b>MyObject_metadata</b> will be created.
     * <p>
     * Child relations between is set through the folder hierarchy and <b>MyObject_metadata</b> contains an
     * RDF data formed by the {@link RDFBridge}.
     * 
     * @param session
     *            {@link Session} object to access the repository
     * @param annotatedGraph
     *            annotated {@link MGraph} with CMS vocabulary annotations. For details see
     *            {@link RDFMapper#storeRDFinRepository(Object, MGraph)}
     */
    @Override
    public void storeRDFinRepository(Object session, MGraph annotatedGraph) {
        List<NonLiteral> rootObjects = RDFBridgeHelper.getRootObjectsOfGraph(annotatedGraph);
        for (NonLiteral root : rootObjects) {
            String documentName = getObjectName(root, annotatedGraph);
            String documentPath = getObjectPath(root, documentName, annotatedGraph);
            Folder rootFolder = checkCreateParentFolders(documentPath, (Session) session);
            if (rootFolder != null) {
                createDocument(rootFolder, root, documentName, annotatedGraph, (Session) session);
            } else {
                log.warn("Failed to get Folder for path: {}", documentPath);
            }
        }
    }

    private void createDocument(Folder parent,
                                NonLiteral documentURI,
                                String documentName,
                                MGraph graph,
                                Session session) {

        Folder containerFolder = createStructureForDocument(documentName, documentURI, parent, session, graph);

        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, documentURI);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = RDFBridgeHelper.getResourceStringValue(childSubject,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
            createDocument(containerFolder, childSubject, childName, graph, session);
        }
    }

    private Folder createStructureForDocument(String documentName,
                                              NonLiteral documentURI,
                                              Folder parentFolder,
                                              Session session,
                                              MGraph graph) {

        String documentPath;
        String parentPath = parentFolder.getPath();
        if (parentPath.endsWith("/")) {
            documentPath = parentPath + documentName;
        } else {
            documentPath = parentPath + "/" + documentName;
        }

        Folder containerFolder = createFolderByPath(parentFolder, documentName, documentPath, session);
        if (containerFolder != null) {
            String rdfDocumentName = documentName + DOCUMENT_RDF;
            createDocumentByPath(containerFolder, rdfDocumentName, documentPath + "/" + rdfDocumentName,
                getDocumentContentStream(rdfDocumentName, documentURI, graph), session);
            createDocumentByPath(containerFolder, documentName, documentPath + "/" + documentName, null,
                session);
        }
        return containerFolder;
    }

    private ContentStream getDocumentContentStream(String documentName, NonLiteral documentURI, MGraph graph) {
        MGraph documentMGraph = collectedDocumentResources(documentURI, graph);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, documentMGraph, SupportedFormat.RDF_XML);
        byte[] serializedGraph = baos.toByteArray();
        InputStream stream = new ByteArrayInputStream(baos.toByteArray());
        BigInteger length = new BigInteger(serializedGraph.length + "");
        ContentStream contentStream = new ContentStreamImpl(documentName, length, DOCUMENT_RDF_MIME_TYPE,
                stream);
        return contentStream;
    }

    private MGraph collectedDocumentResources(NonLiteral subject, MGraph graph) {
        MGraph documentMGraph = new SimpleMGraph();
        Iterator<Triple> it = graph.filter(subject, null, null);
        while (it.hasNext()) {
            documentMGraph.add(it.next());
        }
        return documentMGraph;
    }

    /**
     * Takes a document path and checks folders in which document will be created/updated. If folders do not
     * exist, they are created.
     * 
     * @param documentPath
     *            path in which root objects will be created or existing one will be searched
     * @param session
     *            session to access repository
     * @return {@link Folder} one level up from the document
     */
    private Folder checkCreateParentFolders(String documentPath, Session session) {
        Folder f = session.getRootFolder();
        String[] pathSections = documentPath.split("/");
        String currentPath = "/";
        for (int i = 1; i < pathSections.length - 1; i++) {
            String folderName = pathSections[i];
            currentPath += folderName;
            f = createFolderByPath(f, folderName, currentPath, session);
            if (f != null) {
                currentPath += "/";
            } else {
                return null;
            }
        }
        return f;
    }

    private Folder createFolderByPath(Folder root, String name, String path, Session session) {
        Folder f;
        try {
            CmisObject o = session.getObjectByPath(path);
            if (hasType(o, BaseTypeId.CMIS_FOLDER)) {
                f = (Folder) o;
            } else {
                log.warn(
                    "Object having path: {} does not have Folder base type. It should have Folder base type to allow create documents in it",
                    path);
                return null;
            }
        } catch (CmisObjectNotFoundException e) {
            log.debug("Object having path: {} does not exists, a new one will be created", path);
            f = root.createFolder(getProperties(BaseTypeId.CMIS_FOLDER.value(), name));
        }
        return f;
    }

    private Document createDocumentByPath(Folder parent,
                                          String name,
                                          String path,
                                          ContentStream contentStream,
                                          Session session) {
        Document d;
        try {
            CmisObject o = session.getObjectByPath(path);
            if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
                d = (Document) o;
            } else {
                log.warn(
                    "Object having path: {} does not have Folder base type. It should have Folder base type to allow create documents in it",
                    path);
                return null;
            }
        } catch (CmisObjectNotFoundException e) {
            log.debug("Object having path: {} does not exists, a new one will be created", path);
            d = parent.createDocument(getProperties(BaseTypeId.CMIS_DOCUMENT.value(), name), contentStream,
                VersioningState.NONE);
        }
        return d;
    }

    private Map<String,Object> getProperties(String... properties) {
        Map<String,Object> propMap = new HashMap<String,Object>();
        propMap.put(PropertyIds.OBJECT_TYPE_ID, properties[0]);
        propMap.put(PropertyIds.NAME, properties[1]);
        return propMap;
    }

    private boolean hasType(CmisObject o, BaseTypeId type) {
        return o.getBaseTypeId().equals(type);
    }

    @Override
    public MGraph generateRDFFromRepository(Object session, String rootPath) {
        throw new UnsupportedOperationException("This method is not implemented yet");
    }

    @Override
    public boolean canMap(String connectionType) {
        return connectionType.contentEquals("CMIS");
    }
}
