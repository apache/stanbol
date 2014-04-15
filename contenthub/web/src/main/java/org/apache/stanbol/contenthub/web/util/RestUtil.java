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

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.CorsHelper;

/**
 * Utility class for REST services
 */
public final class RestUtil {

    /**
     * Restrict instantiation
     */
    private RestUtil() {}

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

    public static MediaType getAcceptedMediaType(HttpHeaders headers, MediaType defaultMediaType) {
        MediaType acceptedMediaType = defaultMediaType;
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

    /**
     * Create a {@link Response} with the given parameters. It also add the necessary headers for CORS by
     * calling the {@link CorsHelper#addCORSOrigin(ServletContext, ResponseBuilder, HttpHeaders)} method.
     * 
     * @param servletContext
     * @param status
     * @param entity
     * @param headers
     * @return
     */
    public static Response createResponse(ServletContext servletContext,
                                          Status status,
                                          Object entity,
                                          HttpHeaders headers) {
        ResponseBuilder rb = Response.status(status);
        addCORSOrigin(servletContext, rb, headers);
        if (entity != null) {
            return rb.entity(entity).build();
        } else {
            return rb.build();
        }
    }
}