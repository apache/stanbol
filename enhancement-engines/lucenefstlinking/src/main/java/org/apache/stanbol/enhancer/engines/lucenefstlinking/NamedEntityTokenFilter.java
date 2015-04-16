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

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.opensextant.solrtexttagger.TagClusterReducer;
import org.opensextant.solrtexttagger.TagLL;
import org.opensextant.solrtexttagger.TaggingAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that ensures that only Tokens within a {@link Chunk} with a 
 * {@link NerTag} are processed.<p>
 * This is ensured on two places:<ol>
 * <li> Classifies Tokens in the Solr {@link TokenStream} with the {@link TaggingAttribute}
 * based on {@link NerTag}s present in the {@link AnalysedText}.<p>
 * <li> Implements {@link TagClusterReducer} to ensure that all {@link TagLL tags}
 * that do not cover the whole Named Entity are removed from the Cluster.
 * </ol>
 * <b> Implementation Details</b><p>
 * The {@link TokenStream} implementation of this class does set
 * <code>{@link TaggingAttribute#isTaggable()} == ture</code> if the do overlap 
 * with a {@link Chunk} having an {@link NerTag}
 * <p>
 * The {@link TagClusterReducer} implementation keeps track of Chunks with 
 * {@link NerTag} while iterating over the {@link TokenStream} and adds them to 
 * the end of a List. When {@link TagClusterReducer#reduce(TagLL[])} is called 
 * tags of the cluster are checked if they do cover Chunks with a {@link NerTag}.
 * If they do not they are removed from the cluster.
 * <p>
 * This implementation was derived from the {@link LinkableTokenFilter}
 * 
 * @author Rupert Westenthaler
 *
 */
public final class NamedEntityTokenFilter extends TokenFilter implements TagClusterReducer{

    private final Logger log = LoggerFactory.getLogger(NamedEntityTokenFilter.class);
    
    /**
     * The NLP processing results
     */
    private AnalysedText at;
    /**
     * The language of the text
     */

    /**
     * Iterator over all {@link Chunk}s in the {@link AnalysedText} that do 
     * have an {@link NerTag}
     */
    private Iterator<Chunk> neChunks;
    
    protected final CharTermAttribute termAtt;
    protected final OffsetAttribute offset;
    protected final TaggingAttribute taggable;
    
    private int lookupCount = 0;
    private int incrementCount = 0;

    /**
     * List with {@link Chunk}s having {@link NerTag}s. This is used by
     * the {@link #reduce(TagLL[])} method to check if {@link TagLL tags} 
     * do cover Named Entities detected in the text.
     */
    private List<Chunk> nePhrases;

    private final NavigableMap<int[],Set<String>> nePhrasesTypes;
    
    private Chunk neChunk;

    protected final boolean wildcardType;

    protected final Set<String> neTypes;

    /**
     * A Token Filter for Named Entities of the configured types. Also collects
     * '<code>span -&gt type</code>' mappings for Named Entities.
     * @param input the input token stream for the parsed text
     * @param at the {@link AnalysedText} containing {@link NerTag} values
     * @param lang the language of the text
     * @param neTypes the string {@link NerTag#getType()} and {@link NerTag#getTag()}
     * values of enabled Named Entities. If <code>null</code> or containing the
     * <code>null</code> element all types will be accepted.
     * @param nePhrasesTypes The {@link NavigableMap} used to store the spans of
     * named entities as key and the set o their {@link NerTag#getTag()} and 
     * {@link NerTag#getType()} as values. Those information are collected while
     * iterating over the text (by the {@link NamedEntityPredicate}) and are
     * used later for filtering {@link Match}es based on the type of the Entities.
     * Typically the {@link TaggingSession#entityMentionTypes} is parsed as this
     * parameter.
     */
    protected NamedEntityTokenFilter(TokenStream input, AnalysedText at, String lang,
            Set<String> neTypes, NavigableMap<int[],Set<String>> nePhrasesTypes) {
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
        this.wildcardType = neTypes == null || neTypes.contains(null);
        this.neTypes = neTypes;
        this.nePhrasesTypes = nePhrasesTypes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reset() throws IOException {
        super.reset();
        nePhrases = new LinkedList<Chunk>();
        neChunks = new FilterIterator(at.getChunks(), new NamedEntityPredicate());
    }
    
    @Override
    public boolean incrementToken() throws IOException {
        if(input.incrementToken()){
            incrementCount++;
            if(log.isTraceEnabled()){
	            log.trace("> solr:[{},{}] {}",new Object[]{
	                            offset.startOffset(), offset.endOffset(), termAtt});
            }
            while((neChunk == null || neChunk.getEnd() < offset.startOffset()) && neChunks.hasNext()){
                neChunk = neChunks.next();
                nePhrases.add(neChunk);
            }
            if(neChunk == null){
                taggable.setTaggable(false);
                incrementCount++;
                log.debug("lookup percentage: {}",lookupCount*100/(float)incrementCount);
                return false;
            } else if(offset.endOffset() > neChunk.getStart() 
                    || offset.startOffset() < neChunk.getEnd()){
                //set tagable to true if the tokens overlapps with the current chunk
                taggable.setTaggable(true);
                if(log.isTraceEnabled()){
                    log.trace("lookup: token [{},{}]: {} | named Entity [{},{}]:{}", 
                        new Object[]{ offset.startOffset(), offset.endOffset(), 
                            termAtt, neChunk.getStart(), neChunk.getEnd(),
                            neChunk.getSpan()});
                }
                lookupCount++;
            } else {
                taggable.setTaggable(false);
            }
            incrementCount++;
            return true;
        } else { //no more tokens in the parent token stream
            return false;
        }
    }

    @Override
    public void reduce(TagLL[] head) {
        //(1) reduce Tags based on named entity phrases. 
        for(TagLL tag = head[0]; tag != null; tag = tag.getNextTag()) {
            int start = tag.getStartOffset();
            int end = tag.getEndOffset();
            Chunk nePhrase = nePhrases.isEmpty() ? null : nePhrases.get(0);
            while(nePhrase != null && nePhrase.getEnd() <= start){
                nePhrases.remove(0);
                nePhrase = nePhrases.isEmpty() ? null : nePhrases.get(0);
            }
            if(nePhrase == null || !(start <= nePhrase.getStart() && end >= nePhrase.getEnd())){
                //does not cover any named entity phrase
                tag.removeLL(); //remove the tag from the cluster
                if(log.isTraceEnabled()){
                    log.trace(" > reduce tag {} - does not cover {}", tag, nePhrase);
                }
            } else if(log.isTraceEnabled()) {//the current Tag coveres a named entity phrase
                log.trace(" > keep tag {} for {}", tag, nePhrase);
            }
        }
    }
        
    /**
     * {@link Predicate} used to select Named Entities based on matching 
     * {@link NerTag#getTag()} and {@link NerTag#getType()} values against the
     * {@link NamedEntityTokenFilter#neTypes} configuration. As a side effect
     * this also collects the {@link NamedEntityTokenFilter#nePhrasesTypes}
     * information. This avoids a 2nd pass over the {@link AnalysedText} to
     * collect those information
     * @author Rupert Westenthaler
     *
     */
    final class NamedEntityPredicate implements Predicate {
        @Override
        public boolean evaluate(Object o) {
            if(o instanceof Chunk){
                Chunk chunk = (Chunk)o;
                Value<NerTag> nerValue = chunk.getAnnotation(NER_ANNOTATION);
                if(nerValue != null){
                    NerTag nerTag = nerValue.value();
                    String nerType = nerTag.getType() != null ? 
                            nerTag.getType().getUnicodeString() : null;
                    if( wildcardType || neTypes.contains(nerTag.getTag())
                            || (nerType != null && neTypes.contains(nerType))){
                        int[] span = new int[]{chunk.getStart(), chunk.getEnd()};
                        Set<String> types = nePhrasesTypes.get(span);
                        if(types == null){
                            types = new HashSet<String>(4);
                            nePhrasesTypes.put(span, types);
                        }
                        types.add(nerType);
                        types.add(nerTag.getTag());
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
