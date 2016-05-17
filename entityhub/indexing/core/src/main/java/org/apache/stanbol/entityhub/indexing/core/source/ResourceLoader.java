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
package org.apache.stanbol.entityhub.indexing.core.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLoader {
    private final boolean failOnError;
    
    private static final Logger log = LoggerFactory.getLogger(ResourceLoader.class);
    private final ResourceImporter resourceImporter;
    private final Map<String,ResourceState> files;
    private File importedDir;
    /**
     * for future uses to activate/deactivate parsing of entries within ZIP
     * archives. If <code>false</code> the ZIP archive will be parsed as a 
     * whole. If <code>true</code> the Entries of the ZIP archive will be
     * parsed to the resource handler.
     */
    private boolean loadEntriesWithinZipArchives = true;
    public ResourceLoader(ResourceImporter resourceImporter,boolean failOnError) {
        this(resourceImporter,true,failOnError);
    }
    public ResourceLoader(ResourceImporter resourceImporter, boolean processEntriesWithinArchives,boolean failOnError) {
        if(resourceImporter == null){
            throw new IllegalStateException("The parsed ResourceProcessor instance MUST NOT be NULL!");
        }
        this.resourceImporter = resourceImporter;
        this.loadEntriesWithinZipArchives = processEntriesWithinArchives;
        //use a tree map to have the files sorted
        this.files = new TreeMap<String,ResourceState>();
        this.failOnError = failOnError;
    }

    public void setImportedDir(File importedDir) {
        this.importedDir = importedDir;
        if(importedDir != null){
            if(importedDir.isFile()){
                throw new IllegalArgumentException("The parsed imported directory MUST NOT be a File");
            }
            if(!importedDir.isDirectory()){
                if(!importedDir.mkdirs()){
                    throw new IllegalStateException("Unable to create imported directory "+importedDir);
                }
            }
        }
    }
    /**
     * Adds a new {@link File} resource to this resource loader. In case a
     * directory is parsed, all files directly within this directory will be 
     * also added. Note that hidden Files are ignored.
     * @param fileOrDirectory the file/directory to add.
     */
    public void addResource(File fileOrDirectory){
        if(fileOrDirectory != null){
            for(String file : getFiles(fileOrDirectory)){
                ResourceState state = files.get(file);
                if(state == null){
                    log.debug("File {} registered to this RdfLoader",file);
                    files.put(file, ResourceState.REGISTERED);
                } else if(state == ResourceState.ERROR){
                    log.info("Readding file {} after previous error while loading",file);
                } else {
                    log.info("Ignore file {} because it already present with state {}",file,state);
                }
            }
        }
    }
    
    /**
     * Getter for the read only status of the resource loader.
     * @return the read only view of the status
     */
    public Map<String,ResourceState> getResourceStates(){
        return Collections.unmodifiableMap(files);
    }
    /**
     * Getter for all resources that are currently in the parsed state.
     * This Method returns a copy of all resources in the parsed state.
     * @param state the processing state
     * @return A copy of all resources in the parsed state
     */
    public Collection<String> getResources(ResourceState state){
        if(state == null){
            return Collections.emptySet();
        } else {
            return getResources(EnumSet.of(state));
        }
    }
    /**
     * Getter for all resources that are currently in on of the parsed states.
     * This Method returns a copy of all resources in such states.
     * @param states the processing states
     * @return A copy of all resources in one of the parsed states
     */
    public Collection<String> getResources(Set<ResourceState> states){
        if(states == null){
            return Collections.emptySet();
        } else {
            Collection<String> files = new HashSet<String>();
            synchronized (this.files) {
                for(Entry<String,ResourceState> entry : this.files.entrySet()){
                    if(states.contains(entry.getValue())){
                        files.add(entry.getKey());
                    }
                }
            }
            return files;
        }
    }
    public void loadResources(){
        Collection<String> fileToLoad;
        do { //to support adding of new files while loading
            fileToLoad = getResources(ResourceState.REGISTERED);
            long start=System.currentTimeMillis();
            log.info("Loding {} File{} ...",fileToLoad.size(),fileToLoad.size()>1?"s":"");
            for (String file : fileToLoad) {
                loadResource(file);
            }
            log.info(" ... {} files imported in {} seconds", 
                fileToLoad.size(),(System.currentTimeMillis()-start)/1000);
        } while(!fileToLoad.isEmpty());
    }
    /**
     * Loads a resource from a file
     * @param file the file resource
     */
    private void loadResource(String file) {
        synchronized (files) { 
            //sync to files to avoid two threads loading the same file
            ResourceState state = files.get(file);
            if(state == null || state != ResourceState.REGISTERED){
                log.info("Do not load File {} because of its state {} (null means removed from list)",
                    file,state);
                return; //someone removed it in between
            } else { //set to loading
                setResourceState(file, ResourceState.LOADING, null);
            }
        }
        long startFile = System.currentTimeMillis();
        log.info(" > loading '{}' ...", file);
        String extension = FilenameUtils.getExtension(file);
        if(loadEntriesWithinZipArchives && (
                "zip".equalsIgnoreCase(extension) ||
                "jar".equalsIgnoreCase(extension))){
            log.info("  - processing {}-archive entries:",extension);
            ZipFile zipArchive;
            try {
                zipArchive = new ZipFile(file);
            } catch (IOException e) {
                zipArchive = null;
                setResourceState(file, ResourceState.ERROR,e);
            }
            if(zipArchive != null){
                boolean isError = false;
                Enumeration<ZipArchiveEntry> entries = zipArchive.getEntries();
                while(entries.hasMoreElements()){
                    ZipArchiveEntry entry = entries.nextElement();
                    if(!entry.isDirectory()){
                        String entryName = entry.getName();
                        log.info("     o loading entry '{}'", entryName);
                        try {
                            ResourceState state = resourceImporter.importResource(
                                zipArchive.getInputStream(entry), 
                                FilenameUtils.getName(entryName));
                            if(state == ResourceState.ERROR){
                                isError = true;
                            }
                        } catch (IOException e) {
                            isError = true;
                        }
                    }
                }
                //set the state for the Archive as a whole
                setResourceState(file, 
                    isError ? ResourceState.ERROR : ResourceState.LOADED, null);
            }
        } else {
            InputStream is;
            try {
                is = new FileInputStream(file);
                ResourceState state = resourceImporter.importResource(is,
                    FilenameUtils.getName(file));
                setResourceState(file, state, null);
            } catch (FileNotFoundException e) {
                //during init it is checked that files exists and are files 
                //and there is read access so this can only happen if
                //someone deletes the file in between
                log.warn("Unable to load resource " + file, e);
                setResourceState(file, ResourceState.ERROR, e);
            } catch (IOException e) {
                log.error("Unable to load resource " + file ,e);
                setResourceState(file, ResourceState.ERROR, e);
            } catch (Exception e) {
                log.error("Unable to load resource " + file, e);
                setResourceState(file, ResourceState.ERROR, e);
            }
        }
        log.info("   - completed in {} seconds", 
            (System.currentTimeMillis()-startFile)/1000);
    }
    /**
     * Getter for the files based on a parsed File or Directory. Hidden Files
     * are ignored. Doese not search recursively to the directory structure!
     * @param fileOrDir The file or directory
     * @return the Collection of files found based on the parameter
     */
    private static Collection<String> getFiles(File fileOrDir){
        if(fileOrDir == null){
            return Collections.emptySet();
        } else if(fileOrDir.isHidden()){
            return Collections.emptySet();
        } else if(fileOrDir.isFile()){
            return Collections.singleton(fileOrDir.getPath());
        } else if(fileOrDir.isDirectory()){
            Collection<String> files = new ArrayList<String>();
            for(File file : fileOrDir.listFiles()){
                if(file.isFile() && !file.isHidden()){
                    files.add(FilenameUtils.concat(fileOrDir.getPath(), file.getPath()));
                }
            }
            return files;
        } else { //file does not exist
            return Collections.emptySet();
        }
    }
    /**
     * Logs the Exception and sets the file to the {@link ResourceState#ERROR}
     * state
     * @param file the affected file
     * @param e the Exception
     */
    private void setResourceState(String file, ResourceState state,Exception e) {
        if(e != null){
            log.error("Exception while loading file "+file,e);
        }
        if(state == null){
            //ensure that there are no null values in the map
            throw new IllegalArgumentException("The parsed ProcessingState MUST NOT be NULL!");
        }
        if(file == null){
            //ignore calls if file is null
            return;
        }
        synchronized (files) {
            if(files.containsKey(file)){
                log.debug("File {} now in state {}",file,state);
                if(importedDir != null && ResourceState.LOADED == state){
                    files.remove(file);
                    try {
                        files.put(moveToImportedFolder(new File(file)).toString(), state);
                    } catch (IOException ioe) {
                       log.warn("Unable to move loaded RDFTerm {} to imported Directory! "
                           + "Please move the file manually to {}!",file,importedDir);
                       log.warn("Reason: "+ioe.getMessage(),ioe);
                       files.put(file, state);
                    }
                } else { //this does not use an imported folder or the state is not LOADED
                    files.put(file, state);
                }
                if(ResourceState.ERROR == state){
                	//if failOnError is activated we stop the loading on the first error!                   
                    if (failOnError){
                    	 String msg = "Error while loading RDFTerm "+file;
                    	if(e != null){
                            throw new IllegalStateException(msg,e);
                        } else {
                            throw new IllegalStateException(msg);
                        }
                    }else {
                    	//if failOnError is de-activated ignore the resource loading error and continue..                   
                    	log.info("Ignore Error for File {} and continue", file);
                    }                    
                } 
            } else {
                log.info("Ignore Error for File {} because it is no longer registered with this RdfLoader",
                    file);
            }
        }
    }
    private File moveToImportedFolder(File file) throws IOException {
        if(importedDir == null){
            return file;
        }
        File moved = new File(importedDir,file.getName());
        int i=0;
        while(moved.isFile()){
            i++;
            moved = new File(importedDir,i+"_"+file.getName());
        }
        log.info("   ... moving imported file {} to {}/{}",new Object[] {
                file.getName(),importedDir.getName(),moved.getName()});
        FileUtils.moveFile(file, moved);
        return moved;
    }
}
