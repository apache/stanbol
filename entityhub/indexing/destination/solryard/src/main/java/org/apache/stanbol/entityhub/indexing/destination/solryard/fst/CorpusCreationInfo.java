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

import org.apache.commons.lang.ObjectUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.schema.FieldType;

/**
 * Holds the information required to build an FST corpus for a given language
 * @author Rupert Westenthaler
 *
 */
public class CorpusCreationInfo {

    /**
     * The language
     */
    public final String language;
    /**
     * The Corpus FST
     */
    public final File fst;
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
     * The Solr {@link Analyzer} used for the field
     */
    public final Analyzer analyzer;
    
    /** 
     * @param language
     * @param indexField
     * @param analyzer
     * @param fst
     * @param allowCreation
     */
    protected CorpusCreationInfo(String language, String indexField, String storeField, FieldType fieldType, File fst){
        this.language = language;
        this.indexedField = indexField;
        this.storedField = storeField;
        this.fst = fst;
        this.analyzer = fieldType.getAnalyzer();
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
            .append("]");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return indexedField.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof CorpusCreationInfo && 
                ((CorpusCreationInfo)obj).indexedField.equals(indexedField) &&
                ((CorpusCreationInfo)obj).storedField.equals(storedField) &&
                ObjectUtils.equals(language, language);
    }
}
