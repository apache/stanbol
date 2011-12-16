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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.cmis.utils.CMISUtils;
import org.apache.stanbol.cmsadapter.core.mapping.BaseRDFMapper;
import org.apache.stanbol.cmsadapter.core.mapping.RDFBridgeHelper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.NamespaceEnum;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link RDFMapper} for CMIS repositories.
 * <p>
 * While updating the content repository based on an RDF data, this implementation creates an additional
 * document containing metadata for each CMS object processed as CMIS specification does not allow adding
 * custom properties for content repository objects. For detailed explanation about updating content
 * repository based on RDF data see {@link #storeRDFinRepository(Object, MGraph)}.
 * <p>
 * While generating RDF from content repository, this implementation process all content repository object
 * located under a given path. All properties of objects are transformed into RDF. Furthermore, hierarchical
 * structure of the content repository is reflected to the generated RDF. For more detailed explanation see
 * {@link #generateRDFFromRepository(Object, String)}.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class CMISRDFMapper extends BaseRDFMapper implements RDFMapper {
    private static final Logger log = LoggerFactory.getLogger(CMISRDFMapper.class);

    private static final String DOCUMENT_RDF_MIME_TYPE = "text/plain";

    @Reference
    Serializer serializer;

    @Reference
    Parser parser;

    /**
     * This implementation of {@link RDFMapper#storeRDFinRepository(Object, MGraph)} realizes a workaround to
     * come up with the restriction of not being able to set custom properties to content repository objects.
     * <p>
     * For each resource having {@link CMSAdapterVocabulary#CMS_OBJECT} as rdf:type in the RDF, an additional
     * document containing all resources about a certain content repository object is created in the same
     * folder with the actual object. For example, if a single object named <b>MyObject</b> is assumed to be
     * created from the annotated RDF data in the content repository, an additional document named
     * <b>MyObject_metadata</b> will be created.
     * <p>
     * The type of the object to be created is determined according to assertions in the RDF. More details can
     * be found in
     * {@link #createStructureForDocument(String, NonLiteral, NonLiteral, Folder, Session, MGraph)}.
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
                createObject(rootFolder, root, null, documentName, annotatedGraph, (Session) session);
            } else {
                log.warn("Failed to get Folder for path: {}", documentPath);
            }
        }
    }

    private void createObject(Folder parent,
                              NonLiteral documentURI,
                              NonLiteral parentURI,
                              String documentName,
                              MGraph graph,
                              Session session) {

        Folder containerFolder = createStructureForDocument(documentName, documentURI, parentURI, parent,
            session, graph);

        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, documentURI);
        while (it.hasNext()) {
            NonLiteral childSubject = it.next().getSubject();
            String childName = RDFBridgeHelper.getResourceStringValue(childSubject,
                CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
            createObject(containerFolder, childSubject, documentURI, childName, graph, session);
        }
        // Log level may be set as 'debug'
        log.info(String.format("Created object: %s,  Object parent: %s", documentName, parent.getPath()));
    }

    /**
     * This method creates the actual object in the content repository. The type of the object to be created
     * is determined by following conditions:
     * <p>
     * First if its base type is set by {@link CMSAdapterVocabulary#CMIS_BASE_TYPE_ID} predicate and it is set
     * as <b>cmis:folder</b> or <b>cmis:document</b>.
     * <p>
     * If the base type of the object is not set, its parent assertion is checked. If it has a parent
     * assertion through {@link CMSAdapterVocabulary#CMS_OBJECT_PARENT_REF}, is rdf:type assertion is checked.
     * If the URI specified in <code>parentURI</code> is one of the <b>rdf:type</b>s of processed object. The
     * object is created as a {@link Document} under the folder specified by <code>parentFolder</code>.
     * Otherwise, it is created as a {@link Folder} even if it does not have any children. This assumption is
     * done based on the idea that the created object is a part of a hierarchy.
     * <p>
     * If the parent assertion of the processed object is not set, this means that a root object is being
     * created. If this object has any child objects it is created as a {@link Folder}, otherwise it is
     * created as a {@link Document} based on the assumption that the object represents a single
     * {@link Document}.
     * <p>
     * For any object created an additional metadata document is created. This document has the name <b>
     * <code>objectName + {@link #RDF_METADATA_DOCUMENT_EXTENSION}</code></b>. This document is created so
     * that content management systems would manage semantic information content repository object within
     * their own systems.
     * 
     * @param objectName
     * @param documentURI
     * @param parentURI
     * @param parentFolder
     * @param session
     * @param graph
     * @return
     */
    private Folder createStructureForDocument(String objectName,
                                              NonLiteral documentURI,
                                              NonLiteral parentURI,
                                              Folder parentFolder,
                                              Session session,
                                              MGraph graph) {

        String objectPath;
        String parentPath = parentFolder.getPath();
        boolean createFolder = true;
        if (parentPath.endsWith("/")) {
            objectPath = parentPath + objectName;
        } else {
            objectPath = parentPath + "/" + objectName;
        }

        // determine whether a document or a folder will be created
        @SuppressWarnings("rawtypes")
        Class type = hasBaseType(documentURI, graph);
        if (type != null) {
            if (type.equals(Document.class)) {
                createFolder = false;
            }
        } else {
            if (parentURI == null) {
                // root object is being created
                if (!hasChildren(documentURI, graph)) {
                    createFolder = false;
                }
            } else {
                if (hasParentAsType(documentURI, parentURI, graph)) {
                    createFolder = false;
                }
            }
        }

        Folder createdFolder = null;
        CmisObject createdObject;
        if (createFolder) {
            createdFolder = createFolderByPath(parentFolder, objectName, objectPath, session);
            createdObject = createdFolder;
        } else {
            createdObject = createDocumentByPath(parentFolder, objectName, objectPath, null, session);
        }

        String rdfDocumentName = objectName + CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION;
        String rdfDocumentPath = objectPath + CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION;

        createDocumentByPath(parentFolder, rdfDocumentName, rdfDocumentPath,
            createDocumentContentStream(createdObject, rdfDocumentName, documentURI, graph), session);
        return createdFolder;
    }

    @SuppressWarnings("rawtypes")
    private Class hasBaseType(NonLiteral uri, MGraph graph) {
        Iterator<Triple> it = graph.filter(uri, CMSAdapterVocabulary.CMIS_BASE_TYPE_ID, null);
        if (it.hasNext()) {
            String type = RDFBridgeHelper.parseStringValue(it.next().getObject().toString());
            if (type.contentEquals(BaseTypeId.CMIS_FOLDER.value())) {
                return Folder.class;
            } else if (type.contentEquals(BaseTypeId.CMIS_DOCUMENT.value())) {
                return Document.class;
            } else {
                log.warn("Base type: {} is not supported yet", type);
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean hasChildren(NonLiteral uri, MGraph graph) {
        Iterator<Triple> it = graph.filter(null, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, uri);
        return it.hasNext();
    }

    private boolean hasParentAsType(NonLiteral uri, NonLiteral parentURI, MGraph graph) {
        Iterator<Triple> it = graph.filter(uri, RDFBridgeHelper.RDF_TYPE, parentURI);
        return it.hasNext();
    }

    private ContentStream createDocumentContentStream(CmisObject createdObject,
                                                      String documentName,
                                                      NonLiteral documentURI,
                                                      MGraph graph) {
        MGraph documentMGraph = collectDocumentResources(createdObject, documentURI, graph);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, documentMGraph, SupportedFormat.RDF_XML);
        byte[] serializedGraph = baos.toByteArray();
        InputStream stream = new ByteArrayInputStream(baos.toByteArray());
        BigInteger length = new BigInteger(serializedGraph.length + "");
        ContentStream contentStream = new ContentStreamImpl(documentName, length, DOCUMENT_RDF_MIME_TYPE,
                stream);
        return contentStream;
    }

    private MGraph collectDocumentResources(CmisObject createdObject, NonLiteral subject, MGraph graph) {
        boolean sameObject = true;
        if (createdObject.getId().contentEquals(RDFBridgeHelper.removeEndCharacters(subject.toString()))) {
            sameObject = false;
        }
        MGraph documentMGraph = new SimpleMGraph();
        Iterator<Triple> it = graph.filter(subject, null, null);
        while (it.hasNext()) {
            Triple t = it.next();
            if (sameObject) {
                UriRef pURI = t.getPredicate();
                if (pURI.toString().contains(NamespaceEnum.cmis.getNamespace())) {
                    continue;
                }
            }
            documentMGraph.add(t);
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
                if (contentStream != null) {
                    d.setContentStream(contentStream, true);
                }
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
    public MGraph generateRDFFromRepository(String baseURI, Object session, String rootPath) throws RDFBridgeException {
        MGraph cmsGraph = new SimpleMGraph();
        Session cmisSession = (Session) session;

        Iterator<CmisObject> cmisObjectIt;
        CmisObject rootObject;
        try {
            rootObject = cmisSession.getObjectByPath(rootPath);
        } catch (CmisObjectNotFoundException e) {
            throw new RDFBridgeException(String.format("There is Cmis Object in the path: %s", rootPath), e);
        }
        if (hasType(rootObject, BaseTypeId.CMIS_FOLDER)) {
            cmisObjectIt = ((Folder) rootObject).getChildren().iterator();
        } else {
            throw new RDFBridgeException(String.format("A folder object is expected at the path: %s",
                rootPath));
        }

        while (cmisObjectIt.hasNext()) {
            CmisObject o = cmisObjectIt.next();
            cmsGraph.addAll(getGraphForObject(baseURI, o, (Folder) rootObject, null));
        }
        return cmsGraph;
    }

    private MGraph getGraphForObject(String baseURI, CmisObject o, Folder parentFolder, NonLiteral parentURI) {
        MGraph graph = new SimpleMGraph();
        // check metadata
        if (o.getName().endsWith(CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION)) {
            return graph;
        }

        MGraph metadata = new SimpleMGraph();
        metadata = checkMetadata(parentFolder, o);

        // create CMS Object annotation
        NonLiteral subject = getObjectURI(baseURI, o, metadata);
        graph.add(new TripleImpl(subject, RDFBridgeHelper.RDF_TYPE, CMSAdapterVocabulary.CMS_OBJECT));

        // add parent assertion
        if (parentURI != null) {
            graph.add(new TripleImpl(subject, CMSAdapterVocabulary.CMS_OBJECT_PARENT_REF, parentURI));
        }

        if (hasType(o, BaseTypeId.CMIS_FOLDER)) {
            Folder f = (Folder) o;
            putObjectPropertiesIntoGraph(f, subject, metadata, graph);

            // process children
            Iterator<CmisObject> childIt = f.getChildren().iterator();
            while (childIt.hasNext()) {
                CmisObject child = childIt.next();
                graph.addAll(getGraphForObject(baseURI, child, f, subject));
            }
        } else if (hasType(o, BaseTypeId.CMIS_DOCUMENT)) {
            putObjectPropertiesIntoGraph(o, subject, metadata, graph);
            if (parentURI != null) {
                graph.add(new TripleImpl(subject, RDFBridgeHelper.RDF_TYPE, parentURI));
            }
        }
        return graph;
    }

    private void putObjectPropertiesIntoGraph(CmisObject o, NonLiteral subject, MGraph metadata, MGraph g) {
        g.addAll(metadata);
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        List<Property<?>> docProps = o.getProperties();
        for (Property<?> p : docProps) {
            PropertyType t = p.getType();
            UriRef pURI = getPropertyURI(p);
            if (pURI == null) {
                continue;
            }

            List<Object> values = new ArrayList<Object>();
            if (p.isMultiValued()) {
                values.addAll(CMISUtils.getTypedPropertyValues(t, p.getValues()));
            } else {
                values.add(CMISUtils.getTypedPropertyValue(t, p.getValue()));
            }

            for (Object val : values) {
                if (val != null) {
                    if (val instanceof UriRef) {
                        g.add(new TripleImpl(subject, pURI, (UriRef) val));
                    } else {
                        g.add(new TripleImpl(subject, pURI, literalFactory.createTypedLiteral(val)));
                    }
                }
            }
        }
        /*
         * TODO handle multiple paths both transforming an RDF into repository and generating an RDF from
         * repository
         */
        String path = "";
        if (o instanceof Folder) {
            path = ((Folder) o).getPath();
        } else if (o instanceof Document) {
            path = ((Document) o).getPaths().get(0);
        }
        RDFBridgeHelper.createDefaultPropertiesForRDF(subject, g, path, o.getName());
    }

    private NonLiteral getObjectURI(String baseURI, CmisObject o, MGraph metadata) {
        Iterator<Triple> it = metadata.filter(null, null, null);
        if (it.hasNext()) {
            return it.next().getSubject();
        }

        String nodeURI = RDFBridgeHelper.appendLocalName(baseURI, o.getId());
        return new UriRef(nodeURI);
    }

    private static UriRef getPropertyURI(Property<?> p) {
        String name = p.getQueryName();
        if (!name.contains(":")) {
            name = NamespaceEnum.cms.getPrefix() + ":" + name;
        }
        if (RDFBridgeHelper.isShortNameResolvable(name)) {
            return new UriRef(NamespaceEnum.getFullName(name));
        } else {
            log.warn("Failed to resolve property: {}", name);
            return null;
        }
    }

    private MGraph checkMetadata(Folder parentFolder, CmisObject object) {
        MGraph metadata = new SimpleMGraph();
        Iterator<CmisObject> it = parentFolder.getChildren().iterator();
        while (it.hasNext()) {
            CmisObject o = it.next();
            if (o instanceof Document) {
                if (o.getName().contentEquals(object.getName() + CMISUtils.RDF_METADATA_DOCUMENT_EXTENSION)) {
                    ContentStream cs = ((Document) o).getContentStream();
                    parser.parse(metadata, cs.getStream(), SupportedFormat.RDF_XML);
                }
            }
        }
        return metadata;
    }

    @Override
    public boolean canMapWith(Object session) {
        return session instanceof Session;
    }
}
