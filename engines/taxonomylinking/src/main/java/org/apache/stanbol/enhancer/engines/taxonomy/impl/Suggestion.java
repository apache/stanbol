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
package org.apache.stanbol.enhancer.engines.taxonomy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;

/**
 * Holds information about suggestions created by the {@link TaxonomyLinkingEngine}.
 * used to perform local lookups for queries that would be normally executed
 * on the {@link ReferencedSite}
 * @author Rupert Westenthaler
 *
 */
class Suggestion implements Comparable<Suggestion>{
    
    private final UriRef textAnnotation;
    
    private final Set<UriRef> textAnnotationTypes;
    
    private final Set<UriRef> linkedTextAnnotations;
    private final Set<UriRef> unmodLinked;
    
    private final String searchString;

    private final List<Representation> suggestions;
    
    public Suggestion(String searchString,UriRef textAnnotation,List<Representation> suggestions,Set<UriRef> textAnnotationTypes){
        if(searchString == null || searchString.isEmpty()){
            throw new IllegalArgumentException("The search string MUST NOT be NULL nor emtpy");
        }
        if(textAnnotation == null){
            throw new IllegalArgumentException("The parsed UriRef of the textAnnotation MUST NOT be NULL nor empty");
        }
        if(suggestions == null || suggestions.isEmpty()){
            throw new IllegalArgumentException("The parsed list of suggestions MUST NOT be NULL nor empty");
        }
        if(suggestions.contains(null)){
            //test for NULL element, because this will cause NPE later on that would
            //be hard to debug!
            throw new IllegalArgumentException("The parsed list of suggestions MUST NOT contain the NULL element");
        }
        this.searchString = searchString;
        this.textAnnotation = textAnnotation;
        this.suggestions = Collections.unmodifiableList(new ArrayList<Representation>(suggestions));
        this.linkedTextAnnotations = new HashSet<UriRef>();
        this.unmodLinked = Collections.unmodifiableSet(linkedTextAnnotations);
        if(textAnnotationTypes == null) {
            this.textAnnotationTypes = Collections.emptySet();
        } else {
            this.textAnnotationTypes = Collections.unmodifiableSet(new HashSet<UriRef>(textAnnotationTypes));
        }
    }
    
    public final UriRef getTextAnnotation() {
        return textAnnotation;
    }

    /**
     * Returns an unmodifiable set containing all the other Text annotations
     * for the same {@link #getSearchString() search string}.
     * @return the linked text annotations (read only)
     */
    public final Set<UriRef> getLinkedTextAnnotations() {
        return unmodLinked;
    }
    
    public final boolean addLinked(UriRef textAnnotation){
        if(textAnnotation != null){
            return linkedTextAnnotations.add(textAnnotation);
        } else {
            return false;
        }
    }
    public final boolean removeLinked(UriRef textAnnotation){
        return linkedTextAnnotations.remove(textAnnotation);
    }
    /**
     * Getter for the search string used to calculate the suggestions
     * @return the search string
     */
    public final String getSearchString() {
        return searchString;
    }

    /**
     * Getter for the Representations suggested for the 
     * {@link #getSearchString() search string}
     * @return the suggestions (read only)
     */
    public final List<Representation> getSuggestions() {
        return suggestions;
    }
    
    @Override
    public int hashCode() {
        return searchString.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof Suggestion && ((Suggestion)o).searchString.equals(searchString);
    }
    
    @Override
    public int compareTo(Suggestion o) {
        return searchString.compareTo(o.searchString);
    }

    /**
     * Getter for the values of the {@link Properties#DC_TYPE dc:type}  property of the
     * TextAnnotation. This types need to be used
     * for additional TextAnnotations linked to the one returned by
     * {@link #getTextAnnotation()}
     * @return the @link Properties#DC_TYPE dc:type} values of the
     * {@link #getTextAnnotation() TextAnnotation}.
     */
    public Set<UriRef> getTextAnnotationTypes() {
        return textAnnotationTypes;
    }
    @Override
    public String toString() {
        List<String> suggestedIds = new ArrayList<String>(suggestions.size());
        for(Representation rep : suggestions){
            suggestedIds.add(rep == null ? null : rep.getId());
        }
        return String.format("Suggestion: %s -> %s",
            searchString,suggestedIds);
    }
}
