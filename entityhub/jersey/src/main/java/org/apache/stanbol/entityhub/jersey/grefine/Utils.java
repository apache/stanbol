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
package org.apache.stanbol.entityhub.jersey.grefine;

import static org.apache.commons.lang.StringUtils.getLevenshteinDistance;

import org.apache.commons.lang.StringUtils;

public final class Utils {

   /**
     * Restrict instantiation
     */
    private Utils() {}

   /**
     * Compares two strings (after {@link StringUtils#trim(String) trimming} 
     * by using the Levenshtein's Edit Distance of the two
     * strings. Does not return the {@link Integer} number of changes but
     * <code>1-(changes/maxStringSizeAfterTrim)</code><p>
     * @param s1 the first string
     * @param s2 the second string
     * @return the distance
     * @throws IllegalArgumentException if any of the two parsed strings is NULL
     */
    public static double levenshtein(String s1, String s2) {
        if(s1 == null || s2 == null){
            throw new IllegalArgumentException("NONE of the parsed String MUST BE NULL!");
        }
        s1 = StringUtils.trim(s1);
        s2 = StringUtils.trim(s2);
        return s1.isEmpty() || s2.isEmpty() ? 0 :
            1.0 - (((double)getLevenshteinDistance(s1, s2)) / ((double)(Math.max(s1.length(), s2.length()))));
    }
}
