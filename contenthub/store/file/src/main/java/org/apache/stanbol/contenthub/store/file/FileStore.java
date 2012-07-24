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
package org.apache.stanbol.contenthub.store.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.semanticindex.core.store.ChangeSetImpl;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.store.file.serializer.ContentPartDeserializer;
import org.apache.stanbol.contenthub.store.file.serializer.ContentPartSerializer;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is the file based implementation of {@link Store} interface. It provides storage, retrieval and
 * deletion functionalities for {@link ContentItem}s . All {@link ContentItem}s are stored in a single
 * directory as zip files. {@link ContentItem} parts are serialized into the zip file if there are registered
 * {@link ContentPartSerializer}s for the specific types of content parts. Similarly, to be able to
 * deserialize the content parts from the zip, there must be registered {@link ContentPartDeserializer}s
 * suitable with the types of the parts.
 * </p>
 * <p>
 * Revisions of {@link ContentItem}s submitted to this store is managed through the
 * {@link FileRevisionManager}. Once a document added, updated or deleted the revision of the
 * {@link ContentItem} is set as {@link System#currentTimeMillis()}.
 * </p>
 * <p>
 * To be able to populate the HTML interface, this implementation also provides additional metadata regarding
 * the {@link ContentItem}s. Additional metadata is also stored in the Apache Derby database.
 * </p>
 * 
 * To be able to use other {@link Store} implementations rather than this, the
 * {@link Constants#SERVICE_RANKING} property of other implementations should be set higher than of this one.
 * 
 * @author suat
 * @author meric
 * 
 */
@Component(immediate = true)
@Service
@Properties(value = {@Property(name = Constants.SERVICE_RANKING, intValue = 100)})
public class FileStore implements Store<ContentItem> {
    // @Property(name = Constants.SERVICE_RANKING)
    // private int ranking;

    public static final String RECENTLY_ENHANCED_TABLE_NAME = "recently_enhanced_content_items";

    public static final UriRef CONSTRAINTS_URI = new UriRef("org.apache.stanbol.contenthub.constraints");

    public static final UriRef HTMLMETADATA_URI = new UriRef("org.apache.stanbol.contenthub.htmlmetadata");

    public static final String FIELD_MIME_TYPE = "mimetype";

    public static final String FIELD_ENHANCEMENT_COUNT = "enhancementCount";

    public static final String FIELD_TITLE = "title";

    public static final String FIELD_ID = "id";

    public static final String FILE_STORE_FOLDER_PATH = "datafiles/contenthub";

    public static final String FILE_STORE_NAME = "filestore";

    private static final String SELECT_RECENTLY_ENHANCED_ITEMS = "SELECT t1.id, mimeType, enhancementCount, title FROM "
                                                                 + FileRevisionManager.REVISION_TABLE_NAME
                                                                 + " t1, "
                                                                 + RECENTLY_ENHANCED_TABLE_NAME
                                                                 + " t2 WHERE"
                                                                 + " t1.id=t2."
                                                                 + FIELD_ID
                                                                 + " ORDER BY revision DESC"
                                                                 + " OFFSET ? ROWS";

    private static final String SELECT_RECENTLY_ENHANCED_ITEM = "SELECT " + FIELD_ID + " FROM "
                                                                + RECENTLY_ENHANCED_TABLE_NAME + " WHERE "
                                                                + FIELD_ID + "= ?";

    private static final String INSERT_RECENTLY_ENHANCED_ITEM = "INSERT INTO " + RECENTLY_ENHANCED_TABLE_NAME
                                                                + " (" + FIELD_ID + "," + FIELD_TITLE + ","
                                                                + FIELD_MIME_TYPE + ","
                                                                + FIELD_ENHANCEMENT_COUNT + ") VALUES "
                                                                + " (?,?,?,?)";

    private static final String UPDATE_RECENTLY_ENHANCED_ITEM = "UPDATE " + RECENTLY_ENHANCED_TABLE_NAME
                                                                + " SET " + FIELD_MIME_TYPE + "=?, "
                                                                + FIELD_TITLE + "=?,"
                                                                + FIELD_ENHANCEMENT_COUNT + "=? WHERE "
                                                                + FIELD_ID + "=?";

    private static final String REMOVE_RECENTLY_ENHANCED_ITEM = "DELETE FROM " + RECENTLY_ENHANCED_TABLE_NAME
                                                                + " WHERE " + FIELD_ID + "=?";

    private final Logger log = LoggerFactory.getLogger(FileStore.class);

    private File storeFolder;

    @Reference
    ContentItemFactory contentItemFactory;

    @Reference
    FileRevisionManager revisionManager;

    @Reference
    FileStoreDBManager dbManager;

    @Reference
    private ContentPartSerializer contentPartSerializer;

    @Reference
    private ContentPartDeserializer contentPartDeserializer;

    @Reference
    private EnhancementJobManager jobManager;

    @Activate
    protected void activate(ComponentContext componentContext) throws StoreException {
        // check store folder
        String stanbolHome = componentContext.getBundleContext().getProperty("sling.home");
        storeFolder = new File(stanbolHome + "/" + FILE_STORE_FOLDER_PATH + "/" + FILE_STORE_NAME);
        if (!storeFolder.exists()) {
            storeFolder.mkdirs();
        }
    }

    @Override
    public Class<ContentItem> getItemType() {
        return ContentItem.class;
    }

    @Override
    public ContentItem remove(String id) throws StoreException {
        checkStoreFolder();
        String urlEncodedId = encodeId(id);
        File f = new File(storeFolder.getPath() + "/" + urlEncodedId + ".zip");
        ContentItem ci = null;
        if (f.exists()) {
            ci = get(id);
            f.delete();
            updateTablesForDelete(id);
        } else {
            log.warn("There is no file corresponding to the id: {}", id);
        }
        return ci;
    }

    private void updateTablesForDelete(String id) throws StoreException {
        // update revision
        revisionManager.updateRevision(id);
        // update recently_enhanced table
        removeFromRecentlyEnhancedTable(id);
    }

    private void removeFromRecentlyEnhancedTable(String contentItemID) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();

        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(REMOVE_RECENTLY_ENHANCED_ITEM);
            ps.setString(1, contentItemID);
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
        } catch (SQLException e) {
            log.error("Failed to execute query", e);
            throw new StoreException("Failed to execute query", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    @Override
    public String put(ContentItem ci) throws StoreException {
        try {
            jobManager.enhanceContent(ci);
        } catch (EnhancementException e) {
            throw new StoreException(String.format("Failed to enhance given content item with URI: %s",
                ci.getUri()), e);
        }

        long enhancementCount = getEnhancementCount(ci);
        JSONObject htmlMetadata = getHTMLMetadata(ci);
        htmlMetadata = populateHTMLMetadataPart(ci, htmlMetadata, enhancementCount);
        updateTablesForPut(htmlMetadata);
        archiveContentItem(ci);
        return ci.getUri().getUnicodeString();
    }

    private long getEnhancementCount(ContentItem ci) {
        long enhancementCount = 0;
        Iterator<Triple> it = ci.getMetadata().filter(null,
            org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM,
            new UriRef(ci.getUri().getUnicodeString()));
        while (it.hasNext()) {
            it.next();
            enhancementCount++;
        }
        return enhancementCount;
    }

    private JSONObject getHTMLMetadata(ContentItem ci) throws StoreException {
        JSONObject htmlMetadata;
        Blob htmlMetadataPart;
        try {
            htmlMetadataPart = ci.getPart(HTMLMETADATA_URI, Blob.class);
            String jsonString = IOUtils.toString(htmlMetadataPart.getStream());
            htmlMetadata = new JSONObject(jsonString);
        } catch (NoSuchPartException e) {
            // there is no HTML metadata part yet
            htmlMetadata = new JSONObject();
        } catch (IOException e) {
            throw new StoreException("Failed to read HTML metadata part", e);
        } catch (JSONException e) {
            throw new StoreException("Failed to construct JSONObject from the HTML metadata part", e);
        }
        return htmlMetadata;
    }

    private JSONObject populateHTMLMetadataPart(ContentItem ci, JSONObject htmlMetadata, long enhancementCount) throws StoreException {

        // populate HTML metadata
        try {
            htmlMetadata.put(FIELD_ENHANCEMENT_COUNT, enhancementCount);
            htmlMetadata.put(FIELD_MIME_TYPE, ci.getMimeType());
            htmlMetadata.put(FIELD_ID, ci.getUri().getUnicodeString());
        } catch (JSONException e) {
            throw new StoreException("Failed to create HTML metadata part", e);
        }

        // attach the populated part to content item
        try {
            Blob htmlMetadataPart = contentItemFactory.createBlob(new StringSource(htmlMetadata.toString()));
            ci.addPart(HTMLMETADATA_URI, htmlMetadataPart);
        } catch (IOException e) {
            throw new StoreException("Failed to create blob for HTML part", e);
        }
        return htmlMetadata;
    }

    private void updateTablesForPut(JSONObject htmlMetadata) throws StoreException {
        // update revision
        String title = "";
        try {
            title = htmlMetadata.getString(FIELD_TITLE);
        } catch (JSONException e) {
            // ignore the exception
        }
        try {
            revisionManager.updateRevision(htmlMetadata.getString(FIELD_ID));
            updateRecentlyEnhancedItem(htmlMetadata.getString(FIELD_ID), title,
                htmlMetadata.getString(FIELD_MIME_TYPE), htmlMetadata.getLong(FIELD_ENHANCEMENT_COUNT));
        } catch (JSONException e) {
            throw new StoreException("Failed to read HTML metadata of content item", e);
        }
    }

    private void updateRecentlyEnhancedItem(String contentItemID,
                                            String title,
                                            String mimeType,
                                            long enhancementCount) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        try {
            ps = con.prepareStatement(SELECT_RECENTLY_ENHANCED_ITEM);
            ps.setString(1, contentItemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
            }

        } catch (SQLException e) {
            dbManager.closeConnection(con);
            log.error("Failed to query information of content item", e);
            throw new StoreException("Failed to query information of content item", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
        }

        // update the table
        try {
            if (!recordExist) {
                log.debug("Content item: {} will be added to recently_enhanced table", contentItemID);
                ps = con.prepareStatement(INSERT_RECENTLY_ENHANCED_ITEM);
                ps.setString(1, contentItemID);
                ps.setString(2, title);
                ps.setString(3, mimeType);
                ps.setLong(4, enhancementCount);
            } else {
                log.debug("Content item: {} will be updated in recently_enhanced table", contentItemID);
                ps = con.prepareStatement(UPDATE_RECENTLY_ENHANCED_ITEM);
                ps.setString(1, mimeType);
                ps.setString(2, title);
                ps.setLong(3, enhancementCount);
                ps.setString(4, contentItemID);
            }
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
        } catch (SQLException e) {
            log.error("Failed to update recently_enhanced table", e);
            throw new StoreException("Failed to update recently_enhanced table", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    private void archiveContentItem(ContentItem ci) throws StoreException {
        String fileName = encodeId(ci.getUri().getUnicodeString());
        File file = new File(storeFolder.getPath() + "/" + fileName + ".zip");
        OutputStream os = null;
        ZipOutputStream zos = null;

        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            os = new FileOutputStream(file);
            zos = new ZipOutputStream(os);
        } catch (IOException e) {
            cleanUp(file, zos, os);
            throw new StoreException("Failed to initiate the ZipOutputStream", e);
        }

        JSONObject header = createHeader(ci);
        populateZipStream(file, os, zos, header, ci);

        try {
            zos.finish();
            zos.close();
            os.close();
        } catch (IOException e) {
            throw new StoreException("Failed to finalize the ZipOutputStream", e);
        }
    }

    private void populateZipStream(File file,
                                   OutputStream os,
                                   ZipOutputStream zos,
                                   JSONObject header,
                                   ContentItem ci) throws StoreException {
        ZipEntry entry = null;
        try {
            // add "header" to archive
            entry = new ZipEntry("header");
            zos.putNextEntry(entry);
            zos.write(header.toString().getBytes());
            zos.closeEntry();
        } catch (IOException e) {
            cleanUp(file, zos, os);
            throw new StoreException("Failed to write header to the ZipOutPutStream", e);
        }

        // add "metadata" to archive
        entry = new ZipEntry("metadata");
        try {
            zos.putNextEntry(entry);
            contentPartSerializer.serializeContentPart(zos, ci.getMetadata());
            zos.closeEntry();
        } catch (IOException e) {
            cleanUp(file, zos, os);
            throw new StoreException("Failed to write content item metadata to ZipOutputStream", e);
        }

        try {
            int contentPartNum = header.getInt("contentpartnum");
            for (int jsonIndex = 0; jsonIndex < contentPartNum; jsonIndex++) {
                JSONObject fields = (JSONObject) header.get(Integer.toString(jsonIndex));
                String uri = fields.get("uri").toString();
                String className = fields.get("superclass").toString();
                Class<?> klass;
                try {
                    klass = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to obtain class for class name: {}", className, e);
                    continue;
                }
                entry = new ZipEntry(encodeId(uri));
                try {
                    zos.putNextEntry(entry);
                    contentPartSerializer.serializeContentPart(zos, ci.getPart(new UriRef(uri), klass));
                    zos.closeEntry();
                } catch (IOException e) {
                    cleanUp(file, zos, os);
                    throw new StoreException("Failed to write the entry to the ZipOutputStream for: " + uri,
                            e);
                } catch (RuntimeException e) {
                    // for clean up purposes
                    cleanUp(file, zos, os);
                    throw e;
                }
            }

        } catch (JSONException e) {
            cleanUp(file, zos, os);
            throw new StoreException("Failed to read part information from the header", e);
        }
    }

    private void cleanUp(File file, ZipOutputStream zos, OutputStream os) {
        if (zos != null) {
            try {
                zos.finish();
                zos.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
        }
        file.delete();
    }

    private JSONObject createHeader(ContentItem ci) throws StoreException {
        JSONObject header = new JSONObject();
        JSONObject item = null;
        Set<Class<?>> serializablePartTypes = contentPartSerializer.getSerializableTypes();
        // eliminate subclasses not to obtain same content parts again
        Class<?>[] serializablePartTypesArray = new Class<?>[serializablePartTypes.size()];
        serializablePartTypes.toArray(serializablePartTypesArray);
        Set<Class<?>> toBeRemoved = new HashSet<Class<?>>();
        for (int i = 0; i < serializablePartTypesArray.length; i++) {
            for (int j = i + 1; j < serializablePartTypesArray.length; j++) {
                if (serializablePartTypesArray[j].isAssignableFrom(serializablePartTypesArray[i])) {
                    toBeRemoved.add(serializablePartTypesArray[i]);
                }
            }
        }
        for (Class<?> klass : toBeRemoved) {
            serializablePartTypes.remove(klass);
        }
        // add parts to header
        int i = 0;
        try {
            String mainBlobUri = ci.getPartUri(0).getUnicodeString();

            // append all parts of content item
            for (Class<?> klass : serializablePartTypes) {
                LinkedHashMap<UriRef,?> contentParts = ContentItemHelper.getContentParts(ci, klass);
                for (Entry<UriRef,?> contentPartEntry : contentParts.entrySet()) {
                    Object contentPart = contentPartEntry.getValue();
                    String uri = contentPartEntry.getKey().getUnicodeString();
                    item = new JSONObject();
                    item.put("class", contentPart.getClass().getName());
                    item.put("superclass", klass.getName());
                    item.put("uri", uri);
                    if (contentPart instanceof Blob) {
                        item.put("mimetype", ((Blob) contentPart).getMimeType());
                        // item.put("parameters", new JSONObject(((Blob) contentPart).getParameter()));
                        if (uri.equals(mainBlobUri)) {
                            header.put("mainblobindex", Integer.toString(i));
                        }
                    }
                    header.put(Integer.toString(i++), item);
                }
            }
            header.put("contentpartnum", i);

        } catch (JSONException e) {
            throw new StoreException("Failed to populate header with content parts metadata", e);
        }
        return header;
    }

    @Override
    public ContentItem get(String id) throws StoreException {
        // get the zip file
        String fileName = encodeId(id);
        File file = new File(storeFolder.getPath() + "/" + fileName + ".zip");
        if (!file.exists()) {
            log.info("Failed to get file for the given id: {}", id);
            return null;
        }

        ZipFile zipFile;
        try {
            zipFile = new ZipFile(file);
        } catch (ZipException e) {
            throw new StoreException(String.format("Failed to get file for the given id: %s", id), e);
        } catch (IOException e) {
            throw new StoreException(String.format("Failed to get file for the given id: %s", id), e);
        }

        // deserialize headers
        JSONObject header;
        try {
            InputStream is = zipFile.getInputStream(zipFile.getEntry("header"));
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);
            String headerString = writer.toString();
            header = new JSONObject(headerString);
        } catch (IOException e) {
            throw new StoreException(String.format(
                "Failed to get header entry from the zip for of the content item: %s", id), e);
        } catch (JSONException e) {
            throw new StoreException(String.format(
                "Failed to get header entry from the zip for of the content item: %s", id), e);
        }

        // deserialize metadata
        MGraph metadata;
        try {
            metadata = contentPartDeserializer.deserializeContentPart(
                zipFile.getInputStream(zipFile.getEntry("metadata")), IndexedMGraph.class);
        } catch (IOException e) {
            throw new StoreException(String.format(
                "Failed to get metadata from the zip of the content item %s", e));
        }

        // deserialize main blob
        String mainBlobUri;
        StreamSource blobSource;
        try {
            String mainBlobIndex = header.getString("mainblobindex");
            JSONObject mainBlobMetadata = header.getJSONObject(mainBlobIndex);
            mainBlobUri = mainBlobMetadata.getString("uri");
            blobSource = new StreamSource(zipFile.getInputStream(zipFile.getEntry(encodeId(mainBlobUri))),
                    mainBlobMetadata.getString("mimetype"));
        } catch (JSONException e) {
            throw new StoreException(String.format(
                "Failed to obtain main blob from the zip of the content item: %s", id), e);
        } catch (IOException e) {
            throw new StoreException(String.format(
                "Failed to obtain main blob from the zip of the content item: %s", id), e);
        }

        // create content item using the metadata and main blob
        ContentItem ci;
        try {
            ci = contentItemFactory.createContentItem(id, blobSource, metadata);
        } catch (IOException e) {
            throw new StoreException("Failed to created the content item", e);
        }

        // deserialize other parts
        int contentPartNum;
        try {
            contentPartNum = header.getInt("contentpartnum");
        } catch (JSONException e) {
            throw new StoreException("Failed to get the number of content parts from the header", e);
        }

        for (int i = 0; i < contentPartNum; i++) {
            Object contentPart;
            try {
                JSONObject contentPartMetadata = header.getJSONObject(Integer.toString(i));
                String partUri = contentPartMetadata.getString("uri");
                if (!partUri.equals(mainBlobUri)) {
                    contentPart = deserializeContentPart(ci, contentPartMetadata, zipFile);
                    ci.addPart(new UriRef(partUri), contentPart);
                }
            } catch (JSONException e) {
                throw new StoreException("Failed get content part metadata", e);
            }
        }

        return ci;
    }

    /**
     * This method tries to deserialize content part whose metadata is passed in
     * <code>contentPartMetadata</code> object. During the deserialization process, it first tries to specific
     * type of the part. If the operation is unsuccessful, it tries the associated class which was used when
     * the content part is obtained from the {@link ContentItem}.
     * 
     */
    private <T> T deserializeContentPart(ContentItem contentItem,
                                         JSONObject contentPartMetadata,
                                         ZipFile zipFile) throws StoreException {
        String partType;
        String partUri;
        String partSuperType;
        String partMimeType;
        T contentPart;

        try {
            partType = contentPartMetadata.getString("class");
            partUri = contentPartMetadata.getString("uri");
            partSuperType = contentPartMetadata.getString("superclass");
            try {
                partMimeType = contentPartMetadata.getString("metadata");
            } catch (JSONException e1) {
                partMimeType = null;
            }

        } catch (JSONException e) {
            throw new StoreException("Failed to read content part metadata from the header", e);
        }

        InputStream partStream;
        try {
            partStream = zipFile.getInputStream(zipFile.getEntry(encodeId(partUri)));
        } catch (IOException e) {
            throw new StoreException(String.format(
                "Failed to retrive the entry named %s from the zip of the content item: %s",
                encodeId(partUri), contentItem.getUri().getUnicodeString()), e);
        }
        try {
            contentPart = contentPartDeserializer.deserializeContentPart(partStream, partType, partMimeType);
        } catch (StoreException e) {
            log.warn(
                "Failed to deserialize main blob for the class: {}. Trying to serialize with the superclass: {}",
                partType, partSuperType);
            contentPart = contentPartDeserializer.deserializeContentPart(partStream, partSuperType,
                partMimeType);
        }

        return contentPart;
    }

    @Override
    public ChangeSet<ContentItem> changes(long revision, int batchSize) throws StoreException {
        ChangeSetImpl<ContentItem> changesSet = (ChangeSetImpl<ContentItem>) revisionManager.getChanges(
            revision, batchSize);
        changesSet.setStore(this);
        return changesSet;
    }

    private void checkStoreFolder() throws StoreException {
        if (!storeFolder.exists()) {
            throw new StoreException("Store folder does not exist");
        }
    }

    private String encodeId(String id) throws StoreException {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode id. {}", id, e);
            throw new StoreException("Failed to encode id: " + id, e);
        }
    }

    public List<JSONObject> getRecentlyEnhancedItems(int limit, int offset) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();

        PreparedStatement ps = null;
        ResultSet rs = null;
        List<JSONObject> recentlyEnhancedList = new ArrayList<JSONObject>();
        try {
            ps = con.prepareStatement(SELECT_RECENTLY_ENHANCED_ITEMS);
            ps.setMaxRows(limit);
            ps.setInt(1, offset);
            rs = ps.executeQuery();

            JSONObject htmlData;
            while (rs.next()) {
                htmlData = new JSONObject();
                htmlData.put(FIELD_ID, rs.getString(FIELD_ID));
                htmlData.put(FIELD_MIME_TYPE, rs.getString(FIELD_MIME_TYPE));
                htmlData.put(FIELD_TITLE, rs.getString(FIELD_TITLE));
                htmlData.put(FIELD_ENHANCEMENT_COUNT, rs.getInt(3));
                recentlyEnhancedList.add(htmlData);
            }
        } catch (SQLException e) {
            throw new StoreException("Failed to get recently enhanced items", e);
        } catch (JSONException e) {
            throw new StoreException("Failed to get recently enhanced items", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }

        return recentlyEnhancedList;
    }
}
