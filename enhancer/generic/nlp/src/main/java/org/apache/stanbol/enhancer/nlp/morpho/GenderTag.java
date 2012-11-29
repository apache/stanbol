package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An Definitness tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class GenderTag extends Tag<GenderTag>{

    private final Gender genderCategory;
    /**
     * Creates a new Gender tag for the parsed tag. The created Tag is not
     * assigned to any {@link Gender}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public GenderTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a Gender that is assigned to a {@link Gender}
     * @param tag the tag
     * @param genderCategory the lexical Gender or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public GenderTag(String tag,Gender genderCategory){
        super(tag);
        this.genderCategory = genderCategory;
    }
    /**
     * The Gender of this tag (if known)
     * @return the Gender or <code>null</code> if not mapped to any
     */
    public Gender getGender(){
       return this.genderCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("GENDER %s (%s)", tag,
        	genderCategory == null ? "none" : genderCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof GenderTag &&
            (genderCategory == null && ((GenderTag)obj).genderCategory == null) ||
                    (genderCategory != null && genderCategory.equals(((GenderTag)obj).genderCategory));
    }
    
}
