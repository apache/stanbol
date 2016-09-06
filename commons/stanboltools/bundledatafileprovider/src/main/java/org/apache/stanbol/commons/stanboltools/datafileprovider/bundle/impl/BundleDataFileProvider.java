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
package org.apache.stanbol.commons.stanboltools.datafileprovider.bundle.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link DataFileProvider} interface that uses the
 * {@link Bundle#getResource(String)} method to load data files. This
 * method uses the Bundle classpath to search for resource.<p>
 * Note that this provider searches only the resources within this bundle. The
 * bundle classpath is NOT used!<p>
 * @author Rupert Westenthaler
 *
 */
public class BundleDataFileProvider implements DataFileProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Bundle bundle;
    /**
     * List with the paths to search. Guaranteed to contain at least a single
     * Element. All contained paths end with {@link File#separator} 
     */
    private List<String> searchPaths;
    
    /**
     * Creates a {@link DataFileProvider} that uses the {@link Bundle} to lookup
     * data files in the directories specified by the parsed relatives path.
     * @param bundle the bundle context used to initialise this DataFileProvider
     * @param searchPaths the relative paths to the directories used to search
     * for requested data files. The parsed paths are searches in the provided
     * order. Parsed paths are normalised by adding missing {@link File#separator}
     * to its end. if <code>null</code> or an empty list is parsed data files are
     * searched relative to the root folder of the bundle. Adding an empty
     * String or the <code>null</code> element allows to search the root folder in
     * addition to other paths.
     */
    public BundleDataFileProvider(Bundle bundle,List<String> searchPaths) {
        if(bundle == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        this.bundle = bundle;
        if(searchPaths == null || searchPaths.isEmpty()){
            this.searchPaths = Collections.singletonList(File.separator);
        } else {
            List<String> paths = new ArrayList<String>(searchPaths.size());
            for(String path : searchPaths){
                if(path == null){ //null element is interpreted as the "" path
                    path = "/";
                } else {
                    //we need Unix style '/' to search resources within bundles
                    //even on Windows! (see STANBOL-259)
                    path = FilenameUtils.separatorsToUnix(path);
                    if(!path.endsWith("/")){ //normalise
                        path = path+'/';
                    }
                }
                if(!paths.contains(path)){ //do not add paths more than once
                    paths.add(path);
                }
            }
            this.searchPaths = Collections.unmodifiableList(paths);
        }
    }
    
    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) throws IOException {
        URL resource = getDataFile(bundleSymbolicName, filename);
        log.debug("RDFTerm {} found: {}", (resource == null ? "NOT" : ""), filename);
        return resource != null ? resource.openStream() : null;
    }

    /**
     * @param bundleSymbolicName
     * @param filename
     * @return
     */
    private URL getDataFile(String bundleSymbolicName, String filename) {
        //If the symbolic name is not null check that is equals to the symbolic
        //name used to create this classpath data file provider
        if(bundleSymbolicName != null && 
                !bundle.getSymbolicName().equals(bundleSymbolicName)) {
            log.debug("Requested bundleSymbolicName {} does not match mine ({}), request ignored",
                    bundleSymbolicName, bundle.getSymbolicName());
            return null;
        }
        URL resource = null;
        Iterator<String> relativePathIterator = searchPaths.iterator();
        while(resource == null && relativePathIterator.hasNext()){
            String path = relativePathIterator.next();
            String resourceName = path != null ? path + filename : filename ;
            resource = bundle.getEntry(resourceName);
        }
        return resource;
    }
    @Override
    public boolean isAvailable(String bundleSymbolicName, String filename, Map<String,String> comments) {
        return getDataFile(bundleSymbolicName, filename) != null;
    }
    /**
     * Getter for the search paths
     * @return the search paths (read only)
     */
    public final List<String> getSearchPaths() {
        return searchPaths;
    }
}
