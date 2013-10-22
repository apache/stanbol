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

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.Token;

/**
 * Represents the list of dependency(grammatical) relations that a {@link Token}
 * has with other {@link Token}s from the same {@link Sentence}.
 * 
 * It is attached to a {@link Token} via {@link DEPENDENCY_ANNOTATION}.
 * 
 * @author Cristian Petroaca
 * 
 */
public class DependencyFeatures {

	/**
	 * The set of grammatical relations
	 */
	private Set<DependencyRelation> relations;

	public DependencyFeatures() {
		this.relations = new HashSet<DependencyRelation>();
	}

	public DependencyFeatures(Set<DependencyRelation> relations) {
		this.relations = relations;
	}

	public Set<DependencyRelation> getRelations() {
		return relations;
	}
	//TODO: IMO annotations 
	public void addRelation(DependencyRelation relation) {
		this.relations.add(relation);
	}

	public int hashCode() {
		return ((relations != null) ? relations.hashCode() : 0);
	}

	public boolean equals(Object obj) {
		return super.equals(obj)
				&& (obj instanceof DependencyFeatures)
				&& (this.relations.equals(((DependencyFeatures) obj)
						.getRelations()));
	}
}
