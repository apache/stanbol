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
package org.apache.stanbol.commons.stanboltools.datafileprovider;

import java.util.Date;


/** Used to record a log of {@link DataFileProvider} operations */
public class DataFileProviderEvent {
    private final Date timestamp;
    private final String bundleSymbolicName;
    private final String filename;
    private final String downloadExplanation;
    private final String loadingClass;
    private final String actualFileLocation;
    
    public DataFileProviderEvent(String bundleSymbolicName, String filename, String downloadExplanation, 
            String loadingClass, String actualFileLocation) {
        this.timestamp = new Date();
        this.bundleSymbolicName = bundleSymbolicName;
        this.filename = filename;
        this.downloadExplanation = downloadExplanation;
        this.loadingClass = loadingClass;
        this.actualFileLocation = actualFileLocation;
    }
    
    @Override
    public String toString() {
        
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());

        sb.append(", bundleSymbolicName=");
        sb.append(bundleSymbolicName);
        
        sb.append(", filename=");
        sb.append(filename);

        sb.append(", loadingClass=");
        sb.append(loadingClass);
        
        sb.append(", actualFileLocation=");
        sb.append(actualFileLocation);
        
        return sb.toString();
    }

    /** @return the timestamp of this event */ 
    public Date getTimestamp() {
        return timestamp;
    }

    /** @return the bundle symbolic name that was passed to the DataFileProvider */ 
    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    /** @return the filename that was passed to the DataFileProvider */ 
    public String getFilename() {
        return filename;
    }

    /** @return the download explanation that was passed to the DataFileProvider */ 
    public String getDownloadExplanation() {
        return downloadExplanation;
    }

    /** @return the name of the class which provided the file */ 
    public String getLoadingClass() {
        return loadingClass;
    }
    
    /** @return the actual location of the file that was loaded, empty if file was not found */ 
    public String getActualFileLocation() {
        return actualFileLocation;
    }
}
