package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An Case tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class CaseTag extends Tag<CaseTag>{
    
    private final Case caseCategory;
    /**
     * Creates a new Case tag for the parsed tag. The created Tag is not
     * assigned to any {@link Case}.<p> This constructor can be used
     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
     * (e.g. that is not defined by the configured {@link TagSet}).<p>
     * @param tag the Tag
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public CaseTag(String tag){
        this(tag,null);
    }
    /**
     * Creates a CaseTag that is assigned to a {@link Case}
     * @param tag the tag
     * @param case the lexical case or <code>null</code> if not known
     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
     * or empty.
     */
    public CaseTag(String tag,Case caseCat){
        super(tag);
        this.caseCategory = caseCat;
    }
    /**
     * The case of this tag (if known)
     * @return the case or <code>null</code> if not mapped to any
     */
    public Case getCase(){
       return this.caseCategory; 
    }
    
    @Override
    public String toString() {
        return String.format("CASE %s (%s)", tag,
        	caseCategory == null ? "none" : caseCategory.name());
    }
    
    @Override
    public int hashCode() {
        return tag.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof CaseTag &&
            (caseCategory == null && ((CaseTag)obj).caseCategory == null) ||
                    (caseCategory != null && caseCategory.equals(((CaseTag)obj).caseCategory));
    }
    
}