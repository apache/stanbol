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
package org.apache.stanbol.entityhub.indexing.destination.solryard.fst;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FstConfig {
    
    protected final Logger log = LoggerFactory.getLogger(FstConfig.class);
    
    private final String indexField;
    private final String storeField;
    private final String fstName;
    private File fstDirectory;
    /**
     * FST corpus configuration
     */
    private Map<String,CorpusCreationInfo> corpusInfos = new HashMap<String,CorpusCreationInfo>();

    public FstConfig(String indexField){
        this(indexField,null);
    }
    
    public FstConfig(String indexField, String storeField){
        this.indexField = indexField;
        this.storeField = storeField == null ? indexField : storeField;
        this.fstName = getFstFileName(indexField);
    }

    public void setFstDirectory(File fstDirectory) {
        this.fstDirectory = fstDirectory;
    }
    
    public File getFstDirectory() {
        return fstDirectory;
    }
    
    protected final CorpusCreationInfo addCorpus(CorpusCreationInfo corpus){
        if(corpus != null){
            return corpusInfos.put(corpus.language, corpus);
        } else {
            return null;
        }
    }
    
    public Collection<CorpusCreationInfo> getCorpusCreationInfos(){
        return Collections.unmodifiableCollection(corpusInfos.values());
    }
    
    public CorpusCreationInfo getCorpusCreationInfo(String language){
        return corpusInfos.get(language);
    }
    public boolean isLanguage(String language){
        return corpusInfos.containsKey(language);
    }
    
    public Set<String> getLanguages(){
        return Collections.unmodifiableSet(corpusInfos.keySet());
    }
    /**
     * Inspects the SolrCore to get defined languages for the configured
     * {@link #indexField} and {@link #storeField}. Initialises the
     * {@link #getCorpusCreationInfos()}
     * @param schema the schema of the SolrCore
     * @param indexReader the index reader of the SolrCore
     */
    public void buildConfig(IndexSchema schema, AtomicReader indexReader){
        FieldInfos fieldInfos = indexReader.getFieldInfos(); //we need this twice
        String fieldWildcard = encodeLanguage(indexField,"*");
        for(FieldInfo fieldInfo : fieldInfos){
            //try to match the field names against the wildcard
            if(FilenameUtils.wildcardMatch(fieldInfo.name, fieldWildcard)){
                //for matches parse the language from the field name
                String language = parseLanguage(fieldInfo.name, indexField);
                if(language != null){
                    //generate the FST file name
                    StringBuilder fstFileName = new StringBuilder(fstName);
                    if(!language.isEmpty()){
                        fstFileName.append('.').append(language);
                    }
                    fstFileName.append(".fst");
                    File fstFile = new File(fstDirectory,fstFileName.toString());
                    //get the FieldType of the field from the Solr schema
                    FieldType fieldType = schema.getFieldTypeNoEx(fieldInfo.name);
                    if(fieldType != null){ //if the fieldType is present
                        //we need also to check if the stored field with
                        //the labels is present
                        //get the stored Field and check if it is present!
                        String storeFieldName;
                        if(storeField == null){ //storeField == indexField
                            storeFieldName = fieldInfo.name;
                        } else { // check that the storeField is present in the index
                            storeFieldName = encodeLanguage(storeField, language);
                            FieldInfo storedFieldInfos = fieldInfos.fieldInfo(storeFieldName);
                            if(storedFieldInfos == null){
                                log.warn(" ... ignore language {} because Stored Field {} "
                                        + "for IndexField {} does not exist! ", new Object[]{
                                        language,storeFieldName,fieldInfo.name});
                                storeFieldName = null;
                            }
                            
                        }
                        if(storeFieldName != null){ // == valid configuration
                            CorpusCreationInfo fstInfo = new CorpusCreationInfo(language, 
                                fieldInfo.name, storeFieldName,  
                                fieldType, fstFile);
                            log.info(" ... init {} ", fstInfo);
                            addCorpus(fstInfo);
                        }
                    } else {
                        log.warn(" ... ignore language {} becuase unknown fieldtype "
                            + "for SolrFied {}",language,fieldInfo.name);
                    }
                } //else the field matched the wildcard, but has not passed the
                //encoding test.
            } //Solr field does not match the field definition in the config
        } // end iterate over all fields in the SolrIndex        
    }
    
    protected static String encodeLanguage(String field, String language){
        StringBuilder sb = new StringBuilder();
        sb.append('@').append(language).append('/');
        sb.append(field).append('/');
        return sb.toString();

    }
    
    protected static String parseLanguage(String value, String field){
        int atIndex = value.indexOf('@');
        int slashIndex = value.indexOf('/');
        //expect @{lang}/{field}/
        if(value.indexOf(field, slashIndex) != value.length()-1-field.length()){
            return null; //no match
        }
        if(atIndex == 0 && slashIndex > 0){
            return value.substring(1,slashIndex);
        } else {
            return null;//no match
        }
    }
    
    /**
     * Getter for the default FST file name based on the configured field
     * name. This method returns the '<code>{name}</code>' part of the
     * '<code>{name}.{lang}.fst</code>' name.
     * @param fstFieldName the field name.
     * @return the '<code>{name}</code>' part of the'<code>{name}.{lang}.fst</code>' name
     */
    protected static String getFstFileName(final String fstFieldName) {
        String fstName;
        if(!StringUtils.isAlphanumeric(fstFieldName)) {
            StringBuilder escaped = new StringBuilder(fstFieldName.length());
            for(int i = 0; i < fstFieldName.length();i++){
                int codepoint = fstFieldName.codePointAt(i);
                if(Character.isLetterOrDigit(codepoint)){
                    escaped.appendCodePoint(codepoint);
                } else {
                    escaped.append('_');
                }
            }
            fstName = escaped.toString();
        } else {
            fstName = fstFieldName;
        }
        return fstName;
    }    
    
    @Override
    public String toString() {
        return new StringBuilder("FSTConfig[index: ").append(indexField)
                .append(" | store: ").append(storeField).append(']').toString();
    }
}
