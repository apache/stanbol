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
package org.apache.stanbol.contenthub.search.featured.util;

import org.apache.solr.common.SolrDocument;
import org.apache.stanbol.contenthub.search.featured.DocumentResultImpl;
import org.apache.stanbol.contenthub.servicesapi.search.featured.DocumentResult;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;

public class SolrContentItemConverter {
    /**
     * This method converts a {@link SolrDocument} into a {@link HTMLContentItem}. Note currently, it ignores
     * its metadata produced after enhancement process and stored. Its constraints indexed in Solr are also
     * ignored as these items are only shown as a list in HTML interface.
     */
    public static DocumentResult solrDocument2solrContentItem(SolrDocument solrDocument, String indexName) {
        return solrDocument2solrContentItem(solrDocument, null, indexName);
    }
    
    public static DocumentResult solrDocument2solrContentItem(SolrDocument solrDocument, String baseURI, String indexName) {
        String id = getStringValueFromSolrField(solrDocument, SolrFieldName.ID.toString());
        String mimeType = getStringValueFromSolrField(solrDocument, SolrFieldName.MIMETYPE.toString());
        String title = getStringValueFromSolrField(solrDocument, SolrFieldName.TITLE.toString());
        long enhancementCount = (Long) solrDocument.getFieldValue(SolrFieldName.ENHANCEMENTCOUNT.toString());
        String dereferencableURI = baseURI != null ? (baseURI + "contenthub/" + indexName + "/store/content/" + id) : null;
        title = (title == null || title.trim().equals("") ? id : title);
        DocumentResultImpl resultantDocument = new DocumentResultImpl(id, dereferencableURI, mimeType,
                enhancementCount, title);
        return resultantDocument;
    }

    private static String getStringValueFromSolrField(SolrDocument solrDocument, String field) {
        Object result = solrDocument.getFieldValue(field);
        if (result != null) {
            return result.toString();
        }
        return "";
    }
}
