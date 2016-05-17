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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ORIGIN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;

import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A suggestion of an {@link Entity} for a fise:TextAnnotation processed by the NamedEntityTaggingEngine
 */
public class Suggestion implements Comparable<Suggestion> {

    private static final Logger log = LoggerFactory.getLogger(Suggestion.class);

    private static final LiteralFactory lf = LiteralFactory.getInstance();

    private static final IRI ENTITYHUB_SITE = new IRI(RdfResourceEnum.site.getUri());

    private IRI entityAnnotation;
    private IRI entityUri;
    private Double originalConfidnece;

    private Entity entity;
    private Double normalizedDisambiguationScore;
    private Double disambiguatedConfidence;
    private String site;

    private Suggestion(IRI entityAnnotation) {
        this.entityAnnotation = entityAnnotation;
    }

    public Suggestion(Entity entity) {
        this.entity = entity;
        this.entityUri = new IRI(entity.getId());
        this.site = entity.getSite();
    }

    /**
     * Allows to create Suggestions from existing fise:TextAnnotation contained in the metadata of the
     * processed {@link ContentItem}
     * 
     * @param graph
     * @param entityAnnotation
     * @return
     */
    public static Suggestion createFromEntityAnnotation(Graph graph, IRI entityAnnotation) {
        Suggestion suggestion = new Suggestion(entityAnnotation);
        suggestion.entityUri =
                EnhancementEngineHelper.getReference(graph, entityAnnotation, ENHANCER_ENTITY_REFERENCE);
        if (suggestion.entityUri == null) {
            // most likely not a fise:EntityAnnotation
            log.debug("Unable to create Suggestion for EntityAnnotation {} "
                    + "because property {} is not present", entityAnnotation, ENHANCER_ENTITY_REFERENCE);
            return null;
        }
        suggestion.originalConfidnece =
                EnhancementEngineHelper.get(graph, entityAnnotation, ENHANCER_CONFIDENCE, Double.class, lf);
        if (suggestion.originalConfidnece == null) {
            log.warn("EntityAnnotation {} does not define a value for "
                    + "property {}. Will use '0' as fallback", entityAnnotation, ENHANCER_CONFIDENCE);
            suggestion.originalConfidnece = 0.0;
        }
        suggestion.site = EnhancementEngineHelper.getString(graph, entityAnnotation, ENTITYHUB_SITE);
        if(suggestion.site == null){
            //STANBOL-1411: fall back to fise:orign
            suggestion.site = getOrigin(graph, entityAnnotation);
        }
        // NOTE: site might be NULL
        return suggestion;
    }

    /**
     * The URI of the fise:EntityAnnotation representing this suggestion in the
     * {@link ContentItem#getMetadata() metadata} of the processed {@link ContentItem}. This will be
     * <code>null</code> if this Suggestion was created as part of the Disambiguation process and was not
     * present in the metadata of the content item before the disambiguation.
     * 
     * @return the URI of the fise:EntityAnnotation or <code>null</code> if not present.
     */
    public IRI getEntityAnnotation() {
        return entityAnnotation;
    }

    /**
     * Allows to set the URI of the fise:EntityAnnotation. This is required if the original enhancement
     * structure shared one fise:EntityAnnotation instance for two fise:TextAnnotations (e.g. because both
     * TextAnnotations had the exact same value for fise:selected-text). After disambiguation it is necessary
     * to 'clone' fise:EntityAnnotations like that to give them different fise:confidence values. Because of
     * that it is supported to set the new URI of the cloned fise:EntityAnnotation.
     * 
     * @param uri
     *            the uri of the cloned fise:EntityAnnotation
     */
    public void setEntityAnnotation(IRI uri) {
        this.entityAnnotation = uri;
    }

    /**
     * The URI of the Entity (MUST NOT be <code>null</code>)
     * 
     * @return the URI
     */
    public IRI getEntityUri() {
        return entityUri;
    }

    /**
     * The original confidence of the fise:EntityAnnotation or <code>null</code> if not available.
     * 
     * @return
     */
    public Double getOriginalConfidnece() {
        return originalConfidnece;
    }

    /**
     * The {@link Entity} or <code>null</code> if not available. For Suggestions that are created based on
     * fise:EntityAnnotations the Entity is not available. Entities might be loaded as part of the
     * Disambiguation process.
     * 
     * @return the {@link Entity} or <code>null</code> if not available
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * The score of the disambiguation. This is just the score of the disambiguation that is not yet combined
     * with the {@link #getOriginalConfidnece()} to become the {@link #getDisambiguatedConfidence()}
     * 
     * @return the disambiguation score
     */
    public Double getNormalizedDisambiguationScore() {
        return normalizedDisambiguationScore;
    }

    /**
     * The confidence after disambiguation. Will be <code>null</code> at the beginning
     * 
     * @return the disambiguated confidence or <code>null</code> if not yet disambiguated
     */
    public Double getDisambiguatedConfidence() {
        return disambiguatedConfidence;
    }

    /**
     * The name of the Entityhub {@link Site} the suggested Entity is managed.
     * Both <code>entityhub:site</code> and <code>fise:orign</code> are 
     * considered as sites (see STANBOL-1411).
     * 
     * @return the name of the Entityhub {@link Site}
     */
    public String getSite() {
        return site;
    }

    /**
     * Setter for the normalized [0..1] score of the disambiguation
     * 
     * @param normalizedDisambiguationScore
     */
    public void setNormalizedDisambiguationScore(Double normalizedDisambiguationScore) {
        this.normalizedDisambiguationScore = normalizedDisambiguationScore;
    }

    /**
     * Setter for the confidence after disambiguation
     * 
     * @param disambiguatedConfidence
     */
    public void setDisambiguatedConfidence(Double disambiguatedConfidence) {
        this.disambiguatedConfidence = disambiguatedConfidence;
    }

    @Override
    public int hashCode() {
        return entityUri.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Suggestion && ((Suggestion) obj).entityUri.equals(entityUri);
    }

    /**
     * Compares based on the {@link #getDisambiguatedConfidence()} (if present) and falls back to the
     * {@link #getOriginalConfidnece()}. If the original confidence value is not present or both Suggestions
     * do have the same confidence the natural order of the Entities URI is used. This also ensures
     * <code>(x.compareTo(y)==0) == (x.equals(y))</code> and allows to use this class with {@link SortedMap}
     * and {@link SortedSet} implementations.
     * <p>
     */
    @Override
    public int compareTo(Suggestion other) {
        int result;
        if (disambiguatedConfidence != null && other.disambiguatedConfidence != null) {
            result = other.disambiguatedConfidence.compareTo(disambiguatedConfidence);
        } else if (other.originalConfidnece != null && originalConfidnece != null) {
            result = other.originalConfidnece.compareTo(originalConfidnece);
        } else {
            result = 0;
        }
        // ensure (x.compareTo(y)==0) == (x.equals(y))
        return result == 0 ? entityUri.getUnicodeString().compareTo(other.entityUri.getUnicodeString())
                : result;
    }
    
    private static String getOrigin(Graph graph, IRI entityAnnotation) {
        IRI uOrigin = EnhancementEngineHelper.getReference(graph, entityAnnotation, ENHANCER_ORIGIN);
        if (uOrigin != null) {
            return uOrigin.getUnicodeString();
        } else {
            String sOrigin = EnhancementEngineHelper.getString(graph, entityAnnotation, ENHANCER_ORIGIN);
            if (sOrigin != null) {
                return sOrigin;
            } else {
                Literal lOrigin = EnhancementEngineHelper.get(graph, entityAnnotation, ENHANCER_ORIGIN, Literal.class, lf);
                if (lOrigin != null) {
                    return lOrigin.getLexicalForm();
                } else {
                    return null;
                }
            }
        }
    }

}
