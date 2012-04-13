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

import static org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.DYNAMIC_FIELD_EXTENSIONS;
import static org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.STANBOLRESERVED_PREFIX;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FacetResult;

public class FacetResultImpl implements FacetResult {

	private FacetField facetField;

	private String type;

	public FacetResultImpl(FacetField facetField) {
		this.facetField = facetField;
		this.type = "UNKNOWN";
	}
	
	public FacetResultImpl(FacetField facetField, String type) {
		this.facetField = facetField;
		this.type = type;
	}

	@Override
	public FacetField getFacetField() {
		return this.facetField;
	}

	@Override
	public String getType() {
		return this.type;
	}
	
	public String getHtmlName() {
        String name = getFacetField().getName();
        if (name.startsWith(STANBOLRESERVED_PREFIX)) {
            return name.substring(STANBOLRESERVED_PREFIX.length());
        }

        int lastUnderscore = name.lastIndexOf('_');
        if (lastUnderscore != -1) {
            String underScoreExtension = name.substring(lastUnderscore);
            if (DYNAMIC_FIELD_EXTENSIONS.contains(underScoreExtension)) {
                name = name.substring(0, lastUnderscore);
            }
        }
        return name;
    }

}
