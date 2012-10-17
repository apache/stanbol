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
package org.apache.stanbol.commons.solr.install;

import org.apache.stanbol.commons.solr.install.impl.SolrIndexInstaller;

/**
 * Constants and static configuration used by the {@link SolrIndexInstaller}
 * 
 * @author Rupert Westenthaler
 * 
 */
public final class IndexInstallerConstants {
    private IndexInstallerConstants() { /* do not create instances */}

    /**
     * The schema used for transformed resources.
     */
    public static final String SOLR_INDEX_ARCHIVE_RESOURCE_TYPE = "solrarchive";

    private static final String PROPERTY_PREFIX = "org.apache.stanbol.commons.solr.install.";
    /**
     * The key used for the type of the archive
     */
    public static final String PROPERTY_ARCHIVE_FORMAT = PROPERTY_PREFIX + "archive.format";

}
