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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NamedEntity {
    private static final Logger log = LoggerFactory.getLogger(NamedEntity.class);
    private final BlankNodeOrIRI entity;
    private final String name;
    private final IRI type;
    private NamedEntity(BlankNodeOrIRI entity, String name, IRI type) {
        this.entity = entity;
        this.name = name;
        this.type = type;
    }
    /**
     * Getter for the Node providing the information about that entity
     * @return the entity
     */
    public final BlankNodeOrIRI getEntity() {
        return entity;
    }
    /**
     * Getter for the name
     * @return the name
     */
    public final String getName() {
        return name;
    }
    /**
     * Getter for the type
     * @return the type
     */
    public final IRI getType() {
        return type;
    }
    @Override
    public int hashCode() {
        return entity.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof NamedEntity && entity.equals(((NamedEntity)o).entity);
    }
    @Override
    public String toString() {
        return String.format("NamedEntity %s (name=%s|type=%s)",entity,name,type);
    }
    /**
     * Extracts the information of an {@link NamedEntity} from an
     * {@link TechnicalClasses#ENHANCER_TEXTANNOTATION} instance.
     * @param graph the graph with the information
     * @param textAnnotation the text annotation instance
     * @return the {@link NamedEntity} or <code>null</code> if the parsed
     * text annotation is missing required information.
     */
    public static NamedEntity createFromTextAnnotation(Graph graph, BlankNodeOrIRI textAnnotation){
        String selected = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTED_TEXT);
        if (selected == null) {
            log.debug("Unable to create NamedEntity for TextAnnotation {} "
                    + "because property {} is not present",textAnnotation,ENHANCER_SELECTED_TEXT);
            return null;
        }
        String name = selected.trim();
        if(name.isEmpty()){
            log.debug("Unable to process TextAnnotation {} because its selects "
            		+ "an empty Stirng !",textAnnotation);
            return null;
        }
        // remove punctuation form the search string
        name = cleanupKeywords(name);
        if(name.isEmpty()){
            log.debug("Unable to process TextAnnotation {} because its selects "
                    + "an stirng with punktations only (selected: {})!",
                    textAnnotation, selected);
            return null;
        }
        IRI type = EnhancementEngineHelper.getReference(graph, textAnnotation, DC_TYPE);
        if (type == null) {
            log.warn("Unable to process TextAnnotation {} because property {}"
                     + " is not present!",textAnnotation, DC_TYPE);
            return null;
        }
        return new NamedEntity(textAnnotation,name,type);
    }        
    /**
     * Removes punctuation form a parsed string
     */
    private static String cleanupKeywords(String keywords) {
        return keywords.replaceAll("\\p{P}", " ").trim();
    }
}