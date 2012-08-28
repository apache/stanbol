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
package org.apache.stanbol.enhancer.engine.disambiguation.mlt;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SavedEntity {
    private static final Logger log = LoggerFactory.getLogger(SavedEntity.class);
    /**
     * The {@link LiteralFactory} used to create typed RDF literals
     */
    private final static LiteralFactory literalFactory = LiteralFactory.getInstance();
    private NonLiteral entity;
    private String name;
    private UriRef type;
    private String URI;
    private String context;
    private Integer start;
    private Integer end;
    
    private SavedEntity() {
        //use createFromTextAnnotation to create an instance
    }
    /**
     * Getter for the Node providing the information about that entity
     * @return the entity
     */
    public final NonLiteral getEntity() {
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
    public final UriRef getType() {
        return type;
    }
    @Override
    public int hashCode() {
        return entity.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof SavedEntity && entity.equals(((SavedEntity)o).entity);
    }
    @Override
    public String toString() {
        return String.format("SavedEntity %s (name=%s|type=%s)",entity,name,type);
    }
    /**
     * Extracts the information of an {@link SavedEntity} from an
     * {@link TechnicalClasses#ENHANCER_TEXTANNOTATION} instance.
     * @param graph the graph with the information
     * @param textAnnotation the text annotation instance
     * @return the {@link SavedEntity} or <code>null</code> if the parsed
     * text annotation is missing required information.
     */
    public static SavedEntity createFromTextAnnotation(TripleCollection graph, NonLiteral textAnnotation){
        SavedEntity entity = new SavedEntity();
        String name = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTED_TEXT);
        if (name == null) {
            log.debug("Unable to create SavedEntity for TextAnnotation {} "
                    + "because property {} is not present",textAnnotation,ENHANCER_SELECTED_TEXT);
            return null;
        }
        // remove punctuation form the search string
        entity.name = cleanupKeywords(name);
        if(entity.name.isEmpty()){
            log.debug("Unable to process TextAnnotation {} because its selects "
            		+ "an empty Stirng !",textAnnotation);
            return null;
        }
        entity.type = EnhancementEngineHelper.getReference(graph, textAnnotation, DC_TYPE);
        //NOTE rwesten: TextAnnotations without dc:type should be still OK
//        if (type == null) {
//            log.warn("Unable to process TextAnnotation {} because property {}"
//                     + " is not present!",textAnnotation, DC_TYPE);
//            return null;
//        }
        entity.context = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTION_CONTEXT);
        Integer start = EnhancementEngineHelper.get(graph, textAnnotation, ENHANCER_START,Integer.class,literalFactory);
        Integer end = EnhancementEngineHelper.get(graph, textAnnotation, ENHANCER_END,Integer.class,literalFactory);
        if(start == null || end ==null){
            log.debug("Unable to process TextAnnotation {} because the start and/or the end "
                + "position is not defined (selectedText: {}, start: {}, end: {})",
                new Object[]{textAnnotation, name, start, end});
            
        }
        return entity;
    }        
    /**
     * Removes punctuation form a parsed string
     */
    private static String cleanupKeywords(String keywords) {
        return keywords.replaceAll("\\p{P}", " ").trim();
    }
    
    public String getURI()
    {return this.URI;}

    public String getContext()
    {return this.context;}
    
    public int getStart()
    {return this.start;}

    public int getEnd()
    {return this.end;}
  
}
