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
package org.apache.stanbol.commons.web.base;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Mock {@link HttpHeaders} implementation used to parse request headers for
 * unit tests
 * @author Rupert Westenthaler
 *
 */
public class MockHttpHeaders implements HttpHeaders {

    private MultivaluedMap<String,String> headers;

    protected MockHttpHeaders(MultivaluedMap<String,String> headers){
        this.headers = headers;
    }
    
    @Override
    public List<String> getRequestHeader(String name) {
        return headers.get(name);
    }

    @Override
    public MultivaluedMap<String,String> getRequestHeaders() {
        return headers;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return Collections.singletonList(MediaType.WILDCARD_TYPE);
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return null;
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.TEXT_PLAIN_TYPE;
    }

    @Override
    public Locale getLanguage() {
        return new Locale("en");
    }

    @Override
    public Map<String,Cookie> getCookies() {
        return Collections.emptyMap();
    }

}
