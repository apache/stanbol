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

import static org.apache.commons.lang.StringUtils.getLevenshteinDistance;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine that uses a {@link Site} to search for entities for existing TextAnnotations of an Content Item.
 * 
 * @author ogrisel, rwesten
 */
@Component(configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME)})
public class NamedEntityTaggingEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property // (value = "dbpedia")
    public static final String REFERENCED_SITE_ID = "org.apache.stanbol.enhancer.engines.entitytagging.referencedSiteId";

    @Property(boolValue = false)
    public static final String PERSON_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.personState";

    @Property // (value = "dbp-ont:Person")
    public static final String PERSON_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.personType";

    @Property(boolValue = false)
    public static final String ORG_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.organisationState";

    @Property // (value = "dbp-ont:Organisation")
    public static final String ORG_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.organisationType";

    @Property(boolValue = false)
    public static final String PLACE_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.placeState";

    @Property // (value = "dbp-ont:Place")
    public static final String PLACE_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.placeType";
    /**
     * Use the RDFS label as default
     */
    @Property(value = "rdfs:label")
    public static final String NAME_FIELD = "org.apache.stanbol.enhancer.engines.entitytagging.nameField";

    /**
     * Use the RDFS label as default
     * @deprecated Use a dereference engine instead (STANBOL-336)
     */
    @Deprecated
    @Property(boolValue = false) //changed default to false
    public static final String DEREFERENCE_ENTITIES = "org.apache.stanbol.enhancer.engines.entitytagging.dereference";

    @Property(intValue = 0)
    public static final String SERVICE_RANKING = Constants.SERVICE_RANKING;
    /**
     * The default language for labels included in the enhancement metadata (if not available for the parsed
     * content).
     */
    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Service of the Entityhub that manages all the active referenced Site. This Service is used to lookup
     * the configured Referenced Site when we need to enhance a content item.
     */
    @Reference
    protected SiteManager siteManager;

    /**
     * Used to lookup Entities if the {@link #REFERENCED_SITE_ID} property is set to "entityhub" or "local"
     */
    @Reference
    protected Entityhub entityhub;

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService nsPrefixService;
    
    /**
     * This holds the id of the {@link Site} used to lookup Entities or <code>null</code> if the
     * {@link Entityhub} is used.
     */
    protected String referencedSiteID;

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_PERSON} are enhanced by this engine
     */
    protected boolean personState;

    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_ORGANISATION} are enhanced by this
     * engine
     */
    protected boolean orgState;

    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_PLACE} are enhanced by this engine
     */
    protected boolean placeState;

    /**
     * The rdf:type constraint used to search for persons or <code>null</code> if no type constraint should be
     * used
     */
    protected String personType;

    /**
     * The rdf:type constraint used to search for organisations or <code>null</code> if no type constraint
     * should be used
     */
    protected String orgType;

    /**
     * The rdf:type constraint used to search for places or <code>null</code> if no type constraint should be
     * used
     */
    protected String placeType;

    /**
     * The field used to search for the selected text of the TextAnnotation.
     */
    protected String nameField;

    /**
     * The number of Suggestions to be added
     */
    protected Integer numSuggestions = 3;
    /**
     * Changed default to <code>false</code> now that this feature is deprecated
     * (STANBOL-336).
     */
    protected boolean dereferenceEntities = false;

    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link Site} can operate offline or
     * not.
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "enableOfflineMode", unbind = "disableOfflineMode", strategy = ReferenceStrategy.EVENT)
    private OfflineMode offlineMode;

    /**
     * Called by the ConfigurationAdmin to bind the {@link #offlineMode} if the service becomes available
     * 
     * @param mode
     */
    protected final void enableOfflineMode(OfflineMode mode) {
        this.offlineMode = mode;
    }

    /**
     * Called by the ConfigurationAdmin to unbind the {@link #offlineMode} if the service becomes unavailable
     * 
     * @param mode
     */
    protected final void disableOfflineMode(OfflineMode mode) {
        this.offlineMode = null;
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode} .
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
        Dictionary<String,Object> config = context.getProperties();
        Object referencedSiteID = config.get(REFERENCED_SITE_ID);
        if (referencedSiteID == null) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }

        this.referencedSiteID = referencedSiteID.toString();
        if (this.referencedSiteID.isEmpty()) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        if (Entityhub.ENTITYHUB_IDS.contains(this.referencedSiteID.toLowerCase())) {
            log.debug("Init NamedEntityTaggingEngine instance for the Entityhub");
            this.referencedSiteID = null;
        }
        Object state = config.get(PERSON_STATE);
        personState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(ORG_STATE);
        orgState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(PLACE_STATE);
        placeState = state == null ? true : Boolean.parseBoolean(state.toString());
        Object type = config.get(PERSON_TYPE);
        personType = type == null || type.toString().isEmpty() ? null : 
            NamespaceMappingUtils.getConfiguredUri(nsPrefixService,PERSON_TYPE, type.toString());
        type = config.get(ORG_TYPE);
        orgType = type == null || type.toString().isEmpty() ? null : 
            NamespaceMappingUtils.getConfiguredUri(nsPrefixService,ORG_TYPE,type.toString());
        type = config.get(PLACE_TYPE);
        placeType = type == null || type.toString().isEmpty() ? null : 
            NamespaceMappingUtils.getConfiguredUri(nsPrefixService,PLACE_TYPE,type.toString());
        Object nameField = config.get(NAME_FIELD);
        this.nameField = nameField == null || nameField.toString().isEmpty() ? 
                "http://www.w3.org/2000/01/rdf-schema#label" : 
                    NamespaceMappingUtils.getConfiguredUri(nsPrefixService,NAME_FIELD,nameField.toString());
        Object dereferenceEntities = config.get(DEREFERENCE_ENTITIES);
        this.dereferenceEntities = state == null ? true : Boolean
                .parseBoolean(dereferenceEntities.toString());
        if(this.dereferenceEntities){
            log.warn("DereferenceEntities is deprecated for this Enigne. Please use "
                + "the EntityhubDereferenceEngine instead (see STANBOL-1223 for details)");
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        referencedSiteID = null;
        personType = null;
        orgType = null;
        placeType = null;
        nameField = null;
    }


    
    public void computeEnhancements(ContentItem ci) throws EngineException {
        final Site site;
        if (referencedSiteID != null) { // lookup the referenced site
            site = siteManager.getSite(referencedSiteID);
            // ensure that it is present
            if (site == null) {
                String msg = String.format(
                    "Unable to enhance %s because Referenced Site %s is currently not active!", ci.getUri()
                            .getUnicodeString(), referencedSiteID);
                log.warn(msg);
                // TODO: throwing Exceptions is currently deactivated. We need a
                // more clear
                // policy what do to in such situations
                // throw new EngineException(msg);
                return;
            }
            // and that it supports offline mode if required
            if (isOfflineMode() && !site.supportsLocalMode()) {
                log.warn(
                    "Unable to enhance ci {} because OfflineMode is not supported by ReferencedSite {}.", ci
                            .getUri().getUnicodeString(), site.getId());
                return;
            }
        } else { // null indicates to use the Entityhub to lookup Entities
            site = null;
        }
        Graph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        // Retrieve the existing text annotations (requires read lock)
        Map<NamedEntity,List<IRI>> textAnnotations = new HashMap<NamedEntity,List<IRI>>();
        // the language extracted for the parsed content or NULL if not
        // available
        String contentLangauge;
        ci.getLock().readLock().lock();
        try {
            contentLangauge = EnhancementEngineHelper.getLanguage(ci);
            for (Iterator<Triple> it = graph.filter(null, RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION); it
                    .hasNext();) {
                IRI uri = (IRI) it.next().getSubject();
                if (graph.filter(uri, Properties.DC_RELATION, null).hasNext()) {
                    // this is not the most specific occurrence of this name:
                    // skip
                    continue;
                }
                NamedEntity namedEntity = NamedEntity.createFromTextAnnotation(graph, uri);
                if (namedEntity != null) {
                    // This is a first occurrence, collect any subsumed
                    // annotations
                    List<IRI> subsumed = new ArrayList<IRI>();
                    for (Iterator<Triple> it2 = graph.filter(null, Properties.DC_RELATION, uri); it2
                            .hasNext();) {
                        subsumed.add((IRI) it2.next().getSubject());
                    }
                    textAnnotations.put(namedEntity, subsumed);
                }
            }
        } finally {
            ci.getLock().readLock().unlock();
        }
        // search the suggestions
        Map<NamedEntity,List<Suggestion>> suggestions = new HashMap<NamedEntity,List<Suggestion>>(
                textAnnotations.size());
        for (Entry<NamedEntity,List<IRI>> entry : textAnnotations.entrySet()) {
            try {
                List<Suggestion> entitySuggestions = computeEntityRecommentations(site, entry.getKey(),
                    entry.getValue(), contentLangauge);
                if (entitySuggestions != null && !entitySuggestions.isEmpty()) {
                    suggestions.put(entry.getKey(), entitySuggestions);
                }
            } catch (EntityhubException e) {
                throw new EngineException(this, ci, e);
            }
        }
        // now write the results (requires write lock)
        ci.getLock().writeLock().lock();
        try {
            RdfValueFactory factory = RdfValueFactory.getInstance();
            Map<String,Representation> entityData = new HashMap<String,Representation>();
            for (Entry<NamedEntity,List<Suggestion>> entitySuggestions : suggestions.entrySet()) {
                List<IRI> subsumed = textAnnotations.get(entitySuggestions.getKey());
                List<BlankNodeOrIRI> annotationsToRelate = new ArrayList<BlankNodeOrIRI>(subsumed);
                annotationsToRelate.add(entitySuggestions.getKey().getEntity());
                for (Suggestion suggestion : entitySuggestions.getValue()) {
                    log.debug("Add Suggestion {} for {}", suggestion.getEntity().getId(),
                        entitySuggestions.getKey());
                    EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory, graph, ci.getUri(),
                        annotationsToRelate, suggestion, nameField,
                        // TODO: maybe we want labels in a different
                        // language than the
                        // language of the content (e.g. Accept-Language
                        // header)?!
                        contentLangauge == null ? DEFAULT_LANGUAGE : contentLangauge);
                    if (dereferenceEntities) {
                        entityData.put(suggestion.getEntity().getId(), suggestion.getEntity()
                                .getRepresentation());
                    }
                }
            }
            // if dereferneceEntities is true the entityData will also contain
            // all
            // Representations to add! If false entityData will be empty
            for (Representation rep : entityData.values()) {
                graph.addAll(factory.toRdfRepresentation(rep).getRdfGraph());
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }

    }

    /**
     * Computes the Enhancements
     * 
     * @param site
     *            The {@link SiteException} id or <code>null</code> to use the {@link Entityhub}
     * @param literalFactory
     *            the {@link LiteralFactory} used to create RDF Literals
     * @param contentItemId
     *            the id of the contentItem
     * @param textAnnotation
     *            the text annotation to enhance
     * @param subsumedAnnotations
     *            other text annotations for the same entity
     * @param language
     *            the language of the analysed text or <code>null</code> if not available.
     * @return the suggestions for the parsed {@link NamedEntity}
     * @throws EntityhubException
     *             On any Error while looking up Entities via the Entityhub
     */
    protected final List<Suggestion> computeEntityRecommentations(Site site,
                                                                  NamedEntity namedEntity,
                                                                  List<IRI> subsumedAnnotations,
                                                                  String language) throws EntityhubException {
        // First get the required properties for the parsed textAnnotation
        // ... and check the values

        log.debug("Process {}", namedEntity);
        // if site is NULL use
        // the Entityhub
        FieldQueryFactory queryFactory = site == null ? entityhub.getQueryFactory() : site.getQueryFactory();

        log.trace("Will use a query-factory of type [{}].", queryFactory.getClass().toString());

        FieldQuery query = queryFactory.createFieldQuery();

        // replace spaces with plus to create an AND search for all words in the
        // name!
        Constraint labelConstraint;
        // TODO: make case sensitivity configurable
        boolean casesensitive = false;
        String namedEntityLabel = casesensitive ? namedEntity.getName() : namedEntity.getName().toLowerCase();
        if (language != null) {
            // search labels in the language and without language
            labelConstraint = new TextConstraint(namedEntityLabel, casesensitive, language, null);
        } else {
            labelConstraint = new TextConstraint(namedEntityLabel, casesensitive);
        }
        query.setConstraint(nameField, labelConstraint);
        if (OntologicalClasses.DBPEDIA_PERSON.equals(namedEntity.getType())) {
            if (personState) {
                if (personType != null) {
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(personType));
                }
                // else no type constraint
            } else {
                // ignore people
                return Collections.emptyList();
            }
        } else if (DBPEDIA_ORGANISATION.equals(namedEntity.getType())) {
            if (orgState) {
                if (orgType != null) {
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(orgType));
                }
                // else no type constraint
            } else {
                // ignore people
                return Collections.emptyList();
            }
        } else if (OntologicalClasses.DBPEDIA_PLACE.equals(namedEntity.getType())) {
            if (this.placeState) {
                if (this.placeType != null) {
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(placeType));
                }
                // else no type constraint
            } else {
                // ignore people
                return Collections.emptyList();
            }
        }
        query.setLimit(Math.max(20, this.numSuggestions * 3));

        log.trace("A query has been created of type [{}] and the following settings:\n{}", query.getClass()
                .toString(), query.toString());

        if (null == site) log.trace("A query will be sent to the entity-hub of type [{}].", entityhub
                .getClass());
        else log.trace("A query will be sent to a site [id :: {}][type :: {}].", site.getId(), site
                .getClass());

        QueryResultList<Entity> results = site == null ? // if site is NULL
        entityhub.findEntities(query)
                : // use the Entityhub
                site.findEntities(query); // else the referenced site
        log.debug(" - {} results returned by query {}", results.size(), results.getQuery());
        if (results.isEmpty()) { // no results nothing to do
            return Collections.emptyList();
        }
        // we need to normalise the confidence values from [0..1]
        // * levenshtein distance as absolute (1.0 for exact match)
        // * Solr scores * levenshtein to rank entities relative to each other
        Float maxScore = null;
        Float maxExactScore = null;
        List<Suggestion> matches = new ArrayList<Suggestion>(numSuggestions);
        // assumes entities are sorted by score
        for (Iterator<Entity> guesses = results.iterator(); guesses.hasNext();) {
            Suggestion match = new Suggestion(guesses.next());
            Representation rep = match.getEntity().getRepresentation();
            Float score = rep.getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
            if (maxScore == null) {
                maxScore = score;
            }
            Iterator<Text> labels = rep.getText(nameField);
            while (labels.hasNext() && match.getLevenshtein() < 1.0) {
                Text label = labels.next();
                if (language == null || // if the content language is unknown ->
                                        // accept all labels
                    label.getLanguage() == null || // accept labels with no
                                                   // language
                    // and labels in the same language as the content
                    (language != null && label.getLanguage().startsWith(language))) {
                    double actMatch = levenshtein(
                        casesensitive ? label.getText() : label.getText().toLowerCase(), namedEntityLabel);
                    if (actMatch > match.getLevenshtein()) {
                        match.setLevenshtein(actMatch);
                        match.setMatchedLabel(label);
                    }
                }
            }
            if (match.getMatchedLabel() != null) {
                if (match.getLevenshtein() == 1.0) {
                    if (maxExactScore == null) {
                        maxExactScore = score;
                    }
                    // normalise exact matches against the best exact score
                    match.setScore(score.doubleValue() / maxExactScore.doubleValue());
                } else {
                    // normalise partial matches against the best match and the
                    // Levenshtein similarity with the label
                    match.setScore(score.doubleValue() * match.getLevenshtein() / maxScore.doubleValue());
                }
                matches.add(match);
            } else {
                log.debug("No value of {} for Entity {}!", nameField, match.getEntity().getId());
            }
        }
        // now sort the results
        Collections.sort(matches);
        return matches.subList(0, Math.min(matches.size(), numSuggestions));
    }

    /**
     * This EnhancementEngine can enhance any ContentItem as it does consume existing TextAnnotations with the
     * configured dc:type's
     * 
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngine#canEnhance(org.apache.stanbol.enhancer.servicesapi.ContentItem)
     */
    public int canEnhance(ContentItem ci) {
        return ENHANCE_ASYNC; // Entity tagging now supports asyc processing
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    /**
     * Compares two strings (after {@link StringUtils#trim(String) trimming}) by using the Levenshtein's Edit
     * Distance of the two strings. Does not return the {@link Integer} number of changes but
     * <code>1-(changes/maxStringSizeAfterTrim)</code>
     * <p>
     * 
     * @param s1
     *            the first string
     * @param s2
     *            the second string
     * @return the distance
     * @throws IllegalArgumentException
     *             if any of the two parsed strings is NULL
     */
    private static double levenshtein(String s1, String s2) {
        if (s1 == null || s2 == null) {
            throw new IllegalArgumentException("NONE of the parsed String MUST BE NULL!");
        }
        s1 = StringUtils.trim(s1);
        s2 = StringUtils.trim(s2);
        return s1.isEmpty() || s2.isEmpty() ? 0
                : 1.0 - (((double) getLevenshteinDistance(s1, s2)) / ((double) (Math.max(s1.length(),
                    s2.length()))));
    }
}
