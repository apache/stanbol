package org.apache.stanbol.commons.solr;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexReference {

    private static final Logger log = LoggerFactory.getLogger(IndexReference.class);
    
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
     * @param serverName
     * @return
     */
    public boolean checkServer(String serverName) {
        return server == null || server.equals(serverName);
    }
    
}
