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
package org.apache.stanbol.enhancer.contentitem.file;

import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.DEFAULT_CONTENT_ITEM_PREFIX;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.SHA1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.ContentItemImpl;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentItemFactory that stores the parsed content in Files. This Factory
 * should be preferred to the InMemoryContentItemFactory in cases where content
 * is parsed to the Enhancer that can not be kept in Memory.
 * @author Rupert Westenthaler
 *
 */
@Component(inherit=true)
@Service(value=ContentItemFactory.class)
@Properties(value={
    @Property(name=FileContentItemFactory.PARAM_BASE_DIR,value=""),
    @Property(name=Constants.SERVICE_RANKING, intValue=50)
})
public class FileContentItemFactory extends AbstractContentItemFactory implements ContentItemFactory {

    
    private final Logger log = LoggerFactory.getLogger(FileContentItemFactory.class);
    
    public static final String DEFAULT_BINARY_MIMETYPE = "application/octet-stream";

    public static final String PARAM_BASE_DIR = "stanbol.enhancer.contentitem.file.baseDir";
    
    private static FileContentItemFactory instance;
    
    /**
     * Base directory used to create temp files
     */
    private File baseDir;
    
    /**
     * Getter for the singleton instance of this factory. Within an OSGI 
     * environment this should not be used as this Factory is also registered
     * as OSGI service.
     * @return the singleton instance using the system default temporary file
     * directory.
     */
    public static FileContentItemFactory getInstance(){
        if(instance == null){
            instance = new FileContentItemFactory();
        }
        return instance;
    }
    
    
    public FileContentItemFactory() {
        super(false); //dereference all data on construction
    }
    public FileContentItemFactory(File baseDir) throws IOException {
        this();
        if(baseDir != null){
            this.baseDir = baseDir;
            initBaseDir();
        }
    }
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        Object value = ctx.getProperties().get(PARAM_BASE_DIR);
        if(value != null && !value.toString().isEmpty()){
            String home = ctx.getBundleContext().getProperty("sling.home");
            if(home != null){
                baseDir = new File(home,value.toString());
            } else {
                baseDir = new File(value.toString());
            }
            try {
                initBaseDir();
            } catch (Exception e) {
                new ConfigurationException(PARAM_BASE_DIR, "Unable to initialise"
                    + "configured base Directory '"+value+"' (absolute path: '"
                    + baseDir.getAbsolutePath()+"')!",e);
            }
        }
    }


    /**
     * Internally used to initialise the {@link #baseDir}
     * @throws IllegalStateException if the parsed Directory already exists
     * but is not an directory.
     * @throws IOException if the configured directory does not exists but
     * could not be created
     */
    private void initBaseDir() throws IOException {
        if(baseDir.exists() && !baseDir.isDirectory()){
            baseDir = null;
            throw new IllegalArgumentException("A File with the configured Directory '"
                + baseDir.getAbsolutePath()+ "' already exists, but is not a Directory!");
        }
        log.info("activate {} with temp directory {}",getClass().getSimpleName(),
            baseDir.getAbsolutePath());
        if(!baseDir.isDirectory()){
            if(!baseDir.mkdirs()){
                throw new IOException("Unable to create"
                		+ "temp-directory '"+baseDir.getAbsolutePath()+")!");
            }
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        baseDir = null;
    }
        
    @Override
    protected ContentItem createContentItem(IRI id, Blob blob, Graph metadata) {
        return new FileContentItem(id, blob, metadata);
    }

    @Override
    protected ContentItem createContentItem(String prefix, Blob blob, Graph metadata) {
        return new FileContentItem(prefix, blob, metadata);
    }

    @Override
    public Blob createBlob(ContentSource source) throws IOException {
        return new FileBlob(source);
    }
    @Override
    public ContentSink createContentSink(String mediaType) throws IOException {
        return new FileContentSink(mediaType);
    }
    
    
    protected File createTempFile(String prefix){
        File tmpFile;
        try {
            tmpFile = File.createTempFile(prefix, null, baseDir);
        } catch (IOException e) {
            if(baseDir != null){
                log.warn("Unable to create temp-file in directory "+baseDir
                    + " (try to create in system temp");
                try {
                    tmpFile = File.createTempFile(prefix, null, null);
                } catch (IOException e1) {
                    throw new IllegalStateException("Unable to create temp-file" +
                            "in '"+baseDir+"' and system temp directory",e1);
                }
            } else {
                throw new IllegalStateException("Unable to create temp-file",e);
            }
        }
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    public class FileContentSink implements ContentSink {

        private final WriteableFileBlob blob;
        
        protected FileContentSink(String mediaType){
            blob = new WriteableFileBlob(mediaType);
        }
        @Override
        public OutputStream getOutputStream() {
            return blob.getOutputStream();
        }

        @Override
        public Blob getBlob() {
            return blob;
        }
        
    }
    
    public class WriteableFileBlob implements Blob {
        
        private final File file;
        private final OutputStream out;
        private String mimeType;
        private Map<String,String> parameters;

        protected WriteableFileBlob(String mediaType){
            this.file = createTempFile("blob");
            try {
                this.out = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("temporary file '"
                        + file.getAbsolutePath()+"' was not created as expected!",e);
            }
            Map<String,String> parameters;
            if(mediaType == null){
                this.mimeType = DEFAULT_BINARY_MIMETYPE;
                parameters = new HashMap<String,String>();
            } else {
                parameters = ContentItemHelper.parseMimeType(mediaType);
                this.mimeType = parameters.remove(null);
            }
            this.parameters = Collections.unmodifiableMap(parameters);
        }
        /**
         * Used by the {@link FileContentSink} implementation
         * @return
         */
        protected final OutputStream getOutputStream(){
            return out;
        }
        
        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public InputStream getStream() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("temporary file '"
                        + file.getAbsolutePath()+"' no longer present!",e);
            }
        }

        @Override
        public Map<String,String> getParameter() {
            return parameters;
        }

        @Override
        public long getContentLength() {
            return file.length();
        }
        @Override
        protected void finalize() throws Throwable {
            IOUtils.closeQuietly(out);
            file.delete();
            super.finalize();
        }
    }
    
    /**
     * Blob implementation that store the data in a temp file. NOTE that
     * all the other information such as {@link #getMimeType()},
     * {@link #getParameter()} are kept in memory. So this can NOT be used
     * to persist a ContentItem!
     * @author Rupert Westenthaler
     *
     */
    public class FileBlob implements Blob {

        private final File file;
        /**
         * This implementation generates the sha1 while copying the data
         * in the constructor to the file to avoid reading the data twice if a
         * {@link ContentItem} is created based on a Blob.
         */
        private final String sha1;

        private final String mimeType;

        private final Map<String,String> parameters;
        
        protected FileBlob(ContentSource source) throws IOException {
            if(source == null){
                throw new IllegalArgumentException("The parsed ConentSource MUST NOT be NULL!");
            }
            file = createTempFile("blob");
            OutputStream out = null;
            InputStream in = null;
            try {
                out = new FileOutputStream(file);
                in = source.getStream();
                sha1 = ContentItemHelper.streamDigest(in, out, SHA1);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
            Map<String,String> parameters;
            if(source.getMediaType() == null){
                this.mimeType = DEFAULT_BINARY_MIMETYPE;
                parameters = new HashMap<String,String>();
            } else {
                parameters = ContentItemHelper.parseMimeType(source.getMediaType());
                this.mimeType = parameters.remove(null);
            }
            this.parameters = Collections.unmodifiableMap(parameters);
        }
        /**
         * The tmp file representing this Blob
         * @return the file
         */
        protected final File getFile() {
            return file;
        }

        /**
         * The sha1 of this Blob - typically used to generate the default IDs
         * of a ContentItem
         * @return the sha1
         */
        protected final String getSha1() {
            return sha1;
        }
        
        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public InputStream getStream() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("temporary file '"
                        + file.getAbsolutePath()+"' no longer present!",e);
            }
        }

        @Override
        public Map<String,String> getParameter() {
            return parameters;
        }

        @Override
        public long getContentLength() {
            return file.length();
        }
        @Override
        protected void finalize() throws Throwable {
            //delete the file
            file.delete();
        }
    }
    /**
     * Utility that returns the ID for a FileContentItem based on
     * {@link FileBlob#getSha1()}.<p>
     * This method is part of the {@link FileContentItemFactory} because it
     * is used in the super(..) call of the {@link FileContentItem}. Normally
     * it would be a static method of the inner class (what is a similar scope
     * as a non static method in the outer class).
     * @param blob the blob
     * @return the id
     * @throws IllegalArgumentException if the parsed {@link Blob} or the
     * prefix is <code>null</code>
     * @throws IllegalStateException if the parsed blob is not an {@link FileBlob}
     */
    protected IRI getDefaultUri(Blob blob, String prefix) {
        if(blob == null){
            throw new IllegalArgumentException("The parsed Blob MUST NOT be NULL!");
        }
        if(prefix == null){
            throw new IllegalArgumentException("The parsed prefix MUST NOT be NULL!");
        }
        if(blob instanceof FileBlob) {
            return new IRI(prefix+SHA1.toLowerCase()+ '-' + ((FileBlob)blob).getSha1());
        } else {
            throw new IllegalStateException("FileContentItem expects FileBlobs to be used" +
                    "as Blob implementation (found: "+blob.getClass()+")!");
        }
    }

    protected class FileContentItem extends ContentItemImpl implements ContentItem {
        
        public FileContentItem(IRI id, Blob blob,Graph metadata) {
            super(id == null ? getDefaultUri(blob, DEFAULT_CONTENT_ITEM_PREFIX) : id, blob,
                    metadata == null ? new IndexedGraph() : metadata);
        }
        public FileContentItem(String prefix, Blob blob,Graph metadata) {
            super(getDefaultUri(blob, prefix), blob,
                metadata == null ? new IndexedGraph() : metadata);
        }

        
    }
}
