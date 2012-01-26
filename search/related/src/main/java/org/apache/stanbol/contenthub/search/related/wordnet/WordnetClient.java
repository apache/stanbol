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
import java.util.List;

import org.apache.stanbol.contenthub.search.related.RelatedKeywordImpl;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordNetException;

/**
 * @author anil.sinaci
 * @author cihan
 * 
 */
public class WordnetClient {
    
    public static final int EXPANSION_0 = 1;
    public static final int EXPANSION_1 = 2;
    public static final int EXPANSION_2 = 3;
    public static final int EXPANSION_3 = 4;

    private List<RelatedKeyword> relatedKeywordList;
    private Double maxScore;
    private WordNetDatabase wordnetDatabase;
    private double degradingFactor;
    private int expansionLevel;

    public WordnetClient(String wordnetDatabase, Integer expansionLevel, Double degradingFactor) {
        System.setProperty("wordnet.database.dir", wordnetDatabase);
        this.wordnetDatabase = WordNetDatabase.getFileInstance();
        this.degradingFactor = degradingFactor;
        this.expansionLevel = expansionLevel;
    }

    private void removeDuplicates() {
        for (int i = 0; i < relatedKeywordList.size(); i++) {
            for (int j = i + 1; j < relatedKeywordList.size(); j++) {
                if (relatedKeywordList.get(i).getKeyword().equalsIgnoreCase(relatedKeywordList.get(j).getKeyword())) {
                    relatedKeywordList.remove(j);
                    j--;
                }
            }
        }
    }

    public final List<RelatedKeyword> getScoredWordnetResources(String keyword, double maxScore) throws SearchException {
        relatedKeywordList = new ArrayList<RelatedKeyword>();
        this.maxScore = maxScore;
        Synset[] synsets0;
        try {
            synsets0 = wordnetDatabase.getSynsets(keyword);
        } catch (WordNetException e) {
            throw new SearchException("Error accessing wordnet database", e);
        }
        for (Synset synset : synsets0) {
            String[] wordForms = synset.getWordForms();
            for (String wordForm : wordForms) {
                relatedKeywordList.add(new RelatedKeywordImpl(wordForm, maxScore / degradingFactor, RelatedKeyword.Source.WORDNET));
            }
        }

        if (expansionLevel == WordnetClient.EXPANSION_0) {
            return relatedKeywordList;
        }

        /*
         * Synset[] adjectiveSynsets = wordnetDatabase.getSynsets(keyword, SynsetType.ADJECTIVE); Synset[]
         * adverbSynsets = wordnetDatabase.getSynsets(keyword, SynsetType.ADVERB);
         */
        Synset[] nounSynsets = wordnetDatabase.getSynsets(keyword, SynsetType.NOUN);
        Synset[] verbSynsets = wordnetDatabase.getSynsets(keyword, SynsetType.VERB);

        for (int i = 0; i < expansionLevel - 1; i++) {
            // TODO adjectives and adverbs not implemented yet
            /*
             * adjectiveSynsets = handleAdjectives(adjectiveSynsets, i + 1); adverbSynsets =
             * handleAdverbs(adverbSynsets, i + 1);
             */
            nounSynsets = handleNouns(nounSynsets, i + 1);
            verbSynsets = handleVerbs(verbSynsets, i + 1);
        }

        removeDuplicates();
        return relatedKeywordList;
    }

    // TODO: Adjectives and Adverbs are not included yet

    /*
     * private AdjectiveSynset[] handleAdjectives(Synset[] adjectiveSynsets, int currentExpansionLevel) {
     * 
     * if (adjectiveSynsets == null) { return null; }
     * 
     * return null; }
     * 
     * private AdverbSynset[] handleAdverbs(Synset[] adverbSynsets, int currentExpansionLevel) {
     * 
     * if (adverbSynsets == null) { return null; }
     * 
     * return null; }
     */

    private NounSynset[] handleNouns(Synset[] nounSynsets, int currentExpansionLevel) {

        if (nounSynsets == null) {
            return null;
        }

        List<NounSynset> newNounSynset = new ArrayList<NounSynset>();

        // TODO: Not all methods of a NounSynset is called.

        for (Synset synset : nounSynsets) {
            NounSynset nounSynset = (NounSynset) synset;

            // Hypernyms
            // NounSynset[] hypernyms = nounSynset.getHypernyms();
            // for (NounSynset hypernym : hypernyms) {
            // addWordForms(hypernym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(hypernym);
            // }
            // NounSynset[] instanceHypernyms = nounSynset.getInstanceHypernyms();
            // for (NounSynset instanceHypernym : instanceHypernyms) {
            // addWordForms(instanceHypernym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(instanceHypernym);
            // }
            //
            // // Hyponyms
            // NounSynset[] directHyponyms = nounSynset.getHyponyms();
            // for (NounSynset directHyponym : directHyponyms) {
            // addWordForms(directHyponym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(directHyponym);
            // }
            // NounSynset[] instanceHyponyms = nounSynset.getInstanceHyponyms();
            // for (NounSynset instanceHyponym : instanceHyponyms) {
            // addWordForms(instanceHyponym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(instanceHyponym);
            // }
            //
            // // Holonyms
            // NounSynset[] memberHolonyms = nounSynset.getMemberHolonyms();
            // for (NounSynset memberHolonym : memberHolonyms) {
            // addWordForms(memberHolonym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(memberHolonym);
            // }
            // NounSynset[] substanceHolonyms = nounSynset.getSubstanceHolonyms();
            // for (NounSynset substanceHolonym : substanceHolonyms) {
            // addWordForms(substanceHolonym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(substanceHolonym);
            // }
            // NounSynset[] partHolonyms = nounSynset.getPartHolonyms();
            // for (NounSynset partHolonym : partHolonyms) {
            // addWordForms(partHolonym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(partHolonym);
            // }
            //
            // // Meronyms
            // NounSynset[] memberMeronyms = nounSynset.getMemberMeronyms();
            // for (NounSynset memberMeronym : memberMeronyms) {
            // addWordForms(memberMeronym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(memberMeronym);
            // }
            // NounSynset[] partMeronyms = nounSynset.getPartMeronyms();
            // for (NounSynset partMeronym : partMeronyms) {
            // addWordForms(partMeronym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(partMeronym);
            // }
            //
            //
            // NounSynset[] substanceMeronyms = nounSynset.getSubstanceMeronyms();
            // for (NounSynset substanceMeronym : substanceMeronyms) {
            // addWordForms(substanceMeronym.getWordForms(), currentExpansionLevel);
            // newNounSynset.add(substanceMeronym);
            // }

            handleSynset(nounSynset.getHypernyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getInstanceHypernyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getHyponyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getInstanceHyponyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getMemberHolonyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getSubstanceHolonyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getPartHolonyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getMemberMeronyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getSubstanceMeronyms(), newNounSynset, currentExpansionLevel);
            handleSynset(nounSynset.getPartMeronyms(), newNounSynset, currentExpansionLevel);
        }

        return newNounSynset.toArray(new NounSynset[newNounSynset.size()]);
    }

    private void handleSynset(NounSynset[] parts, List<NounSynset> accumulator, int currentExpansionLevel) {
        for (NounSynset part : parts) {
            addWordForms(part.getWordForms(), currentExpansionLevel);
            accumulator.add(part);
        }
    }

    private VerbSynset[] handleVerbs(Synset[] verbSynsets, int currentExpansionLevel) {

        if (verbSynsets == null) {
            return null;
        }

        List<VerbSynset> newVerbSynset = new ArrayList<VerbSynset>();

        // TODO: Not all methods of a VerbSynset is called.

        for (Synset synset : verbSynsets) {
            VerbSynset verbSynset = (VerbSynset) synset;

            VerbSynset[] hypernyms = verbSynset.getHypernyms();
            for (VerbSynset hypernym : hypernyms) {
                addWordForms(hypernym.getWordForms(), currentExpansionLevel);
                newVerbSynset.add(hypernym);
            }

            VerbSynset[] troponyms = verbSynset.getTroponyms();
            for (VerbSynset troponym : troponyms) {
                addWordForms(troponym.getWordForms(), currentExpansionLevel);
                newVerbSynset.add(troponym);
            }

            VerbSynset[] entailments = verbSynset.getEntailments();
            for (VerbSynset entailment : entailments) {
                addWordForms(entailment.getWordForms(), currentExpansionLevel);
                newVerbSynset.add(entailment);
            }

            VerbSynset[] outcomes = verbSynset.getOutcomes();
            for (VerbSynset outcome : outcomes) {
                addWordForms(outcome.getWordForms(), currentExpansionLevel);
                newVerbSynset.add(outcome);
            }

        }

        return newVerbSynset.toArray(new VerbSynset[newVerbSynset.size()]);
    }

    private void addWordForms(String[] wordForms, int currentExpansionLevel) {
        for (String wordForm : wordForms) {
            relatedKeywordList.add(new RelatedKeywordImpl(wordForm,
                    maxScore / (degradingFactor * currentExpansionLevel), RelatedKeyword.Source.WORDNET));
        }
    }
}
