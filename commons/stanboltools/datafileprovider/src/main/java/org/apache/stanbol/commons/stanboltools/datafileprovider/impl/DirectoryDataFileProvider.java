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

package org.apache.stanbol.commons.stanboltools.datafileprovider.impl;

import static org.apache.stanbol.commons.stanboltools.datafileprovider.impl.MainDataFileProvider.requireProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,policy=ConfigurationPolicy.REQUIRE, 
    configurationFactory=true, metatype=true)
@Service
@Property(name=Constants.SERVICE_RANKING, intValue=0)
public class DirectoryDataFileProvider implements DataFileProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Property
    public static final String DATA_FILES_FOLDER_PROP = MainDataFileProvider.DATA_FILES_FOLDER_PROP;
    private File dataFilesFolder;

    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        String folderName = requireProperty(ctx.getProperties(), DATA_FILES_FOLDER_PROP, String.class);
        dataFilesFolder = new File(folderName);
        if(!dataFilesFolder.exists()){
            if(!dataFilesFolder.mkdirs()){
                throw new ConfigurationException(DATA_FILES_FOLDER_PROP, "Unable to create the configured Directory "+dataFilesFolder);
            }
        } else if(!dataFilesFolder.isDirectory()){
            throw new ConfigurationException(DATA_FILES_FOLDER_PROP, "The configured DataFile directory "+dataFilesFolder+" does already exists but is not a directory!");
        } //else exists and is a directory!
    }

    
    @Override
    public InputStream getInputStream(final String bundleSymbolicName, final String filename, Map<String,String> comments)
            throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                @Override
                public InputStream run() throws IOException {
                    File dataFile = getDataFile(bundleSymbolicName, filename);
                    if(dataFile == null){
                        throw new IOException(new StringBuilder("Datafile '").append(filename)
                            .append("' not present in directory '"+dataFilesFolder+"'").toString());
                    } else {
                        return new FileInputStream(dataFile);
                    }
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
    
    

    @Override
    public boolean isAvailable(final String bundleSymbolicName, final String filename, Map<String,String> comments) {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws IOException {
                    return getDataFile(bundleSymbolicName, filename) != null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            throw RuntimeException.class.cast(e);
        }
    }
    
    
    /**
     * @param bundleSymbolicName
     * @param filename
     * @return
     */
    private File getDataFile(String bundleSymbolicName, final String filename) {
        // First look for the file in our data folder,
        // with and without bundle symbolic name prefix
        final String [] candidateNames = bundleSymbolicName == null ? 
                new String[]{filename} : 
                    new String[]{
                        bundleSymbolicName + "-" + filename,
                        filename
                    };
        File dataFile = null;
        for(String name : candidateNames) {
            dataFile = new File(dataFilesFolder, name);
            log.debug("Looking for file {}", dataFile.getAbsolutePath());
            if(dataFile.exists() && dataFile.canRead()) {
                log.debug("File found in data files folder: {}", filename);
                break;
            } else {
                dataFile = null;
            }
        }
        return dataFile;
    }

}
