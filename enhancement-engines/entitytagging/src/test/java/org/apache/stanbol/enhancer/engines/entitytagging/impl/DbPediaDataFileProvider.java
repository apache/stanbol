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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;

/**
 * 
 * The Standalone implementation of the ManagedSolrServer uses 
 * {@link ServiceLoader} to search for {@link DataFileProvider}.
 * This implementation ensures that the DBpedia default data index can be loaded
 * and initialised by the StandaloneManagedSolrServer.
 * 
 * @author Rupert Westenthaler
 *
 */
public class DbPediaDataFileProvider implements DataFileProvider {

    private static String DBPEDIA_PREFIX = "org/apache/stanbol/data/site/dbpedia/default/index/";
    
    @Override
    public InputStream getInputStream(String bundleSymbolicName, String filename, Map<String,String> comments) throws IOException {
        String resource = DBPEDIA_PREFIX+filename;
        URL resourceUri = lookupResource(resource);
        if(resourceUri != null){
            return resourceUri.openStream();
        } else {
            throw new IOException("RDFTerm '"+resource+"' not found");
        }
    }

    @Override
    public boolean isAvailable(String bundleSymbolicName, String filename, Map<String,String> comments) {
        return lookupResource(DBPEDIA_PREFIX+filename) != null;
    }
    /**
     * @param resource
     * @return
     */
    private URL lookupResource(String resource) {
        ClassLoader cl = DbPediaDataFileProvider.class.getClassLoader();
        URL resourceUri = cl.getResource(resource);
        if(resourceUri == null){
            cl = Thread.currentThread().getContextClassLoader();
            resourceUri = cl.getResource(resource);
        }
        if(resourceUri == null){
            resourceUri = ClassLoader.getSystemResource(resource);
        }
        return resourceUri;
    }

}
