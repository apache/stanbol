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

	public DependencyRelation(GrammaticalRelationTag grammaticalRelationTag, boolean isDependent,
			Span partner) {
		if (grammaticalRelationTag == null) {
			throw new IllegalArgumentException("The grammatical relation tag cannot be null");
		}
		
		this.grammaticalRelationTag = grammaticalRelationTag;
		this.isDependent = isDependent;
		this.partner = partner;
	}

	public GrammaticalRelationTag getGrammaticalRelationTag() {
		return grammaticalRelationTag;
	}

	public boolean isDependent() {
		return isDependent;
	}

	public Span getPartner() {
		return this.partner;
	}
	
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + grammaticalRelationTag.hashCode();
        result = prime * result + (isDependent ? 1231 : 1237);
        result = prime * result + ((partner == null) ? 0 : partner.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        DependencyRelation other = (DependencyRelation) obj;
        
        if (partner == null) {
            if (other.partner != null)
                return false;
        } else if (!partner.equals(other.partner))
            return false;
        
        return (grammaticalRelationTag.equals(other.grammaticalRelationTag))
        	&& (isDependent == other.isDependent);
    }
}
