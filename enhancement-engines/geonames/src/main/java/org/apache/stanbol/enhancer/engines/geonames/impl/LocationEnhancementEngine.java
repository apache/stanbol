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
package org.apache.stanbol.enhancer.engines.geonames.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum.dbpedia_ont;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_REQUIRES;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.geonames.impl.GeonamesAPIWrapper.SearchRequestPropertyEnum;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service
@Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value=LocationEnhancementEngine.DEFAULT_ENGINE_NAME),
    @Property(name=Constants.SERVICE_RANKING,intValue=LocationEnhancementEngine.DEFAULT_SERVICE_RANKING)
})
public class LocationEnhancementEngine 
        extends AbstractEnhancementEngine<IOException,RuntimeException> 
        implements EnhancementEngine, ServiceProperties {

    public static final String DEFAULT_ENGINE_NAME = "geonames";
    /**
     * Default value for the {@link Constants#SERVICE_RANKING} used by this engine.
     * This is a negative value to allow easy replacement by this engine depending
     * to a remote service with one that does not have this requirement
     */
    public static final int DEFAULT_SERVICE_RANKING = -100;
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    /**
     * This maps geonames.org feature classes to dbpedia.org ontology classes
     */
    public static final Map<FeatureClass, Collection<IRI>> FEATURE_CLASS_CONCEPT_MAPPINGS;

    public static final Map<String, Collection<IRI>> FEATURE_TYPE_CONCEPT_MAPPINGS;

    private static final Logger log = LoggerFactory.getLogger(LocationEnhancementEngine.class);

    /**
     * Default value for minimum scores of search results are added to the
     * metadata of the parsed ContentItem
     */
    private static final double DEFAULT_MIN_SCORE = 0.33;

    @Property(doubleValue = DEFAULT_MIN_SCORE)
    public static final String MIN_SCORE = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.min-score";

    /**
     * Default values for the number of results returned by search requests
     * to the geonames.org web service
     */
    private static final int DEFAULT_MAX_LOCATION_ENHANCEMENTS = 3;

    @Property(intValue = DEFAULT_MAX_LOCATION_ENHANCEMENTS)
    public static final String MAX_LOCATION_ENHANCEMENTS = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.max-location-enhancements";

    /**
     * Default value for the minimum score of search results used to also add
     * the hierarchy
     */
    private static final double DEFAULT_MIN_HIERARCHY_SCORE = 0.70;

    @Property(doubleValue = DEFAULT_MIN_HIERARCHY_SCORE)
    public static final String MIN_HIERARCHY_SCORE = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.min-hierarchy-score";

    public static final IRI CONCEPT_GEONAMES_FEATURE = new IRI(NamespaceEnum.geonames.toString() + "Feature");
    @Property(value = GeonamesAPIWrapper.DEFAULT_GEONAMES_ORG_WEBSERVICE_URL)
    public static final String GEONAMES_SERVER_URL = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.serverURL";
    /**
     * The useage of the anonymous server is deactivated by default because it
     * is often overloaded and therefore causes randomly errors.
     */
    @Property(boolValue=false)
    public static final String GEONAMES_ANONYMOUS_SERVICE_STATE = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.allow-anonymous-service";
    @Property
    public static final String GEONAMES_USERNAME = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.username";
    @Property
    public static final String GEONAMES_TOKEN = "org.apache.stanbol.enhancer.engines.geonames.locationEnhancementEngine.token";

    /**
     * Only activate this engine in online mode
     */
    @SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode;

    /**
     * The geonames.org API wrapper used to make service requests
     */
    protected GeonamesAPIWrapper geonamesService;

    static {
        Map<FeatureClass, Collection<IRI>> mappings = new EnumMap<FeatureClass, Collection<IRI>>(FeatureClass.class);
        //first add the concepts of the geonames ontology
        for (FeatureClass fc : FeatureClass.values()) {
            List<IRI> conceptMappings = new ArrayList<IRI>();
            conceptMappings.add(CONCEPT_GEONAMES_FEATURE); //all things are features
            conceptMappings.add(DBPEDIA_PLACE); //all things are dbpedia places
            mappings.put(fc, conceptMappings);
        }
        //now add additional mappings to the dbpedia Ontology
        IRI populatedPlace = new IRI(dbpedia_ont + "PopulatedPlace");
        mappings.get(FeatureClass.P).addAll(Arrays.asList(populatedPlace, new IRI(dbpedia_ont + "Settlement")));
        mappings.get(FeatureClass.A).addAll(Arrays.asList(populatedPlace, new IRI(dbpedia_ont + "AdministrativeRegion")));
        mappings.get(FeatureClass.H).add(new IRI(dbpedia_ont + "BodyOfWater"));
        mappings.get(FeatureClass.R).add(new IRI(dbpedia_ont + "Infrastructure"));
        mappings.get(FeatureClass.S).add(new IRI(dbpedia_ont + "Building"));
        mappings.get(FeatureClass.T).add(new IRI(dbpedia_ont + "Mountain"));
        //now write the unmodifiable static final constant
        FEATURE_CLASS_CONCEPT_MAPPINGS = Collections.unmodifiableMap(mappings);

        //Mappings for known FeatureTypes
        Map<String, Collection<IRI>> typeMappings = new HashMap<String, Collection<IRI>>();
        Collection<IRI> lakeTypes = Arrays.asList(new IRI(dbpedia_ont + "Lake"));
        typeMappings.put("H.LK", lakeTypes);
        typeMappings.put("H.LKS", lakeTypes);
        typeMappings.put("H.LKI", lakeTypes);
        typeMappings.put("H.LKN", lakeTypes);
        typeMappings.put("H.LK", lakeTypes);
        typeMappings.put("H.LKO", lakeTypes);
        typeMappings.put("H.LKX", lakeTypes);
        typeMappings.put("H.LKC", lakeTypes);
        typeMappings.put("H.LKNI", lakeTypes);
        typeMappings.put("H.LKSI", lakeTypes);
        typeMappings.put("H.LKOI", lakeTypes);
        typeMappings.put("H.LKSN", lakeTypes);
        typeMappings.put("H.LKSC", lakeTypes);
        typeMappings.put("H.LKSB", lakeTypes);
        typeMappings.put("H.LKSNI", lakeTypes);
        typeMappings.put("H.RSV", lakeTypes);

        IRI stream = new IRI(dbpedia_ont + " Stream");
        Collection<IRI> canalTypes = Arrays.asList(stream, new IRI(dbpedia_ont + "Canal"));
        typeMappings.put("H.CNL", canalTypes);
        typeMappings.put("H.CNLA", canalTypes);
        typeMappings.put("H.CNLB", canalTypes);
        typeMappings.put("H.CNLI", canalTypes);
        typeMappings.put("H.CNLD", canalTypes);
        typeMappings.put("H.CNLSB", canalTypes);
        typeMappings.put("H.CNLN", canalTypes);
        typeMappings.put("H.CNLQ", canalTypes);
        typeMappings.put("H.CNLX", canalTypes);

        Collection<IRI> riverTypes = Arrays.asList(stream, new IRI(dbpedia_ont + "River"));
        typeMappings.put("H.STM", riverTypes);
        typeMappings.put("H.STMI", riverTypes);
        typeMappings.put("H.STMB", riverTypes);
        typeMappings.put("H.STMD", riverTypes);
        typeMappings.put("H.STMM", riverTypes);
        typeMappings.put("H.STMA", riverTypes);
        typeMappings.put("H.STMC", riverTypes);
        typeMappings.put("H.STMX", riverTypes);
        typeMappings.put("H.STMIX", riverTypes);
        typeMappings.put("H.STMH", riverTypes);
        typeMappings.put("H.STMSB", riverTypes);
        typeMappings.put("H.STMQ", riverTypes);
        typeMappings.put("H.STMS", riverTypes);
        typeMappings.put("H.STM", riverTypes);
        typeMappings.put("H.STM", riverTypes);
        typeMappings.put("H.STM", riverTypes);

        Collection<IRI> caveTypes = Arrays.asList(new IRI(dbpedia_ont + "Cave"));
        typeMappings.put("H.LKSB", caveTypes);
        typeMappings.put("R.TNLN", caveTypes);
        typeMappings.put("S.CAVE", caveTypes);
        typeMappings.put("S.BUR", caveTypes);

        Collection<IRI> countryTypes = Arrays.asList(new IRI(dbpedia_ont + "Country"));
        typeMappings.put("A.PCLI", countryTypes);

        IRI settlement = new IRI(dbpedia_ont + "Settlement");
        Collection<IRI> cityTypes = Arrays.asList(settlement, new IRI(dbpedia_ont + "City"));
        Collection<IRI> villageTypes = Arrays.asList(settlement, new IRI(dbpedia_ont + "Village"));
        typeMappings.put("P.PPLG", cityTypes);
        typeMappings.put("P.PPLC", cityTypes);
        typeMappings.put("P.PPLF", villageTypes);
        typeMappings.put("P.PPLA", cityTypes);
        //write the mappings as unmodifiable map the the static final constant
        FEATURE_TYPE_CONCEPT_MAPPINGS = Collections.unmodifiableMap(typeMappings);
    }


    private Integer maxLocationEnhancements;
    private Double minScore;
    private Double minHierarchyScore;

    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ce) throws IOException, ConfigurationException {
        super.activate(ce);
        Dictionary<String, Object> properties = ce.getProperties();
        log.debug("activating ...");
        //NOTE: The type of the values is ensured by the default values in the
        //      @Property annotations e.g. doubleValue -> Double
        setMinScore((Double) properties.get(MIN_SCORE));
        setMaxLocationEnhancements((Integer) properties.get(MAX_LOCATION_ENHANCEMENTS));
        setMinHierarchyScore((Double) properties.get(MIN_HIERARCHY_SCORE));
        //parse geonames.org service specific configuration
        Object value = properties.get(GEONAMES_ANONYMOUS_SERVICE_STATE);
        boolean allowAnonymous;
        if(value instanceof Boolean){
            allowAnonymous = ((Boolean)value).booleanValue();
        } else if(value != null){
            allowAnonymous = Boolean.parseBoolean(value.toString());
        } else {
            allowAnonymous = false;
        }
        String serverUrl = (String) properties.get(GEONAMES_SERVER_URL);
        String userName = (String) properties.get(GEONAMES_USERNAME);
        String token = (String) properties.get(GEONAMES_TOKEN);
        if(userName == null || userName.isEmpty()){
            if(allowAnonymous) {
                log.info("Anonymous Access is enabled and no User-Name is configured." +
                		"Ignore configred server URL {} and will use the anonymous server {}",
                		serverUrl,GeonamesAPIWrapper.ANONYMOUS_GEONAMES_ORG_WEBSERVICE_URL);
                serverUrl = GeonamesAPIWrapper.ANONYMOUS_GEONAMES_ORG_WEBSERVICE_URL;
            } else {
                throw new ConfigurationException(GEONAMES_USERNAME, 
                    "A User-Name MUST be configured if anonymous access to 'http://ws.geonames.org' is deactivated");
            }
        } else {
            if( token == null || token.isEmpty()){
                throw new ConfigurationException(GEONAMES_TOKEN, 
                    "The Token MUST NOT be NULL nor empty if a User-Name is defined!");
            }
            if(serverUrl == null || serverUrl.isEmpty()){
                log.info("No ServerUrl is configured. Will use the default {}",
                    GeonamesAPIWrapper.DEFAULT_GEONAMES_ORG_WEBSERVICE_URL);
                serverUrl = GeonamesAPIWrapper.DEFAULT_GEONAMES_ORG_WEBSERVICE_URL;
            }
        }
        log.info(String.format("create Geonames Client for server: %s and user: %s (token not logged)",
                serverUrl, userName));
        geonamesService = new GeonamesAPIWrapper(serverUrl, userName, token);
    }

    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        setMinScore(null);
        setMaxLocationEnhancements(null);
        setMinHierarchyScore(null);
        geonamesService = null;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        IRI contentItemId = ci.getUri();
        Graph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        //get all the textAnnotations
        /*
         * this Map holds the name as key and all the text annotations of
         * dc:type dbpedia:Place that select this name as value
         * this map is used to avoid multiple lookups for text annotations
         * selecting the same name.
         */
        Map<String, Collection<BlankNodeOrIRI>> name2placeEnhancementMap = new HashMap<String, Collection<BlankNodeOrIRI>>();
        Iterator<Triple> iterator = graph.filter(null, DC_TYPE, DBPEDIA_PLACE);
        while (iterator.hasNext()) {
            BlankNodeOrIRI placeEnhancement = iterator.next().getSubject(); //the enhancement annotating an place
            //this can still be an TextAnnotation of an EntityAnnotation
            //so we need to filter TextAnnotation
            Triple isTextAnnotation = new TripleImpl(placeEnhancement, RDF_TYPE, ENHANCER_TEXTANNOTATION);
            if (graph.contains(isTextAnnotation)) {
                //now get the name
                String name = EnhancementEngineHelper.getString(graph, placeEnhancement, ENHANCER_SELECTED_TEXT);
                if (name == null) {
                    log.warn("Unable to process TextAnnotation " + placeEnhancement
                            + " because property" + ENHANCER_SELECTED_TEXT + " is not present");
                } else {
                    Collection<BlankNodeOrIRI> placeEnhancements = name2placeEnhancementMap.get(name);
                    if (placeEnhancements == null) {
                        placeEnhancements = new ArrayList<BlankNodeOrIRI>();
                        name2placeEnhancementMap.put(name, placeEnhancements);
                    }
                    placeEnhancements.add(placeEnhancement);
                }
            } else {
                //TODO: if we also ant to process EntityAnnotations with the dc:type dbpedia:Place
                //      than we need to parse the name based on the enhancer:entity-name property
            }
        }
        //Now we do have all the names we need to lookup
        Map<SearchRequestPropertyEnum, Collection<String>> requestParams = new EnumMap<SearchRequestPropertyEnum, Collection<String>>(SearchRequestPropertyEnum.class);
        if (getMaxLocationEnhancements() != null) {
            requestParams.put(SearchRequestPropertyEnum.maxRows, Collections.singleton(getMaxLocationEnhancements().toString()));
        }
        for (Map.Entry<String, Collection<BlankNodeOrIRI>> entry : name2placeEnhancementMap.entrySet()) {
            List<Toponym> results;
            try {
                requestParams.put(SearchRequestPropertyEnum.name, Collections.singleton(entry.getKey()));
                results = geonamesService.searchToponyms(requestParams);
            } catch (Exception e) {
                /*
                     * TODO: Review if it makes sense to catch here for each name, or
                     * to catch the whole loop.
                     * This depends if single requests can result in Exceptions
                     * (e.g. because of encoding problems) or if usually Exceptions
                     * are thrown because of general things like connection issues
                     * or service unavailability.
                     */
                throw new EngineException(this, ci, e);
            }
            if (results != null) {
                Double maxScore = results.isEmpty() ? null : results.get(0).getScore();
                for (Toponym result : results) {
                    log.debug("process result {} {}",result.getGeoNameId(),result.getName());
                    Double score = getToponymScore(result,maxScore);
                    log.debug("  > score {}",score);
                    if (score != null) {
                        if (score < minScore) {
                            //if score is lower than the under bound, than stop
                            break;
                        }
                    } else {
                        log.warn("NULL returned as Score for " + result.getGeoNameId() + " " + result.getName());
                        /*
                         * NOTE: If score is not present all suggestions are
                         * added as enhancements to the metadata of the content
                         * item.
                         */
                    }
                    //write the enhancement!
                    BlankNodeOrIRI locationEnhancement = writeEntityEnhancement(
                            contentItemId, graph, literalFactory, result, entry.getValue(), null, score);
                    log.debug("  > {}  >= {}",score,minHierarchyScore);
                    if (score != null && score >= minHierarchyScore) {
                        log.debug("  > getHierarchy for {} {}",result.getGeoNameId(),result.getName());
                        //get the hierarchy
                        try {
                            Iterator<Toponym> hierarchy = getHierarchy(result).iterator();
                            for (int level = 0; hierarchy.hasNext(); level++) {
                                Toponym hierarchyEntry = hierarchy.next();
                                //TODO: filter the interesting entries
                                //  maybe add an configuration
                                if (level == 0) {
                                    //Mother earth -> ignore
                                    continue;
                                }
                                //write it as dependent to the locationEnhancement
                                if (result.getGeoNameId() != hierarchyEntry.getGeoNameId()) {
                                    //TODO: add additional checks based on possible
                                    //      configuration here!
                                    log.debug("    - write hierarchy {} {}",hierarchyEntry.getGeoNameId(),hierarchyEntry.getName());
                                    /*
                                     * The hierarchy service dose not provide a score, because it would be 1.0
                                     * so we need to set the score to this value.
                                     * Currently is is set to the value of the suggested entry
                                     */
                                    writeEntityEnhancement(contentItemId, graph, literalFactory, hierarchyEntry,
                                            null, Collections.singletonList(locationEnhancement), 1.0);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Unable to get Hierarchy for " + result.getGeoNameId() + " " + result.getName(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Getter for the socre in a range from [0..1]<p>
     * NOTE (2014.05.27, rw): as described by STANBOL-1303 the scores returned
     * by Geonames changed. So this method was adapted to calculate scores
     * relative to the highest returned one.
     *
     * @param toponym the toponym
     * @param maxScore the highest score or <code>null</code> if no highest score
     * is yet known (assuming that the parsed toponym is the highest score
     *
     * @return the score in a range [0..1] (relative to the highest score)
     */
    private Double getToponymScore(Toponym toponym, Double maxScore) {
        return toponym.getScore() == null ? null : maxScore == null ? 1 : Math.log1p(toponym.getScore())/Math.log1p(maxScore);
    }

    /**
     * Returns the hierarchy for the parsed toponym. The planet Earth will be
     * at the first position of the list and the parsed toponum represents the
     * last element of the list.
     *
     * @param toponym the toponym
     *
     * @return The list containing the hierarchy
     *
     * @throws Exception on any error while accessing the webservice
     */
    protected Collection<Toponym> getHierarchy(Toponym toponym) throws Exception {
        return geonamesService.getHierarchy(toponym.getGeoNameId());
    }

    /**
     * Writes an entity enhancement for the content item in the parsed graph
     * based on the parsed toponym.
     *
     * @param contentItemId The id of the contentItem
     * @param graph The graph used to write the triples
     * @param literalFactory the literal factory used to create literals
     * @param toponym the toponym
     * @param relatedEnhancements related enhancements
     * @param requiresEnhancements required enhancements
     * @param defaultScore the score used as default id not present. This is
     * used to parse the score of the Toponym if this method is used to add a
     * parent Toponym.
     *
     * @return The IRI of the created entity enhancement
     */
    private IRI writeEntityEnhancement(IRI contentItemId, Graph graph,
            LiteralFactory literalFactory, Toponym toponym,
            Collection<BlankNodeOrIRI> relatedEnhancements, Collection<BlankNodeOrIRI> requiresEnhancements,
            Double score) {
        IRI entityRef = new IRI("http://sws.geonames.org/" + toponym.getGeoNameId() + '/');
        FeatureClass featureClass = toponym.getFeatureClass();
        log.debug("  > featureClass " + featureClass);
        IRI entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(graph, this, contentItemId);
        // first relate this entity annotation to the text annotation(s)
        if (relatedEnhancements != null) {
            for (BlankNodeOrIRI related : relatedEnhancements) {
                graph.add(new TripleImpl(entityAnnotation, DC_RELATION, related));
            }
        }
        if (requiresEnhancements != null) {
            for (BlankNodeOrIRI requires : requiresEnhancements) {
                graph.add(new TripleImpl(entityAnnotation, DC_REQUIRES, requires));
                //STANBOL-767: also add dc:relation link
                graph.add(new TripleImpl(entityAnnotation, DC_RELATION, requires));
            }
        }
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityRef));
        log.debug("  > name " + toponym.getName());
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(toponym.getName())));
        if (score != null) {
            graph.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(score)));
        }
        //now get all the entity types for the results
        Set<IRI> entityTypes = new HashSet<IRI>();
        //first based on the feature class
        Collection<IRI> featureClassTypes = FEATURE_CLASS_CONCEPT_MAPPINGS.get(featureClass);
        if (featureClassTypes != null) {
            entityTypes.addAll(featureClassTypes);
        }
        //second for the feature Code
        String featureCode = toponym.getFeatureCode();
        Collection<IRI> featureCodeTypes = FEATURE_TYPE_CONCEPT_MAPPINGS.get(featureCode);
        if (featureCodeTypes != null) {
            entityTypes.addAll(featureCodeTypes);
        }
        //third add the feature Code as additional type
        entityTypes.add(new IRI(NamespaceEnum.geonames + featureClass.name() + '.' + featureCode));
        //finally add the type triples to the enhancement
        for (IRI entityType : entityTypes) {
            graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, entityType));
        }
        return entityAnnotation;
    }

    /**
     * Getter for the maximum number of enhancements added for a single
     * TextAnnotation.
     *
     * @return The maximum number of location enhancements added for a single
     *         text annotation
     */
    public final Integer getMaxLocationEnhancements() {
        return maxLocationEnhancements;
    }

    /**
     * Setter for the maximum number of enhancements added for a single
     * TextAnnotation. If the parsed value is <code>null</code> or < 1, than the
     * value is set to {@link LocationEnhancementEngine#DEFAULT_MAX_LOCATION_ENHANCEMENTS}.
     *
     * @param maxNumber the maximum number of enhancements added to a singel
     * text annotation
     */
    public final void setMaxLocationEnhancements(Integer maxNumber) {
        if (maxNumber == null) {
            maxNumber = DEFAULT_MAX_LOCATION_ENHANCEMENTS;
        }
        if (maxNumber < 1) {
            maxNumber = DEFAULT_MAX_LOCATION_ENHANCEMENTS;
        }
        this.maxLocationEnhancements = maxNumber;
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

    /**
     * Setter for the minimum score used to decide if results of geoname.org
     * search results are added as EntityEnhancements to the ContentItem.
     * If <code>null</code> is parsed the value is set to
     * {@link LocationEnhancementEngine#DEFAULT_MIN_SCORE}. For values > 1 the
     * value is set to 1 and for values < 0 the value is set to 0.
     *
     * @param minScore the minScore to set
     */
    public void setMinScore(Double minScore) {
        if (minScore == null) {
            minScore = DEFAULT_MIN_SCORE;
        } else if (minScore > 1) {
            minScore = Double.valueOf(1);
        } else if (minScore < 0) {
            minScore = Double.valueOf(0);
        }
        this.minScore = minScore;
    }

    /**
     * Getter for the minimum score used to decide if results of geoname.org
     * search results are added as EntityEnhancements to the ContentItem
     *
     * @return the minScore
     */
    public Double getMinScore() {
        return minScore;
    }

    /**
     * Setter for the minimum value used to decide based on the score of
     * locations returned by the geonames.org web service if also the hierarchy
     * of that point should be added as enhancements to the analysed
     * content item. <br>
     * If <code>null</code> is parsed the value is set to
     * {@link LocationEnhancementEngine#DEFAULT_MIN_HIERARCHY_SCORE}.
     * For values > 1 the value is set to 1 and for values < 0 the value
     * is set to 0.
     *
     * @param minHierarchyScore the minHierarchyScore to set
     */
    public void setMinHierarchyScore(Double minHierarchyScore) {
        if (minHierarchyScore == null) {
            minHierarchyScore = DEFAULT_MIN_HIERARCHY_SCORE;
        } else if (minHierarchyScore > 1) {
            minHierarchyScore = Double.valueOf(1);
        } else if (minScore < 0) {
            minHierarchyScore = Double.valueOf(0);
        }
        this.minHierarchyScore = minHierarchyScore;
    }

    /**
     * Setter for the minimum value used to decide based on the score of
     * locations returned by the geonames.org web service if also the hierarchy
     * of that point should be added as enhancements to the analysed
     * content item.
     *
     * @return the minHierarchyScore
     */
    public Double getMinHierarchyScore() {
        return minHierarchyScore;
    }

}
