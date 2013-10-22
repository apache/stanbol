/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.enhancer.nlp.dependency;

import org.apache.stanbol.enhancer.nlp.model.Span;

/**
 * Represents the grammatical relation that a {@link Token} can have with
 * another {@link Token} from the same {@link Sentence}
 * 
 * @author Cristian Petroaca
 * 
 */
public class DependencyRelation {

	/**
	 * The actual grammatical relation tag
	 */
	private GrammaticalRelationTag grammaticalRelationTag;

	/**
	 * Denotes whether the {@link Token} which has this relation is dependent in
	 * the relation
	 */
	private boolean isDependent;

	/**
	 * The {@link Token} with which the relation is made.
	 */
	private Span partner;

	public DependencyRelation() {
	}

	public DependencyRelation(GrammaticalRelationTag grammaticalRelationTag) {
		this.grammaticalRelationTag = grammaticalRelationTag;
	}

	public DependencyRelation(GrammaticalRelationTag grammaticalRelationTag,
			boolean isDependent, Span partner) {
		this(grammaticalRelationTag);

		this.isDependent = isDependent;
		this.partner = partner;
	}

	public GrammaticalRelationTag getGrammaticalRelationTag() {
		return grammaticalRelationTag;
	}

	public void setGrammaticalRelationTag(
			GrammaticalRelationTag grammaticalRelationTag) {
		this.grammaticalRelationTag = grammaticalRelationTag;
	}

	public boolean isDependent() {
		return isDependent;
	}

	public void setDependent(boolean isDependent) {
		this.isDependent = isDependent;
	}

	public Span getPartner() {
		return this.partner;
	}

	public void setPartner(Span partner) {
		this.partner = partner;
	}

	public int hashCode() {
		return grammaticalRelationTag.hashCode()
				+ ((partner != null) ? partner.hashCode() : 0)
				+ +(isDependent ? 1 : 0);
	}

	public boolean equals(Object obj) {
		return super.equals(obj)
				&& (obj instanceof DependencyRelation)
				&& (this.grammaticalRelationTag
						.equals(((DependencyRelation) obj)
								.getGrammaticalRelationTag()))
				&& (this.isDependent == ((DependencyRelation) obj)
						.isDependent())
				&& (this.partner
						.equals(((DependencyRelation) obj).getPartner()));
	}
}
