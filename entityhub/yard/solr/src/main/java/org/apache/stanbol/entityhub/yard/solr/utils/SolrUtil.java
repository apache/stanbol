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

}
