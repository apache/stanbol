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
package org.apache.stanbol.contenthub.web.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * Utility class for REST services
 */
public class RestUtil {
    
    public static final Set<String> supportedMediaTypes;
    static {
        Set<String> types = new HashSet<String>();
        types.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }
    
    /**
     * @param parameter
     *            parameter to be checked
     * @return <code>null</code> if parameter has an empty content, otherwise trimmed <code>parameter</code>
     */
    public static String nullify(String parameter) {
        if (parameter != null) {
            parameter = parameter.trim();
            if (parameter.isEmpty() || parameter.equals("null")) {
                parameter = null;
            }
        }
        return parameter;
    }
    
    public static boolean isJSONaccepted(HttpHeaders headers) {
        if (!headers.getAcceptableMediaTypes().isEmpty()) {
            for (MediaType accepted : headers.getAcceptableMediaTypes()) {
                if (!accepted.isWildcardType()) {
                    if (accepted.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean isHTMLaccepted(HttpHeaders headers) {
        if (!headers.getAcceptableMediaTypes().isEmpty()) {
            for (MediaType accepted : headers.getAcceptableMediaTypes()) {
                if (!accepted.isWildcardType()) {
                    if (accepted.isCompatible(MediaType.TEXT_HTML_TYPE)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static MediaType getAcceptedMediaType(HttpHeaders headers) {
        MediaType acceptedMediaType = MediaType.APPLICATION_JSON_TYPE; // default
        if (!headers.getAcceptableMediaTypes().isEmpty()) {
            for (MediaType accepted : headers.getAcceptableMediaTypes()) {
                if (!accepted.isWildcardType()) {
                    if (accepted.isCompatible(MediaType.TEXT_HTML_TYPE)) {
                        acceptedMediaType = MediaType.TEXT_HTML_TYPE;
                        break;
                    } else if (accepted.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                        acceptedMediaType = MediaType.APPLICATION_JSON_TYPE;
                        break;
                    }
                }
            }
        }
        return acceptedMediaType;
    }
}