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
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ORIGIN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SavedEntity {
    private static final Logger log = LoggerFactory.getLogger(SavedEntity.class);

    /**
     * The {@link LiteralFactory} used to create typed RDF literals
     */
    private final static LiteralFactory literalFactory = LiteralFactory.getInstance();
    private String name;
    private IRI type;
    private IRI uri;
    private String context;
    private Integer start;
    private Integer end;

    /**
     * Map with the suggestion. The key is the URI of the fise:EntityAnnotation and the value is the Triple
     * with the confidence value
     */
    private Map<IRI,Suggestion> suggestions = new LinkedHashMap<IRI,Suggestion>();

    /**
     * The name of the Entityhub {@link Site} managing the suggestions of this fise:TextAnnotation
     */
    private String site;

    /**
     * private constructor only used by {@link #createFromTextAnnotation(Graph, BlankNodeOrIRI)}
     */
    private SavedEntity() {}

    /**
     * creates a SavedEntity instance for the parsed fise:TextAnnotation
     * 
     * @param graph
     *            the graph with the information
     * @param textAnnotation
     *            the fise:TextAnnotation
     * @return the {@link SavedEntity} or <code>null</code> if the parsed text annotation is missing required
     *         information.
     */
    public static SavedEntity createFromTextAnnotation(Graph graph, IRI textAnnotation) {
        SavedEntity entity = new SavedEntity();
        entity.uri = textAnnotation;
        entity.name = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTED_TEXT);
        if (entity.name == null) {
            log.debug("Unable to create SavedEntity for TextAnnotation {} "
                    + "because property {} is not present", textAnnotation, ENHANCER_SELECTED_TEXT);
            return null;
        }
        // NOTE rwesten: I think one should not change the selected text
        // remove punctuation form the search string
        // entity.name = cleanupKeywords(name);
        if (entity.name.isEmpty()) {
            log.debug("Unable to process TextAnnotation {} because its selects " + "an empty Stirng !",
                textAnnotation);
            return null;
        }
        entity.type = EnhancementEngineHelper.getReference(graph, textAnnotation, DC_TYPE);
        // NOTE rwesten: TextAnnotations without dc:type should be still OK
        // if (type == null) {
        // log.warn("Unable to process TextAnnotation {} because property {}"
        // + " is not present!",textAnnotation, DC_TYPE);
        // return null;
        // }
        entity.context = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTION_CONTEXT);
        Integer start =
                EnhancementEngineHelper.get(graph, textAnnotation, ENHANCER_START, Integer.class,
                    literalFactory);
        Integer end =
                EnhancementEngineHelper.get(graph, textAnnotation, ENHANCER_END, Integer.class,
                    literalFactory);
        if (start == null || end == null) {
            log.debug("Unable to process TextAnnotation {} because the start and/or the end "
                    + "position is not defined (selectedText: {}, start: {}, end: {})", new Object[] {
                    textAnnotation, entity.name, start, end});

        }
        entity.start = start;
        entity.end = end;

        // parse the suggestions

        // all the entityhubSites that manage a suggested Entity
        // (hopefully only a single one)
        Set<String> entityhubSites = new HashSet<String>();
        List<Suggestion> suggestionList = new ArrayList<Suggestion>();
        Iterator<Triple> suggestions = graph.filter(null, Properties.DC_RELATION, textAnnotation);
        // NOTE: this iterator will also include dc:relation between fise:TextAnnotation's
        // but in those cases NULL will be returned as suggestion
        while (suggestions.hasNext()) {
            IRI entityAnnotation = (IRI) suggestions.next().getSubject();
            Suggestion suggestion = Suggestion.createFromEntityAnnotation(graph, entityAnnotation);
            if (suggestion != null) {
                suggestionList.add(suggestion);
                if (suggestion.getSite() != null) {
                    entityhubSites.add(suggestion.getSite());
                }
            }
        }
        if (suggestionList.isEmpty()) {
            log.warn("TextAnnotation {} (selectedText: {}, start: {}) has no" + "suggestions.", new Object[] {
                    entity.uri, entity.name, entity.start});
            return null; // nothing to disambiguate
        } else {
            Collections.sort(suggestionList); // sort them based on confidence
            // the LinkedHashMap will keep the order (based on the original
            // confidence)
            for (Suggestion suggestion : suggestionList) {
                entity.suggestions.put(suggestion.getEntityUri(), suggestion);
            }
        }
        if (entityhubSites.isEmpty()) {
            log.debug("TextAnnotation {} (selectedText: {}, start: {}) has "
                    + "suggestions do not have 'entityhub:site' information. "
                    + "Can not disambiguate because origin is unknown.", new Object[] {entity.uri,
                    entity.name, entity.start});
            return null; // Ignore TextAnnotatiosn with suggestions of unknown origin.
        } else if (entityhubSites.size() > 1) {
            log.warn("TextAnnotation {} (selectedText: {}, start: {}) has "
                    + "suggestions originating from multiple Entityhub Sites {}", new Object[] {entity.uri,
                    entity.name, entity.start, entityhubSites});
            return null; // TODO: Ignore those for now
        } else {
            entity.site = entityhubSites.iterator().next();
        }
        return entity;
    }

    /**
     * Removes punctuation form a parsed string
     */
    private static String cleanupKeywords(String keywords) {
        return keywords.replaceAll("\\p{P}", " ").trim();
    }

    /**
     * Getter for the name
     * 
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Getter for the type
     * 
     * @return the type
     */
    public final IRI getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SavedEntity && uri.equals(((SavedEntity) o).uri);
    }

    @Override
    public String toString() {
        return String.format("SavedEntity %s (name=%s | type=%s)", uri, name, type);
    }

    public IRI getUri() {
        return this.uri;
    }

    public String getContext() {
        return this.context;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    public Collection<Suggestion> getSuggestions() {
        return suggestions.values();
    }

    public Suggestion getSuggestion(IRI uri) {
        return suggestions.get(uri);
    }

    /**
     * The name of the Entityhub {@link Site} managing the suggestions
     * 
     * @return
     */
    public String getSite() {
        return site;
    }
}
