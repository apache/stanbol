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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.UNICASE_SCRIPT_LANUAGES;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.ProcessingState;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.SectionData;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.TokenData;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.opensextant.solrtexttagger.TaggingAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classifies Tokens in the Solr {@link TokenStream} with the {@link TaggingAttribute}
 * based on NLP processing results present in the {@link AnalysedText}. This
 * implementation Classifies Token similar to the {@link EntityLinkingEngine}.
 * It uses the {@link TextProcessingConfig} for its configuration.<p>
 * <b> Implementation Details</b><p>
 * While this code does not directly use {@link ProcessingState} it serves a
 * similar purpose.<p>
 * <ul>
 * <li>This code needs to deal with potential different tokenization present
 * in the {@link AnalysedText} and the {@link TokenStream}. The implemented 
 * semantics does mark Tokens in the {@link TokenStream} as 
 * <code>{@link TaggingAttribute#isTaggable()} == ture</code> if the do overlap 
 * with a {@link TokenData#isLinkable} token in the {@link AnalysedText}.
 * <li> {@link TokenData#isMatchable} tokens are also considered as
 * <code>{@link TaggingAttribute#isTaggable()} == ture</code> if a 
 * {@link TokenData#isMatchable} token is following within two tokens of the
 * {@link AnalysedText}. This Range is extended if other matchable tokens are
 * within the lookahead range. However the range is never extended over a
 * section border.
 * </ul>
 * @author Rupert Westenthaler
 *
 */
public class LinkableTokenFilterStream extends TokenFilter {

    private final Logger log = LoggerFactory.getLogger(LinkableTokenFilterStream.class);
    
    /**
     * Required to use {@link SectionData}
     */
    private static final Set<SpanTypeEnum> PROCESSED_SPAN_TYPES = EnumSet.of(
        SpanTypeEnum.Chunk,SpanTypeEnum.Token);
    /**
     * The NLP processing results
     */
    private AnalysedText at;
    /**
     * The language of the text
     */
    //private String lang;
    /**
     * If the language is unicase or not
     */
    private boolean isUnicaseLanguage;
    /**
     * Defines how NLP processing results are processed to determine Words that
     * need to be looked-up in the vocabulary
     */
    private LanguageProcessingConfig lpc;

    /**
     * Iterator over all sections of the {@link AnalysedText}
     */
    private Iterator<? extends Section> sections;
    /**
     * The current section
     */
    private SectionData sectionData;
    /**
     * Iterator over all {@link Token}s in the current section
     */
    private Iterator<TokenData> tokenIt;
    /**
     * The current Token
     */
    private TokenData token;
    
    private int lookupCount = 0;
    private int incrementCount = 0;
    
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offset = addAttribute(OffsetAttribute.class);
    private final TaggingAttribute taggable = addAttribute(TaggingAttribute.class);
    
    protected LinkableTokenFilterStream(TokenStream input, AnalysedText at, 
            String lang, LanguageProcessingConfig lpc) {
        super(input);
        this.at = at;
        //this.lang = lang;
        this.lpc = lpc;
        this.isUnicaseLanguage = lang != null && !lang.isEmpty() &&
                UNICASE_SCRIPT_LANUAGES.contains(lang);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        Iterator<Sentence> sentences = at.getSentences();
        this.sections = sentences.hasNext() ? sentences : Collections.singleton(at).iterator();
        sectionData = null;
        tokenIt = null;
        incrementCount = 0;
        lookupCount = 0;
    }
    
    @Override
    public boolean incrementToken() throws IOException {
        if(input.incrementToken()){
            incrementCount++;
            boolean first = true;
            TokenData token; 
            boolean lookup = false;
            int lastMatchable = -1;
            int lastIndex = -1;
            while((token = nextToken(first)) != null){
                first = false;
                if(token.isLinkable){
                    lookup = true;
                } else if (token.isMatchable){
                    lastMatchable = token.index;
                    lastIndex = lastMatchable;
                } //else if(token.hasAlphaNumeric){
                //    lastIndex = token.index;
                //}
            }
            //lookahead
            if(!lookup && lastIndex >= 0 && sectionData != null){
                List<TokenData> tokens = sectionData.getTokens();
                int maxLookahead = Math.max(lastIndex, lastMatchable+3);
                for(int i = lastIndex+1;!lookup && i < maxLookahead && i < tokens.size(); i++){
                    token = tokens.get(i);
                    if(token.isLinkable){
                        lookup = true;
                    } else if(token.isMatchable && (i+1) == maxLookahead){
                        maxLookahead++; //increase lookahead for matchable tokens
                    }
                }
            }
            this.taggable.setTaggable(lookup);
            if(lookup){
                if(log.isTraceEnabled()){
                    log.trace("Solr Token: [{},{}]: {}", new Object[]{
                            offset.startOffset(), offset.endOffset(), termAtt});
                }
                lookupCount++;
            }
            return true;
        } else {
            log.debug("lookup percentage: {}",lookupCount*100/(float)incrementCount);
            return false;
        }
    }

    /**
     * Iterating over TokensData requires to iterate over two hierarchy levels:
     * (1) sections (likely Sentences) and (2) Tokens <p>
     * <b>NOTE</b> that this method modifies a lot of fields to update the
     * state of the iteration accordingly. If the {@link #token} field is
     * <code>null</code> after a call to this method this indicates that the
     * end of the {@link Token} in the {@link AnalysedText} was reached.
     * @param first is this the first call for the current {@link #offset} state?
     * @return the token or <code>null</code> if there are no more tokens for
     * the current {@link #offset}
     */
    private TokenData nextToken(boolean first){
        final boolean isToken;
        if(token == null || //on the first call 
                !first || //not the first call within on #incrementToken()
                //current Token is before the current offset
                token.token.getEnd() <= offset.startOffset()){
            if(incrementTokenData()){ //get the next token
                //the next token still overlaps with the current offset
                isToken = token.token.getStart() < offset.endOffset(); 
            } else { //end of stream
                isToken = false;
            }
        } else { //check the current #token
            isToken = token.token.getStart() < offset.endOffset(); 
        }
        return isToken ? token : null;
    }
    /**
     * Increments the {@link #token} and - if necessary also the {@link #sectionData
     * section}.
     * @return <code>true</code> unless there are no more tokens
     */
    private boolean incrementTokenData(){
        if(tokenIt == null || !tokenIt.hasNext()){
            sectionData = null;
            tokenIt = null;
            while(sections.hasNext() && (tokenIt == null || !tokenIt.hasNext())){
                //analyse NLP results for the next Section
                sectionData = new SectionData(lpc, sections.next(), 
                    PROCESSED_SPAN_TYPES, isUnicaseLanguage);
                tokenIt = sectionData.getTokens().iterator();
            }
            if(tokenIt != null && tokenIt.hasNext()){
                token = tokenIt.next(); //first token of the next section
                return true;
            } else { //reached the end .. clean up
                sectionData = null;
                tokenIt = null;
                return false;
            }
        } else { //more token in the same section
            token = tokenIt.next();
            return true;
        }
    }
    
}
