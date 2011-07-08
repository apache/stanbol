package org.apache.stanbol.commons.opennlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.Span;

/**
 * Simple version of a {@link Chunker} that uses the POS tags to build chunks.
 * It does not implement the {@link Chunker} interface because implementing
 * methods other than the {@link Chunker#chunkAsSpans(String[], String[])}
 * is not feasible.
 * 
 * TODO: <ul>
 *   <li> Test if POS tags are the same for different languages
 *   <li> Check if it is possible to implement the {@link Chunker} interface
 *   </ul>
 * @author Rupert Westenthaler
 *
 */
public class PosTypeChunker {
    /**
     * Set of POS tags used to build chunks of no {@link Chunker} is used.
     * NOTE that all tags starting with 'N' (Nouns) are included anyway
     */
    public static final Set<String> DEFAULT_FOLLOW_POS_TYPES = Collections.unmodifiableSet(
        new TreeSet<String>(Arrays.asList(
            "#","$"," ","(",")",",",".",":","``","POS","CD","IN","FW",
            "NN","NNP","NNPS","NNS")));//,"''")));
    /**
     * Set of POS tags used for searches.
     * NOTE that all tags starting with 'N' (Nouns) are included anyway
     */
    public static final Set<String> DEFAULT_BUILD_CHUNK_POS_TYPES = Collections.unmodifiableSet(
        new TreeSet<String>(Arrays.asList(
            "NN","NNP","NNPS","NNS","FW")));//,"''")));
    
    public final Set<String> followTypes;
    public final Set<String> buildTypes;

    /**
     * Initialise a new PosTypeChunker with the default POS type configuration
     */
    public PosTypeChunker(){
        this(null,null);
    }
    /**
     * Initialise a new PosTypeChunker for the parsed Types.<p>
     * Note that buildPosTypes are not automatically followed. They need be
     * explicitly added to the followPosTypes!.
     * @param buildPosTypes the POS types that trigger a new Chunk
     * @param followPosType the POS types followed to build Chunks
     */
    public PosTypeChunker(Set<String> buildPosTypes,Set<String> followPosType){
        this.buildTypes = buildPosTypes == null ?
                DEFAULT_BUILD_CHUNK_POS_TYPES :
                    new HashSet<String>(buildPosTypes);
        this.followTypes = followPosType == null ?
                DEFAULT_FOLLOW_POS_TYPES :
                    new HashSet<String>(followPosType);
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
     * Build the chunks based on the parsed tokens and tags. <p>
     * This method is the equivalent to {@link Chunker#chunkAsSpans(String[], String[])}
     * @param tokens the tokens
     * @param tags the POS tags for the tokens
     * @return the chunks as spans over the parsed tokens
     */
    public Span[] chunkAsSpans(String[] tokens, String[] tags) {
        int consumed = -1;
        List<Span> chunks = new ArrayList<Span>();
        for(int i=0;i<tokens.length;i++){
            if(includePOS(tags[i])){
                int start = i;
                while(start-1 > consumed && followPOS(tags[start-1])){
                    start--; //follow backwards until consumed
                }
                int end = i;
                while(end+1 < tokens.length && followPOS(tags[end+1])){
                    end++; //follow forwards until consumed
                }
                chunks.add(new Span(start,end));
                consumed = end;
                i = end;
            }//build no chunk for this token
        }
        return chunks.toArray(new Span[chunks.size()]);
    }


}
