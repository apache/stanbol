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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.clerezza.commons.rdf.IRI;

import org.apache.stanbol.enhancer.engines.entitycoreference.Constants;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.NounPhrase;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.PlaceAdjectival;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.osgi.service.cm.ConfigurationException;

/**
 * Contains information about several terms and properties of words we use in the {@link CoreferenceFinder}.
 * 
 * @author Cristian Petroaca
 * 
 */
class Dictionaries {
    /**
     * Contains the list of place adjectivals in the form: language -> adjectival -> IRI -> adjectival ->
     * IRI There are Places that have multiple adjectivals so in this map there are adjectivals that point
     * to the same IRI but that ensures a fast lookup.
     */
    private Map<String,Map<String,IRI>> placeAdjectivalsMap;
    
    public Dictionaries(String[] languages, String entityUriBase) throws ConfigurationException {
        placeAdjectivalsMap = new HashMap<>();

        for (String language : languages) {
            String line = null;
            Map<String,IRI> languagePlaceAdjMap = new HashMap<>();
            InputStream langIn = null;
            BufferedReader reader = null;

            try {
                langIn = Dictionaries.class.getResourceAsStream(Constants.PLACE_ADJECTIVALS_FOLDER + "/"
                                                                + language);
                reader = new BufferedReader(new InputStreamReader(langIn));

                while ((line = reader.readLine()) != null) {
                    String[] splittedLine = line.split("\t");
                    String place = splittedLine[0];
                    String adjectivals = splittedLine[1];
                    IRI ref = new IRI(entityUriBase + place.trim());
                    String[] adjectivalsArray = adjectivals.split(",");

                    for (String adjectival : adjectivalsArray) {
                        languagePlaceAdjMap.put(adjectival.trim().toLowerCase(), ref);
                    }
                }

                placeAdjectivalsMap.put(language, languagePlaceAdjMap);
            } catch (IOException ioe) {
                throw new ConfigurationException("", "Could not read " + Constants.PLACE_ADJECTIVALS_FOLDER
                                                     + "/" + language, ioe);
            } finally {
                if (langIn != null) {
                    try {
                        langIn.close();
                    } catch (IOException e) {}
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    /**
     * Checks whether a {@link NounPhrase} contains a place adjectival and returns it.
     * 
     * @param language
     * @param nounPhrase
     * @return the {@link PlaceAdjectival} if the {@link NounPhrase} contains one or null if not.
     */
    public PlaceAdjectival findPlaceAdjectival(String language, NounPhrase nounPhrase) {
        List<Span> tokens = nounPhrase.getTokens();
        Map<String,IRI> langPlaceAdjectivalsMap = placeAdjectivalsMap.get(language);
        /*
         * Go through all 1-grams and 2-grams and see if we have a match in the place adjectivals map. 2-grams
         * should be good enough since there are no 3-gram places at least from what I saw.
         */
        for (int i = 0; i < tokens.size(); i++) {
            Span currentToken = tokens.get(i);
            String currentTokenString = currentToken.getSpan().toLowerCase();
            // First the current 1-gram
            if (langPlaceAdjectivalsMap.containsKey(currentTokenString)) {
                return new PlaceAdjectival(currentToken.getStart(), currentToken.getEnd(),
                        langPlaceAdjectivalsMap.get(currentTokenString));
            }

            // Then use the 2-gram with the token before it
            StringBuilder concatTokens = new StringBuilder();
            String concatTokensString = null;

            if (i > 0) {
                Span previousToken = tokens.get(i - 1);
                String previousTokenString = previousToken.getSpan().toLowerCase();
                concatTokens = new StringBuilder();
                concatTokens.append(previousTokenString);
                concatTokens.append(" ");
                concatTokens.append(currentTokenString);
                concatTokensString = concatTokens.toString();

                if (langPlaceAdjectivalsMap.containsKey(concatTokensString.toLowerCase())) {
                    return new PlaceAdjectival(previousToken.getStart(), currentToken.getEnd(),
                            langPlaceAdjectivalsMap.get(concatTokensString));
                }
            }

            // Now use the 2-gram with the token after it
            if (i < tokens.size() - 1) {
                Span nextToken = tokens.get(i + 1);
                String nextTokenString = nextToken.getSpan().toLowerCase();
                concatTokens = new StringBuilder();
                concatTokens.append(currentTokenString);
                concatTokens.append(" ");
                concatTokens.append(nextTokenString);

                concatTokensString = concatTokens.toString();

                if (langPlaceAdjectivalsMap.containsKey(concatTokens.toString())) {
                    return new PlaceAdjectival(currentToken.getStart(), nextToken.getEnd(),
                            langPlaceAdjectivalsMap.get(concatTokensString));
                }
            }
        }

        return null;
    }
}
