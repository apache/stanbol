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
package org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.opennlp.PosTagsCollectionEnum;
import org.apache.stanbol.commons.opennlp.PosTypeChunker;
import org.apache.stanbol.commons.opennlp.PosTypeCollectionType;
import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.TextAnalyzerConfig;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.AnalysedContent;
/**
 * Factory to {@link #create(String, String)} {@link AnalysedContent} instances
 * based on OpenNLP and the {@link TextAnalyzer} utility.<p>
 * This factory allows to configure a set of POS types that are used to
 * determine if {@link Token}s are processed (used to search for terms) or not.
 * This configuration is used by all {@link AnalysedContent} instances created
 * by using this Factory.<p>
 * Preconfigured sets of POS types are available by the 
 * {@link PosTagsCollectionEnum}. The {@link PosTagsCollectionEnum#EN_NOUN}
 * set is used as default.
 *
 * @author Rupert Westenthaler
 *
 */
public class OpenNlpAnalysedContentFactory {
    

    private final OpenNLP openNLP;
    
    private final TextAnalyzerConfig config;
    
    private final Map<String,Set<String>> languagePosTags = new HashMap<String,Set<String>>();
    /**
     * The set of POS (Part-of-Speech) tags also used by the 
     * {@link PosTypeChunker#DEFAULT_BUILD_CHUNK_POS_TYPES} as defaults.
     * This will select Nouns and foreign words as defined in the 
     * <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
     * Penn Treebank</a> tag set <p>
     */
    public final Set<String> DEFAULT_POS_TAGS = PosTagsCollectionEnum.EN_NOUN.getTags();
        
    
    public static OpenNlpAnalysedContentFactory getInstance(OpenNLP openNLP, TextAnalyzerConfig config){
        return new OpenNlpAnalysedContentFactory(openNLP,config);
    }
    /**
     * Setter for the POS tags used to process Words in the given language.
     * The <code>null</code> language is used whenever no configuration is
     * available for a given language. Setting the posTags to <code>null</code>
     * will remove a language from the configuration.
     * If a configuration for a given language is missing and there is also no
     * default configuration (e.g. after calling 
     * <code>setLanguagePosTags(null, null)</code>) {@link AnalysedContent}
     * instances created by this factory will always return <code>false</code>
     * on calls to {@link AnalysedContent#processPOS(String)};
     * @param language the language
     * @param posTags the pos tags
     */
    public void setLanguagePosTags(String language, Set<String> posTags){
        if(posTags != null){
            languagePosTags.put(language, Collections.unmodifiableSet(posTags));
        } else {
            languagePosTags.remove(language);
        }
    }
    
    protected OpenNlpAnalysedContentFactory(OpenNLP openNLP,TextAnalyzerConfig config){
        if(openNLP == null){
            throw new IllegalArgumentException("The parsed OpenNLP instance MUST NOT be NULL!");
        }
        this.openNLP = openNLP;
        this.config = config;
        setLanguagePosTags(null, DEFAULT_POS_TAGS);
    }
    
    public AnalysedContent create(String text,String language){
        TextAnalyzer analyzer = new TextAnalyzer(openNLP, language,config);
        return new OpenNlpAnalysedContent(text, analyzer);
    }

    /**
     * Implementation of the {@link AnalysedContent} based on OpenNLP and the
     * {@link TextAnalyzer} component
     * @author Rupert Westenthaler
     *
     */
    private final class OpenNlpAnalysedContent implements AnalysedContent{
        private final TextAnalyzer analyzer;
        private final double minPosTagProbability;
        private final double minExcludePosTagProbability;
        private final Iterator<AnalysedText> sentences;
        private final Set<String> posTags;
        private final Tokenizer tokenizer;

        private OpenNlpAnalysedContent(String text, TextAnalyzer analyzer){
            this.analyzer = analyzer;
            this.sentences = analyzer.analyse(text);
            this.posTags = PosTagsCollectionEnum.getPosTagCollection(
                analyzer.getLanguage(), PosTypeCollectionType.NOUN);
            this.tokenizer = analyzer.getTokenizer();
            this.minPosTagProbability = analyzer.getConfig().getMinPosTypeProbability();
            this.minExcludePosTagProbability = minPosTagProbability/2;
        }
        
        /**
         * Getter for the Iterator over the analysed sentences. This Method
         * is expected to return always the same Iterator instance.
         * @return the iterator over the analysed sentences
         */
        public Iterator<AnalysedText> getAnalysedText() {
            return sentences;
        }
        /**
         * This uses now two Tag Probabilities<ul>
         * <li> {@link TextAnalyzerConfig#getMinPosTypeProbability()} for
         * accepting POS tags that represent Nouns and
         * <li> <code>minPosTypeProb/2</code> for rejecting POS tags that 
         * are not nouns
         * </ul>
         * Assuming that the <code>minPosTypePropb=0.667</code> a<ul>
         * <li> noun with the prop 0.8 would result in returning <code>true</code>
         * <li> noun with prop 0.5 would return <code>null</code>
         * <li> verb with prop 0.4 would return <code>false</code>
         * <li> verb with prop 0.3 would return <code>null</code>
         * </ul>
         * This new algorithm makes it less likely that non nouns are processed
         * by the KeywordLinkingEngine as returning <code>null</code> as the
         * minimum probability requirement is now much lower.<p> 
         * <i>NOTE:</i> Returning <code>null</code> usually results in using
         * the fall-back (typically minTokenLnegh = 3) so most of those tokens
         * where processed by the KeywordLinkingEngine.
         * (see also STANBOL-685)
         */
        @Override
        public Boolean processPOS(String posTag, double posProb) {
            if(posTags != null){
                if(posTags.contains(posTag)){
                    if(posProb >= minPosTagProbability){
                        return Boolean.TRUE;
                    } else {
                        return null; //probability to low
                    }
                } else {
                    if(posProb >= minExcludePosTagProbability){
                        return Boolean.FALSE;
                    } else {
                        return null; //probability to low
                    }
                }
            } else {
                return null;
            }
        }
        /**
         * Not yet implemented.
         * @param chunkTag the type of the chunk
         * @param chunkProb the probability of the parsed chunk tag
         * @return returns always <code>true</code>
         */
        @Override
        public Boolean processChunk(String chunkTag, double chunkProb) {
            // TODO implement
            return null;
        }
        @Override
        public String[] tokenize(String label) {
            return tokenizer.tokenize(label);
        }
    }
}
