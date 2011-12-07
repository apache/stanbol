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

package org.apache.stanbol.contenthub.core.search.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of {@link QueryKeyword}.
 * 
 * @author cihan
 * 
 */
public class QueryKeywordImpl extends KeywordImpl implements QueryKeyword {

    QueryKeywordImpl(Node n,
                     EnhGraph g,
                     String keyword,
                     Double weight,
                     Double score,
                     SearchContextFactoryImpl factory) {
        super(n, g, keyword, weight, score, factory);
        this.addProperty(RDF.type, SearchVocabulary.QUERY_KEYWORD);
    }

    @Override
    public Map<String,List<Keyword>> getRelatedKeywords() {
        Map<String,List<Keyword>> keywords = new HashMap<String,List<Keyword>>();
        for (RDFNode node : this.listPropertyValues(SearchVocabulary.RELATED_KEYWORD).toList()) {
            Keyword keyword = factory.getKeyword(node.asResource().getURI());
            String keywordSource = keyword.getSource();
            if (keywordSource != null) {
                List<Keyword> keywordGroup = keywords.get(keywordSource);
                if (keywordGroup == null) {
                    keywordGroup = new ArrayList<Keyword>();
                    keywords.put(keywordSource, keywordGroup);
                }
                keywordGroup.add(keyword);
            }
        }
        return Collections.unmodifiableMap(keywords);
    }

    @Override
    public void addRelatedKeyword(Keyword keyword) {
        this.addProperty(SearchVocabulary.RELATED_KEYWORD, factory.getKeyword(factory.getKeywordURI(keyword)));
        factory.getKeyword(factory.getKeywordURI(keyword)).addProperty(
            SearchVocabulary.RELATED_QUERY_KEYWORD, this);
    }

    @Override
    public QueryKeyword getRelatedQueryKeyword() {
        return this;
    }

}
