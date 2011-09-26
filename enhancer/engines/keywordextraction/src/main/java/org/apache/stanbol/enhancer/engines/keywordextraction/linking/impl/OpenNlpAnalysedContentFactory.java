package org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.stanbol.commons.opennlp.PosTagsCollectionEnum;
import org.apache.stanbol.commons.opennlp.PosTypeChunker;
import org.apache.stanbol.commons.opennlp.PosTypeCollectionType;
import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
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
    

    private final TextAnalyzer textAnalyzer;
    
    private final Map<String,Set<String>> languagePosTags = new HashMap<String,Set<String>>();
    /**
     * The set of POS (Part-of-Speech) tags also used by the 
     * {@link PosTypeChunker#DEFAULT_BUILD_CHUNK_POS_TYPES} as defaults.
     * This will select Nouns and foreign words as defined in the 
     * <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
     * Penn Treebank</a> tag set <p>
     */
    public final Set<String> DEFAULT_POS_TAGS = PosTagsCollectionEnum.EN_NOUN.getTags();
        
    
    public static OpenNlpAnalysedContentFactory getInstance(TextAnalyzer textAnalyzer){
        return new OpenNlpAnalysedContentFactory(textAnalyzer);
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
    
    protected OpenNlpAnalysedContentFactory(TextAnalyzer textAnalyzer){
        if(textAnalyzer == null){
            throw new IllegalArgumentException("The parsed TextAnalyzer MUST NOT be NULL!");
        }
        this.textAnalyzer = textAnalyzer;
        setLanguagePosTags(null, DEFAULT_POS_TAGS);
    }
    
    public AnalysedContent create(String text,String language){
        return new OpenNlpAnalysedContent(text, language);
    }

    /**
     * Implementation of the {@link AnalysedContent} based on OpenNLP and the
     * {@link TextAnalyzer} component
     * @author Rupert Westenthaler
     *
     */
    private class OpenNlpAnalysedContent implements AnalysedContent{
        private final String language;
        private final Iterator<AnalysedText> sentences;
        private final Set<String> posTags;
        private final Tokenizer tokenizer;

        private OpenNlpAnalysedContent(String text, String lang){
            this.language = lang;
            this.sentences = textAnalyzer.analyse(text, lang);
            this.posTags = PosTagsCollectionEnum.getPosTagCollection(lang, PosTypeCollectionType.NOUN);
            this.tokenizer = textAnalyzer.getTokenizer(lang);
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
         * Called to check if a {@link Token} should be used to search for
         * Concepts within the Taxonomy based on the POS tag of the Token.
         * @param posTag the POS tag to check
         * @return <code>true</code> if Tokens with this POS tag should be
         * included in searches. Otherwise <code>false</code>. Also returns
         * <code>true</code> if no POS type configuration is available for the
         * language parsed in the constructor
         */
        @Override
        public Boolean processPOS(String posTag) {
            return posTags != null ? Boolean.valueOf(posTags.contains(posTag)) : null;
        }
        /**
         * Not yet implemented.
         * @param chunkTag the type of the chunk
         * @return returns always <code>true</code>
         */
        @Override
        public Boolean processChunk(String chunkTag) {
            // TODO implement
            return null;
        }
        @Override
        public String[] tokenize(String label) {
            return tokenizer.tokenize(label);
        }
    }
}
