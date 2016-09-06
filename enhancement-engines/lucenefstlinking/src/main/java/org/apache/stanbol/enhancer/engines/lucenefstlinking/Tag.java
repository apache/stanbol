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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;

/**
 * minimal helper class to represent a Tag.<p>
 * TODO: This will need to collect additional values from the suggested
 * SolrDocuments:<ul>
 * <li> the type information - {@link EntityLinkerConfig#TYPE_FIELD} values
 * <li>
 * <li>
 * <li>
 * </ul>
 * @author Rupert Westenthaler
 *
 */
class Tag {
    
    /**
     * the start index within the {@link AnalysedText}
     */
    final int[] span;
    /**
     * Matching documents
     */
    private Set<Match> ids;
    
    private List<Match> suggestions;
    private String anchor;

    Tag(int start, int end) {
        span = new int[]{start,end};
    }
    Tag(int[] span) {
        this.span = span;
    }
    
    public void addIds(Set<Match> ids){
        if(this.ids == null){
            this.ids = ids;
        } else {
            this.ids.addAll(ids);
        }
    }
    @SuppressWarnings("unchecked")
    public Set<Match> getMatches(){
        return ids == null ? Collections.EMPTY_SET : ids;
    }
    public int getStart() {
        return span[0];
    }
    
    public int getEnd() {
        return span[1];
    }
    /**
     * Setter for the Anchor text 
     * @param anchor
     */
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }
    /**
     * Getter for the Anchor text
     * @return the fise:selected-text value
     */
    public String getAnchor() {
        return anchor;
    }
    
    public void setSuggestions(List<Match> suggestions) {
        this.suggestions = suggestions;
    }
    
    public List<Match> getSuggestions() {
        return suggestions;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(span);
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof Tag && Arrays.equals(span, ((Tag)o).span);
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Tag").append(Arrays.toString(span)).toString();
    }
    
    static final Comparator<int[]> SPAN_COMPARATOR = new Comparator<int[]>() {

        @Override
        public int compare(int[] a, int[] b) {
            int c = a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0;
            if(c == 0){
                c = a[1] > b[1] ? -1 : a[1] < b[1] ? 1 : 0;
            }
            return c;
        }
        
    };

    /**
     * Returns the score of the best {@link #getSuggestions() suggestion}
     * @return
     */
    public double getScore() {
        return suggestions == null || suggestions.isEmpty() ? 0 : 
            suggestions.get(0).getScore();
    }
    
}
