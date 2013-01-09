/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.paoding.analysis.knife;


/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 */
public class LetterKnife extends CombinatoricsKnife {

	public static final String[] DEFAULT_NOISE = { "a", "an", "and", "are", "as", "at",
			"be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
			"not", "of", "on", "or", "such", "that", "the", "their", "then",
			"there", "these", "they", "this", "to", "was", "will", "with",
			"www" };

	
	public LetterKnife() {
		super(DEFAULT_NOISE);
	}

	public LetterKnife(String[] noiseWords) {
		super(noiseWords);
	}

	public int assignable(Beef beef, int offset, int index) {
		char ch = beef.charAt(index);
		if (CharSet.isLantingLetter(ch)) {
			return ASSIGNED;
		}
		if (index > offset) {
			if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '_') {
				return POINT;
			}
		}
		return LIMIT;
	}
	

}
