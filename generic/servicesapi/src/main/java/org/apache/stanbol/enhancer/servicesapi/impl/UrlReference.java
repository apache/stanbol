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
package org.apache.stanbol.enhancer.servicesapi.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;


/**
 * Allows to use a URL for referencing a content.
 */
public class UrlReference implements ContentReference {

    final URL url;
    /**
     * Uses the passed URI string to parse the URL.
     * @param uri an absolute URI that can be converted to an URL
     * @throws IllegalArgumentException if the passed URI string is <code>null</code>
     * or can not be converted to an {@link URL}
     */
    public UrlReference(String uri) {
        if(uri == null){
            throw new IllegalArgumentException("The parsed URI reference MUST NOT be NULL!");
        }
        try {
            this.url = URI.create(uri).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("the passed URI can not be converted to an URL",e);
        }
    }
    public UrlReference(URL url) {
        if(url == null){
            throw new IllegalArgumentException("The parsed URL MUST NOT be NULL!");
        }
        this.url = url;
    }
    
    @Override
    public String getReference() {
        return url.toString();
    }

    @Override
    public ContentSource dereference() throws IOException {
        URLConnection uc = url.openConnection();
        return new StreamSource(uc.getInputStream(),
            uc.getContentType(), uc.getHeaderFields());
    }
    
}