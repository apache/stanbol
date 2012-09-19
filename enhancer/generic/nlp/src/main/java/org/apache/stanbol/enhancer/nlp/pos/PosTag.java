package org.apache.stanbol.enhancer.nlp.pos;

import org.apache.stanbol.enhancer.nlp.Tag;
import org.apache.stanbol.enhancer.nlp.TagSet;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An POS (part-of-speech) tag typically assigned by an POS-Tagger (an
 * NLP component) to a {@link Token} by using the {@link POS#POSAnnotation}<p>
 * The only required field is {@link #getTag()} - the string tag assigned by
 * the POS Tagger.<p>
 * PosTags can be mapped to a {@link LexicalCategory} and be part of an
 * {@link TagSet}. NOTE that the {@link TagSet} is set by
 * the {@link TagSet#addTag(PosTag)} method.<p>
 */
public class PosTag extends Tag<PosTag>{
    
    private final LexicalCategory category;
    /**
     * Creates a new POS tag for the parsed tag. The created Tag is not
     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a PosTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PosTag(String tag,LexicalCategory category){
        super(tag);
        this.category = category;
    }
    /**
     * The LecxialCategory of this tag (if known)
     * @return the category or <code>null</code> if not mapped to any
     */
    public LexicalCategory getCategory(){
       return category; 
    }
    
    @Override
    public String toString() {
        return String.format("POS %s (%s)", tag,
            category == null ? "none" : category.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof PosTag &&
            (category == null && ((PosTag)obj).category == null) ||
                    (category != null && category.equals(((PosTag)obj).category));
    }
}
