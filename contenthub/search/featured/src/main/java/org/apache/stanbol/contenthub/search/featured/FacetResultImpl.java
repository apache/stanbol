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
package org.apache.stanbol.contenthub.search.featured;

import static org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.STANBOLRESERVED_PREFIX;

import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FacetResult;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;

public class FacetResultImpl implements FacetResult {

    private FacetField facetField;

    public FacetResultImpl(FacetField facetField) {
        this.facetField = facetField;
    }

    @Override
    public String getName() {
        return facetField.getName();
    }

    @Override
    public String getHtmlName() {
        String name = getName();
        if (name.startsWith(STANBOLRESERVED_PREFIX)) {
            return name.substring(STANBOLRESERVED_PREFIX.length());
        }

        int lastUnderscore = name.lastIndexOf('_');
        if (lastUnderscore != -1) {
            String underScoreExtension = name.substring(lastUnderscore);
            if (SolrVocabulary.DYNAMIC_FIELD_EXTENSIONS.contains(underScoreExtension)) {
                name = name.substring(0, lastUnderscore);
            }
        }
        return name;
    }

    @Override
    public List<Count> getValues() {
        return facetField.getValues();
    }
}
