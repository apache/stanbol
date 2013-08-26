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

import org.apache.commons.lang.StringUtils;

/**
 * Enumeration that describes supported Solr Field encodings for multiple
 * languages. In Solr this might be represented by multiple explicitly
 * defined fields, or by a dynamic field configuration. With the Lucene FST 
 * based entity linking engine this is used to allow the configuration
 * of a single field (e.g. rdfs:label) and an algorithm based generation
 * of field names for specific languages (e.g. "rdfs:label@en", 
 * "rdfs:label@de", "rdfs:label@de-AT", ...).
 * <p>
 * This enumeration also defines some utility methods for encoding and decoding
 * of fields
 * 
 * @author Rupert Westenthaler
 *
 */
public enum FieldEncodingEnum {
    /**
     * No encoding is used. This will require to explicitly set a field for
     * all processed languages.
     */
    None,
    /**
     * The encoding as used by the SolrYard implementation of the Entityhub
     */
    SolrYard,
    /**
     * '<code>{lang}-{field-name}</code>' encoding
     */
    MinusPrefix(true,false,'-'),
    /**
     * '<code>{lang}_{field-name}</code>' encoding
     */
    UnderscorePrefix(true,false,'_'),
    /**
     * '<code>{field-name}-{lang}</code>' encoding
     */
    MinusSuffix(false,true,'-'),
    /**
     * '<code>{field-name}_{lang}</code>' encoding
     */
    UnderscoreSuffix(false,true,'_'),
    /**
     * '<code>{lang}@{field-name}</code>' encoding
     */
    AtPrefix(true,false,'@'),
    /**
     * '<code>{field-name}@{lang}</code>' encoding
     */
    AtSuffix(false,true,'@');
    
    private boolean prefix;
    private boolean suffix;
    private char sep;

    private FieldEncodingEnum(){
        this(false,false,' ');
    }
    private FieldEncodingEnum(boolean prefix, boolean suffix, char sep){
        this.prefix = prefix;
        this.suffix = suffix;
        this.sep = sep;
    }
    
    /**
     * Encodes a Solr index field holding {@link Double} values.
     * @param field the field (as configured by the user)
     * @param encoding the Encoding
     * @return Encodes the field if {@link FieldEncodingEnum#SolrYard}. Otherwise
     * it returns the parsed field value.
     */
    public static String encodeDouble(String field, FieldEncodingEnum encoding){
        if(encoding == SolrYard){
            return new StringBuilder("dou").append('/').append(field).append('/').toString();
        } else {
            return field;
        }
    }
    /**
     * Encodes a Solr index field holding {@link Float} values.
     * @param field the field (as configured by the user)
     * @param encoding the Encoding
     * @return Encodes the field if {@link FieldEncodingEnum#SolrYard}. Otherwise
     * it returns the parsed field value.
     */
    public static String encodeFloat(String field, FieldEncodingEnum encoding) {
        if(encoding == SolrYard){
            return new StringBuilder("flo").append('/').append(field).append('/').toString();
        } else {
            return field;
        }
    }
    
    /**
     * Encodes a Solr index field holding URIs.
     * @param field the field (as configured by the user)
     * @param encoding the Encoding
     * @return Encodes the field if {@link FieldEncodingEnum#SolrYard}. Otherwise
     * it returns the parsed field value.
     */
    public static String encodeUri(String field, FieldEncodingEnum encoding){
        if(encoding == SolrYard){
            return  new StringBuilder("ref").append('/').append(field).append('/').toString();
        } else {
            return field;
        }
    }
    
    /**
     * encodes the parsed field and language based on the encoding defined
     * by this enum instance
     * @param field the field
     * @param language the language
     * @return the encoded field
     */
    public static String encodeLanguage(String field, FieldEncodingEnum encoding, String language){
        if(encoding == None){
            return field;
        }
        if(encoding == SolrYard){
            StringBuilder sb = new StringBuilder();
            sb.append('@').append(language).append('/');
            sb.append(field).append('/');
            return sb.toString();
        }
        StringBuilder encoded = new StringBuilder();
        if(encoding.prefix && !StringUtils.isEmpty(language)){
            encoded.append(language).append(encoding.sep);
        }
        encoded.append(field);
        if(encoding.suffix  && !StringUtils.isEmpty(language)){
            encoded.append(encoding.sep).append(language);
        }
        return encoded.toString();
    }
    public static String parseLanguage(String value, FieldEncodingEnum encoding, String field){
        if(encoding == None){
            return null;
        }
        if(encoding == SolrYard){
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
        if(encoding.prefix){
            if(!value.endsWith(field)){
                return null;//no match
            }
            //just subtract the field and the sep from the value
            return value.substring(0,value.length()-field.length()-1);
        }
        if(encoding.suffix){
            if(!value.startsWith(field)){
                return null; //no match
            }
            //just cut the field and the sep from the value
            return value.substring(field.length()+1);
        }
        return null;
    }
}