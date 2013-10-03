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
package org.apache.stanbol.enhancer.engines.lucenefstlinking.cache;

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrDocument;

/**
 * A Cache for {@link SolrDocument}s holding Entity information required for
 * entity linking. This cache is intended to avoid disc access for loading
 * entity data of entities detected by the FST tagging in the parsed document.
 * @author Rupert Westenthaler
 *
 */
public interface EntityCache {
    
    /**
     * if the current version of the index does not equals this version
     * the Cache need to be renewed.
     * @return the version this cache is build upon
     */
    Object getVersion();
    /**
     * Getter for the Document based on the Lucene Document ID
     * @param docId the Lucene document ID (the unique key)
     * @return the Document or <code>null</code> if not in the cache
     */
    Document get(Integer docId);
    
    /**
     * Caches the document for the parsed Lucene document id
     * @param docId the Lucene document id
     * @param doc the Document
     */
    void cache(Integer docId, Document doc);

    /**
     * The size of the cache of <code>-1</code> if not available
     * @return the size or <code>-1</code> if not known
     */
    int size();
    
    /**
     * The statistics for this Cache
     * @return 
     */
    String printStatistics();
}
