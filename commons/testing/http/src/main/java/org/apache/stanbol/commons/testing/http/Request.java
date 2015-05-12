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
package org.apache.stanbol.commons.testing.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

/**
 * Request class with convenience withXxx methods to
 * add headers, parameters, etc.
 */
public class Request {

    private final HttpRequestBase request;
//configure on unit test level
//    private String username;
//    private String password;
    private boolean redirects = true;

    Request(HttpRequestBase r) {
        request = r;
    }

    public HttpRequestBase getRequest() {
        return request;
    }

    public Request withHeader(String name, String value) {
        request.addHeader(name, value);
        return this;
    }

    public Request withRedirects(boolean followRedirectsAutomatically) {
        redirects = followRedirectsAutomatically;
        return this;
    }

    private HttpEntityEnclosingRequestBase getHttpEntityEnclosingRequestBase() {
        if (request instanceof HttpEntityEnclosingRequestBase) {
            return (HttpEntityEnclosingRequestBase) request;
        } else {
            throw new IllegalStateException("Request is not an HttpEntityEnclosingRequestBase: "
                    + request.getClass().getName());
        }
    }

    public Request withContent(String content) throws UnsupportedEncodingException {
        return withEntity(new StringEntity(content, "UTF-8"));
    }
    /**
     * Encodes the parsed form content. Strings at even indexes are interpreted
     * as names. Values are {@link URLEncoder#encode(String, String) url encoded}.
     * @param values the [{key-1},{value-1},...,{key-n},{key-n}] values for the form
     * @return the Request with the form content added as {@link StringEntity}.
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     * @throws IllegalArgumentException if an uneven number of elements are in the
     * parsed values or if any parsed key is <code>null</code> or empty.
     */
    public Request withFormContent(String...values) throws UnsupportedEncodingException{
        if(values == null || values.length == 0){
            return withContent("");
        }
        if((values.length%2) != 0){
            throw new IllegalArgumentException("The number of values MUST BE an even number");
        }
        StringBuilder content = new StringBuilder();
        for(int i = 0;i<values.length;i+=2){
            if(values[i] == null || values[i].isEmpty()){
               throw new IllegalArgumentException("The name of the '"+(i/2)+
                   "' parameter MUST NOT be NULL nor empty (value='"+
                   values[i+1]+"')!");
            }
            if(i > 0){
                content.append('&');
            }
            content.append(values[i]);
            if(values[i+1] != null && !values[i+1].isEmpty()){
                content.append('=')
                    .append(URLEncoder.encode(values[i+1], "UTF-8"));
            }
            
        }
        return withContent(content.toString());
    }
    public Request withEntity(HttpEntity e) {
        getHttpEntityEnclosingRequestBase().setEntity(e);
        return this;
    }

//    public String getUsername() {
//        return username;
//    }

//    public String getPassword() {
//        return password;
//    }

    public boolean getRedirects() {
        return redirects;
    }
}
