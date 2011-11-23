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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Used to record a log of {@link DataFileProvider} operations.
 */
public class DataFileProviderEvent {

    //replaced by Collections#emptyMap() to ensure that a shared instance
    //between events is not changed!
    private static final Map<String, String> EMPTY_COMMENTS = Collections.emptyMap();

    private final Date timestamp;
    private final String bundleSymbolicName;
    private final String filename;
    private final Map<String, String> comments;
    private final String actualFileLocation;
    
    public DataFileProviderEvent(String bundleSymbolicName, String filename, Map<String, String> comments,
            String actualFileLocation) {
        this.timestamp = new Date();
        this.bundleSymbolicName = bundleSymbolicName;
        this.filename = filename;
        this.comments = comments == null ? EMPTY_COMMENTS : comments;
        this.actualFileLocation = actualFileLocation;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        if(bundleSymbolicName != null) {
            sb.append(", bundleSymbolicName=");
            sb.append(bundleSymbolicName);
        }
        sb.append(", filename=");
        sb.append(filename);

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

    /** @return the optional comments about this file */ 
    public Map<String, String> getComments() {
        return comments;
    }

    /** @return the actual location of the file that was loaded, 
     *      null if file was not found */ 
    public String getActualFileLocation() {
        return actualFileLocation;
    }
}