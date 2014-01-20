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

		if (grammaticalRelation == null) {
			throw new IllegalArgumentException("The grammatical relation cannot be null");
		}
		
		this.grammaticalRelation = grammaticalRelation;
	}

	public GrammaticalRelation getGrammaticalRelation() {
		return grammaticalRelation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
        int result = super.hashCode();
        result = prime * result + grammaticalRelation.hashCode();
        
        return result;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj)
				&& obj instanceof GrammaticalRelationTag
				&& grammaticalRelation
						.equals(((GrammaticalRelationTag) obj).grammaticalRelation);
	}
}
