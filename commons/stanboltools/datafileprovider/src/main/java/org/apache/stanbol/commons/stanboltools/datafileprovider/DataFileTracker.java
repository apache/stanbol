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
package org.apache.stanbol.commons.stanboltools.datafileprovider;

import java.util.Map;

public interface DataFileTracker {

    public abstract void add(DataFileListener resourceListener, String name, Map<String,String> propertis);

    public abstract void add(DataFileListener resourceListener, String bundleSymbolicName, String name, Map<String,String> propertis);

    public abstract void remove(DataFileListener resourceListener, String resource);

    public abstract void remove(DataFileListener resourceListener, String bundleSymbolicName, String name);

    public abstract void removeAll(DataFileListener resourceListener);
    
    public boolean isTracked(String bundleSymbolicName,String resourceName);
    
    public boolean isTracked(DataFileListener resourceListener,String bundleSymbolicName,String resourceName);
    
    
}