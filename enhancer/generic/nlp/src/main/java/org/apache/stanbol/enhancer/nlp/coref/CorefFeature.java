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
package org.apache.stanbol.enhancer.nlp.coref;

import java.util.Collections;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.Span;

/**
 * Represents a coreference resolution feature attached to a {@link Token}. It
 * contains information about other {@link Token}s which refer to the
 * aforementioned {@link Token}.
 * 
 * @author Cristian Petroaca
 * 
 */
public class CorefFeature {
	/**
	 * Shows whether the {@link Token} to which this object is attached is the
	 * representative mention in the chain.
	 */
	private boolean isRepresentative;

	/**
	 * A set of {@link Token}s representing metions of the {@link Token} to
	 * which this object is attached.
	 */
	private Set<Span> mentions;

	public CorefFeature(boolean isRepresentative, Set<Span> mentions) {
		if (mentions == null || mentions.isEmpty()) {
			throw new IllegalArgumentException("The mentions set cannot be null or empty");
		}
		
		this.isRepresentative = isRepresentative;
		this.mentions = Collections.unmodifiableSet(mentions);
	}

	/**
	 * Getter whether the {@link Token} to which this object is attached is the
	 * representative mention in the chain.
	 * 
	 * @return the representative state
	 */
	public boolean isRepresentative() {
		return this.isRepresentative;
	}

	/**
	 * Getter for the set of {@link Token}s representing mentions of the
	 * {@link Token} to which this object is attached.
	 * 
	 * @return
	 */
	public Set<Span> getMentions() {
		return this.mentions;
	}

	public int hashCode() {
		final int prime = 31;
        int result = 1;
        result = prime * result + (isRepresentative ? 1231 : 1237);
        result = prime * result + mentions.hashCode();
		
        return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
            return false;
		
        CorefFeature other = (CorefFeature) obj;
        
		return (isRepresentative == other.isRepresentative)
			&& (mentions.equals(other.mentions));
	}
}
