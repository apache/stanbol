package org.apache.stanbol.enhancer.nlp.dependency;

import org.apache.stanbol.enhancer.nlp.model.tag.Tag;

/**
 * Represents a grammatical relation tag between two {@link Token}s
 * 
 * @author Cristian Petroaca
 * 
 */
public class GrammaticalRelationTag extends Tag<GrammaticalRelationTag> {

	/**
	 * The actual grammatical relation object
	 */
	private GrammaticalRelation grammaticalRelation;

	public GrammaticalRelationTag(String tag) {
		super(tag);
	}

	public GrammaticalRelationTag(String tag,
			GrammaticalRelation grammaticalRelation) {
		this(tag);

		this.setGrammaticalRelation(grammaticalRelation);
	}

	public GrammaticalRelation getGrammaticalRelation() {
		return grammaticalRelation;
	}

	public void setGrammaticalRelation(GrammaticalRelation grammaticalRelation) {
		this.grammaticalRelation = grammaticalRelation;
	}
}
