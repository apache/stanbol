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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.BenchmarkParser;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
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

    @Reference
    private ContentItemFactory ciFactory;
    
    @Reference
    private Serializer graphSerializer;
    /**
     * Needed to lookup active enhancement changes as parsed by {@link ChainState}
     */
    @Reference
    private ChainManager chainManager;
   
    public static final String PARAM_CONTENT = "content";
    private static final String PARAM_CHAIN = "chain";
    
    public static final String DEFAULT_MOUNT_PATH = "/benchmark";
    public static final String DEFAULT_BENCHMARK = "default.txt";
    public static final String EXAMPLE_BENCHMARKS_RESOURCE_ROOT = "/examples";
    
    @Property(value=DEFAULT_MOUNT_PATH)
    public static final String MOUNT_PATH_PROPERTY = "mount.path";

    private String mountPath; 
    private final VelocityEngine velocity = new VelocityEngine();
    
    // Formatter for benchmark graphs
    public static class GraphFormatter {
        private final Serializer serializer;
        
        GraphFormatter(Serializer s) {
            serializer = s;
        }
        
        public String format(ImmutableGraph g, String mimeType) throws UnsupportedEncodingException {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serializer.serialize(bos, g, mimeType);
            return bos.toString("UTF-8");
        }
    };

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
        config.put("class.resource.loader.description", "Velocity Classpath RDFTerm Loader");
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
        
        final String path = request.getPathInfo() == null ? "" : request.getPathInfo(); 
        if(path.endsWith(".css")) {
            // Serve our css
            final Template t = getTemplate("/velocity/benchmark.css");
            response.setContentType("text/css");
            response.setCharacterEncoding("UTF-8");
            t.merge(getVelocityContext(request, null), response.getWriter());
            
        } else if(path.length() < 2){
            // No benchmark specified -> redirect to default
            response.sendRedirect(getExampleBenchmarkPath(request, DEFAULT_BENCHMARK));
            
        } else {
            // Benchmark input form pre-filled with selected example
            final Template t = getTemplate("/velocity/benchmark-input.html");
            final VelocityContext ctx = getVelocityContext(request, "Benchmark Input");
            ctx.put("formAction", request.getContextPath() + mountPath);
            ctx.put("benchmarkText", getBenchmarkText(path)); 
            ctx.put("benchmarkPaths", getExampleBenchmarkPaths(request)); 
            ctx.put("currentBenchmarkPath", path); 
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            t.merge(ctx, response.getWriter());
        }
    }

    /**
     * @return
     */
    private Template getTemplate(String templatePath) {
        final Template t;
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BenchmarkServlet.class.getClassLoader()); 
        try {
            t = velocity.getTemplate(templatePath);
        } finally {
            Thread.currentThread().setContextClassLoader(tcl);
        }
        return t;
    }
    
    private String getExampleBenchmarkPath(HttpServletRequest request, String name) {
        return request.getContextPath() + mountPath + "/" + name;
    }
    
    private List<String> getExampleBenchmarkPaths(HttpServletRequest request) throws IOException {
        // TODO how to enumerate bundle resources?
        final String list = getBenchmarkText("/LIST.txt");
        final LineIterator it = new LineIterator(new StringReader(list));
        final List<String> result = new LinkedList<String>();
        while(it.hasNext()) {
            result.add(getExampleBenchmarkPath(request, it.nextLine()));
        }
        return result;
    }

    /** Return example benchmark text from our class resources */
    private String getBenchmarkText(String path) throws IOException {
        final InputStream is = getClass().getResourceAsStream(EXAMPLE_BENCHMARKS_RESOURCE_ROOT + path);
        if(is == null) {
            return "";
        }
        
        try {
            return IOUtils.toString(is);
        } finally {
            is.close();
        }
    }
    
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) 
    throws ServletException, IOException {
        final String content = request.getParameter(PARAM_CONTENT);
        if(content == null) {
            throw new ServletException("Missing " + PARAM_CONTENT + " parameter");
        }
        String chainName = request.getParameter(PARAM_CHAIN);
        final Template t = AccessController.doPrivileged(new PrivilegedAction<Template>() {
            @Override
            public Template run() {
                return getTemplate("/velocity/benchmark-results.html");
            }
        });
        final VelocityContext ctx = getVelocityContext(request, "Benchmark Results");
        ctx.put("contentItemFactory", ciFactory);
        ctx.put("jobManager", jobManager);
        List<? extends Benchmark> benchmarks = parser.parse(new StringReader(content));
        if(chainName != null && !chainName.isEmpty()){
            Chain chain = chainManager.getChain(chainName);
            if(chain == null){
                response.setStatus(404);
                PrintWriter w = response.getWriter();
                w.println("Unable to perform benchmark on EnhancementChain '"
                    + StringEscapeUtils.escapeHtml(chainName) +"' because no chain with that name is active!");
                IOUtils.closeQuietly(w);
                return;
            }
            for(Benchmark benchmark : benchmarks){
                benchmark.setChain(chain);
            }
        }
        ctx.put("benchmarks", benchmarks);
        ctx.put("graphFormatter", new GraphFormatter(graphSerializer));
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run()  throws IOException{
                    t.merge(ctx, response.getWriter());
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof IOException){
                throw (IOException)e;
            } else {
                throw RuntimeException.class.cast(e);
            }
            
        }
    }
}