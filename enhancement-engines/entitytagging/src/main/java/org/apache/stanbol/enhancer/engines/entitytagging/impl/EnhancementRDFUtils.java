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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Utility taken form the engine.autotagging bundle and adapted from using TagInfo to {@link Entity}.
 * 
 * @author Rupert Westenthaler
 * @author ogrisel (original utility)
 */
public final class EnhancementRDFUtils {

    /**
     * Restrict instantiation
     */
    private EnhancementRDFUtils() {}

    /**
     * @param literalFactory
     *            the LiteralFactory to use
     * @param graph
     *            the Graph to use
     * @param contentItemId
     *            the contentItemId the enhancement is extracted from
     * @param relatedEnhancements
     *            enhancements this textAnnotation is related to
     * @param suggestion
     *            the entity suggestion
     * @param nameField the field used to extract the name
     * @param lang the preferred language to include or <code>null</code> if none
     */
    public static IRI writeEntityAnnotation(EnhancementEngine engine,
                                               LiteralFactory literalFactory,
                                               Graph graph,
                                               IRI contentItemId,
                                               Collection<BlankNodeOrIRI> relatedEnhancements,
                                               Suggestion suggestion,
                                               String nameField, 
                                               String lang) {
        Representation rep = suggestion.getEntity().getRepresentation();
        // 1. extract the "best label"
        //Start with the matched one
        Text label = suggestion.getMatchedLabel();
        //if the matched label is not in the requested language
        boolean langMatch = (lang == null && label.getLanguage() == null) ||
                (label.getLanguage() != null && label.getLanguage().startsWith(lang));
            //search if a better label is available for this Entity
        if(!langMatch){
            Iterator<Text> labels = rep.getText(nameField);
            while (labels.hasNext() && !langMatch) {
                Text actLabel = labels.next();
                langMatch = (lang == null && actLabel.getLanguage() == null) ||
                        (actLabel.getLanguage() != null && actLabel.getLanguage().startsWith(lang));
                if(langMatch){ //if the language matches ->
                    //override the matched label
                    label = actLabel;
                }
            }
        } //else the matched label will be the best to use
        Literal literal;
        if (label.getLanguage() == null) {
            literal = new PlainLiteralImpl(label.getText());
        } else {
            literal = new PlainLiteralImpl(label.getText(), new Language(label.getLanguage()));
        }
        // Now create the entityAnnotation
        IRI entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(graph, engine,
            contentItemId);
        // first relate this entity annotation to the text annotation(s)
        for (BlankNodeOrIRI enhancement : relatedEnhancements) {
            graph.add(new TripleImpl(entityAnnotation, DC_RELATION, enhancement));
        }
        IRI entityUri = new IRI(rep.getId());
        // add the link to the referred entity
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityUri));
        // add the label parsed above
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, literal));
        if (suggestion.getScore() != null) {
            graph.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, literalFactory
                .createTypedLiteral(suggestion.getScore())));
        }

        Iterator<Reference> types = rep.getReferences(RDF_TYPE.getUnicodeString());
        while (types.hasNext()) {
            graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, new IRI(types.next()
                    .getReference())));
        }
        //add the name of the ReferencedSite that manages the Entity
        if(suggestion.getEntity().getSite() != null){
            graph.add(new TripleImpl(entityAnnotation, 
                new IRI(RdfResourceEnum.site.getUri()), 
                new PlainLiteralImpl(suggestion.getEntity().getSite())));
        }
        
        return entityAnnotation;
    }

}
