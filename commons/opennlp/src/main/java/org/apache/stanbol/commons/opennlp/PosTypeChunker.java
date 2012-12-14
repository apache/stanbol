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
 * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
 *
 */
public class PosTypeChunker {
    
    private final double minPosProb;
    
    private final Set<String> followTypes;

    private final Set<String> buildTypes;

    /**
     * Creates an instance for the given language based on the configuration
     * within the {@link PosTagsCollectionEnum}.
     * @param lang The language
     * @param minPosTagProbaility The minimum probability of a POS tag so that
     * it is processed. In case of lower Probabilities POS tags are ignored and
     * assumed to be matching.
     * @return the instance or <code>null</code> if no configuration for the
     * parsed language is present in the {@link PosTagsCollectionEnum}.
     */
    public static PosTypeChunker getInstance(String lang,double minPosTagProbaility){
        Set<String> nounPosTagCollection = 
            PosTagsCollectionEnum.getPosTagCollection(lang, PosTypeCollectionType.NOUN);
        if(nounPosTagCollection != null && !nounPosTagCollection.isEmpty()){
            return new PosTypeChunker(nounPosTagCollection, 
                PosTagsCollectionEnum.getPosTagCollection(
                    lang,PosTypeCollectionType.FOLLOW),minPosTagProbaility);
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
    public PosTypeChunker(Set<String> buildPosTypes,Set<String> followPosTypes,double minPosProb){
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
        if(minPosProb > 1){
            throw new IllegalArgumentException("The minimum POS tag probalility MUST BE set to a value [0..1] or values < 0 to deactivate this feature (parsed="+minPosProb+")!");
        } else {
            this.minPosProb = minPosProb;
        }
    }
    /**
     * @param props the probabilities of the pos tags or <code>null</code> if
     * not available
     * @param pos the POS tags
     * @return <code>true</code> if follow
     */
    private boolean followPOS(double[] props,String... pos){
        boolean reject = false;
        for(int i=0;i<pos.length;i++){
            if(props == null || props[i] >= minPosProb){
                if(followTypes.contains(pos[i])){
                    return true;
                } else {
                    reject = true;
                }
            } //else  prob to low ... do not process
        }
        //in case we have not found a POS tag with a prob > minPosProb
        //return TRUE
        return !reject;
    }
    
    private boolean includePOS(double[] props,String... pos){
        boolean reject = false;
        for(int i=0;i<pos.length;i++){
            if(props == null || props[i] >= minPosProb){
                if(buildTypes.contains(pos[i])){
                    return true;
                } else { 
                    reject = true;
                } 
            }
        }
        //in case we have not found a POS tag with a prob > minPosProb
        //return TRUE
        return !reject;
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
     * Build the chunks based on the parsed tokens and POS tags. <p>
     * This method is the equivalent to 
     * {@link opennlp.tools.chunker.Chunker#chunkAsSpans(String[], String[])}
     * @param tokens the tokens
     * @param tags the POS tags for the tokens
     * @return the chunks as spans over the parsed tokens
     */
    public Span[] chunkAsSpans(String[] tokens, String[] tags) {
      int consumed = -1;
        List<Span> chunks = new ArrayList<Span>();
        for(int i=0;i<tokens.length;i++){
            if(includePOS(null,tags[i])){
                int start = i;
                while(start-1 > consumed && followPOS(null,tags[start-1])){
                    start--; //follow backwards until consumed
                }
                int followEnd = i;
                int end = i;
                while(followEnd+1 < tokens.length && followPOS(null,tags[followEnd+1])){
                    followEnd++; //follow
                    if(includePOS(null,tags[followEnd])){
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
    /**
     * Build the chunks based on the parsed tokens and the one or more detected
     * POS tags alternatives for the tokens. <p>
     * @param tokens the tokens
     * @param tags the POS tags for the tokens (1D:tokens; 2D:POS tags)
     * @return the chunks as spans over the parsed tokens
     */
    public Span[] chunkAsSpans(String[] tokens, String[][] tags,double[][]props) {
        //NOTE: this is a 1:1 copy of the above method!! However this is the
        //      only solution, because merging them into a single one would
        //      need to copy the Stirng[] of the other into a String[][1] as
        //      used by this one :(
        //      If someone has a better Idea feel free to change!
        //      Rupert Westenthaler (28.Sep.2011)
        int consumed = -1;
        List<Span> chunks = new ArrayList<Span>();
        for(int i=0;i<tokens.length;i++){
            if(includePOS(props[i],tags[i])){
                int start = i;
                //do not follow backwards!
                while(start-1 > consumed && followPOS(props[start-1],tags[start-1])){
                    start--; //follow backwards until consumed
                }
                int followEnd = i;
                int end = i;
                while(followEnd+1 < tokens.length && followPOS(props[followEnd+1],tags[followEnd+1])){
                    followEnd++; //follow
                    if(includePOS(props[followEnd],tags[followEnd])){
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
