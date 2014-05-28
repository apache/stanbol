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
package org.apache.stanbol.commons.httpqueryheaders.impl;

import static org.apache.stanbol.commons.httpqueryheaders.Constants.HEARDER_PREFIX;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.httpqueryheaders.OverwriteableHeaderHttpServletRequest;

@Component(immediate=true)
@Service(Filter.class)
@Property(name="pattern",value=".*")
public class QueryHeadersFilter implements Filter {
    
    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                             ServletException {
        HttpServletRequest httpRequest;
        try {
            httpRequest = (HttpServletRequest) request;
        } catch (ClassCastException e) {
            // no Http request -> ignore
            chain.doFilter(request, response);
            return;
        }
        OverwriteableHeaderHttpServletRequest wrapped = null;
        
        Map<String,List<String>> queryParams = parseQueryParams(httpRequest.getQueryString());
        for(Entry<String,List<String>> entry : queryParams.entrySet()){
            String param = entry.getKey();
            if(param != null && param.startsWith(HEARDER_PREFIX) && param.length() > HEARDER_PREFIX.length()+1){
                String header = param.substring(HEARDER_PREFIX.length());
                List<String> values = entry.getValue();
                if(values != null && !values.isEmpty()){
                    if(wrapped == null ){ //lazzy initialisation 
                        wrapped = new OverwriteableHeaderHttpServletRequest(httpRequest);
                    }
                    wrapped.setHeader(header, values.toArray(new String[values.size()]));
                }
            }
        }
        if(wrapped != null){
            chain.doFilter(wrapped, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Get properties parsed to the Filter
        //filterConfig
    }
    private static Map<String, List<String>> parseQueryParams(String query){
        if(query == null){
            return Collections.emptyMap();
        }
        String[] params;
        try {
            params = URLDecoder.decode(query, "UTF-8").split("&");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage(),e);
        }
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String param : params) {
            int idx = param.indexOf('=');
            String name;
            String value;
            if(idx < 0 ){
                name = param;
                value = null;
            } else {
                name = param.substring(0, idx);
                value = param.substring(idx+1);
            }
            List<String> values = map.get(name);
            if(values == null){
                values = new ArrayList<String>(4);
                map.put(name, values);
            }
            values.add(value);
        }
        return map;
    }
}
