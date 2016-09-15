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
package org.apache.stanbol.commons.solr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexReference {

    private static final Logger log = LoggerFactory.getLogger(IndexReference.class);
    private static final String CONSTRAINT = "(%s=%s)";
    
    public static IndexReference parse(String uriOrPathOrReference){
        String[] referencedCore = new String[2];
        boolean isFile;
        if(uriOrPathOrReference.startsWith("file:")){ //file URL
            isFile = true;
            File file = null; 
            try {
                file = FileUtils.toFile(new URL(uriOrPathOrReference));
            }catch (MalformedURLException e) {
                log.error("Unable to parse file URL '"+uriOrPathOrReference+"'!",e);
                file = null;
            }
            referencedCore[0] = null; //no server name for such values
            if(file != null){
                referencedCore[1] = FilenameUtils.normalize(file.getAbsolutePath()+File.separatorChar);
            } else {
                return null;
            }
        } else if(uriOrPathOrReference.indexOf(File.separatorChar) >=0 ||
                uriOrPathOrReference.indexOf('/') >=0){ //also support UNIX style on Windows
            //we assume a File Reference
            isFile= true;
            File file = new File(FilenameUtils.separatorsToSystem(uriOrPathOrReference));
            referencedCore[0] = null;
            referencedCore[1] = FilenameUtils.normalize(file.getAbsolutePath()+File.separatorChar);
        } else { //reference in the style [{server-name}:]{core-name}
            isFile = false;
            int index = uriOrPathOrReference.indexOf(':');
            if(index < 0){
                referencedCore[0] = null;
                referencedCore[1] = uriOrPathOrReference;
            } else {
                referencedCore[0] = uriOrPathOrReference.substring(0,index);
                referencedCore[1] = uriOrPathOrReference.substring(index+1);
                validateIndexName(referencedCore[1],uriOrPathOrReference);
            }
        }
        return new IndexReference(referencedCore[0],referencedCore[1],isFile);
    }
    /**
     * Validates the indexName
     * @param indexName the name to validate
     * @param indexRef the parsed indexRef
     * @throws IllegalArgumentException if the validation fails
     */
    private static void validateIndexName(String indexName, String indexRef) {
        if(indexName == null){
            throw new IllegalArgumentException("The index name MUST NOT be NULL!");
        }
        if(indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index reference '"+
                indexRef+"' MUST NOT contain an empty index name" +
                "(e.g. such as ending with ':')!");
        }
        if(indexName.indexOf('\\')>=0 ||
                indexName.indexOf('/')>=0 ||
                indexName.indexOf(':')>=0 ||
                indexName.indexOf('.')>=0){
            throw new IllegalArgumentException("The index name '"+
                indexName+"' of the prased index reference '"+
                indexRef+"' MUST NOT contain any of the " +
                "following chars '"+Arrays.toString(
                    new char[]{'\\','/',':',':','.'})+"'!");
        }
    }
    
    private final String server;
    private final String index;
    private final boolean isFile;
    /**
     * Creates a new IndexReference for the parsed server and index
     * @param server the server or <code>null</code> if not known
     * @param index the index. MUST NOT be <code>null</code> nor empty
     */
    public IndexReference(String server,String index) {
        validateIndexName(index, server+':'+index);
        this.server = server;
        this.index = index;
        this.isFile = false;
    }
    public IndexReference(String server,String index,boolean isFile) {
        this.server = server;
        this.index = index;
        this.isFile = isFile;
    }
    
    /**
     * @return the server
     */
    public final String getServer() {
        return server;
    }
    /**
     * @return the index
     */
    public final String getIndex() {
        return index;
    }

    public boolean isPath(){
        return isFile;
    }
    
    public boolean isName(){
        return !isFile;
    }
    /**
     * Checks if the referenced index could be on the parsed server
     * @param serverName Server Name to be checked
     * @return True if the serverName is consistent with the parsed server 
     */
    public boolean checkServer(String serverName) {
        return server == null || server.equals(serverName);
    }
 
    /**
     * Getter for the {@link Filter} that can be used to track the
     * {@link SolrCore} referenced by this IndexReference.
     * @return the string representation of the OSGI {@link Filter}.
     */
    public String getIndexFilter(){
        StringBuilder filterString = new StringBuilder("(&");
        //first filter for the type
        filterString.append(String.format(CONSTRAINT, Constants.OBJECTCLASS,SolrCore.class.getName()));
        if(isFile){
            filterString.append(String.format(CONSTRAINT, SolrConstants.PROPERTY_CORE_DIR,getIndex()));
        } else { //isName
            filterString.append(String.format(CONSTRAINT, SolrConstants.PROPERTY_CORE_NAME,getIndex()));
        }
        addServerFilterConstraint(filterString);
        filterString.append(')');
        return filterString.toString();
    }
    /**
     * Getter for the {@link Filter} that can be used to track the
     * {@link CoreContainer} referenced by this IndexReference. If no
     * server is defined. This will track all {@link CoreContainer} instances.
     * Note that the {@link CoreContainer} with the highest 
     * {@link Constants#SERVICE_RANKING} is expected to be the default server
     * @return Filter string
     */
    public String getServerFilter(){
        StringBuilder filterString;
        if(getServer() != null){ //add AND for class and name constraint
            filterString = new StringBuilder("(&");
        } else { //if no server is defined we have only one constraint
            filterString = new StringBuilder();
        }
        filterString.append(String.format(CONSTRAINT, Constants.OBJECTCLASS,CoreContainer.class.getName()));
        addServerFilterConstraint(filterString);
        if(getServer() != null){
            filterString.append(')');
        }
        return filterString.toString();
    }
    /**
     * @param filterString
     */
    private void addServerFilterConstraint(StringBuilder filterString) {
        if(getServer() != null){
            filterString.append(String.format(CONSTRAINT, SolrConstants.PROPERTY_SERVER_NAME,getServer()));
        }
    }
    
    @Override
    public String toString() {
        return String.format("IndexReference[server:%s,index:%s]",getServer(),getIndex());
    }
}
