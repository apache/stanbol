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
/**
 * 
 */
package org.apache.stanbol.commons.solr.managed;

import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_ARCHIVES;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_NAME;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.SERVER_NAME;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.SYNCHRONIZED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link Properties} with getter and setter for the metadata used
 * by managed Solr indexes.
 * @author Rupert Westenthaler
 *
 */
public final class IndexMetadata extends Properties {
    
    private final Logger log = LoggerFactory.getLogger(IndexMetadata.class);
    
    /**
     * generated
     */
    private static final long serialVersionUID = 5831848470486994628L;
    /**
     * The Archive currently used for the index
     */
    protected static final String ARCHIVE = "Archive";
    /**
     * The current state of this index
     */
    protected static final String STATE = "State";
    /**
     * The Directory of this index on the local file system
     */
    protected static final String DIRECTORY = "Directory";
    /**
     * If the current {@link #getState()} == {@link ManagedIndexState#ERROR}
     * this property is used to store the {@link Exception#printStackTrace() stack trace}
     * as reported by the the {@link Exception} that caused the Error
     */
    protected static final String STACK_TRACE = "Stack-Trace";
    
    private static List<String> EMPTY_LIST_OF_STRING = Collections.emptyList();
    
    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        validate(false);
    }
    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        validate(false);
    }
    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException,
                                                        InvalidPropertiesFormatException {
        super.loadFromXML(in);
        validate(false);
    }
    @Override
    public void store(OutputStream out, String comments) throws IOException {
        validate(true);
        super.store(out, comments);
    }
    @Override
    public void store(Writer writer, String comments) throws IOException {
        validate(true);
        super.store(writer, comments);
    }
    @Override
    public synchronized void storeToXML(OutputStream os, String comment) throws IOException {
        validate(true);
        super.storeToXML(os, comment);
    }
    @Override
    public synchronized void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        validate(true);
        super.storeToXML(os, comment, encoding);
    }
    @Override
    @Deprecated
    public synchronized void save(OutputStream out, String comments) throws UnsupportedOperationException{
        throw new UnsupportedOperationException("deprecated Method not supported");
    }
    
    /**
     * validates the values of the IndexProperties
     * @throws IOException
     */
    private void validate(boolean store) throws IOException {
        if(isSynchronized() && getIndexArchives().isEmpty()){
            throw new IOException("Unable to "+(store?"store":"read")+
                " IndexPropertis where Synchronized=true and no Index-Archives are defined!");
        }
        ManagedIndexState state = getState();
        if(state == null){
            throw new IOException("Unable to "+(store?"store":"read")+
                " IndexMetadata without the required key '"+STATE+
                "' set to one of the values '"+
                Arrays.toString(ManagedIndexState.values())+"'!");
        }
        if(isActive()){
            if(getDirectory() == null){
                throw new IOException("Unable to "+(store?"store":"read")+
                    " IndexPropertis where Active=true and no Directory is defined!");
            }
        }
        String name = getIndexName();
        if(name == null || name.isEmpty()){
            throw new IOException("Unable to "+(store?"store":"read")+
            	" IndexPropertis where the required key '"+
            	INDEX_NAME+"' is not defined or empty!");
        }
    }
    
    public List<String> getIndexArchives(){
        String value = getProperty(INDEX_ARCHIVES);
        return value == null || value.isEmpty() ? 
                EMPTY_LIST_OF_STRING : Arrays.asList(value.split(","));
    }
    public void setIndexArchives(List<String> archives){
        StringBuilder value = new StringBuilder();
        boolean first = true;
        for(String archive:archives){
            if(archive != null){
                if(!first){
                    value.append(',');
                } else {
                    first = false;
                }
                value.append(archive);
            }
        }
        setProperty(INDEX_ARCHIVES, value.toString());
    }
    public boolean isSynchronized(){
        String value = getProperty(SYNCHRONIZED);
        return Boolean.parseBoolean(value);
    }
    public void setSynchronized(boolean state){
        setProperty(SYNCHRONIZED, Boolean.toString(state));
    }
    public String getArchive(){
        return getProperty(ARCHIVE);
    }
    public void setArchive(String archive){
        if(archive == null){
            remove(ARCHIVE);
        } else if (archive.isEmpty()){
            throw new IllegalArgumentException("The parsed archive MUST NOT be empty!");
        } else {
            setProperty(ARCHIVE, archive);
        }
    }
    public String getIndexName(){
        return getProperty(INDEX_NAME);
    }
    public void setIndexName(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The Index-Name MUST NOT be NULL nor empty");
        }
        setProperty(INDEX_NAME, name);
    }
    public String getServerName(){
        return getProperty(SERVER_NAME);
    }
    public void setServerName(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The Server-Name MUST NOT be NULL nor empty");
        }
        setProperty(SERVER_NAME, name);
    }
    public String getDirectory(){
        return getProperty(DIRECTORY);
    }
    public void setDirectory(String directory){
        if(directory == null){
            remove(DIRECTORY);
        } else if (directory.isEmpty()){
            throw new IllegalArgumentException("The parsed directory MUST NOT be empty!");
        } else {
            setProperty(DIRECTORY, directory);
        }
    }
    public ManagedIndexState getState(){
        String state = getProperty(STATE);
        if(state == null){
            log.warn("No ManagedIndexState (key: '"+STATE+"') present in the" +
            		"IndexMetadata for '"+getIndexReference()+"'! -> return null");
            return null;
        } else {
            try {
                return ManagedIndexState.valueOf(state);
            } catch (IllegalArgumentException e) {
                log.error("Unable to parse ManagedIndexState from value '"+
                    state+"'! -> return null",e);
                return null;
            }
        }
    }
    /**
     * Checks if this index is in the {@link ManagedIndexState#ACTIVE} state
     * @return if this index is active or not
     * @see #getState()
     */
    public boolean isActive(){
        ManagedIndexState state = getState();
        return state != null && state == ManagedIndexState.ACTIVE;
    }
    /**
     * Checks if this index is in the {@link ManagedIndexState#INACTIVE} state
     * @return if this index is inactive or not
     * @see #getState()
     */
    public boolean isInactive(){
        ManagedIndexState state = getState();
        return state != null && state == ManagedIndexState.INACTIVE;
    }
    /**
     * Checks if this index is in the {@link ManagedIndexState#ERROR} state
     * @return if this index has an error or not
     * @see #getState()
     */
    public boolean isError(){
        ManagedIndexState state = getState();
        return state != null && state == ManagedIndexState.ERROR;
    }
    /**
     * Checks if this index is in the {@link ManagedIndexState#UNINITIALISED} state
     * @return if this index is still not initialised
     * @see #getState()
     */
    public boolean isUninitialised(){
        ManagedIndexState state = getState();
        return state != null && state == ManagedIndexState.UNINITIALISED;
    }
    
    public void setState(ManagedIndexState state){
        if(state == null){
            throw new IllegalArgumentException("The parsed ManagedIndexState MUST NOT be NULL!");
        }
        setProperty(STATE, state.name());
        if(state != ManagedIndexState.ERROR){
            remove(STACK_TRACE);
        }
    }
    /**
     * Getter for the {@link IndexReference} based on the {@link #getServerName()} and
     * {@link #getIndexName()} values
     * @return the {@link IndexReference} to the index described by this metadata
     */
    public IndexReference getIndexReference(){
        return new IndexReference(getServerName(), getIndexName());
    }
    /**
     * Sets the {@link #getState()} to {@link ManagedIndexState#ERROR} and also
     * stores the stack trace of the parsed {@link Exception} to {@link #STACK_TRACE}.
     * @param e The Exception or <code>null</code> it none
     */
    public void setError(Exception e) {
        setState(ManagedIndexState.ERROR);
        if(e != null){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            //writer.append(e.getMessage());
            //writer.append('\n');
            e.printStackTrace(writer);
            writer.close(); //close and flush the writer
            setProperty(STACK_TRACE, out.toString());
            IOUtils.closeQuietly(writer);
            out = null;
        }
    }
    /**
     * The stack trace of the Exception caused this index to be in the
     * {@link ManagedIndexState#ERROR} state or <code>null</code> if not present
     * @return The stack trace or <code>null</code> if not present
     */
    public String getErrorStackTrace(){
        return getProperty(STACK_TRACE);
    }
    
    /**
     * Converts the parsed {@link IndexMetadata} to an {@link Map} with 
     * {@link String} keys and values as used by the  {@link DataFileProvider} 
     * interface
     * @return the Map with {@link String} as key and values
     */
    public static Map<String, String> toStringMap(IndexMetadata metadata){
        Map<String,String> map = new HashMap<String,String>();
        for(java.util.Map.Entry<?,?> entry : metadata.entrySet()){
            map.put((String)entry.getKey(), 
                entry.getValue() == null ? null : entry.getValue().toString());
        }
        return map;
    }
}