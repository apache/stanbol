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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.enhancer.benchmark.BenchmarkEngine;
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
    private BenchmarkEngine benchmarkEngine;
    
    public static final String PARAM_CONTENT = "content";
    public static final String DEFAULT_MOUNT_PATH = "/benchmark";
    
    @Property(value=DEFAULT_MOUNT_PATH)
    public static final String MOUNT_PATH_PROPERTY = "mount.path";
    private String mountPath; 

    // TODO temp example
    public static final String EXAMPLE =
        "= INPUT =\n"
        + "# Comments such as this one are ignored\n"
        + "# This is the enhancer input, can be split on several lines\n"
        + "Bob\n"
        + "Marley was born in Kingston, Jamaica.\n"
        + "\n"
        + "= EXPECT =\n"
        + "# EXPECT defines groups of predicate/object matchers that we expect to find in the output\n"
        + "# Each group applies to one given enhancement: for the expectation to succeed, at least\n"
        + "# one enhancement must match all lines in the group\n"
        + "\n"
        + "Description: Kingston must be found\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Kingston%2C_Jamaica\n"
        + "\n"
        + "Description: This one should fail\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Basel\n"
        + "\n"
        + "# The description: line starts a new group\n"
        + "Description: Bob Marley must be found as a musical artist\n"
        + "http://fise.iks-project.eu/ontology/entity-type URI http://dbpedia.org/ontology/MusicalArtist\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Bob_Marley\n"
        + "\n"
        + "= COMPLAIN =\n"
        + "\n"
        + "Description: Miles Davis must not be found\n"
        + "http://fise.iks-project.eu/ontology/entity-type URI http://dbpedia.org/ontology/MusicalArtist\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Miles_Davis\n"
        + "\n"
        + "Description: Bob Marley in the COMPLAIN section should fail\n"
        + "http://fise.iks-project.eu/ontology/entity-type URI http://dbpedia.org/ontology/MusicalArtist\n"
        + "http://fise.iks-project.eu/ontology/entity-reference URI http://dbpedia.org/resource/Bob_Marley\n"
    ;
        
    /** Register with HttpService when activated */
    public void activate(ComponentContext ctx) throws ServletException, NamespaceException {
        mountPath = (String)ctx.getProperties().get(MOUNT_PATH_PROPERTY);
        if(mountPath == null) {
            mountPath = DEFAULT_MOUNT_PATH;
        }
        
        httpService.registerServlet(mountPath, this, null, null);
        log.info("Servlet mounted at {}", mountPath);
    }
    
    public void deactivate(ComponentContext ctx) {
        httpService.unregister(mountPath);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        
        // TODO need a nicer page...
        final StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<form method='POST'>");
        sb.append("<input type='submit'/><br/>");
        sb.append("<textarea name='content' rows='60' cols='120'>" + EXAMPLE + "</textarea>");
        sb.append("</form>");
        sb.append("</body></html>");
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(sb.toString());
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
        final String content = request.getParameter(PARAM_CONTENT);
        if(content == null) {
            throw new ServletException("Missing " + PARAM_CONTENT + " parameter");
        }
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        try {
            benchmarkEngine.runBenchmark(content, response.getWriter());
        } catch(Exception e) {
            // TODO better error reporting
            log.error("Exception in runBenchmark", e);
            response.getWriter().write(e.toString());
        }
  }
 }