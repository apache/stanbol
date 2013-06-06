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
/**
 * 
 */
package org.apache.stanbol.commons.stanboltools.datafileprovider;

import java.io.InputStream;

/**
 * Callback used in case tracked DataFiles become available/unavailable.
 * @author westei
 *
 */
public interface DataFileListener {
    /**
     * Notifies that a requested resource is now available and provides the
     * name and optionally the InputStream for that resource.
     * @param resourceName the name of the now available resource
     * @param is Optionally the InputStream for that resource. If <code>null</code>
     * receiver of this notification need to requested the resource with the
     * parsed name by the DataFileProvider.
     * @return If <code>true</code> the registration for this event is 
     * automatically removed. Otherwise the registration is kept and can be used 
     * to track if the resource get unavailable. 
     */
    boolean available(String resourceName, InputStream is);
    
    /** 
     * Called as soon as a previous available resource is no longer available 
     * @param resource The name of the unavailable resource 
     * @return If <code>true</code> the registration for this event is 
     * automatically removed. Otherwise the component receiving this call needs 
     * to remove the registration them self if it dose not want to retrieve
     * further events (such as if the resource becomes available again)
     **/ 
    boolean unavailable(String resource);
}