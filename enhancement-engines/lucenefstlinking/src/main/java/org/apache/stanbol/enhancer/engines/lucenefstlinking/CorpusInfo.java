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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.schema.FieldType;
import org.opensextant.solrtexttagger.TaggerFstCorpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the information required for Lucene FST based tagging in a specific
 * language by using a given field.
 * @author Rupert Westenthaler
 *
 */
public class CorpusInfo {

    private final Logger log = LoggerFactory.getLogger(CorpusInfo.class);
    
    /**
     * The language
     */
    public final String language;
    /**
     * The Corpus FST
     */
    protected final File fst;
    /**
     * used to detect fst file changes
     */
    private Date fstDate;
    /**
     * The Solr field used for FST indexing (already encoded)
     */
    public final String indexedField;
    /**
     * The Solr stored field holding the labels indexed in the FST corpus 
     */
    public final String storedField;
    /**
     * TODO: partial matches are currently deactivated
     */
    public final boolean partialMatches = false;
    /**
     * if the FST corpus can be created on the fly
     */
    public final boolean allowCreation;
    /**
     * The Solr {@link Analyzer} used for the field
     */
    public final Analyzer analyzer;
    
    public final Analyzer taggingAnalyzer;
    
    protected final ReadWriteLock corpusLock = new ReentrantReadWriteLock();
    
    protected Reference<TaggerFstCorpus> taggerCorpusRef;
    
    private Future<TaggerFstCorpus> enqueuedCorpus;
    /**
     * Allows to store an error message encountered while loading/creating the
     * FST corpus.
     */
    private String errorMessage;
    /**
     * Indicated an Error during loading the {@link #fst} file
     */
    private boolean fstFileError = false;
    /**
     * Indicates an Error during the runtime creation
     */
    private boolean creationError = false;
    
    
    /** 
     * @param language
     * @param indexField
     * @param analyzer
     * @param fst
     * @param allowCreation
     */
    protected CorpusInfo(String language, String indexField, String storeField, FieldType fieldType, File fst, boolean allowCreation){
        this.language = language;
        this.indexedField = indexField;
        this.storedField = storeField;
        this.fst = fst;
        this.allowCreation = allowCreation;
        this.analyzer = fieldType.getAnalyzer();
        this.taggingAnalyzer = fieldType.getQueryAnalyzer();
        this.fstDate = fst.isFile() ? new Date(fst.lastModified()) : null;
    }
    /**
     * Allows to set an error occurring during the creation of 
     * @param message
     */
    protected void setError(String message){
        this.errorMessage = message;
        this.creationError = true;
        setCorpus(null);
    }
    public boolean isFstFile(){
        return fst != null && fst.isFile();
    }
    
    public boolean isFstFileError(){
        return fstFileError;
    }
    
    public boolean isFstCreationError(){
        return creationError;
    }
    
    public String getErrorMessage(){
        return errorMessage;
    }
    
    /**
     * Allows to explicitly set the corpus after runtime creation has finished.
     * The corpus will be linked by using a {@link WeakReference} to allow the
     * GC to free the memory it consumes. If this happens the corpus will be
     * loaded from the {@link #fst} file.
     * @param enqueued the version of the corpus
     * @param corpus the corpus
     */
    protected final void setCorpus(final TaggerFstCorpus corpus) {
        corpusLock.writeLock().lock();
        try {
            enqueuedCorpus = null; //clear the future ref
            if(taggerCorpusRef != null){
                taggerCorpusRef.clear();
                taggerCorpusRef = null;
            }
            if(corpus != null){
                //reset any error
                this.errorMessage = null; 
                this.creationError = false;
                //we set the corpus as a weak reference. This allows the
                //GC to free the corpus earlier.
                //This is done, because here the corpus was just built and not
                //yet requested. So we want those to be GCed earlier.
                taggerCorpusRef = new WeakReference<TaggerFstCorpus>(corpus);
            }
        } finally {
            corpusLock.writeLock().unlock();
        }
        //Store the newly built FST corpus to disc. A read level lock is sufficient
        //for this.
        //NOTE: the WeakReference to the corpus can only be GC'ed after we
        //      have written the corpus to disc, as we still have a reference
        //      to corpus!
        if(corpus != null){
            try {
                corpusLock.readLock().lock();
                try { //STANBOL-1177: save FST models in AccessController.doPrivileged(..)
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws IOException {
                            if(fst.exists()){
                                if(!FileUtils.deleteQuietly(fst)){
                                    log.warn("Unable to delete existing FST file for {}", fst);
                                }
                            }
                            corpus.save(fst);
                            return null; //not used
                        }
                    });
                } finally {
                    corpusLock.readLock().unlock();
                }
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if(e instanceof IOException){ //IO Exception while loading the file
                    log.warn("Unable to store FST corpus to "
                            + fst.getAbsolutePath() + "!", e);
                    //if we can not save the FST corpus we replace the WeakReference
                    //with a SoftReference to avoid frequent rebuilding of the corpus
                    corpusLock.writeLock().lock();
                    try {
                        if(taggerCorpusRef instanceof WeakReference<?>){
                            taggerCorpusRef.clear();
                            taggerCorpusRef = new SoftReference<TaggerFstCorpus>(corpus);
                        }
                    } finally {
                        corpusLock.writeLock().lock();
                    }
                } else { //Runtime exception
                    throw RuntimeException.class.cast(e);
                }
            }
        }
    }

    public TaggerFstCorpus getCorpus() {
        TaggerFstCorpus corpus;
        corpusLock.readLock().lock();
        try {
            corpus = taggerCorpusRef == null ? null : taggerCorpusRef.get();
            if(corpus != null){
                //on first usage replace a WeakReference with a SoftReference
                if(taggerCorpusRef instanceof WeakReference<?>){
                    log.debug(" ... convert Weak to Soft Reference for Corpus {}", fst);
                    taggerCorpusRef.clear();
                    taggerCorpusRef = new SoftReference<TaggerFstCorpus>(corpus);
                }
            } else if(taggerCorpusRef != null){
                taggerCorpusRef = null; //reset to null as the reference was taken
            }
        } finally {
            corpusLock.readLock().unlock();
        }
        if(corpus == null) {
            log.info(" ... load FST corpus {}",fst);
            corpusLock.writeLock().lock();
            try { //STANBOL-1177: load FST models in AccessController.doPrivileged(..)
                corpus = taggerCorpusRef == null ? null : taggerCorpusRef.get();
                if(corpus == null){ //corpus not loaded while waiting for the write lock
                    corpus = AccessController.doPrivileged(new PrivilegedExceptionAction<TaggerFstCorpus>() {
                        public TaggerFstCorpus run() throws IOException {
                            if(fst.exists() && //if the file exists AND the file was not yet failing to load 
                                    //OR the file is newer as the last version failing to load
                                    (!fstFileError || FileUtils.isFileNewer(fst, fstDate))){
                                TaggerFstCorpus corpus = TaggerFstCorpus.load(fst);
                                if(corpus != null){
                                    //I need to set fstDate here, because I can not
                                    //access lastModified() outside doPrivileged
                                    fstDate = new Date(fst.lastModified());
                                    if(log.isInfoEnabled()){
                                        log.info(" ... loaded FST (date: {})", 
                                            SimpleDateFormat.getDateTimeInstance().format(fstDate));
                                    }
                                } else {
                                    log.warn(" ... no corpus loaded from {}",fst);
                                }
                                return corpus;
                            } else {
                                log.warn(" ... unable to load FST from {} (exists: {}, fileError {})",
                                    new Object[]{fst, fst.exists(),fstFileError});
                                return null;
                            }
                        }
                    });
                    if(corpus != null){
                        fstFileError = false;
                        taggerCorpusRef = new SoftReference<TaggerFstCorpus>(corpus);
                    } //else not loaded from file
                } //else corpus was loaded while waiting for the write lock
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if(e instanceof IOException){ //IO Exception while loading the file
                    this.errorMessage = new StringBuilder("Unable to load FST corpus from "
                            + "FST file: '").append(fst.getAbsolutePath())
                            .append("' (Message: ").append(e.getMessage()).append(")!").toString();
                        log.warn(errorMessage,e);
                        fstFileError = true;
                } else { //Runtime exception
                    throw RuntimeException.class.cast(e);
                }
            } finally {
                corpusLock.writeLock().unlock();
            }
        }
        return corpus;
    }
    /**
     * Called after the curpus was enqueued for rebuilding
     */
    protected void enqueued(Future<TaggerFstCorpus> enqueued){
        this.enqueuedCorpus = enqueued;
    }
    /**
     * Allows to get the {@link Future} of a ongoing {@link CorpusCreationTask}.
     * @return returns a {@link Future} that allows to wait for a corpus that is
     * currently be built. 
     */
    public Future<TaggerFstCorpus> getEnqueued(){
        return enqueuedCorpus;
    }
    
    /**
     * Returns if the FST corpus described by this FST info is queued for
     * generation. NOTE: that {@link #getCorpus()} might still return a 
     * {@link TaggerCorpus}, but in this case it will be based on an outdated
     * version of the index.
     * @return <code>true</code> if the FST corpus is enqueued for (re)generation.
     */
    public boolean isEnqueued(){
        return taggerCorpusRef != null;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FST Info[language: ").append(language);
        if(indexedField.equals(storedField)){
            sb.append(" | field: ").append(indexedField);
        } else {
            sb.append(" | fields(index:").append(indexedField).append(", stored:")
                .append(storedField).append(')');
        }
        sb.append(" | file: ").append(fst.getName())
            .append("(exists: ").append(fst.isFile()).append(')')
            .append(" | runtime creation: ").append(allowCreation)
            .append("]");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return indexedField.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof CorpusInfo && 
                ((CorpusInfo)obj).indexedField.equals(indexedField) &&
                ((CorpusInfo)obj).storedField.equals(storedField) &&
                ObjectUtils.equals(language, language);
    }
}
