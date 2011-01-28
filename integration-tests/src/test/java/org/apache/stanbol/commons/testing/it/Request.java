/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.commons.testing.it;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

/** Request class with convenience with... methods to 
 *  add headers, parameters etc.
 */
public class Request {
    private final HttpUriRequest request;
    
    Request(HttpUriRequest r) {
        request = r;
    }
    
    public HttpUriRequest getRequest() {
        return request;
    }
    
    public Request withHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }
    
    public Request withContent(String content) throws UnsupportedEncodingException {
        if(request instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase)request).setEntity(new StringEntity(content, "UTF-8"));
        } else {
            throw new IllegalStateException("Cannot add content to request " 
                    + request.getClass().getName());
        }
        return this;
    }    
}
