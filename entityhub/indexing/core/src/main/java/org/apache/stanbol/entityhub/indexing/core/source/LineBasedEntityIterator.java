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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Implementation of the {@link EntityIterator} based on reading data line wise
 * from an {@link InputStream}
 * @author Rupert Westenthaler
 *
 */
public class LineBasedEntityIterator implements EntityIterator {
    /**
     * Parameter used to configure the position of the entity score (starting
     * with 1)
     */
    public static final String PARAM_SCORE_POS = "score-pos";
    /**
     * If <code>true</code> ids are parsed to the {@link NamespacePrefixService}
     * to convert '{prefix}:{localname}' like URIs to URIs. By default this is 
     * deactivated.</p>
     * NOTE: that {@link #PARAM_ID_NAMESPACE} is applied first. Meaning that
     * '{prefixes}' can be used for this parameter of this feature is enabled.
     */
    public static final String PARAM_NS_PREFIX_STATE = "ns-prefix-state";
    /**
     * Default position for the entity score=2
     */
    public static final int DEFAULT_SCORE_POS = 2;
    /**
     * Parameter used to configure the position of the entity id (starting
     * with 1)
     */
    public static final String PARAM_ID_POS = "id-pos";
    /**
     * Default position for the entity id=1
     */
    public static final int DEFAULT_ID_POS = 1;
    public static final String PARAM_ID_NAMESPACE = "id-namespace";


    private static final Logger log = LoggerFactory.getLogger(LineBasedEntityIterator.class);
    
    public static final String PARAM_SEPARATOR = "separator";
    /**
     * The default separator to split the entity id with the score "\t" (tab)
     */
    public static final String DEFAULT_SEPARATOR = "\t";
    /**
     * The default encoding used to read the data from the parsed {@link InputStream}
     * (UTF-8)
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * Parameter used to configure the name of the source file within the
     * {@link IndexingConfig#getSourceFolder()}
     */
    public static final String PARAM_ENTITY_SCORE_FILE = "source";
    /**
     * The default name used for the {@link #PARAM_ENTITY_SCORE_FILE}
     */
    public static final String DEFAULT_ENTITY_SCORE_FILE = "entityScores.tsv";
    /**
     * Parameter used to configure if the Entity IDs should be {@link URLEncoder
     * URL encoded} (the default is {@value #DEFAULT_ENCODE_ENTITY_IDS})
     */
    public static final String PARAM_URL_ENCODE_ENTITY_IDS = "encodeIds";
    public static final String PARAM_URL_DECODE_ENTITY_IDS = "decodeIds";
    /**
     * Parameter used to configure the text encoding used by the source file
     * (the default is {@value #DEFAULT_ENCODING})
     */
    public static final String PARAM_CHARSET = "charset";
    /**
     * Parameter used to configure if {@link String#trim()} should be called
     * on lines.
     */
    public static final String PARAM_TRIM_LINE = "trimLine";
    /**
     * The default value for {@link #PARAM_TRIM_LINE} is <code>false</code>
     */
    public static final boolean DEFAULT_TRIM_LINE = false;
    /**
     * Parameter used to configure if {@link String#trim()} should be called
     * on the element used to parse the entity id.
     */
    public static final String PARAM_TRIM_ID = "trimEntity";
    /**
     * The default value vor {@link #DEFAULT_TRIM_ENTITY} is <code>true</code>
     */
    public static final boolean DEFAULT_TRIM_ENTITY = true;
    
    protected File scoreFile;
    protected BufferedReader reader;
    private String separator;
    private String charset;
    /**
     * Used to indicate if Entity IDs should be URL encoded/decoded<p>
     * <source><pre>
     * 0 ... no action
     * -1 ... decode
     * +1 ... encode
     * </pre></source>
     */
    private int encodeEntityIds;
    private long lineCounter = 0;
    private EntityScore nextEntity;
    private int scorePos;
    private int idPos;
    protected String namespace;
    private boolean trimLine;
    private boolean trimEntityId;
    private NamespacePrefixService nsPrefixService;
    private boolean nsPrefixState;
    /**
     * Holds the prefix [0] and namespace [1] of the last encountered prefix
     */
    private String[] lastNsPrefix = new String[2];
    
    /**
     * Default constructor relaying on {@link #setConfiguration(Map)} is used
     * to provide the configuration
     */
    public LineBasedEntityIterator(){
        this(null);
    }
    /**
     * Constructs an EntityScoreIterator that reads {@link EntityScore}s based 
     * on lines provided by the parsed InputStream. <p> Separator, Charset and
     * encoding of Entity ids are initialised based on the default values.
     * @param is the InputStream to read the data from
     * @throws IOException On any error while initialising the {@link BufferedReader}
     * based on the parsed {@link InputStream}
     */
    public LineBasedEntityIterator(InputStream is) {
        this(is,null,null);
    }
    /**
     * Constructs an EntityScoreIterator based on the parsed parameters. The
     * default values are used if <code>null</code> is parsed for any parameter
     * other than the InputStream.
     * @param is the InputStream to read the data from
     * @param charset
     * @param separator
     * @throws IOException On any error while initialising the {@link BufferedReader}
     * based on the parsed {@link InputStream}
     * @throws IllegalArgumentException if <code>null</code> is parsed as InputStream
     */
    public LineBasedEntityIterator(InputStream is,String charset,String separator) throws IllegalArgumentException {
        if(charset == null){
            this.charset = DEFAULT_ENCODING;
        } else {
            this.charset = charset;
        }
        if(separator == null){
            this.separator = DEFAULT_SEPARATOR;
        } else {
            this.separator = separator;
        }
        if(is != null){
            initReader(is);
        }
        scorePos = DEFAULT_SCORE_POS;
        idPos = DEFAULT_ID_POS;
        trimEntityId = DEFAULT_TRIM_ENTITY;
        trimLine = DEFAULT_TRIM_LINE;
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        log.info("Configure {} :",getClass().getSimpleName());
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        if(indexingConfig != null) { //will be null if used for post processing
            nsPrefixService = indexingConfig.getNamespacePrefixService();
        }
        Object value = config.get(PARAM_CHARSET);
        if(value != null && value.toString() != null){
            this.charset = value.toString();
            log.info("Set charset to '{}'",charset);
        }
        //parse encode/decode EntityIDs
        value = config.get(PARAM_URL_ENCODE_ENTITY_IDS);
        boolean encodeIds;
        if(value != null){
            encodeIds = Boolean.parseBoolean(value.toString());
        } else if (config.containsKey(PARAM_URL_ENCODE_ENTITY_IDS)){
            encodeIds = true;
        } else {
            encodeIds = false;
        }
        value = config.get(PARAM_URL_DECODE_ENTITY_IDS);
        boolean decodeIds;
        if(value != null){
            decodeIds = Boolean.parseBoolean(value.toString());
        } else if (config.containsKey(PARAM_URL_DECODE_ENTITY_IDS)){
            decodeIds = true;
        } else {
            decodeIds = false;
        }
        if(encodeIds && decodeIds){
            throw new IllegalArgumentException(String.format(
                "One can not enable both Parameters '{}' and '{}'!",
                PARAM_URL_DECODE_ENTITY_IDS,PARAM_URL_DECODE_ENTITY_IDS));
        } else if(encodeIds){
            this.encodeEntityIds = 1;
            log.info("activate URL encoding of Entity IDs");
        } else if(decodeIds){
            this.encodeEntityIds = -1;
            log.info("activate URL decoding of Entity IDs");
        }
        value = config.get(PARAM_ENTITY_SCORE_FILE);
        if(reader == null){
            if(value == null || value.toString().isEmpty()){
                scoreFile = indexingConfig.getSourceFile(DEFAULT_ENTITY_SCORE_FILE);
            } else {
                scoreFile = indexingConfig.getSourceFile(value.toString());
            }
            log.info("Set Source File to '"+this.scoreFile+"'");
        } //else reader parsed in the constructor ... nothing todo
        //now done in the initialise() method
//        try {
//            initReader(new FileInputStream(scoreFile));
//        } catch (FileNotFoundException e) {
//            throw new IllegalArgumentException("The File with the entity scores "+scoreFile.getAbsolutePath()+" does not exist",e);
//        }
        value = config.get(PARAM_ID_POS);
        if(value != null){
            try {
                setIdPos(Integer.parseInt(value.toString()));
                log.info("Set Entity ID Position to '{}'",idPos);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse the position of the entity id from "+value,e);
            }
        }
        value = config.get(PARAM_SCORE_POS);
        if(value != null){
            try {
                setScorePos(Integer.parseInt(value.toString()));
                log.info("Set Score Position to '{}'",scorePos);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse the position of the entity score from "+value,e);
            }
        }
        if(idPos == scorePos){
            throw new IllegalArgumentException("The position of the ID and the Score " +
            		"values MUST NOT be the same value "+ idPos+"! Use "+
            		PARAM_ID_POS+"(default="+DEFAULT_ID_POS+") and "+
            		PARAM_SCORE_POS+"(default="+DEFAULT_SCORE_POS+") to configure " +
            		"other values than the defaults.");
        }
        value = config.get(PARAM_ID_NAMESPACE);
        if(value != null){
            this.namespace = StringEscapeUtils.unescapeJava(value.toString());
            log.info("Set Namespace to ''",namespace);
        }
        value = config.get(PARAM_SEPARATOR);
        if(value != null && !value.toString().isEmpty()){
            this.separator = value.toString();
            log.info("Set Separator to '{}'",separator);
        }
        value = config.get(PARAM_TRIM_LINE);
        if(value != null){
            trimLine = Boolean.parseBoolean(value.toString());
            log.info("Set Trim Line State to '{}'",trimLine);
        } else if(config.containsKey(PARAM_TRIM_LINE)){
            //also accept the key without value as TRUE
            trimLine = true;
            log.info("Set Trim Line State to '{}'",trimLine);
        }
        value = config.get(PARAM_TRIM_ID);
        if(value != null){
            trimEntityId = Boolean.parseBoolean(value.toString());
            log.info("Set Entity ID State to '{}'",trimEntityId);
        } else if(config.containsKey(PARAM_TRIM_ID)){
            //also accept the key without value as TRUE
            trimEntityId = true;
            log.info("Set Entity ID State to '{}'",trimEntityId);
        }
        //STANBOL-1015
        value = config.get(PARAM_NS_PREFIX_STATE);
        if(value instanceof Boolean){
            nsPrefixState = ((Boolean)value).booleanValue();
        } else if(value != null){
            nsPrefixState = Boolean.parseBoolean(value.toString());
        } else {
            nsPrefixState = false; //deactivate as default
        }
        if(nsPrefixState && nsPrefixService == null){
            throw new IllegalStateException("Unable to enable Namespace Prefix support, "
                + "because no NamespacePrefixService is preset!");
        }
        log.info("Set Namespace Prefix State to {}"+nsPrefixState);
    }
    private void setIdPos(int idPos) {
        if(idPos <= 0){
            throw new IllegalArgumentException("The position of the entity id MUST BE >= 1");
        }
        this.idPos = idPos;
        
    }
    private void setScorePos(int scorePos) {
        if(scorePos <= 0){
            throw new IllegalArgumentException("The position of the entity score MUST BE >= 1");
        }
        this.scorePos = scorePos;
        
    }
    /**
     * used by the constructors and {@link #setConfiguration(Map)} to initialise
     * the reader based on the provided File/InputStream.
     * @param is the input stream
     * @param charset the charset
     */
    private void initReader(InputStream is) {
        try {
            reader = new BufferedReader(new InputStreamReader(is, this.charset));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("The parsed encoding "+charset+" is not supported",e);
        }
    }
    @Override
    public boolean hasNext() {
        if(nextEntity == null){ //consumed or first or end reached
            nextEntity = getNext(); //search for the next
        }
        return nextEntity != null; //return the found state
    }

    @Override
    public EntityScore next() {
        EntityScore entity = nextEntity;
        nextEntity = null; //consume
        if(entity == null){ //no next element (e.g. hasNext was not called)
            entity = getNext(); //search for it now
        }
        if(entity == null){ //if not found
            throw new NoSuchElementException();
        }
        return entity;
    }
    protected EntityScore getNext(){
        String line;
        try {
            while((line = reader.readLine()) != null){
                lineCounter++;
                log.debug("> line = {}",line);
                EntityScore entity = parseEntityFormLine(line);
                if(entity != null){
                    return entity;
                }
            }
            return null; //no more elements!
        } catch (IOException e) {
           throw new IllegalStateException("Unable to read next EntityScore",e);
        }
    }
   /**
     * @param line
     * @return
     */
    protected EntityScore parseEntityFormLine(String line) {
        if(line == null){
            return null;
        }
        if(trimLine){
            line = line.trim();
        }
        String[] parts = line.split(separator);
        String entity;
        Float score;
        if(parts.length >=idPos){
            try {
                String value;
                if(trimEntityId){
                    value = parts[idPos-1].trim();
                } else {
                    value = parts[idPos-1];
                }
                String id;
                if(encodeEntityIds > 0){
                    id = URLEncoder.encode(value,"UTF-8");
                } else if(encodeEntityIds < 0){
                    id = URLDecoder.decode(value, "UTF-8");
                } else {
                    id = value;
                }
                id = StringEscapeUtils.unescapeJava(id);
                log.debug(" - id = {}",id);
                entity = namespace != null ? namespace+id : id;
                if(nsPrefixState){
                    //this optimises for cases where all entities do start
                    //with the same prefix
                    String prefix = NamespaceMappingUtils.getPrefix(entity);
                    if(prefix != null){
                        if(!prefix.equals(lastNsPrefix[0])){ //other prefix (or first)
                            lastNsPrefix[0] = prefix;
                            lastNsPrefix[1] = nsPrefixService.getNamespace(prefix);
                            if(lastNsPrefix == null){
                                throw new IllegalStateException("Missing Namespace "
                                    + "Prefix mapping for Prefix '"+prefix+"'!");
                            }
                        }
                        entity = new StringBuilder(lastNsPrefix[1]).append(
                            entity,prefix.length()+1,entity.length()).toString();
                    } //else this entity does not use a prefix
                }
                log.debug(" - entity = {}",entity);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Unable to URLEncode EntityIds",e);
            }
        } else {
            return null; //No id -> no entity ...
        }
        if(parts.length >=scorePos){
            try {
                score = Float.parseFloat(parts[scorePos-1]);
            } catch (NumberFormatException e) {
                log.warn(String.format("Unable to parse the score for " +
                		"Entity %s from value %s in line %s! Use NULL as score 0",
                		entity,parts[scorePos-1],lineCounter));
                score = null;
            }
        } else {
            log.debug("No score for Entity {} in line {}! Use NULL as score",parts[0],lineCounter);
            score = null;
        }
        log.debug(" - score = ",score);
        return new EntityScore(entity,score);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal form the EnityScore list is not supported");
    }
    @Override
    public boolean needsInitialisation() {
        return reader == null;
    }
    @Override
    public void initialise() {
        try {
            initReader(new FileInputStream(scoreFile));
        } catch (FileNotFoundException e) {
           throw new IllegalStateException("The file with the Entity Scores is missing",e);
        }
    }
        
    @Override
    public void close(){
        if(reader != null){
            try {
                reader.close();
            }catch (IOException e) {
                //ignore
            }
        }
    }
    
}
