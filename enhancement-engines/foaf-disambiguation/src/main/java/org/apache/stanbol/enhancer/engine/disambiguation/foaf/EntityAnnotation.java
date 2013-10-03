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
package org.apache.stanbol.enhancer.engine.disambiguation.foaf;

import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;

import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
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
 * An abstraction of an EntityAnnotation
 * 
 * @author Dileepa Jayakody
 */
public class EntityAnnotation implements Comparable<EntityAnnotation> {

	private static final Logger log = LoggerFactory
			.getLogger(EntityAnnotation.class);

	/**
	 * foaf:name disambiguation ratio (2.0)
	 */
	public static final double FOAFNAME_DISAMBIGUATION_RATIO = 2.0;
	/**
	 * URI Reference correlation disambiguation ratio (2.0)
	 */
	public static final double URI_CORRELATION_DISAMBIGUATION_RATIO = 2.0;
	/**
	 * Default ratio for the original fise:confidence of suggested entities
	 */
	public static final double ORIGINAL_CONFIDNECE_RATIO = 1.0;

	/**
	 * The weight for foaf:name disambiguation scores
	 */
	private double foafNameDisambiguationWeight = FOAFNAME_DISAMBIGUATION_RATIO
			/ (FOAFNAME_DISAMBIGUATION_RATIO + ORIGINAL_CONFIDNECE_RATIO + URI_CORRELATION_DISAMBIGUATION_RATIO);
	/**
	 * The weight for uri-correlation disambiguation scores
	 */
	private double uriCorrelationDisambiguationWeight = URI_CORRELATION_DISAMBIGUATION_RATIO
			/ (FOAFNAME_DISAMBIGUATION_RATIO + ORIGINAL_CONFIDNECE_RATIO + URI_CORRELATION_DISAMBIGUATION_RATIO);
	/**
	 * The weight for the original confidence scores
	 * 
	 */
	private double confidenceWeight = ORIGINAL_CONFIDNECE_RATIO
			/ (FOAFNAME_DISAMBIGUATION_RATIO + ORIGINAL_CONFIDNECE_RATIO + URI_CORRELATION_DISAMBIGUATION_RATIO);

	private static final LiteralFactory lf = LiteralFactory.getInstance();

	private static final UriRef ENTITYHUB_SITE = new UriRef(
			RdfResourceEnum.site.getUri());

	private UriRef uriLink;
	private UriRef entityUri;
	private Entity entity;

	private Double originalConfidence = 0.0;
	// private Double entityReferenceDisambiguationScore = 0.0;
	private Double foafNameDisambiguationScore = 0.0;
	private Double disambiguatedConfidence = 0.0;
	private Double entityReferenceDisambiguatedConfidence = 0.0;
	private Double foafNameDisambiguatedConfidence = 0.0;
	// the score assigned based on the number of uri correlations with other
	// entities
	private int correlationScore;
	// uri-references from this entity
	private int referencesFromEntity;
	private String site;
	private String entityType;
	private String entityLabel;

	private EntityAnnotation(UriRef entityAnnotation) {
		this.uriLink = entityAnnotation;
	}

	public EntityAnnotation(Entity entity) {
		this.entity = entity;
		this.entityUri = new UriRef(entity.getId());
		this.site = entity.getSite();
	}

	/**
	 * Allows to create EntityAnnotations from existing fise:TextAnnotation
	 * contained in the metadata of the processed {@link ContentItem}
	 * 
	 * @param graph
	 * @param uri
	 * @return EntityAnnotation
	 */
	public static EntityAnnotation createFromUri(TripleCollection graph,
			UriRef uri) {
		EntityAnnotation entityAnnotation = new EntityAnnotation(uri);
		entityAnnotation.entityUri = EnhancementEngineHelper.getReference(
				graph, uri, Properties.ENHANCER_ENTITY_REFERENCE);
		if (entityAnnotation.entityUri == null) {
			// most likely not a fise:EntityAnnotation
			log.debug("Unable to create Suggestion for EntityAnnotation {} "
					+ "because property {} is not present", uri,
					Properties.ENHANCER_ENTITY_REFERENCE);
			return null;
		}
		entityAnnotation.originalConfidence = EnhancementEngineHelper.get(
				graph, uri, Properties.ENHANCER_CONFIDENCE, Double.class, lf);
		if (entityAnnotation.originalConfidence == null) {
			log.warn("EntityAnnotation {} does not define a value for "
					+ "property {}. Will use '0' as fallback", uri,
					Properties.ENHANCER_CONFIDENCE);
			entityAnnotation.originalConfidence = 0.0;
		}
		entityAnnotation.site = EnhancementEngineHelper.getString(graph, uri,
				ENTITYHUB_SITE);
		entityAnnotation.entityType = EnhancementEngineHelper.getString(graph,
				uri, Properties.ENHANCER_ENTITY_TYPE);
		entityAnnotation.entityLabel = EnhancementEngineHelper.getString(graph,
				uri, Properties.ENHANCER_ENTITY_LABEL);
		return entityAnnotation;
	}

	public void calculateDisambiguatedConfidence() {
		this.disambiguatedConfidence = (originalConfidence * confidenceWeight)
				+ this.foafNameDisambiguatedConfidence
				+ this.entityReferenceDisambiguatedConfidence;
	}

	public void calculateFoafNameDisambiguatedConfidence() {
		this.foafNameDisambiguatedConfidence = (foafNameDisambiguationScore * foafNameDisambiguationWeight);
	}

	/**
	 * Calculates the disambiguation score obtained for entity's URIReference
	 * correlations. The score is normalized between [0..1]
	 * 
	 * @param maximum
	 *            correlation score of entities int max
	 * @param minimum
	 *            correlation score of entities int min
	 */
	public void calculateEntityReferenceDisambiguatedConfidence(int max, int min) {
		if ((max - min) > 0) {
			double normalizedCorrelationScore = (correlationScore - min)
					/ (max - min);
			this.entityReferenceDisambiguatedConfidence = (normalizedCorrelationScore * uriCorrelationDisambiguationWeight);
		}
	}

	/**
	 * The URI of the fise:EntityAnnotation representing this suggestion in the
	 * {@link ContentItem#getMetadata() metadata} of the processed
	 * {@link ContentItem}. This will be <code>null</code>
	 * 
	 * @return the URI of the fise:EntityAnnotation or <code>null</code> if not
	 *         present.
	 */
	public UriRef getUriLink() {
		return uriLink;
	}

	/**
	 * Allows to set the URI of the fise:EntityAnnotation. This is required if
	 * the original enhancement structure shared one fise:EntityAnnotation
	 * instance for two fise:TextAnnotations (e.g. because both TextAnnotations
	 * had the exact same value for fise:selected-text). After disambiguation it
	 * is necessary to 'clone' fise:EntityAnnotations like that to give them
	 * different fise:confidence values. Because of that it is supported to set
	 * the new URI of the cloned fise:EntityAnnotation.
	 * 
	 * @param uri
	 *            the uri of the cloned fise:EntityAnnotation
	 */
	public void setEntityAnnotation(UriRef uri) {
		this.uriLink = uri;
	}

	/**
	 * The URI of the Entity (MUST NOT be <code>null</code>)
	 * 
	 * @return the URI
	 */
	public UriRef getEntityUri() {
		return entityUri;
	}

	/**
	 * The original confidence of the fise:EntityAnnotation or <code>null</code>
	 * if not available.
	 * 
	 * @return
	 */
	public Double getOriginalConfidnece() {
		return originalConfidence;
	}

	/**
	 * The {@link Entity} or <code>null</code> if not available. For Suggestions
	 * that are created based on fise:EntityAnnotations the Entity is not
	 * available. Entities might be loaded as part of the Disambiguation
	 * process.
	 * 
	 * @return the {@link Entity} or <code>null</code> if not available
	 */
	public Entity getEntity() {
		return entity;
	}

	/**
	 * The confidence after disambiguation. Will be <code>null</code> at the
	 * beginning
	 * 
	 * @return the disambiguated confidence or <code>null</code> if not yet
	 *         disambiguated
	 */
	public Double getDisambiguatedConfidence() {
		return disambiguatedConfidence;
	}

	/**
	 * The name of the Entityhub {@link Site} the suggested Entity is managed.
	 * 
	 * @return the name of the Entityhub {@link Site}
	 */
	public String getSite() {
		return site;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityLabel(String entityLabel) {
		this.entityLabel = entityLabel;
	}

	public String getEntityLabel() {
		return entityLabel;
	}

	public void setCorrelationScore(int correlationScore) {
		this.correlationScore = correlationScore;
	}

	/**
	 * Setter for the confidence after disambiguation
	 * 
	 * @param disambiguatedConfidence
	 */
	public void setDisambiguatedConfidence(Double disambiguatedConfidence) {
		this.disambiguatedConfidence = disambiguatedConfidence;
	}

	public void increaseCorrelationScore(int corefEntities) {
		this.correlationScore += corefEntities;
	}

	public int getCorrelationScore() {
		return correlationScore;
	}

	public void setReferencesFromEntity(int linksFromEntity) {
		this.referencesFromEntity = linksFromEntity;
	}

	public int getReferencesFromEntity() {
		return referencesFromEntity;
	}

	public void setFoafNameDisambiguationScore(
			Double foafNameDisambiguationScore) {
		this.foafNameDisambiguationScore = foafNameDisambiguationScore;
	}

	public Double getFoafNameDisambiguationScore() {
		return foafNameDisambiguationScore;
	}

	public void setEntityReferenceDisambiguatedConfidence(
			Double entityReferenceDisambiguatedConfidence) {
		this.entityReferenceDisambiguatedConfidence = entityReferenceDisambiguatedConfidence;
	}

	public Double getEntityReferenceDisambiguatedConfidence() {
		return entityReferenceDisambiguatedConfidence;
	}

	public void setFoafNameDisambiguatedConfidence(
			Double foafNameDisambiguatedConfidence) {
		this.foafNameDisambiguatedConfidence = foafNameDisambiguatedConfidence;
	}

	public Double getFoafNameDisambiguatedConfidence() {
		return foafNameDisambiguatedConfidence;
	}

	@Override
	public int hashCode() {
		return entityUri.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EntityAnnotation
				&& ((EntityAnnotation) obj).entityUri.equals(entityUri);
	}

	/**
	 * Compares based on the {@link #getDisambiguatedConfidence()} (if present)
	 * and falls back to the {@link #getOriginalConfidnece()}. If the original
	 * confidence value is not present or both Suggestions do have the same
	 * confidence the natural order of the Entities URI is used. This also
	 * ensures <code>(x.compareTo(y)==0) == (x.equals(y))</code> and allows to
	 * use this class with {@link SortedMap} and {@link SortedSet}
	 * implementations.
	 * <p>
	 */
	@Override
	public int compareTo(EntityAnnotation other) {
		int result;
		if (disambiguatedConfidence != null
				&& other.disambiguatedConfidence != null) {
			result = other.disambiguatedConfidence
					.compareTo(disambiguatedConfidence);
		} else if (other.originalConfidence != null
				&& originalConfidence != null) {
			result = other.originalConfidence.compareTo(originalConfidence);
		} else {
			result = 0;
		}
		// ensure (x.compareTo(y)==0) == (x.equals(y))
		return result == 0 ? entityUri.getUnicodeString().compareTo(
				other.entityUri.getUnicodeString()) : result;
	}

}
