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
public class DefinitnessTag extends Tag<DefinitnessTag>{ 
    private final Definitness definitnessCategory;
    /**
     * Creates a new Definitness tag for the parsed tag. The created Tag is not
     * assigned to any {@link Definitness}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public DefinitnessTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a DefinitnessTag that is assigned to a {@link Definitness}
     * @param tag the tag
     * @param case the lexical case or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public DefinitnessTag(String tag, Definitness numberCategory){
        super(tag);
        this.definitnessCategory = numberCategory;
    }
    /**
     * The definitness of this tag (if known)
     * @return the Definitness or <code>null</code> if not mapped to any
     */
    public Definitness getDefinitness(){
       return this.definitnessCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("DEFINITNESS %s (%s)", tag,
        	definitnessCategory == null ? "none" : definitnessCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof DefinitnessTag &&
            (definitnessCategory == null && ((DefinitnessTag)obj).definitnessCategory == null) ||
                    (definitnessCategory != null && definitnessCategory.equals(((DefinitnessTag)obj).definitnessCategory));
    }
}
