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
package org.apache.stanbol.enhancer.engines.entitycoreference.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.stanbol.enhancer.engines.entitycoreference.Constants;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.NounPhrase;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.osgi.service.cm.ConfigurationException;

/**
 * Filters out bad {@link NounPhrase}s based on pos information.
 * 
 * @author Cristian Petroaca
 * 
 */
/*
 * TODO - create a NounPhraseFilterer interface with multiple implementations to separate languages with
 * appositional definite article from the others.
 */
public class NounPhraseFilterer {
    private final static String WITHIN_TEXT_DET_PROP = "within.text.referencing.determiners";
    private final static short MIN_POS_NUMBER = 2;

    /**
     * Set of determiners based on language which make a {@link NounPhrase} valid for being a coref mention.
     */
    private Map<String,Set<String>> withinTextRefDeterminers;

    public NounPhraseFilterer(String[] languages) throws ConfigurationException {
        withinTextRefDeterminers = new HashMap<String,Set<String>>();

        for (String language : languages) {
            Properties props = new Properties();
            String propertiesFile = Constants.POS_CONFIG_FOLDER + "/" + language + ".properties";
            InputStream in = null;

            try {
                in = NounPhraseFilterer.class.getResourceAsStream(propertiesFile);
                props.load(in);
            } catch (IOException e) {
                throw new ConfigurationException("", "Could not read " + propertiesFile);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }
            }

            String determinersProperty = props.getProperty(WITHIN_TEXT_DET_PROP);

            if (determinersProperty == null) {
                throw new ConfigurationException(WITHIN_TEXT_DET_PROP, "Missing property in "
                                                                       + propertiesFile);
            }

            Set<String> langDeterminerSet = new HashSet<String>();
            for (String determiner : determinersProperty.split(",")) {
                langDeterminerSet.add(determiner);
            }

            withinTextRefDeterminers.put(language, langDeterminerSet);
        }
    }

    /**
     * Filters out noun phrases which do not contain a determiner from the given config and do not a token
     * count bigger than 2 - TODO : should this be configurable to be able to also include 1 word noun
     * phrases?
     * 
     * @param nounPhrases
     * @param language
     */
    public void filter(List<NounPhrase> nounPhrases, String language) {
        Set<String> langDeterminerSet = withinTextRefDeterminers.get(language);
        Iterator<NounPhrase> it = nounPhrases.iterator();

        while (it.hasNext()) {
            NounPhrase nounPhrase = it.next();
            boolean hasGoodDeterminer = false;
            short nounNo = 0;

            for (Span token : nounPhrase.getTokens()) {
                Value<PosTag> pos = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);

                if (pos != null) {
                    PosTag posTag = pos.value();

                    if (posTag.hasCategory(LexicalCategory.Noun)
                        || posTag.hasCategory(LexicalCategory.Adjective)) {
                        nounNo++;
                    }

                    if (!hasGoodDeterminer && posTag.hasPos(Pos.Determiner)
                        && langDeterminerSet.contains(token.getSpan().toLowerCase())) {
                        hasGoodDeterminer = true;
                    }
                }
            }

            if (!hasGoodDeterminer || nounNo < MIN_POS_NUMBER) {
                it.remove();
            }
        }
    }

    public boolean supportsLanguage(String language) {
        return withinTextRefDeterminers.containsKey(language);
    }
}
