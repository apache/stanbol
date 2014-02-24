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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionData {

    private static final Logger log = LoggerFactory.getLogger(SectionData.class);
    
    /**
     * The section
     */
    public final Section section;
    /**
     * Holds the {@link Token}s of the current {@link #sentence} 
     * to allow fast index based access.
     */
    private List<TokenData> tokens = new ArrayList<TokenData>(64);
    /**
     * If a linkable token is present in this section
     */
    private boolean hasLinkableToken = false;

    public SectionData(LanguageProcessingConfig tpc, Section section, 
            Set<SpanTypeEnum> enclosedSpanTypes, boolean isUnicaseLanguage){
        this.section = section;
        Iterator<Span> enclosed = section.getEnclosed(enclosedSpanTypes);
        List<ChunkData> activeChunks = new ArrayList<ChunkData>();
        while(enclosed.hasNext()){
            Span span = enclosed.next();
            if(span.getStart() >= span.getEnd()){ //save guard against empty spans
                log.warn("Detected Empty Span {} in section {}: '{}'",
                    new Object[]{span,section, section.getSpan()});
            }
            if(span.getType() == SpanTypeEnum.Chunk){
                ChunkData chunkData = new ChunkData(tpc,(Chunk)span);
                if(chunkData.isProcessable()){
                	activeChunks.add(0, chunkData);
                	chunkData.startToken = tokens.size();
                    if(log.isDebugEnabled()){
                        log.debug(">> Chunk: (type:{}, startPos: {}) text: '{}'",
                            new Object []{
                        		chunkData.chunk.getType(),
                        		chunkData.startToken,
                        		chunkData.chunk.getSpan()
                            });
                    } 
                } //else ignore chunks that are not processable
            } else if(span.getType() == SpanTypeEnum.Token){
                TokenData tokenData = new TokenData(tpc,tokens.size(),(Token)span,
                		activeChunks.isEmpty() ? null : activeChunks.get(0));
                if(log.isDebugEnabled()){
                    log.debug("  > {}: {} {}(pos:{}) chunk: '{}'",
                        new Object[]{tokenData.index,tokenData.token,
                            tokenData.morpho != null ? ("(lemma: "+tokenData.morpho.getLemma()+") ") : "",
                            tokenData.token.getAnnotations(POS_ANNOTATION),
                            tokenData.inChunk != null ? tokenData.inChunk.chunk.getSpan() : "none"});
                }
                if(!tokenData.hasAlphaNumeric){
                    tokenData.isLinkable = false;
                    tokenData.isMatchable = false;
                } else {
                    // (1) apply basic rules for linkable/processable tokens
                    //determine if the token should be linked/matched
                    tokenData.isLinkable = tokenData.isLinkablePos != null ? tokenData.isLinkablePos : false;
                    //matchabel := linkable OR has matchablePos
                    tokenData.isMatchable = tokenData.isLinkable || 
                            (tokenData.isMatchablePos != null && tokenData.isMatchablePos);
                    
                    //(2) for non linkable tokens check for upper case rules
                    if(!tokenData.isLinkable && tokenData.upperCase && 
                            tokenData.index > 0 && //not a sentence or sub-sentence start
                            !tokens.get(tokenData.index-1).isSubSentenceStart){
                        //We have an upper case token!
                        if(tpc.isLinkUpperCaseTokens()){
                            if(tokenData.isMatchable) { //convert matchable to 
                                tokenData.isLinkable = true; //linkable
                                tokenData.isMatchable = true;
                            } else { // and other tokens to
                                tokenData.isMatchable = true; //matchable
                            }
                        } else { 
                            //finally we need to convert other Tokens to matchable
                            //if MatchUpperCaseTokens is active
                            if(!tokenData.isMatchable && tpc.isMatchUpperCaseTokens()){
                                tokenData.isMatchable = true;
                            }
                        }
                    } //else not an upper case token
                    
                    //(3) Unknown POS tag Rules (see STANBOL-1049)
                    if(!tokenData.isLinkable && (tokenData.isLinkablePos == null || 
                            tokenData.isMatchablePos == null)){
                        if(isUnicaseLanguage || !tpc.isLinkOnlyUpperCaseTokensWithUnknownPos()){
                            if(tokenData.isLinkablePos == null && tokenData.hasSearchableLength){
                                tokenData.isLinkable = true;
                                tokenData.isMatchable = true;
                            } //else no need to change the state
                        } else { //non unicase language and link only upper case tokens enabled
                            if(tokenData.upperCase && // upper case token
                                    tokenData.index > 0 && //not a sentence or sub-sentence start
                                    !tokens.get(tokenData.index-1).isSubSentenceStart){
                                if(tokenData.hasSearchableLength && tokenData.isLinkablePos == null){
                                    tokenData.isLinkable = true;
                                    tokenData.isMatchable = true;
                                } else if(tokenData.isMatchablePos == null){
                                    tokenData.isMatchable = true;
                                }
                            } else if(tokenData.hasSearchableLength &&  //lower case and long token
                                    tokenData.isMatchablePos == null){ 
                                tokenData.isMatchable = true;
                            } //else lower case and short word 
                        }
                    } //else already linkable or POS tag present
                }
                log.debug("    - {}",tokenData); 
                //add the token to the list
                tokens.add(tokenData);
                if(!hasLinkableToken){
                    hasLinkableToken = tokenData.isLinkable;
                }
                Iterator<ChunkData> activeChunkIt = activeChunks.iterator();
                while(activeChunkIt.hasNext()){
                	ChunkData activeChunk = activeChunkIt.next();
                    if (tokenData.isLinkable){
                        activeChunk.hasLinkable = true;
                        //ignore matchableCount in Chunks with linkable Tokens
                        activeChunk.matchableCount = -10; //by setting the count to -10
                    } else if(tokenData.isMatchable){
                        activeChunk.matchableCount++;
                    }
                    if(tokenData.isMatchable){ //for matchable tokens
                        //update the matchable span within the active chunk
                        if(activeChunk.matchableStart < 0){
                            activeChunk.matchableStart = tokenData.index;
                            activeChunk.matchableStartCharIndex = tokenData.token.getStart();
                        }
                        if(activeChunk.matchableStart >= 0){ //if start is set also set end
                            activeChunk.matchableEnd = tokenData.index;
                            activeChunk.matchableEndCharIndex = tokenData.token.getEnd();
                        }
                    }
                    if(span.getEnd() >= activeChunk.getEndChar()){
                        //this is the last token in the current chunk
                        activeChunk.endToken = tokens.size()-1;
                        if(log.isDebugEnabled()){
	                        log.debug(" << end Chunk {} '{}' @pos: {}", new Object[]{
	                        		activeChunk.chunk, activeChunk.chunk.getSpan(),
	                        		activeChunk.endToken});
                        }
                        if(tpc.isLinkMultiMatchableTokensInChunk() && 
                                activeChunk.getMatchableCount() > 1 ){
                            log.debug("   - multi-matchable Chunk:");
                            //mark the last of two immediate following matchable
                            //tokens as processable
                            for(int i = activeChunk.endToken-1;i >= activeChunk.startToken+1;i--){
                                TokenData ct = tokens.get(i);
                                TokenData pt = tokens.get(i-1);
                                if(ct.isMatchable && pt.isMatchable){
                                    if(!ct.isLinkable) { //if not already processable
                                        log.debug("     > convert Token {}: {} (pos:{}) from matchable to processable",
                                            new Object[]{i,ct.token.getSpan(),ct.token.getAnnotations(POS_ANNOTATION)});
                                        ct.isLinkable = true;
                                        if(!hasLinkableToken){
                                            hasLinkableToken = true;
                                        }
                                    }
                                    i--;//mark both (ct & pt) as processed
                                }
                            }
                        }
                        //remove the closed chunk from the list with active
                        activeChunkIt.remove(); 
                    }
                }
            }
        }
    }
    
    public List<TokenData> getTokens() {
        return tokens;
    }

    public boolean hasLinkableToken() {
        return hasLinkableToken;
    }
}