package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An Tense tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class TenseTag extends Tag<TenseTag>{
    private final Tense tenseCategory;
    /**
     * Creates a new TenseTag for the parsed tag. The created Tag is not
     * assigned to any {@link Tense}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public TenseTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a TenseTag that is assigned to a {@link Case}
     * @param tag the tag
     * @param tenseCategory the lexical Tense or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public TenseTag(String tag, Tense tenseCategory){
        super(tag);
        this.tenseCategory = tenseCategory;
    }
    /**
     * The case of this tag (if known)
     * @return the Tense or <code>null</code> if not mapped to any
     */
    public Tense getTense(){
       return this.tenseCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("TENSE %s (%s)", tag,
        	tenseCategory == null ? "none" : tenseCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof TenseTag &&
            (tenseCategory == null && ((TenseTag)obj).tenseCategory == null) ||
                    (tenseCategory != null && tenseCategory.equals(((TenseTag)obj).tenseCategory));
    }
}
