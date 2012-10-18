package org.apache.stanbol.enhancer.nlp.morpho;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.tag.Tag;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An Person tag typically assigned by a Morphological Analyzer (an
 * NLP component) to a {@link Token} <p>
 * @author Alessio Bosca
 */
public class PersonTag extends Tag<PersonTag>{
	 private final Person personCategory;
	    /**
	     * Creates a new PersonTag for the parsed tag. The created Tag is not
	     * assigned to any {@link Person}.<p> This constructor can be used
	     * by {@link EnhancementEngine}s that encounter an Tag they do not know 
	     * (e.g. that is not defined by the configured {@link TagSet}).<p>
	     * @param tag the Tag
	     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
	     * or empty.
	     */
	    public PersonTag(String tag){
	        this(tag,null);
	    }
	    /**
	     * Creates a PersonTag that is assigned to a {@link Person}
	     * @param tag the tag
	     * @param personCategory the lexical Person or <code>null</code> if not known
	     * @throws IllegalArgumentException if the parsed tag is <code>null</code>
	     * or empty.
	     */
	    public PersonTag(String tag, Person personCategory){
	        super(tag);
	        this.personCategory = personCategory;
	    }
	    /**
	     * The case of this tag (if known)
	     * @return the case or <code>null</code> if not mapped to any
	     */
	    public Person getPerson(){
	       return this.personCategory; 
	    }
	    
	    @Override
	    public String toString() {
	        return String.format("PERSON %s (%s)", tag,
	        		personCategory == null ? "none" : personCategory.name());
	    }
	    
	    @Override
	    public int hashCode() {
	        return tag.hashCode();
	    }
	    
	    @Override
	    public boolean equals(Object obj) {
	        return super.equals(obj) && obj instanceof PersonTag &&
	            (personCategory == null && ((PersonTag)obj).personCategory == null) ||
	                    (personCategory != null && personCategory.equals(((PersonTag)obj).personCategory));
	    }
	    
}
