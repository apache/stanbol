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
package org.apache.stanbol.enhancer.engines.entitycoreference.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.COREF_ANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.clerezza.commons.rdf.IRI;


import org.apache.stanbol.enhancer.engines.entitycoreference.Constants;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.NounPhrase;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.PlaceAdjectival;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.coref.CorefFeature;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.cm.ConfigurationException;

/**
 * Uses the list of NERs and the list of {@link NounPhrase}s found in the analyzed text to find possible
 * co-references.
 * 
 * @author Cristian Petroaca
 * 
 */
public class CoreferenceFinder {
    /**
     * The configured {@link SiteManager} for {@link Entity} storage.
     */
    private SiteManager siteManager;

    /**
     * The default {@link Entity} storage.
     */
    private Entityhub entityHub;

    /**
     * The name of the configured site for the {@link SiteManager}.
     */
    private String referencedSiteID;

    /**
     * In memory cache storing {@link Entity} types which are often used.
     */
    private InMemoryEntityTypeIndex entityTypeIndex;

    /**
     * Class holding configuration params.
     */
    private CoreferenceFinderConfig config;

    /**
     * Holds vocabulary.dictionary info such as the list of place adjectivals by language.
     */
    private Dictionaries dictionaries;

    public CoreferenceFinder(String[] languages,
                             SiteManager siteManager,
                             Entityhub entityHub,
                             String referencedSiteID,
                             int maxDistance,
                             String entityUriBase,
                             String spatialAttrForPerson,
                             String spatialAttrForOrg,
                             String spatialAttrForPlace,
                             String orgAttributesForPerson,
                             String entityClassesToExclude) throws ConfigurationException {
        this.siteManager = siteManager;
        this.entityHub = entityHub;
        this.referencedSiteID = referencedSiteID;
        this.entityTypeIndex = new InMemoryEntityTypeIndex();
        this.config = new CoreferenceFinderConfig(maxDistance, spatialAttrForPerson, 
        		spatialAttrForOrg, spatialAttrForPlace, orgAttributesForPerson, entityClassesToExclude);
        this.dictionaries = new Dictionaries(languages, entityUriBase);
    }

    /**
     * Performs the actual coreference resolution by iterating through all the NERs and all the
     * {@link NounPhrase}s which are after the given Ner in the text. If any coreferences are found they are
     * written as {@link NlpAnnotation}s in the NER and noun phrase {@link Span}s.
     * 
     * @param ners
     * @param nounPhrases
     * @param language
     * @throws EngineException
     */
    public void extractCorefs(Map<Integer,List<Span>> ners, List<NounPhrase> nounPhrases, String language) throws EngineException {
        for (Map.Entry<Integer,List<Span>> entry : ners.entrySet()) {
            int nerSentenceNo = entry.getKey();
            List<Span> nerSpans = entry.getValue();
            int maxDistance = this.config.getMaxDistance();

            for (Span ner : nerSpans) {
                Entity entity = null;
                Set<String> typeLabels = null;
                Set<Span> corefs = new HashSet<Span>();

                for (NounPhrase nounPhrase : nounPhrases) {
                    int nounPhraseSentenceNo = nounPhrase.getSentenceNo();

                    if (nounPhrase.getChunk().getStart() > ner.getStart()
                        && (maxDistance != Constants.MAX_DISTANCE_NO_CONSTRAINT
                            && nounPhraseSentenceNo > nerSentenceNo && nounPhraseSentenceNo - nerSentenceNo <= maxDistance)) {

                        if (entity == null) {
                            entity = lookupEntity(ner, language);

                            /*
                             * If the entity is still null there's nothing to do but go to the next ner.
                             */
                            if (entity == null) break;

                            if (typeLabels == null) {
                                typeLabels = buildEntityTypeLabels(entity, language);
                            }
                        }

                        if (isCoreferent(typeLabels, entity, ner, nounPhrase, language)) {
                            Set<Span> coreferencedNer = new HashSet<Span>();
                            coreferencedNer.add(ner);
                            Span chunk = nounPhrase.getChunk();

                            chunk.addAnnotation(COREF_ANNOTATION,
                                Value.value(new CorefFeature(false, coreferencedNer)));
                            corefs.add(chunk);
                        }
                    }
                }

                if (corefs.size() > 0) {
                    ner.addAnnotation(COREF_ANNOTATION, Value.value(new CorefFeature(true, corefs)));
                }
            }
        }
    }

    /**
     * Gets an Entity from the configured {@link Site} based on the NER text and type.
     * 
     * @param ner
     * @param language
     * @return
     * @throws EngineException
     */
    private Entity lookupEntity(Span ner, String language) throws EngineException {
        Site site = getReferencedSite();
        FieldQueryFactory queryFactory = site == null ? entityHub.getQueryFactory() : site.getQueryFactory();
        FieldQuery query = queryFactory.createFieldQuery();

        Constraint labelConstraint;
        String namedEntityLabel = ner.getSpan();
        labelConstraint = new TextConstraint(namedEntityLabel, false, language, null);
        query.setConstraint(RDFS_LABEL.getUnicodeString(), labelConstraint);
        query.setConstraint(RDF_TYPE.getUnicodeString(),
            new ReferenceConstraint(ner.getAnnotation(NlpAnnotations.NER_ANNOTATION).value().getType()
                    .getUnicodeString()));
        query.setLimit(1);
        QueryResultList<Entity> results = site == null ? // if site is NULL
        entityHub.findEntities(query)
                : // use the Entityhub
                site.findEntities(query); // else the referenced site

        if (results.isEmpty()) return null;

        // We set the limit to 1 so if it found anything it should contain just 1 entry
        return results.iterator().next();
    }

    /**
     * Performs the coreference matching rules: 1. Match the entity type. 2. If the {@link NounPhrase}
     * contains any NERs match the NER to any spatial/org membership/functional Entity properties from the
     * {@link Site}. 3. If {@link NounPhrase} contains any place adjectivals perform spatial co-reference
     * based on the entity spatial properties.
     * 
     * @param typeLabels
     *            - a list of types (classes) that the given entity has.
     * @param entity
     *            - the entity for which we want to do the coref.
     * @param ner
     *            - the ner in the text for which we want to do the coref.
     * @param nounPhrase
     *            - the {@link NounPhrase} which we want to test for coref.
     * @param language
     *            - the language of the text.
     * @return
     * @throws EngineException
     */
    private boolean isCoreferent(Set<String> typeLabels,
                                 Entity entity,
                                 Span ner,
                                 NounPhrase nounPhrase,
                                 String language) throws EngineException {
        /*
         * 1. Try to match the entity class to the noun phrase.
         */
        String matchedClass = null;
        String nounPhraseText = nounPhrase.getChunk().getSpan().toLowerCase();
        int classStart = 0;
        int classEnd = 0;

        for (String label : typeLabels) {
            if (nounPhraseText.matches(".*\\b" + label + "\\b.*")
                && (matchedClass == null || label.split("\\s").length > matchedClass.split("\\s").length)) {
                matchedClass = label;
                classStart = nounPhrase.getChunk().getStart() + nounPhraseText.indexOf(label);
                classEnd = classStart + label.length();
            }
        }

        if (matchedClass == null) return false;

        /*
         * 2. See if there are any NERs in the noun phrase to further identify the coref. Any NERs found
         * should be separate words from the class matches from point 1.
         */
        /*
         * TODO - devise a coref confidence scheme?
         */
        if (nounPhrase.hasNers()) {
            List<Span> npNers = nounPhrase.getNerChunks();
            IRI nerType = ner.getAnnotation(NlpAnnotations.NER_ANNOTATION).value().getType();

            for (Span npNer : npNers) {
                /*
                 * Don't go any further if for some reason it turns out that the ner text is the same as the
                 * entity class text.
                 */
                if ((npNer.getStart() >= classStart && npNer.getStart() <= classEnd)
                    || (npNer.getEnd() >= classStart && npNer.getEnd() <= classEnd)) continue;

                Entity npEntity = lookupEntity(npNer, language);

                if (npEntity != null) {
                    IRI npNerType = npNer.getAnnotation(NlpAnnotations.NER_ANNOTATION).value().getType();
                    Set<String> rulesOntologyAttr = new HashSet<String>();

                    if (OntologicalClasses.DBPEDIA_PLACE.equals(npNerType)) {
                        rulesOntologyAttr = this.config.getSpatialAttributes(nerType);
                    } else if (OntologicalClasses.DBPEDIA_ORGANISATION.equals(npNerType)) {
                    	rulesOntologyAttr = this.config.getOrgMembershipAttributes(nerType);
                    }

                    if (valueExistsInEntityAttributes(rulesOntologyAttr, entity, npEntity.getId())) {
                        return true;
                    }
                }
            }
        }

        /*
         * 3. Detect any place adjectivals in noun phrases and use them for spatial coreference. Any place
         * adjectivals found should be separate words from the class matches from point 1.
         */
        PlaceAdjectival placeAdjectival = this.dictionaries.findPlaceAdjectival(language, nounPhrase);

        if (placeAdjectival != null
            && (placeAdjectival.getEnd() < classStart || placeAdjectival.getStart() > classEnd)) {
            /*
             * We use the same spatial rules ontology attributes as before.
             */
            Set<String> rulesOntologyAttr = this.config.getSpatialAttributes(ner
                    .getAnnotation(NlpAnnotations.NER_ANNOTATION).value().getType());

            if (valueExistsInEntityAttributes(rulesOntologyAttr, entity, placeAdjectival.getPlaceUri()
                    .getUnicodeString())) {
                return true;
            }
        }

        /*
         * If there was no additional info to do the coref and if the entity class matched and has more than 1
         * word then we consider this a good enough coreference.
         */
        if (matchedClass.split("\\s").length > 1) return true;

        return false;
    }

    /**
     * Builds a Set of Entity Type labels given the Entity type uris.
     * 
     * @param entity
     * @param language
     * @return
     * @throws EngineException
     */
    private Set<String> buildEntityTypeLabels(Entity entity, String language) throws EngineException {
        Iterator<Object> typeUris = entity.getRepresentation().get(RDF_TYPE.getUnicodeString());
        Set<String> allTypeLabels = new HashSet<String>();

        while (typeUris.hasNext()) {
            String typeUri = typeUris.next().toString();

            if (this.config.shouldExcludeClass(typeUri)) continue;

            // First try the in memory index
            Set<String> labels = this.entityTypeIndex.lookupEntityType(new IRI(typeUri), language);

            if (labels == null) {
                Site site = getReferencedSite();
                Entity entityType = (site == null) ? this.entityHub.getEntity(typeUri) : site
                        .getEntity(typeUri);

                if (entityType != null) {
                    labels = new HashSet<String>();
                    Iterator<Text> labelIterator = entityType.getRepresentation().get(
                        RDFS_LABEL.getUnicodeString(), language);

                    while (labelIterator.hasNext()) {
                        labels.add(labelIterator.next().getText());
                    }

                    this.entityTypeIndex.addEntityType(new IRI(typeUri), language, labels);
                }
            }
            
            if (labels != null) allTypeLabels.addAll(labels);
        }

        return allTypeLabels;
    }

    /**
     * Checks whether any of the attributes in rulesOntologyAttr from the given Entity contain the given
     * value.
     * 
     * @param rulesOntologyAttr
     * @param entity
     * @param value
     * @return
     */
    private boolean valueExistsInEntityAttributes(Set<String> rulesOntologyAttr, Entity entity, String value) {
        for (String attribute : rulesOntologyAttr) {
            Iterator<Object> entityAttributes = entity.getRepresentation().get(attribute);

            while (entityAttributes.hasNext()) {
                Object entityAttribute = entityAttributes.next();

                if (entityAttribute.toString().equals(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Retrieves the configured {@link Site} which holds the NER properties.
     * 
     * @return
     * @throws EngineException
     */
    private Site getReferencedSite() throws EngineException {
        Site site = null;

        if (referencedSiteID != null) { // lookup the referenced site
            site = siteManager.getSite(referencedSiteID);
            // ensure that it is present
            if (site == null) {
                String msg = String
                        .format("Unable to enhance because Referenced Site %s is currently not active!",
                            referencedSiteID);

                throw new EngineException(msg);
            }
        }

        return site;
    }
}
