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
package org.apache.stanbol.enhancer.engines.entitycomention.impl;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.commons.collections.IteratorUtils;
import org.apache.stanbol.enhancer.engines.entitycomention.CoMentionConstants;
import org.apache.stanbol.enhancer.engines.entitycomention.EntityCoMentionEngine;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.EntityLinker;

/**
 * {@link Entity} implementation used by the {@link EntityCoMentionEngine}. It
 * overrides the {@link #getText(IRI)} and {@link #getReferences(IRI)}
 * methods to use the a different labelField if 
 * {@link CoMentionConstants#CO_MENTION_LABEL_FIELD} is parsed as parameter.
 * This allows the {@link EntityLinker} to use different properties for different
 * Entities when linking against the {@link InMemoryEntityIndex}.
 * @author Rupert Westenthaler
 *
 */
public class EntityMention extends Entity {
    /**
     * The label field of this Entity
     */
    private final IRI nameField;
    /**
     * The type field of this Entity
     */
    private final IRI typeField;
    /**
     * The start/end char indexes char index of the first mention
     */
    private final Integer[] span;
    
    private static int CO_MENTION_FIELD_HASH = CoMentionConstants.CO_MENTION_LABEL_FIELD.hashCode();
    private static int CO_MENTION_TYPE_HASH = CoMentionConstants.CO_MENTION_TYPE_FIELD.hashCode();

    /**
     * Creates a new MentionEntity for the parsed parameters
     * @param uri the {@link IRI} of the Entity 
     * @param data the {@link Graph} with the data for the Entity
     * @param labelField the {@link IRI} of the property holding the
     * labels of this Entity. This property will be used for all calls to
     * {@link #getText(IRI)} and {@link #getReferences(IRI)} if
     * {@link CoMentionConstants#CO_MENTION_LABEL_FIELD} is parsed as parameter
     * @param span the start/end char indexes of the mention
     */
    public EntityMention(IRI uri, Graph data, IRI labelField, IRI typeField, Integer[] span) {
        super(uri, data);
        if(labelField == null){
            throw new IllegalArgumentException("The LabelField MUST NOT be NULL!");
        }
        this.nameField = labelField;
        if(typeField == null){
            throw new IllegalArgumentException("The TypeFeild MUST NOT be NULL!");
        }
        this.typeField = typeField;
        if(span != null && (span.length != 2 || span[0] == null || span[1] == null)){
            throw new IllegalArgumentException("If a span is parsed the length of the Array MUST BE 2 " +
            		"AND start, end MUST NOT be NULL (parsed: "+span+")!");
        }
        this.span = span;
        
    }
    /**
     * Wrapps the parsed Entity and redirects calls to 
     * {@link CoMentionConstants#CO_MENTION_LABEL_FIELD} to the parsed labelField
     * @param entity the Entity to wrap
     * @param labelField the {@link IRI} of the property holding the
     * labels of this Entity. This property will be used for all calls to
     * {@link #getText(IRI)} and {@link #getReferences(IRI)} if
     * {@link CoMentionConstants#CO_MENTION_LABEL_FIELD} is parsed as parameter
     * @param index the char index of the initial mention in the document
     */
    public EntityMention(Entity entity, IRI labelField, IRI typeField, Integer[] span) {
        this(entity.getUri(), entity.getData(),labelField,typeField,span);
    }

    @Override
    public Iterator<Literal> getText(IRI field) {
        if(CO_MENTION_FIELD_HASH == field.hashCode() && //avoid calling equals
                CoMentionConstants.CO_MENTION_LABEL_FIELD.equals(field)){
            return super.getText(nameField);
        } else if(CO_MENTION_TYPE_HASH == field.hashCode() && //avoid calling equals
                CoMentionConstants.CO_MENTION_TYPE_FIELD.equals(field)){
            return super.getText(typeField);
        } else {
            return super.getText(field);
        }
    }
    
    @Override
    public Iterator<IRI> getReferences(IRI field) {
        if(CO_MENTION_FIELD_HASH == field.hashCode() && //avoid calling equals
                CoMentionConstants.CO_MENTION_LABEL_FIELD.equals(field)){
            return super.getReferences(nameField);
        } else if(CO_MENTION_TYPE_HASH == field.hashCode() && //avoid calling equals
                CoMentionConstants.CO_MENTION_TYPE_FIELD.equals(field)){
            return super.getReferences(typeField);
        } else {
            return super.getReferences(field);
        }
    }
    /**
     * Checks if this mention does have a span assigned. EntityMentions without
     * a span are considered to be valid from the begin of the document. Examples
     * could be manually tagged entities or entities extracted from the metadata
     * of an document.
     * @return if this entity has a span or not.
     */
    public boolean hasSpan(){
        return span != null;
    }
    /**
     * The start of the span selected by this mention or <code>null</code> if this
     * mention does not have a span assigned.
     * @return the start char position of the mention or <code>null</code> if none
     */
    public Integer getStart(){
        return span != null ? span[0] : null;
    }
    /**
     * The end of the span selected by this mention or <code>null</code> if this
     * mention does not have a span assigned.
     * @return the end char position of the mention or <code>null</code> if none
     */
    public Integer getEnd(){
        return span != null ? span[1] : null;
    }
    /**
     * The field used to obtain the names of the entities. For EntityMentions
     * this is set on a per instance base, as the field my differ between
     * different {@link EntityMention}s
     * @return the field (property) used to obtain the labels of this mention
     * @see EntityLinkerConfig#getNameField()
     */
    public IRI getNameField() {
        return nameField;
    }
    /**
     * The field used to obtain the types of entities. For EntityMentions
     * this is set on a per instance base, as the field my differ between
     * different {@link EntityMention}s
     * @return the field (property) used to obtain the type of this mention
     * @see EntityLinkerConfig#getTypeField()
     */
    public IRI getTypeField() {
        return typeField;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(EntityMention.class.getSimpleName());
        sb.append(' ').append(getId()).append(" [labels: ");
        sb.append(IteratorUtils.toList(getText(nameField)).toString());
        if(hasSpan()){
            sb.append(" | span:[").append(getStart()).append(',').append(getEnd()).append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
