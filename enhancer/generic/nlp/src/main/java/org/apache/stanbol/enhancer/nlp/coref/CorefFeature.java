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

	public CorefFeature() {
		this(false, Collections.unmodifiableSet(Collections
				.<Span> emptySet()));
	}

	public CorefFeature(boolean isRepresentative) {
		this(isRepresentative, Collections.unmodifiableSet(Collections
				.<Span> emptySet()));
	}

	public CorefFeature(boolean isRepresentative, Set<Span> mentions) {
		this.isRepresentative = isRepresentative;
		this.mentions = mentions;
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
		return (this.mentions != null) ? this.mentions.hashCode() : 0;
	}

	public boolean equals(Object obj) {
		return (obj instanceof CorefFeature)
				&& (this.mentions.equals(((CorefFeature) obj).getMentions()));
	}
}
