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
package org.apache.stanbol.ontologymanager.ontonet.conf;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for configuring the Ontology Network Manager offline mode.
 * 
 * @author alessandro
 * 
 */
public class OfflineConfiguration {

    /**
     * The paths of local directories to be searched for ontologies.
     */
    private Set<File> localDirs = new HashSet<File>();

    public void addDirectory(File directory) {
        if (directory.isDirectory()) localDirs.add(directory);
    }

    public void clearDirectories() {
        localDirs.clear();
    }

    public Set<File> getDirectories() {
        return localDirs;
    }

    public void removeDirectory(File directory) {
        if (directory.isDirectory()) localDirs.remove(directory);
    }

}
