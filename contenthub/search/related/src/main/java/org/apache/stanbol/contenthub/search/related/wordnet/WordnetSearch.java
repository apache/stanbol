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

package org.apache.stanbol.contenthub.search.related.wordnet;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.index.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.index.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.index.search.related.RelatedKeywordSearch;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author cihan
 * 
 */
@Component(metatype = true)
@Service
public class WordnetSearch implements RelatedKeywordSearch {
    private static final Logger log = LoggerFactory.getLogger(WordnetSearch.class);

    @Property(name = WordnetSearch.WORDNET_DATABASE, value = "")
    public static final String WORDNET_DATABASE = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.database";

    @Property(name = WordnetSearch.WORDNET_EXPANSION_LEVEL, value = "1", options = {
                                                                                    @PropertyOption(name = "1", value = "1"),
                                                                                    @PropertyOption(name = "2", value = "2"),
                                                                                    @PropertyOption(name = "3", value = "3"),
                                                                                    @PropertyOption(name = "4", value = "4")})
    public static final String WORDNET_EXPANSION_LEVEL = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.expansionLevel";

    @Property(name = WordnetSearch.WORDNET_DEGRADING_FACTOR, value = "1.0", options = {
                                                                                       @PropertyOption(name = "1.0", value = "1.0"),
                                                                                       @PropertyOption(name = "1.9", value = "1.9"),
                                                                                       @PropertyOption(name = "2.0", value = "2.0")})
    public static final String WORDNET_DEGRADING_FACTOR = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.degradingFactor";

    private WordnetClient wordnetClient;

    private void checkProperties(@SuppressWarnings("rawtypes") Dictionary properties) {
        Object databasePath = properties.get(WORDNET_DATABASE);
        if (!(databasePath instanceof String) || "".equals(databasePath)) {
            throw new IllegalArgumentException("Wordnet database path can not be empty");
        }

    }

    @Activate
    public final void activate(ComponentContext cc) {
        @SuppressWarnings("rawtypes")
        Dictionary properties = cc.getProperties();
        checkProperties(properties);
        String wordnetDatabase = (String) properties.get(WORDNET_DATABASE);
        Integer expansionLevel = Integer.parseInt((String) properties.get(WORDNET_EXPANSION_LEVEL));
        Double degradingFactor = Double.parseDouble((String) properties.get(WORDNET_DEGRADING_FACTOR));
        wordnetClient = new WordnetClient(wordnetDatabase, expansionLevel, degradingFactor);
    }

    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword) throws SearchException {
        Map<String,List<RelatedKeyword>> relatedKeywordsMap = new HashMap<String,List<RelatedKeyword>>();
        List<RelatedKeyword> relatedKeywords = new ArrayList<RelatedKeyword>();
        if (wordnetClient != null) {
            log.debug("Getting Wordnet related words for {}", keyword);
            relatedKeywords = wordnetClient.getScoredWordnetResources(keyword, 1.0);
        }
        relatedKeywordsMap.put(RelatedKeyword.Source.WORDNET.toString(), relatedKeywords);
        return relatedKeywordsMap;
    }

    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword, String ontologyURI) throws SearchException {
        return search(keyword);
    }
}
