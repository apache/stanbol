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

import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
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
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Engine that uses a {@link ReferencedSite} to search for entities for existing TextAnnotations of an Content
 * Item.
 * 
 * @author ogrisel, rwesten
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
specVersion = "1.1", metatype = true, immediate = true)
@Service
public class NamedEntityTaggingEngine implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property//(value = "dbpedia")
    public static final String REFERENCED_SITE_ID = "org.apache.stanbol.enhancer.engines.entitytagging.referencedSiteId";

    @Property//(boolValue = true)
    public static final String PERSON_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.personState";

    @Property//(value = "dbp-ont:Person")
    public static final String PERSON_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.personType";

    @Property//(boolValue = true)
    public static final String ORG_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.organisationState";

    @Property//(value = "dbp-ont:Organisation")
    public static final String ORG_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.organisationType";

    @Property//(boolValue = true)
    public static final String PLACE_STATE = "org.apache.stanbol.enhancer.engines.entitytagging.placeState";

    @Property//(value = "dbp-ont:Place")
    public static final String PLACE_TYPE = "org.apache.stanbol.enhancer.engines.entitytagging.placeType";
    /**
     * Use the RDFS label as default
     */
    @Property(value = "rdfs:label")
    public static final String NAME_FIELD = "org.apache.stanbol.enhancer.engines.entitytagging.nameField";

    /**
     * Service of the Entityhub that manages all the active referenced Site. This Service is used to lookup the
     * configured Referenced Site when we need to enhance a content item.
     */
    @Reference
    protected ReferencedSiteManager siteManager;

    /**
     * Used to lookup Entities if the {@link #REFERENCED_SITE_ID} property is
     * set to "entityhub" or "local"
     */
    @Reference
    protected Entityhub entityhub;
    
    /**
     * This holds the id of the {@link ReferencedSite} used to lookup Entities
     * or <code>null</code> if the {@link Entityhub} is used. 
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
    private String nameField;

    /**
     * The number of Suggestions to be added
     */
    public Integer numSuggestions = 3;

    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link ReferencedSite} can operate
     * offline or not.
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
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
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
        if(Entityhub.ENTITYHUB_IDS.contains(this.referencedSiteID.toLowerCase())){
            log.info("Init NamedEntityTaggingEngine instance for the Entityhub");
            this.referencedSiteID = null;
        }
        Object state = config.get(PERSON_STATE);
        personState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(ORG_STATE);
        orgState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(PLACE_STATE);
        placeState = state == null ? true : Boolean.parseBoolean(state.toString());
        Object type = config.get(PERSON_TYPE);
        personType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type
                .toString());
        type = config.get(ORG_TYPE);
        orgType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type
                .toString());
        type = config.get(PLACE_TYPE);
        placeType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type
                .toString());
        Object nameField = config.get(NAME_FIELD);
        this.nameField = nameField == null || nameField.toString().isEmpty() ? NamespaceEnum.rdfs + "label"
                : NamespaceEnum.getFullName(nameField.toString());
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        referencedSiteID = null;
        personType = null;
        orgType = null;
        placeType = null;
        nameField = null;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        final ReferencedSite site;
        if(referencedSiteID != null) { //lookup the referenced site
            site = siteManager.getReferencedSite(referencedSiteID);
            //ensure that it is present
            if (site == null) {
                String msg = String.format(
                    "Unable to enhance %s because Referenced Site %s is currently not active!", ci.getId(),
                    referencedSiteID);
                log.warn(msg);
                // TODO: throwing Exceptions is currently deactivated. We need a more clear
                // policy what do to in such situations
                // throw new EngineException(msg);
                return;
            }
            //and that it supports offline mode if required
            if (isOfflineMode() && !site.supportsLocalMode()) {
                log.warn("Unable to enhance ci {} because OfflineMode is not supported by ReferencedSite {}.",
                    ci.getId(), site.getId());
                return;
            }
        } else { // null indicates to use the Entityhub to lookup Entities
            site = null;
        }
        UriRef contentItemId = new UriRef(ci.getId());

        MGraph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        // Retrieve the existing text annotations
        Map<UriRef,List<UriRef>> textAnnotations = new HashMap<UriRef,List<UriRef>>();
        for (Iterator<Triple> it = graph.filter(null, RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION); it
                .hasNext();) {
            UriRef uri = (UriRef) it.next().getSubject();
            if (graph.filter(uri, Properties.DC_RELATION, null).hasNext()) {
                // this is not the most specific occurrence of this name: skip
                continue;
            }
            // This is a first occurrence, collect any subsumed annotations
            List<UriRef> subsumed = new ArrayList<UriRef>();
            for (Iterator<Triple> it2 = graph.filter(null, Properties.DC_RELATION, uri); it2.hasNext();) {
                subsumed.add((UriRef) it2.next().getSubject());
            }
            textAnnotations.put(uri, subsumed);
        }

        for (Map.Entry<UriRef,List<UriRef>> entry : textAnnotations.entrySet()) {
            try {
                computeEntityRecommentations(site,literalFactory, graph, contentItemId, entry.getKey(),
                    entry.getValue());
            } catch (EntityhubException e) {
                throw new EngineException(this, ci, e);
            }
        }
    }

    /**
     * Computes the Enhancements
     * @param site The {@link ReferencedSiteException} id or <code>null</code> to
     * use the {@link Entityhub}
     * @param literalFactory the {@link LiteralFactory} used to create RDF Literals
     * @param graph the graph to write the lined entities
     * @param contentItemId the id of the contentItem
     * @param textAnnotation the text annotation to enhance
     * @param subsumedAnnotations other text annotations for the same entity 
     * @return the suggested {@link Entity entities}
     * @throws EntityhubException On any Error while looking up Entities via
     * the Entityhub
     */
    protected final Iterable<Entity> computeEntityRecommentations(ReferencedSite site,
            LiteralFactory literalFactory,
            MGraph graph,
            UriRef contentItemId,
            UriRef textAnnotation,
            List<UriRef> subsumedAnnotations) throws EntityhubException {
        // First get the required properties for the parsed textAnnotation
        // ... and check the values
        String name = EnhancementEngineHelper.getString(graph, textAnnotation, ENHANCER_SELECTED_TEXT);
        if (name == null) {
            log.info("Unable to process TextAnnotation " + textAnnotation + " because property"
                     + ENHANCER_SELECTED_TEXT + " is not present");
            return Collections.emptyList();
        }
        if(name.isEmpty()){
            log.info("Unable to process TextAnnotation " + textAnnotation + 
                " because an empty Stirng is selected by " + ENHANCER_SELECTED_TEXT + "");
            return Collections.emptyList();
        }

        UriRef type = EnhancementEngineHelper.getReference(graph, textAnnotation, DC_TYPE);
        if (type == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation + " because property" + DC_TYPE
                     + " is not present");
            return Collections.emptyList();
        }
        // remove punctuation form the search string
        name = cleanupKeywords(name);

        log.debug("Process TextAnnotation " + name + " type=" + type);
        FieldQuery query = site == null ? //if site is NULL use the Entityhub
                entityhub.getQueryFactory().createFieldQuery() : 
                    site.getQueryFactory().createFieldQuery();
        // replace spaces with plus to create an AND search for all words in the name!
        query.setConstraint(nameField, new TextConstraint(name));// name.replace(' ', '+')));
        if (OntologicalClasses.DBPEDIA_PERSON.equals(type)) {
            if (personState) {
                if (personType != null) {
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(personType));
                }
                // else no type constraint
            } else {
                // ignore people
                return Collections.emptyList();
            }
        } else if (DBPEDIA_ORGANISATION.equals(type)) {
            if (orgState) {
                if (orgType != null) {
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(orgType));
                }
                // else no type constraint
            } else {
                // ignore people
                return Collections.emptyList();
            }
        } else if (OntologicalClasses.DBPEDIA_PLACE.equals(type)) {
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
        query.setLimit(Math.max(20,this.numSuggestions*3));
        QueryResultList<Entity> results = site == null? //if site is NULL
                entityhub.findEntities(query) : //use the Entityhub
                    site.findEntities(query); //else the referenced site
        log.debug("{} results returned by query {}", results.size(), query);

        List<NonLiteral> annotationsToRelate = new ArrayList<NonLiteral>();
        annotationsToRelate.add(textAnnotation);
        annotationsToRelate.addAll(subsumedAnnotations);
        Float maxScore = null;
        int exactCount = 0;
        List<Entity> matches = new ArrayList<Entity>(numSuggestions);
        for (Iterator<Entity> guesses = results.iterator();guesses.hasNext() && exactCount<numSuggestions;) {
            Entity guess = guesses.next();
            Representation rep = guess.getRepresentation();
            if(maxScore == null){
                maxScore = rep.getFirst(RdfResourceEnum.resultScore.getUri(),Float.class);
            }
            Iterator<Text> labels = rep.getText(nameField);
            boolean found = false;
            while(labels.hasNext() && !found){
                Text label = labels.next();
                if(label.getLanguage() == null || label.getLanguage().startsWith("en")){
                    if(label.getText().equalsIgnoreCase(name)){
                        found = true;
                    }
                }
            }
            if(found){
                matches.add(exactCount,guess);
                exactCount++;
            } else if(matches.size()<numSuggestions){
                matches.add(guess);
            }
        }
        //now write the results
        for(int i=0;i<matches.size();i++){
            Representation rep = matches.get(i).getRepresentation();
            if(i<exactCount){ //and boost the scores of the exact matches
                if(maxScore == null){
                    rep.set(RdfResourceEnum.resultScore.getUri(), 1.0f);
                } else {
                    Float score = rep.getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
                    rep.set(RdfResourceEnum.resultScore.getUri(), 
                        maxScore.doubleValue()+(score != null?score.doubleValue():0));
                }
            }
            log.debug("Adding {} to ContentItem {}", rep.getId(), contentItemId);
            EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory, graph, contentItemId,
                annotationsToRelate, rep, nameField);
        }
        
        return results;
    }

    public int canEnhance(ContentItem ci) {
        /*
         * This engine consumes existing enhancements because of that it can enhance any type of ci! TODO: It
         * would also be possible to check here if there is an TextAnnotation and use that as result!
         */
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    /**
     * Removes punctuation form a parsed string
     */
    private static String cleanupKeywords(String keywords) {
        return keywords.replaceAll("\\p{P}", " ").trim();
    }
}
