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

package org.apache.stanbol.contenthub.search.engines.wordnet;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.search.engine.EngineProperties;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngineException;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword.RelatedKeywordSource;
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
public class WordnetEngine implements SearchEngine, EngineProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordnetEngine.class);

    @Property(name = WordnetEngine.WORDNET_DATABASE, value = "")
    public static final String WORDNET_DATABASE = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.database";

    @Property(name = WordnetEngine.WORDNET_EXPANSION_LEVEL, value = "1", options = {
                                                                                    @PropertyOption(name = "1", value = "1"),
                                                                                    @PropertyOption(name = "2", value = "2"),
                                                                                    @PropertyOption(name = "3", value = "3"),
                                                                                    @PropertyOption(name = "4", value = "4")})
    public static final String WORDNET_EXPANSION_LEVEL = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.expansionLevel";

    @Property(name = WordnetEngine.WORDNET_DEGRADING_FACTOR, value = "1.0", options = {
                                                                                       @PropertyOption(name = "1.0", value = "1.0"),
                                                                                       @PropertyOption(name = "1.9", value = "1.9"),
                                                                                       @PropertyOption(name = "2.0", value = "2.0")})
    public static final String WORDNET_DEGRADING_FACTOR = "org.apache.stanbol.contenthub.search.engines.wordnet.Wordnet.degradingFactor";

    private WordnetClient wordnetClient;
    private Map<String,Object> engineProperties = new HashMap<String,Object>();

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
        this.engineProperties.put(EngineProperties.PROCESSING_ORDER, EngineProperties.PROCESSING_PRE);
    }

    @Override
    public final void search(SearchContext searchContext) throws SearchEngineException {
        SearchContextFactory f = searchContext.getFactory();
        for (QueryKeyword qkw : searchContext.getQueryKeyWords()) {
            // First keyword is always with the highest score

            LOGGER.debug("Getting related words for {}, {}", qkw.getKeyword(), qkw.getScore());
            List<Scored> keywords = wordnetClient.getScoredWordnetResources(normalize(qkw.getKeyword()),
                qkw.getScore());
            for (Scored wordnetFinding : keywords) {
                LOGGER.debug("\t {}:{}", wordnetFinding.getKeyword(), wordnetFinding.getScore());
                f.createKeyword(wordnetFinding.getKeyword(), wordnetFinding.getScore(), qkw,RelatedKeywordSource.WORDNET.getName());
            }
        }
    }

    private String normalize(String keyword) {
        return keyword.replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    public final Map<String,Object> getEngineProperties() {
        return this.engineProperties;
    }
}
