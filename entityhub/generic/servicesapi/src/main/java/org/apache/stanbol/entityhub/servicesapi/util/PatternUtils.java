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
package org.apache.stanbol.entityhub.servicesapi.util;

import java.util.regex.Pattern;

public final class PatternUtils {
    private PatternUtils(){}
    /**
     * Converts a Wildcard search string to REGEX. If strict is enabled, than
     * the REGEX pattern searches only full labels ("^Patt?er.*$") otherwise
     * it searches the whole text ("Patt?er.*")
     * @param wildcard the wildcard pattern
     * @param strict if <code>true</code> than the REGEX pattern searches
     * whole words.
     * @return the pattern
     */
    public static String wildcardToRegex(String wildcard,boolean strict){
        StringBuilder regex = new StringBuilder();
        if(strict){
            regex.append('^');
        }
        encodeWildcard(wildcard, regex);
        if(strict){
            regex.append('$');
        }
        return regex.toString();
    }

    /**
     * Converts a Wildcard search string to REGEX matching whole words in 
     * the text. 
     * @param wildcard the wildcard pattern
     * @return the regex pattern for the parsed wildcard
     * @since 0.12.1
     */
    public static String wildcardWordToRegex(String wildcard){
        StringBuilder regex = new StringBuilder("\\b");
        encodeWildcard(wildcard, regex);
        return regex.append("\\b").toString();
    }
    /**
     * Internally used to convert a wildcard to a regex
     * @param wildcard
     * @param regex
     */
    private static void encodeWildcard(String wildcard, StringBuilder regex) {
        for (char c : wildcard.toCharArray()) {
            switch(c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    regex.append("\\");//add the escape char
                default:
                    regex.append(c);//add the escape char
                    break;
            }
        }
    }
    
    public static String value2Regex(String value){
        return '^'+escapeRegex(value)+'$';
    }
    /**
     * Creates a regex that matches vales against whole words
     * ('<code>\b{value}\b</code>)
     * @param word the word to match
     * @return the regex to match words
     * @since 0.12.1
     */
    public static String word2Regex(String word){
        return escapeRegex(word, new StringBuilder("\\b")).append("\\b").toString();
    }
    
    public static String escapeRegex(String value){
        return escapeRegex(value, null).toString();
    }
    /**
     * 
     * @param value the value to escape
     * @param sb the {@link StringBuilder} or <code>null</code> if a new 
     * instance should be created
     * @return the parsed {@link StringBuilder} with the escaped value added.
     * @since 0.12.1
     */
    public static StringBuilder escapeRegex(String value, StringBuilder sb){
        if(sb == null){
            sb = new StringBuilder();
        }
        for (char c : value.toCharArray()) {
            switch(c) {
                case '*': case '?': case '(': case ')': case '[': case ']':
                case '$': case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    sb.append("\\"); //add the escape char
                default:
                    sb.append(c); //add the char
                    break;
            }
        }
        return sb;
    }
    public static final Pattern PREFIX_REGEX_PATTERN = Pattern.compile("[\\?\\*]");
    /**
     * Returns <code>true</code> if the parsed value contains an '?' or '*'
     * @param value the value to check
     * @return <code>true</code> if the parsed value contains an '?' or '*'
     */
    public static boolean usesWildCard(String value){
        return value==null?false:PREFIX_REGEX_PATTERN.matcher(value).find();
    }
}
