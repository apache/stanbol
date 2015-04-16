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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.ChunkData;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.ProcessingState;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.SectionData;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.TokenData;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.opensextant.solrtexttagger.TagClusterReducer;
import org.opensextant.solrtexttagger.TagLL;
import org.opensextant.solrtexttagger.TaggingAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that ensures that only {@link TokenData#isLinkable linkable} Tokens
 * are processed.<p>
 * This is ensured on two places:<ol>
 * <li> Classifies Tokens in the Solr {@link TokenStream} with the {@link TaggingAttribute}
 * based on NLP processing results present in the {@link AnalysedText}. This
 * implementation classifies Token similar to the {@link EntityLinkingEngine}.
 * It uses the {@link TextProcessingConfig} for its configuration.<p>
 * <li> Implements {@link TagClusterReducer} to ensure that all {@link TagLL tags}
 * that do not overlap with any {@link TokenData#isLinkable linkable} are
 * removed from the Cluster.
 * </ol>
 * <b> Implementation Details</b><p>
 * The {@link TokenStream} implementation of this class serves a similar
 * purpose as the {@link ProcessingState} used by the EntityLinkingEngine.
 * The main differences are:<p>
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
 * The {@link TagClusterReducer} implementation keeps track of linkable tokens
 * while iterating over the {@link TokenStream} and adds them to the end of a
 * List. When {@link TagClusterReducer#reduce(TagLL[])} is called tags of the
 * cluster are checked if they do overlap with any linkable Token at the start
 * of the list. Tokens with earlier ends as the start of the tags are removed
 * from the list. 
 * @author Rupert Westenthaler
 *
 */
public final class LinkableTokenFilter extends TokenFilter implements TagClusterReducer{

    private final Logger log = LoggerFactory.getLogger(LinkableTokenFilter.class);
    
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
     * The current Token(s). {@link #incrementToken()} will add tokens to the
     * end of the list and {@link #nextToken(boolean)} with <code>true</code>
     * will remove earlier tokens as {@link #offset} from the list.<p>
     * We need to hold multiple tokens because the TokenStream might parse
     * multiple tokens with 
     * <code>{@link PositionIncrementAttribute#getClass() posInc} == 0</code>
     * covering multiple {@link TokenData tokens}.
     */
    private List<TokenData> tokens = new LinkedList<TokenData>();
    /**
     * The cursor within the {@link #tokens} list of the currently active Token
     */
    private int tokensCursor = -1; //the cursor within the tokens list
    
    private int lookupCount = 0;
    private int incrementCount = 0;
    
    protected final CharTermAttribute termAtt;
    protected final OffsetAttribute offset;
    protected final TaggingAttribute taggable;
    /**
     * List with {@link TokenData#isLinkable linkable} {@link Token}s used by
     * the {@link #reduce(TagLL[])} method to check if {@link TagLL tags} 
     * do overlap with any linkable token.
     */
    private final List<LinkableTokenContext> linkableTokens = new LinkedList<LinkableTokenContext>();
    /**
     * The minimum score a tag needs to match processable tokens within a
     * {@link Chunk} so that is is not omitted. 
     */
    private double minChunkMatchScore;
    /**
     * The minimum amount of matched (matchable) Tokens so that an Entity is
     * considered. Only used within processable chunks
     */
    private int minFoundTokens;
    
    protected LinkableTokenFilter(TokenStream input, AnalysedText at, 
            String lang, LanguageProcessingConfig lpc, double minChunkMatchScore, int minFoundTokens) {
        super(input);
        //STANBOL-1177: add attributes in doPrivileged to avoid 
        //AccessControlException: access denied ("java.lang.RuntimePermission" "getClassLoader")
        termAtt = AccessController.doPrivileged(new PrivilegedAction<CharTermAttribute>() {
            @Override public CharTermAttribute run() {
                return addAttribute(CharTermAttribute.class);
            }});
        offset = AccessController.doPrivileged(new PrivilegedAction<OffsetAttribute>() {
            @Override public OffsetAttribute run() {
                return addAttribute(OffsetAttribute.class);
            }});
        taggable = AccessController.doPrivileged(new PrivilegedAction<TaggingAttribute>() {
            @Override public TaggingAttribute run() {
                return addAttribute(TaggingAttribute.class);
            }});
        this.at = at;
        //this.lang = lang;
        this.lpc = lpc;
        this.isUnicaseLanguage = lang != null && !lang.isEmpty() &&
                UNICASE_SCRIPT_LANUAGES.contains(lang);
        this.minChunkMatchScore = minChunkMatchScore;
        this.minFoundTokens = minFoundTokens;
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
            log.trace("> solr:[{},{}] {}",new Object[]{
                            offset.startOffset(), offset.endOffset(), termAtt});
            while((token = nextToken(first)) != null){
                log.trace("  < [{},{}]:{} (link {}, match; {})",new Object[]{
                        token.token.getStart(), token.token.getEnd(),token.getTokenText(),
                        token.isLinkable, token.isMatchable});
                first = false;
                if(token.isLinkable){
                    log.trace("  + lookup because {} is linkable", token);
                    lookup = true;
                } else if (token.isMatchable){
                    lastMatchable = token.index;
                    lastIndex = lastMatchable;
                }
                //special rules for processable chunks (typically noun phrases)
                //accept all tokens in processable chunks with a linkable or
                //multiple matchable tokens.
                if(!lookup && (!lpc.isIgnoreChunks()) && token.inChunk != null 
                        && token.inChunk.isProcessable){
                    if(token.inChunk.isNamedEntity()){
                        if(log.isTraceEnabled()){
                            log.trace("  + lookup because {} is part of Named Entity '{}'",
                               token.token, token.inChunk.chunk.getSpan());
                        }
                        lookup = true;
                    }
                    if(token.inChunk.hasLinkable() || 
                            (lpc.isLinkMultiMatchableTokensInChunk() && 
                                    token.inChunk.getMatchableCount() > 1)){
                        if(log.isTraceEnabled()){
                            log.trace("  + lookup because {} is part of a linkable chunk '{}'", 
                                token.token, token.inChunk.chunk.getSpan());
                        }
                        lookup = true;
                    }
                }
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
                    TokenData t = getToken();
                    log.trace("lookup: token [{},{}]: {} | word [{},{}]:{}", new Object[]{
                            offset.startOffset(), offset.endOffset(), termAtt,
                            t.token.getStart(), t.token.getEnd(),
                            t.getTokenText()});
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
        int startOffset = offset.startOffset();
        int endOffset = offset.endOffset();
        if(first){ //on the first call for a token
            tokensCursor = -1; //reset cursor to zero
            while(!tokens.isEmpty()){
                //remove tokens earlier as the current offset
                if(tokens.get(0).token.getEnd() <= startOffset){
                    tokens.remove(0);
                } else { //stop on the first overlapping token
                    break;
                }
            } //else nothing to do
        }
        if(tokensCursor >= tokens.size()-1){
            if(!incrementTokenData()){ //adds a new token to the list
                return null; //EoF
            }
        }
        TokenData cursorToken = tokens.get(tokensCursor+1);
        if(cursorToken.token.getStart() < endOffset){
            tokensCursor++; //set the next token as current
            return cursorToken; //and return it
        } else {
            return null;
        }
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
                addToken(tokenIt.next());
                return true;
            } else { //reached the end .. clean up
                sectionData = null;
                tokenIt = null;
                return false;
            }
        } else { //more token in the same section
            addToken(tokenIt.next());
            return true;
        }
    }
    /**
     * Adds a token. Also cares about adding tokens to {@link #linkableTokens}
     * @param token the tokens - MUST NOT be NULL.
     */
    private void addToken(TokenData token){
        tokens.add(token);
        if(token.isLinkable){
            //add to the list of linkable for #reduce(TagLL[])
            linkableTokens.add(new LinkableTokenContext(token,sectionData.getTokens()));
        } else if(token.isMatchable && !lpc.isIgnoreChunks() &&//matchable token
                token.inChunk != null && //in processable chunks with more
                token.inChunk.isProcessable && //as two matchable tokens
                token.inChunk.getMatchableCount() > 1){ //matchable tokens
            linkableTokens.add(new LinkableTokenContext(token, sectionData.getTokens()));
        }
    }
    /**
     * Getter for the current Token
     * @return
     */
    private TokenData getToken(){
        return tokens.isEmpty() ? null : tokens.get(tokensCursor);
    }
    @Override
    public void reduce(TagLL[] head) {
        //this implements a two phase reduce
        //(1) reduce Tags with no linkable tokens and not matching enough of the
        //    current chunk.
        //(2) reduce remaining Tags in the cluster similar to TagClusterReducer
        //    but only considering the "matchable span" of the Tags. Meaning the
        //    span over matchable Tokens and not the full Text.
        
        //this map holds the matchable spans for Tags. Filled during phase (1) and
        //used for phase(2)
        Map<TagLL,int[]> matchableTagSpan = new HashMap<TagLL,int[]>();
        
        //(1) reduce Tags based on link-/matchable tokens as well as chunks. 
    	LinkableTokenContext linkableTokenContext;
        for(TagLL tag = head[0]; tag != null; tag = tag.getNextTag()) {
            int start = tag.getStartOffset();
            int end = tag.getEndOffset();
            linkableTokenContext = linkableTokens.isEmpty() ? null : linkableTokens.get(0);
            while(linkableTokenContext != null && linkableTokenContext.linkableToken.token.getEnd() <= start){
                linkableTokens.remove(0);
                linkableTokenContext = linkableTokens.isEmpty() ? null : linkableTokens.get(0);
            }
            if(linkableTokenContext == null || linkableTokenContext.linkableToken.token.getStart() >= end){
                //does not overlap any linkable token
                tag.removeLL(); //remove the tag from the cluster
                if(log.isTraceEnabled()){
                    CharSequence tagSequence = at.getText().subSequence(start, end);
                    log.trace(" > reduce tag {} - no overlapp with linkable token", tagSequence);
                }
            } else { //if the tag overlaps a linkable token 
                TokenData linkableToken = linkableTokenContext.linkableToken;
                List<TokenData> tokens = linkableTokenContext.context;
                //calculate the matchable start/end span of the current TagLL
                int[] mSpan = new int[]{
                        Math.max(start,linkableToken.token.getStart()),
                        Math.min(end,linkableToken.token.getEnd())};
                if(mSpan[0] > start){
                    for(int i = linkableToken.index-1; i >= 0; i--){
                        TokenData token = tokens.get(i);
                        int tStart = token.token.getStart();
                        if(tStart < start){
                            break;
                        } else if(token.isMatchable){
                            mSpan[0] = tStart;
                        }
                    }
                }
                if(mSpan[1] < end){
                    for(int i= linkableToken.index+1; i < tokens.size();i++){
                        TokenData token = tokens.get(i);
                        int tEnd = token.token.getEnd();
                        if(tEnd > end){
                            break;
                        } else if(token.isMatchable){
                            mSpan[1] = tEnd;
                        }
                    }
                }
                if(log.isTraceEnabled()){
                    CharSequence text = at.getText();
                    log.trace(" - matchable Span {}{} for Tag {}[{},{}]", 
                        new Object[]{ text.subSequence(mSpan[0],mSpan[1]),
                            Arrays.toString(mSpan), text.subSequence(start, end),
                            start, end});
                }
                matchableTagSpan.put(tag, mSpan);
                ChunkData cd = linkableToken.inChunk; //check if it matches > 50% of the chunk
                if(!lpc.isIgnoreChunks() && cd != null && cd.isProcessable){
                    int cstart = cd.getMatchableStartChar() >= 0 ? cd.getMatchableStartChar() :
                        start;
                    int cend = cd.getMatchableEndChar();
                    if(cstart < start || cend > end){ //if the tag does not cover the whole chunk
                        int num = 0;
                        int match = 0;
                        for(int i = cd.getMatchableStart(); i <= cd.getMatchableEnd(); i++){
                            TokenData td = tokens.get(i);
                            if(td.isMatchable){
                                num++;
                                if(match < 1 && td.token.getStart() >= start ||
                                        match > 0 && td.token.getEnd() <= end){
                                    match++;
                                }
                            }
                        }
                        //only accept tags with more as half of the matchable
                        //tokens in the Chunk are matched!
                        if(((float)match/(float)num) < minChunkMatchScore &&
                                match < minFoundTokens){
                            tag.removeLL(); //ignore
                            matchableTagSpan.remove(tag);
                            if(log.isTraceEnabled()){
                                CharSequence text = at.getText();
                                log.trace(" - reduce tag {}[{},{}] - does only match "
                                    + "{} of {} of matchable Chunk {}[{},{}]", 
                                    new Object[]{text.subSequence(start, end), start, end, match,  
                                            num, text.subSequence(cstart, cend), cstart, cend});
                            }
                        } else if(log.isTraceEnabled()){
                            CharSequence text = at.getText();
                            log.trace(" + keep tag {}[{},{}] - matches {} of {} "
                                + "matchable Tokens for matchable Chunk {}[{},{}]", 
                                new Object[]{text.subSequence(start, end), start, end, match,
                                        num, text.subSequence(cstart, cend), cstart, cend});
                        }
                    } else {
                        if(log.isTraceEnabled()){
                            CharSequence text = at.getText();
                            log.trace(" + keep tag {}[{},{}] - matches whole Chunk {}[{},{}]", 
                                new Object[]{text.subSequence(start, end), start, end, 
                                     text.subSequence(cstart, cend), cstart, cend});
                        }
                    }
                } else if(log.isTraceEnabled()){
                    CharSequence tagSequence = at.getText().subSequence(start, end);
                    log.trace(" + keep tag {} - not in processable chunk", tagSequence);
                }
            }
        }
        //(2) reduce Tags base on longest dominant right based on the matchable
        //    spans
        //NOTE: This is the same code as TagClusterReducer#LONGEST_DOMINANT_RIGHT
        //      but adapted to use the matchable spans instead of the full Tag
        //      spans
        if (head.length == 0 || head[0] == null || head[0].getNextTag() == null) {
            return; //no tag left from phase one or single token optimization
        }
        Set<TagLL> marked = new HashSet<TagLL>(); //can not use TagLL#mark
        while (true) {
            // --Find longest not already marked
            TagLL longest = null;
            int longestMCharLen = -1;
            int[] longestMSpan = null;
            for (TagLL t = head[0]; t != null; t = t.getNextTag()) {
                int[] mSpan = matchableTagSpan.get(t);
                int mCharLen = mSpan[1] - mSpan[0];
                if (!marked.contains(t) && (longest == null || mCharLen >= longestMCharLen)) {
                    longest = t;
                    longestMSpan = mSpan;
                    longestMCharLen = mCharLen;
                }
            }
            if (longest == null) break;
            // --Mark longest (so we return it eventually)
            marked.add(longest);
            // --Remove tags overlapping this longest
            for (TagLL t = head[0]; t != null; t = t.getNextTag()) {
                if (marked.contains(t)) {
                    continue;
                }
                int[] mSpan = matchableTagSpan.get(t);
                boolean overlaps =
                        mSpan[0] < longestMSpan[0] ? mSpan[1] > longestMSpan[1] : mSpan[0] < longestMSpan[1];
                if (overlaps) {
                    t.removeLL();
                } else if (mSpan[0] >= longestMSpan[1]) {
                    break;// no subsequent can possibly overlap
                }
            }
        }// loop
    }
    /**
     * Holds the context for a linkable {@link Token}s. This ensures that the
     * list of Tokens of the current {@link Section} (typically a {@link Sentence}) 
     * is still available even if the {@link LinkableTokenFilter#sectionData} does hold
     * already tokens for the next section.<p>
     * This is necessary as {@link LinkableTokenFilter#reduce(TagLL[])} can
     * be called for the previous sentence in cases where a Tag cluster includes
     * the last {@link Token} of a {@link Section}.
     * @author Rupert Westenthaler
     *
     */
    private static class LinkableTokenContext {
        final TokenData linkableToken;
        final List<TokenData> context;
        
        LinkableTokenContext(TokenData linkableToken, List<TokenData> context){
            this.linkableToken = linkableToken;
            this.context = context;
        }
    }
    
}
