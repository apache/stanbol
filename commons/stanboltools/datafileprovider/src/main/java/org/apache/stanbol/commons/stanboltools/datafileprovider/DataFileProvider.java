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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Used to provide read-only data files (indexes, models etc.) from various
 * locations (bundle resources, filesystem folders etc.) allowing users to
 * overrides default data files with their own.
 *
 * See STANBOL-146 for requirements.
 */
public interface DataFileProvider {

    /**
     * Get the InputStream of the specified data file, according to this provider's
     * priority rules.
     *
     * @param bundleSymbolicName can be used to differentiate
     *        between files which have the same name. It is also used by child
     *        DataFileProvider to only process requests of there own bundle. 
     *        If <code>null</code> any file with the requested name is accepted
     *        and any DataFileProvider processes the request
     * @param filename name of the file to open
     * @param comments Optional - how to get a more complete version
     *        of the data file, licensing information, etc.
     *
     * @return InputStream to read the file, must be closed by
     *         caller when done
     *
     * @throws IOException problem finding or reading the file
     */
    InputStream getInputStream(
            String bundleSymbolicName,
            String filename,
            Map<String,String> comments) throws IOException;
    
    /**
     * Tests if a given DataFile is available without opening an InputStream
     * @param bundleSymbolicName can be used to differentiate
     *        between files which have the same name. It is also used by child
     *        DataFileProvider to only process requests of there own bundle. 
     *        If <code>null</code> any file with the requested name is accepted
     *        and any DataFileProvider processes the request
     * @param filename name of the file to open
     * @param comments Optional - how to get a more complete version
     *        of the data file, licensing information, etc.
     * @return <code>true</code> if the requested RDFTerm is available.
     * Otherwise <code>false</code>
     */
    boolean isAvailable(String bundleSymbolicName,
            String filename,
            Map<String,String> comments);

}
