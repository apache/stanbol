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
package org.apache.stanbol.entityhub.yard.solr.impl.install;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Constants and static configuration used by the {@link SolrIndexInstaller}
 * @author Rupert Westenthaler
 *
 */
public final class IndexInstallerConstants {
    private IndexInstallerConstants(){ /* do not create instances*/ }
    
    /**
     * Supported archive types.
     */
    public static final Map<String,String> SUPPORTED_COMPRESSION_FORMAT;
    static {
        Map<String,String> cfm = new HashMap<String,String>();
        cfm.put("SOLR_INDEX_ARCHIVE_EXTENSION", "zip"); //the default if not specified
        cfm.put("gz", "gz");
        cfm.put("bz2", "bz2");
        cfm.put("zip", "zip");
        cfm.put("jar", "zip");
        SUPPORTED_COMPRESSION_FORMAT = Collections.unmodifiableMap(cfm);
    }
    /**
     * Use &lt;indexName&gt;.solrindex[.&lt;archiveType&gt;] as file name
     */
    public static final String SOLR_INDEX_ARCHIVE_EXTENSION = "solrindex";
    /**
     * The schema used for transformed resources.
     */
    public static final String SOLR_INDEX_ARCHIVE_RESOURCE_TYPE = "solrarchive";
    
    private static final String PROPERTY_PREFIX = "org.apache.stanbol.yard.solr.installer.";
    
    /**
     * The key used for the name of the index
     */
    public static final String PROPERTY_INDEX_NAME = PROPERTY_PREFIX+"index.name";
    /**
     * The key used for the type of the archive
     */
    public static final String PROPERTY_ARCHIVE_FORMAT = PROPERTY_PREFIX+"archive.format";

}
