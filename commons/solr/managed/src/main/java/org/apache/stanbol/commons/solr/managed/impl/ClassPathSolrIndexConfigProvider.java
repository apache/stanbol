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
package org.apache.stanbol.commons.solr.managed.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassPathSolrIndexConfigProvider implements DataFileProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Solr Core configuration are loaded form "solr/core/{core-name}
     */
    public static final String INDEX_BASE_PATH = "solr/core/";
    
    private final String symbolicName;
    /**
     * Creates a DataFileProvider that loads SolrIndexConfigurations via the
     * classpath relative to {@value #INDEX_BASE_PATH}.
     * @param bundleSymbolicName the symbolic name of the bundle to accept
     * requests from or <code>null</code> to accept any request.
     */
    public ClassPathSolrIndexConfigProvider(String bundleSymbolicName) {
        symbolicName = bundleSymbolicName;
    }
    
    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) 
    throws IOException {
        final URL dataFile = getDataFile(bundleSymbolicName, filename);
        
        // Returning null is fine - if we don't have the data file, another
        // provider might supply it
        return dataFile != null ? dataFile.openStream() : null;
    }
    
    @Override
    public boolean isAvailable(String bundleSymbolicName, String filename, Map<String,String> comments) {
        return getDataFile(bundleSymbolicName, filename) != null;
    }

    /**
     * @param bundleSymbolicName
     * @param filename
     * @return
     */
    private URL getDataFile(String bundleSymbolicName, String filename) {
        //if the parsed bundleSymbolicName is null accept any request
        //if not, than check if the request is from the correct bundle.
        if(bundleSymbolicName != null && !bundleSymbolicName.equals(bundleSymbolicName)) {
            log.debug("Requested bundleSymbolicName {} does not match mine ({}), request ignored",
                    bundleSymbolicName, symbolicName);
            return null;
        }
        
        // load default OpenNLP models from classpath (embedded in the defaultdata bundle)
        final String resourcePath = INDEX_BASE_PATH + filename;
        final URL dataFile = getClass().getClassLoader().getResource(resourcePath);
        //log.debug("Resource {} found: {}", (in == null ? "NOT" : ""), resourcePath);
        return dataFile;
    }

}
