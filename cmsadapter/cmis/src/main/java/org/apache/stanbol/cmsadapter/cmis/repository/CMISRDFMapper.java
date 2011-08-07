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
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RDFMapper} for CMIS repositories. While transforming annotated RDF data to
 * repository, this class first takes root objects, i.e object having no
 * {@link CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF} annotations. All children of root objects are created as
 * documents in the same folder with the top root object as CMIS allows setting a hierarchy with Folders only.<br>
 * <br>
 * For example, if an object has path /a/b/c/object path and has child objects as cobject1 and cobject2. First
 * a, b, c folders are tried to be created and then all three object are created in c folder. <br>
 * <br>
 * Custom properties to be mapped, child and parent annotations for a document are selected from the annotated
 * graph collected in a separate graph, serialized as RDF/XML and serialized RDF is set as
 * {@link ContentStream} of document.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class CMISRDFMapper implements RDFMapper {
    private static final Logger log = LoggerFactory.getLogger(CMISRDFMapper.class);

    private static final String DOCUMENT_RDF = "document_RDF";

    private static final String DOCUMENT_RDF_MIME_TYPE = "text/plain";

    @Reference
    Serializer serializer;

    @Override
    public void storeRDFinRepository(Object session, MGraph annotatedGraph) throws RDFBridgeException {
        List<NonLiteral> rootObjects = RDFBridgeHelper.getRootObjetsOfGraph(annotatedGraph);
        for (NonLiteral root : rootObjects) {
            String documentPath = RDFBridgeHelper.getResourceStringValue(root,
                CMSAdapterVocabulary.CMS_OBJECT_PATH, annotatedGraph);
            String documentName = RDFBridgeHelper.getResourceStringValue(root,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, annotatedGraph);
            Folder rootFolder = checkCreateParentNodes(documentPath, (Session) session);
            createDocument(rootFolder, root, documentName, annotatedGraph, (Session) session);
        }
    }

    private void createDocument(Folder parent,
                                NonLiteral documentURI,
                                String documentName,
                                MGraph graph,
                                Session session) throws RDFBridgeException {

        String documentPath;
        String parentPath = parent.getPath();
        if (parentPath.endsWith("/")) {
            documentPath = parentPath + documentName;
        } else {
            documentPath = parentPath + "/" + documentName;
        }
        Document d = null;
        CmisObject o = null;
        try {
            o = session.getObjectByPath(documentPath);
            if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
                d = (Document) o;
                d.setContentStream(getDocumentContentStream(documentURI, graph), true);
            } else {
                log.warn(
                    "Object having path: {} does not have Folder base type. It should have Folder base type to allow create documents in it",
                    documentPath);
                throw new RDFBridgeException("Existing object having path: " + documentPath
                                             + " which does not have Folder base type");
            }
        } catch (CmisObjectNotFoundException e) {
            log.debug("Object having path: {} does not exists, a new one will be created", documentPath);
            d = parent.createDocument(getProperties(BaseTypeId.CMIS_DOCUMENT.value(), documentName),
                getDocumentContentStream(documentURI, graph), VersioningState.NONE);
        }

        // create child objects of root object in the same folder with parent
        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, documentURI);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = RDFBridgeHelper.getResourceStringValue(childSubject,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
            createDocument(parent, childSubject, childName, graph, session);
        }
    }

    private ContentStream getDocumentContentStream(NonLiteral documentURI, MGraph graph) {
        MGraph documentMGraph = collectedDocumentResources(documentURI, graph);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, documentMGraph, SupportedFormat.RDF_XML);
        byte[] serializedGraph = baos.toByteArray();
        InputStream stream = new ByteArrayInputStream(baos.toByteArray());
        BigInteger length = new BigInteger(serializedGraph.length + "");
        ContentStream contentStream = new ContentStreamImpl(DOCUMENT_RDF, length, DOCUMENT_RDF_MIME_TYPE,
                stream);
        return contentStream;
    }

    private MGraph collectedDocumentResources(NonLiteral subject, MGraph graph) {
        MGraph documentMGraph = new SimpleMGraph();
        // put selected properties to the graph
        Iterator<Triple> it = graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_HAS_PROPERTY, null);
        while (it.hasNext()) {
            UriRef tempPropURI = new UriRef(RDFBridgeHelper.removeEndCharacters(it.next().getObject()
                    .toString()));
            UriRef propURI = RDFBridgeHelper.getResourceURIValue(tempPropURI,
                CMSAdapterVocabulary.CMS_OBJECT_PROPERTY_URI, graph);
            Iterator<Triple> propTriples = graph.filter(subject, propURI, null);
            while (propTriples.hasNext()) {
                documentMGraph.add(propTriples.next());
            }
        }
        // put selected children annotations to the graph
        // The process below may be improved by changing RDF annotation mechanism.
        it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, subject);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            Iterator<Triple> itt = graph.filter(subject, null, childSubject);
            if (itt.hasNext()) {
                documentMGraph.add(itt.next());
            }
        }

        // put parent annotations to the graph
        it = graph.filter(subject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, null);
        while (it.hasNext()) {
            documentMGraph.add(it.next());
        }
        return documentMGraph;
    }

    /**
     * Takes path of a root object in the annotated RDF and tries to check parent folders. If parent folders
     * do not exist, they are created.
     * 
     * @param nodePath
     *            path of a root object
     * @param session
     *            session to access repository
     * @return
     * @throws RDFBridgeException
     *             when another object which is not a folder in the specified path
     */
    private Folder checkCreateParentNodes(String nodePath, Session session) throws RDFBridgeException {
        Folder f = session.getRootFolder();
        String[] pathSections = nodePath.split("/");
        String currentPath = "/";
        for (int i = 1; i < pathSections.length - 1; i++) {
            String folderName = pathSections[i];
            currentPath += folderName;
            CmisObject o;
            try {
                o = session.getObjectByPath(currentPath);
                if (hasType(o, BaseTypeId.CMIS_FOLDER)) {
                    f = (Folder) o;
                    currentPath += "/";
                } else {
                    log.warn(
                        "Object having path: {} does not have Folder base type. It should have Folder base type to allow create documents in it",
                        currentPath);
                    throw new RDFBridgeException("Existing object having path: " + currentPath
                                                 + " which does not have Folder base type");
                }
            } catch (CmisObjectNotFoundException e) {
                log.debug("Object having path: {} does not exists, a new one will be created", currentPath);
                f = f.createFolder(getProperties(BaseTypeId.CMIS_FOLDER.value(), folderName));
            }
        }

        return f;
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
}
