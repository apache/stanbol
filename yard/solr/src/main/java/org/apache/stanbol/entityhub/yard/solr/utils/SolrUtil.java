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
package org.apache.stanbol.entityhub.yard.solr.utils;

import java.util.regex.Pattern;

import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;


public final class SolrUtil {
    private SolrUtil(){}

    private static final String LUCENE_ESCAPE_CHARS = "[\\\\+\\-\\!\\(\\)\\:\\^\\[\\]\\{\\}\\~\\*\\?]";
    private static final Pattern LUCENE_PATTERN = Pattern.compile(LUCENE_ESCAPE_CHARS);
    private static final String REPLACEMENT_STRING = "\\\\$0";


    /**
     * Escapes all special chars in an string (field name or constraint) to be
     * used in an SolrQuery.
     * @param string the string to be escaped
     * @return the escaped string
     */
    public static String escapeSolrSpecialChars(String string) {
        return string != null?LUCENE_PATTERN.matcher(string).replaceAll(REPLACEMENT_STRING):null;
    }
    /**
     * This method encodes a parsed index value as needed for queries.<p> 
     * In case of TXT it is assumed that a whitespace tokenizer is used
     * by the index. Therefore values with multiple words need to be
     * treated and connected with AND to find only values that contain all.
     * In case of STR no whitespace is assumed. Therefore spaces need to
     * be replaced with '+' to search for tokens with the exact name.
     * In all other cases the string need not to be converted.
     * 
     * Note also that text queries are converted to lower case
     * @param value the index value
     * @return the (possible multiple) values that need to be connected with AND
     */
    public static String[] encodeQueryValue(IndexValue indexValue,boolean escape){
        if(indexValue == null){
            return null;
        }
        String[] queryConstraints;
        String value;
        if(escape){
            value = SolrUtil.escapeSolrSpecialChars(indexValue.getValue());
        } else {
            value = indexValue.getValue();
        }
        if(IndexDataTypeEnum.TXT.getIndexType().equals(indexValue.getType())){
            value = value.toLowerCase();
            queryConstraints = value.split(" ");
        } else if(IndexDataTypeEnum.STR.equals(indexValue.getType())){
            value = value.toLowerCase();
            queryConstraints = new String[]{value.replace(' ', '+')};
        } else {
            queryConstraints = new String[]{value};
        }
        return queryConstraints;
    }

}
