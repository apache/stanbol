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
package org.apache.stanbol.entityhub.ldpath;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public final class LDPathUtils {

    /**
     * Restrict instantiation
     */
    private LDPathUtils() {}

   /**
     * Utility method that creates a reader over the parsed String using UTF-8 
     * as encoding.<p> This is necessary because currently LDPath only accepts
     * Reader as parameter for parsing {@link Program}s
     * Note that it is not necessary to call {@link InputStream#close()} on the
     * returned {@link InputStreamReader} because this is backed by an in-memory
     * {@link ByteArrayInputStream}.
     * @param string the string to be read by the Reader
     * @return A reader over the parsed string
     * @throws IllegalStateException if 'utf-8' is not supported
     * @throws IllegalArgumentException if <code>null</code> is parsed as string
     */
    public static final Reader getReader(String string) {
        if(string == null){
            throw new IllegalArgumentException("The parsed string MUST NOT be NULL!");
        }
        try {
            return new InputStreamReader(new ByteArrayInputStream(string.getBytes("utf-8")), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Encoding 'utf-8' is not supported by this system!",e);
        }
    }

}
