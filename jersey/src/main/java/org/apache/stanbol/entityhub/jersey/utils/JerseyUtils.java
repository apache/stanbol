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
package org.apache.stanbol.entityhub.jersey.utils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

public final class JerseyUtils {

    private JerseyUtils() {/* do not create instance of Util Classes */}

    /**
     * Searches the Header for acceptable media types and returns the first found
     * that is not the wildcard type. If no one is found the parsed default type
     * is returned.
     *
     * @param headers the request headers
     * @param defaultType the default type if no or only the wildcard type was found in
     * the header
     * @return the acceptable media type
     */
    public static MediaType getAcceptableMediaType(HttpHeaders headers, MediaType defaultType) {
        MediaType acceptedMediaType = null;
        if (!headers.getAcceptableMediaTypes().isEmpty()) {
            for (MediaType accepted : headers.getAcceptableMediaTypes()) {
                if (!accepted.isWildcardType()) {
                    acceptedMediaType = accepted;
                    break;
                }
            }
        }
        if (acceptedMediaType == null) {
            acceptedMediaType = defaultType;
        }
        return acceptedMediaType;
    }

}
