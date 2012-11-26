package org.apache.stanbol.enhancer.nlp.phrase;

import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

public class PhraseTag extends Tag<PhraseTag>{

    private final LexicalCategory category;

    /**
     * Creates a new Phrase tag for the parsed tag. The created Tag is not
     * assigned to any {@link LexicalCategory}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a PhraseTag that is assigned to a {@link LexicalCategory}
     * @param tag the tag
     * @param category the lexical category or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public PhraseTag(String tag,LexicalCategory category){
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
        return String.format("Phrase %s (%s)", tag,
            category == null ? "none" : category.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof PhraseTag &&
            (category == null && ((PhraseTag)obj).category == null) ||
                    (category != null && category.equals(((PhraseTag)obj).category));
    }
    
    
}
