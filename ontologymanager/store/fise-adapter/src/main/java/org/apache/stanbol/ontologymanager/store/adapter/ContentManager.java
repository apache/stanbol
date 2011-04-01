package org.apache.stanbol.ontologymanager.store.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.stanbol.ontologymanager.store.adapter.util.IOUtil;
import org.apache.stanbol.ontologymanager.store.adapter.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentManager.class.getName());

    private static final String CONTENT_INDEX_FILE = "content_index.properties";

    private Map<String,Triple<String,byte[],String>> contentMap = new HashMap<String,Triple<String,byte[],String>>();

    private Properties contentIndex = new Properties();

    private File contentFolder;

    public ContentManager(File contentFolder) {

        File indexFile;

        this.contentFolder = contentFolder;
        Thread.currentThread().getContextClassLoader();
        checkContentFolder();
        LOGGER.info("Content folder OK");

        indexFile = getIndexFile();
        LOGGER.info("Index file OK");

        loadIndexEntries(indexFile);
        LOGGER.info("Content loaded OK");

        printContentMap();

    }

    public void put(String id, Triple<String,byte[],String> triple) {
        IOUtil ioUtil = IOUtil.getInstance();
        UUID contentAlias = UUID.randomUUID();
        String contentType = triple.getEntry1();
        byte[] content = triple.getEntry2();
        String uri = triple.getEntry3();
        if (contentMap.get(id) != null) {
            LOGGER.warn("Overriding previous content for: " + id);
            delete(id);
        }

        contentIndex.put(id,
            new Triple<String,String,String>(contentType, contentAlias.toString(), uri).toString());

        File f = new File(contentFolder.getAbsolutePath() + "/" + contentAlias);
        try {
            f.createNewFile();
            ioUtil.writeBytesToFile(f, content);
        } catch (IOException e) {
            LOGGER.error("Unable to create file: " + f.getAbsolutePath());
            delete(id);
            return;
        }

        contentMap.put(id, new Triple<String,byte[],String>(contentType, content, uri));
        store();
    }

    public void delete(String id) {
        File contentFile = new File(contentFolder.getAbsolutePath() + "/"
                                    + Triple.createTriple(contentIndex.getProperty(id)).getEntry2());
        contentFile.delete();
        contentIndex.remove(id);
        contentMap.remove(id);
    }

    public Triple<String,byte[],String> getContent(String id) {
        return contentMap.get(id);
    }

    public void store() {
        IOUtil ioUtil = IOUtil.getInstance();
        try {
            contentIndex.store(new FileOutputStream(contentFolder.getAbsolutePath() + "/"
                                                    + CONTENT_INDEX_FILE), "Content Index");

            for (Entry<String,Triple<String,byte[],String>> item : contentMap.entrySet()) {
                String fileName = contentIndex.getProperty(item.getKey()).split(",")[1];

                File file = new File(contentFolder.getAbsolutePath() + "/" + fileName);
                if (!file.exists()) {
                    LOGGER.warn("Content file for " + item.getValue().getEntry1()
                                + " does not exist. Sycnhronizing ...");
                    ioUtil.writeBytesToFile(file, item.getValue().getEntry2());
                    LOGGER.info(". Sycnhronization Completed ...");
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Cannot find file", e);
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        }
    }

    private void loadIndexEntries(File indexFile) {
        IOUtil ioUtil = IOUtil.getInstance();
        List<Object> danglingItems = new ArrayList<Object>();

        // Read index porperties file
        try {
            contentIndex.load(new FileInputStream(indexFile));
        } catch (FileNotFoundException e) {
            LOGGER.error("Can not get index file");
        } catch (IOException e) {
            LOGGER.error("Can not get content of index file: " + e.getMessage());
        }

        // Populate in memory cache with real content-contentType-uri triples
        for (Entry<Object,Object> item : contentIndex.entrySet()) {
            Triple<String,String,String> metaTriple = Triple.createTriple((String) item.getValue());
            File contentFile = new File(contentFolder, metaTriple.getEntry2());
            byte[] content = null;
            try {
                content = ioUtil.getBytesFromFile(contentFile);
            } catch (IOException e) {
                contentMap.remove(item.getKey());
                danglingItems.add(item.getKey());
                continue;
            }
            contentMap.put((String) item.getKey(), new Triple<String,byte[],String>(metaTriple.getEntry1(),
                    content, metaTriple.getEntry3()));
        }

        // Remove dangling content entries
        for (Object di : danglingItems) {
            contentIndex.remove(di);
            LOGGER.info("Dangling Item Removed: " + di);
        }

    }

    private File getIndexFile() {
        File indexFile = new File(contentFolder.getAbsolutePath() + "/" + CONTENT_INDEX_FILE);
        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (IOException e) {
                LOGGER.error("Can not create index file: " + e.getMessage());
                return null;
            }
        }
        return indexFile;
    }

    private void checkContentFolder() {
        if (!contentFolder.exists()) {
            contentFolder.mkdir();
        } else if (contentFolder.exists() && contentFolder.isFile()) {
            contentFolder.delete();
            contentFolder.mkdir();
        }
    }

    private void printContentMap() {
        LOGGER.info("Content Items Dump");
        for (Entry<String,Triple<String,byte[],String>> entry : this.contentMap.entrySet()) {
            LOGGER.info("Item: " + entry.getKey() + "," + entry.getValue().getEntry1() + "  "
                        + entry.getValue().getEntry2().length);
        }

    }
}
