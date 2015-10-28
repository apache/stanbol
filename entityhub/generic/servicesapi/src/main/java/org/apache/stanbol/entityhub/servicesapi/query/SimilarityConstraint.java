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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;

/**
 * Ensure that results have fields that is contextually similar. The implementation is typically based on a
 * cosine similarity score a normalized vector space of term frequencies - inverse document frequencies as
 * done by the MoreLikeThis feature of Solr for instance.
 * <p>
 * This type of constraint might not be supported by all the yard implementations. If it is not supported it
 * is just ignored.
 * <p>
 * With version <code>0.12.0</code> support for {@link Text} and {@link Reference}
 * contexts where added. The {@link #getContextType()} can be used to determine the
 * type of the parsed context and the {@link #getStringContext()}, 
 * {@link #getTextContext()} and {@link #getReferenceContext()} methods can be
 * used to get the typed context versions. The {@link #getContext()}
 */
public class SimilarityConstraint extends Constraint {

    protected final String context;

    protected final DataTypeEnum contextType;
    
    protected final List<String> additionalFields;

    private final Collection<String> languages;

    /**
     * Constructs a Similarity Constraint with a given context. The value is
     * interpreted as {@link DataTypeEnum#Text} with unknown language.
     * @param context the context
     * @deprecated use one of the constructor explicitly parsing the
     * {@link DataTypeEnum} or the languages (assuming {@link DataTypeEnum#Text}
     */
    public SimilarityConstraint(String context) {
        this(context, DataTypeEnum.Text, null,null);
    }
    /**
     * Constructs a Similarity Constraint with a given context. The value is
     * interpreted as {@link DataTypeEnum#Text} with unknown language.
     * @param context the context
     * @param additionalFields additional fields to include in the similarity search
     * @deprecated use one of the constructor explicitly parsing the
     * {@link DataTypeEnum} or the languages (assuming {@link DataTypeEnum#Text}
     */
    public SimilarityConstraint(String context,List<String> additionalFields) {
        this(context, DataTypeEnum.Text, null,null);
    }
    public SimilarityConstraint(Collection<String> context, Collection<String> languages) {
        this(getCollectionContext(context), DataTypeEnum.Text, languages, null);
    }
    public SimilarityConstraint(Collection<String> context,DataTypeEnum contextType) {
        this(getCollectionContext(context), contextType, null, null);
    }
    public SimilarityConstraint(Collection<String> context, Collection<String> languages,List<String> additionalFields) {
        this(getCollectionContext(context), DataTypeEnum.Text, languages, additionalFields);
    }
    public SimilarityConstraint(Collection<String> context,DataTypeEnum contextType,List<String> additionalFields) {
        this(getCollectionContext(context), contextType, null, additionalFields);
    }

    private SimilarityConstraint(String context, DataTypeEnum contextType, 
            Collection<String> languages, List<String> additionalFields){
        super(ConstraintType.similarity);
        if(context == null){
            throw new IllegalArgumentException("The parsed Context MUST NOT be NULL nor empty");
        }
        this.context = context;
        this.contextType = contextType;
        this.languages = languages;
        if(additionalFields == null || additionalFields.isEmpty()){
            this.additionalFields = Collections.emptyList();
        } else {
            List<String> fields = new ArrayList<String>(additionalFields.size());
            for(String field : additionalFields){
                if(field != null && !field.isEmpty()){
                    fields.add(field);
                }
            }
            this.additionalFields = Collections.unmodifiableList(fields);
        }
    }
    
    /**
     * Additional fields used for similarity calculations
     * @return
     */
    public List<String> getAdditionalFields() {
        return additionalFields;
    }
    /**
     * The languages for the Context, or <code>null</code> if none are defined
     * @return the languages or <code>null</code> if none. NOTE that the 
     * returned collection may contain the <code>null</code> value
     * as it represents the default language
     */
    public Collection<String> getLanguages() {
        return languages;
    }
    /**
     * Getter for the context
     * @return the string representation of the context
     */
    public String getContext() {
        return context;
    }
 
    /**
     * The type of the Context. Can be {@link DataTypeEnum#String String},
     * {@link DataTypeEnum#Text Text} and {@link DataTypeEnum#Reference Reference}
     * @return the type of the context.
     */
    public DataTypeEnum getContextType() {
        return contextType;
    }
    
    private static String getCollectionContext(Collection<String> context) {
        if(context == null || context.isEmpty()){
            return null;
        }
        StringBuilder refContext = new StringBuilder();
        boolean first = true;
        for(String string : context){
            if(!first){
                refContext.append(' ');
            } else {
                first = false;
            }
            refContext.append(string);
        }
        return refContext.toString();
    }
    
    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[context: ")
                .append(context.length() > 20 ? (context.substring(0,18)+"..") : context)
                .append(" | contextType: ").append(contextType).append(" | languages: ")
                .append(languages).append(']').toString();
    }
}
