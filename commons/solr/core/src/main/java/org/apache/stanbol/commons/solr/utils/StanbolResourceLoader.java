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
package org.apache.stanbol.commons.solr.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr {@link ResourceLoader} implementation that supports adding an parent as
 * well as parsing the classloader used for 
 * {@link #newInstance(String, Class)}.<p>
 * This implementation can be used in combination with the 
 * {@link DataFileResourceLoader} to allow providing resources via the
 * Stanbol {@link DataFileProvider} infrastructure.<p>
 * The {@link #newInstance(String, Class)} method uses the same algorithm as
 * the {@link SolrResourceLoader#newInstance(String, Class)} method to
 * build candidate class names. It supports the default packages.
 * @author Rupert Westenthaler
 *
 */
public class StanbolResourceLoader implements ResourceLoader {

    private Logger log = LoggerFactory.getLogger(StanbolResourceLoader.class);
    
    static final String project = "solr";
    static final String base = "org.apache" + "." + project;
    static final String[] packages = {"","analysis.","schema.","handler.","search.","update.","core.","response.","request.","update.processor.","util.", "spelling.", "handler.component.", "handler.dataimport." };
    
    protected final ClassLoader classloader;
    protected final ResourceLoader parent;
    
    public StanbolResourceLoader(){
        this(null,null);
    }
    
    public StanbolResourceLoader(ClassLoader classloader){
        this(classloader,null);
    }
    
    public StanbolResourceLoader(ResourceLoader parent){
        this(null,parent);
    }
    
    public StanbolResourceLoader(ClassLoader classloader, ResourceLoader parent){
        this.classloader = classloader == null ? StanbolResourceLoader.class.getClassLoader() : classloader;
        this.parent = parent;
    }
    
    
    @Override
    public InputStream openResource(String resource) throws IOException {
        InputStream in;
        String parentMessage = null;
        if(parent != null){
            try {
                in = parent.openResource(resource);
            } catch (IOException e) {
                in = null;
                parentMessage = e.getMessage();
            } catch (SecurityException e) { //do not catch security related exceptions
                throw e;
            } catch (RuntimeException e) {
                in = null;
                parentMessage = e.getMessage();
            }
        } else {
            in = null;
        }
        if(in == null){
            in = classloader.getResourceAsStream(resource);
        }
        if(in == null){
            throw new IOException("Unable to load RDFTerm '"+resource+"' from "
                + (parent != null ? ("parent (message: "+parentMessage+") and from") : "")
                + "classpath!");
        }
        return in;
    }

    public List<String> getLines(String resource) throws IOException {
        List<String> lines = new ArrayList<String>();
        LineIterator it = IOUtils.lineIterator(openResource(resource), "UTF-8");
        while(it.hasNext()){
            String line = it.nextLine();
            if(line != null && !line.isEmpty() && line.charAt(0) != '#'){
                lines.add(line);
            }
        }
        return lines;
    }
    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
        String parentMessage = null;
        Class<? extends T> clazz = null;
        if(parent != null){
            try {
                clazz = parent.findClass(cname, expectedType);
            } catch (SecurityException e) { //do not catch security related exceptions
                throw e;
            } catch (RuntimeException e) {
                parentMessage = e.getMessage();
            }
        }
        if(clazz == null){
            try {
                clazz = (Class<T>) classloader.loadClass(cname);
            } catch (Exception e) {
                String newName = cname;
                if (newName.startsWith(project)) {
                    newName = cname.substring(project.length() + 1);
                }
                for (String subpackage : packages) {
                    try {
                        String name = base + '.' + subpackage + newName;
                        log.trace("Trying class name " + name);
                        clazz = (Class<T>) classloader.loadClass(name);
                        break;
                    } catch (Exception e1) {
                        // ignore... assume first exception is best.
                    }
                }
            }
            
        }
        if(clazz == null){
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, 
                "Error loading class '" + cname + "' " + (parent != null ? 
                        ("from parent (message: "+parentMessage+") and ") : "")
                        + "via Classloader "+classloader);

        }
        return clazz;
    }

    @Override
    public <T> T newInstance(String cname, Class<T> expectedType) {
        Class<? extends T> clazz = findClass(cname, expectedType);
        try {
          return clazz.newInstance();
        } catch (Exception e) {
          throw new SolrException( SolrException.ErrorCode.SERVER_ERROR,
              "Error instantiating class: '" + clazz.getName()+"'", e);
        }
        

    }
}
