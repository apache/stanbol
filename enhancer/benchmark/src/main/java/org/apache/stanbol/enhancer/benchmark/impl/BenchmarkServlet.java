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
package org.apache.stanbol.enhancer.benchmark.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.enhancer.benchmark.BenchmarkParser;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.EscapeTool;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Front-end servlet for the benchmark module */
@Component(immediate=true, metatype=false)
@SuppressWarnings("serial")
public class BenchmarkServlet extends HttpServlet {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference
    private HttpService httpService;
    
    @Reference
    private BenchmarkParser parser;
    
    @Reference
    private EnhancementJobManager jobManager;
    
    public static final String PARAM_CONTENT = "content";
    public static final String DEFAULT_MOUNT_PATH = "/benchmark";
    
    @Property(value=DEFAULT_MOUNT_PATH)
    public static final String MOUNT_PATH_PROPERTY = "mount.path";
    private String mountPath; 
    private final VelocityEngine velocity = new VelocityEngine();

    /** Register with HttpService when activated */
    public void activate(ComponentContext ctx) throws ServletException, NamespaceException {
        mountPath = (String)ctx.getProperties().get(MOUNT_PATH_PROPERTY);
        if(mountPath == null) {
            mountPath = DEFAULT_MOUNT_PATH;
        }
        if(mountPath.endsWith("/")) {
            mountPath = mountPath.substring(mountPath.length() - 1);
        }
        
        httpService.registerServlet(mountPath, this, null, null);
        log.info("Servlet mounted at {}", mountPath);
        
        final Properties config = new Properties();
        config.put("class.resource.loader.description", "Velocity Classpath Resource Loader");
        config.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        config.put("resource.loader","class");
        velocity.init(config);
    }
    
    public void deactivate(ComponentContext ctx) {
        httpService.unregister(mountPath);
    }
    
    @Override
    public String getServletInfo() {
        return "Apache Stanbol Enhancer Benchmarks";
    }

    private VelocityContext getVelocityContext(HttpServletRequest request, String pageTitle) {
        final VelocityContext ctx = new VelocityContext();
        ctx.put("title", getServletInfo() + " - " + pageTitle);
        ctx.put("contextPath", request.getContextPath());
        ctx.put("cssPath", request.getContextPath() + mountPath + "/benchmark.css");
        ctx.put("esc", new EscapeTool());
        return ctx;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        
        if(request.getPathInfo() != null && request.getPathInfo().endsWith(".css")) {
            final Template t = velocity.getTemplate("/velocity/benchmark.css");
            response.setContentType("text/css");
            response.setCharacterEncoding("UTF-8");
            t.merge(getVelocityContext(request, null), response.getWriter());
        } else {
            final Template t = velocity.getTemplate("/velocity/benchmark-input.html");
            final VelocityContext ctx = getVelocityContext(request, "Benchmark Input");
            ctx.put("formAction", request.getContextPath() + mountPath);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            t.merge(ctx, response.getWriter());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        final String content = request.getParameter(PARAM_CONTENT);
        if(content == null) {
            throw new ServletException("Missing " + PARAM_CONTENT + " parameter");
        }
        
        final Template t = velocity.getTemplate("/velocity/benchmark-results.html");
        final VelocityContext ctx = getVelocityContext(request, "Benchmark Results");
        ctx.put("jobManager", jobManager);
        ctx.put("benchmarks", parser.parse(new StringReader(content)));
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        t.merge(ctx, response.getWriter());
    }
}