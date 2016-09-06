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
package org.apache.stanbol.commons.cors;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSConfigurationException;
import com.thetransactioncompany.cors.CORSFilter;

/**
 * Wraps a {@link CORSFilter} in order to provide CORS support.
 * @author Cristian Petroaca
 *
 */
@Component(immediate = true)
@Service(Filter.class)
@Property(name = "pattern", value = ".*")
public class StanbolCorsFilter implements Filter {
    private static CORSFilter corsFilter;
    
    public StanbolCorsFilter() throws CORSConfigurationException {
        Properties props = new Properties();
        props.put("cors.allowGenericHttpRequests", true);
        props.put("cors.allowOrigin", "*");
        props.put("cors.allowSubdomains", true);
        props.put("cors.supportedMethods", "GET, POST, HEAD, OPTIONS");
        props.put("cors.supportedHeaders", "*");
        props.put("cors.supportsCredentials", true);
        props.put("cors.maxAge", 1800);
        
        corsFilter = new CORSFilter(new CORSConfiguration(props));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //NOOP
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        corsFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
        corsFilter.destroy();
    }
}
