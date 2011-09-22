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
package org.apache.stanbol.commons.opennlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import opennlp.tools.util.Span;

/**
 * Simple version of a {@link opennlp.tools.chunker.Chunker} that uses the POS tags to build chunks.
 * It does not implement the {@link opennlp.tools.chunker.Chunker} interface because implementing
 * methods other than the {@link opennlp.tools.chunker.Chunker#chunkAsSpans(String[], String[])}
 * is not feasible.<p>
 * Defaults are based on the <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
 * Penn Treebank</a> tag set

 * 
 * TODO: <ul>
 *   <li> Test if POS tags are the same for different languages
 *   <li> Check if it is possible to implement the {@link opennlp.tools.chunker.Chunker} interface
 *   </ul>
 * @author Rupert Westenthaler
 *
 */
public class PosTypeChunker {
    
    public final Set<String> followTypes;

    public final Set<String> buildTypes;

    /**
     * Creates an instance for the given language based on the configuration
     * within the {@link PosTagsCollectionEnum}.
     * @param lang The language
     * @return the instance or <code>null</code> if no configuration for the
     * parsed language is present in the {@link PosTagsCollectionEnum}.
     */
    public static PosTypeChunker getInstance(String lang){
        Set<String> nounPosTagCollection = 
            PosTagsCollectionEnum.getPosTagCollection(lang, PosTypeCollectionType.NOUN);
        if(nounPosTagCollection != null && !nounPosTagCollection.isEmpty()){
            return new PosTypeChunker(nounPosTagCollection, 
                PosTagsCollectionEnum.getPosTagCollection(
                    lang,PosTypeCollectionType.FOLLOW));
        } else {
            return null;
        }
        
    }
    /**
     * Initialise a new PosTypeChunker for the parsed POS tag collections. This
     * Constructor can be used if no predefined Configuration for a given 
     * language is available in the {@link PosTagsCollectionEnum}<p>
     * Note that buildPosTypes are added to the followed once. Therefore the
     * followPosTypes may or may not include some/all buildPosTypes.
     * @param buildPosTypes the POS types that trigger a new Chunk (MUST NOT be
     * <code>null</code> nor {@link Set#isEmpty() empty}).
     * @param followPosTypes additional POS types followed to extend Chunks (MAY
     * BE <code>null</code> or empty).
     */
    public PosTypeChunker(Set<String> buildPosTypes,Set<String> followPosTypes){
        if(buildPosTypes == null || buildPosTypes.isEmpty()){
            throw new IllegalArgumentException("The set of POS types used to" +
            		"build Chunks MUST NOT be NULL nor empty!");
        }
        this.buildTypes = Collections.unmodifiableSet(new TreeSet<String>(buildPosTypes));
        Set<String> follow = new TreeSet<String>();
        follow.addAll(buildTypes);
        if(followPosTypes != null){
            follow.addAll(followPosTypes);
        }
        this.followTypes = Collections.unmodifiableSet(follow);
    }
    /**
     * TODO: This might be language specific!
     * @param pos
     * @return
     */
    private boolean followPOS(String pos){
        return followTypes.contains(pos);
    }
    private boolean includePOS(String pos){
        return buildTypes.contains(pos);
    }
    /**
     * The set of POS types followed to extend Chunks. This includes the
     * {@link #getChunkPosTypes()} values
     * @return the followTypes
     */
    public final Set<String> getFollowedPosTypes() {
        return followTypes;
    }
    /**
     * The set of POS types used to create Chunks
     * @return the buildTypes
     */
    public final Set<String> getChunkPosTypes() {
        return buildTypes;
    }

    /**
     * Build the chunks based on the parsed tokens and tags. <p>
     * This method is the equivalent to 
     * {@link opennlp.tools.chunker.Chunker#chunkAsSpans(String[], String[])}
     * @param tokens the tokens
     * @param tags the POS tags for the tokens
     * @return the chunks as spans over the parsed tokens
     */
    public Span[] chunkAsSpans(String[] tokens, String[] tags) {
//        int consumed = -1;
        List<Span> chunks = new ArrayList<Span>();
        for(int i=0;i<tokens.length;i++){
            if(includePOS(tags[i])){
                int start = i;
                //do not follow backwards!
//                while(start-1 > consumed && followPOS(tags[start-1])){
//                    start--; //follow backwards until consumed
//                }
                int followEnd = i;
                int end = i;
                while(followEnd+1 < tokens.length && followPOS(tags[followEnd+1])){
                    followEnd++; //follow
                    if(includePOS(tags[followEnd])){
                        end = followEnd; //extend end only if act is include
                    }
                }
                chunks.add(new Span(start,end));
//                consumed = end;
                i = followEnd;
            }//build no chunk for this token
        }
        return chunks.toArray(new Span[chunks.size()]);
    }


}
