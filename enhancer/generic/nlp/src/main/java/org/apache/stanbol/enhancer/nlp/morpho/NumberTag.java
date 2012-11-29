package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An Number tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class NumberTag extends Tag<NumberTag>{
    private final NumberFeature numberCategory;
    /**
     * Creates a new Number tag for the parsed tag. The created Tag is not
     * assigned to any {@link NumberFeature}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public NumberTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a NumberFeature tag that is assigned to a {@link NumberFeature}
     * @param tag the tag
     * @param numberCategory the lexical Number  or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public NumberTag(String tag, NumberFeature numberCategory){
        super(tag);
        this.numberCategory = numberCategory;
    }
    /**
     * Get the Number of this tag (if known)
     * @return the NumberFeature or <code>null</code> if not mapped to any
     */
    public NumberFeature getNumber(){
       return this.numberCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("NUMBER %s (%s)", tag,
        	numberCategory == null ? "none" : numberCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof NumberTag &&
            (numberCategory == null && ((NumberTag)obj).numberCategory == null) ||
                    (numberCategory != null && numberCategory.equals(((NumberTag)obj).numberCategory));
    }
    
}
