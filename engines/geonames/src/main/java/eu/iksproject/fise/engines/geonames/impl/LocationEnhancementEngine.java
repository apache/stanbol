package eu.iksproject.fise.engines.geonames.impl;

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
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.EngineException;
import eu.iksproject.fise.servicesapi.EnhancementEngine;
import eu.iksproject.fise.servicesapi.ServiceProperties;
import eu.iksproject.fise.servicesapi.helper.EnhancementEngineHelper;
import eu.iksproject.fise.servicesapi.rdf.NamespaceEnum;

import static eu.iksproject.fise.servicesapi.rdf.NamespaceEnum.dbpedia_ont;
import static eu.iksproject.fise.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static eu.iksproject.fise.servicesapi.rdf.Properties.*;
import static eu.iksproject.fise.servicesapi.rdf.TechnicalClasses.FISE_TEXTANNOTATION;

@Component(immediate = true, metatype = true)
@Service
//@Property(name="service.ranking",intValue=5)
public class LocationEnhancementEngine implements EnhancementEngine, ServiceProperties {

    
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    /**
     * This maps geonames.org feature classes to dbPedia.org ontology classes
     */
    public static final Map<FeatureClass, Collection<UriRef>> FEATURE_CLASS_CONCEPT_MAPPINGS;

    public static final Map<String, Collection<UriRef>> FEATURE_TYPE_CONCEPT_MAPPINGS;

    private static final Logger log = LoggerFactory.getLogger(EnhancementEngineHelper.class);

    /**
     * Default value for minimum scores of search results are added to the
     * metadata of the parsed ContentItem
     */
    private static final double DEFAULT_MIN_SCORE = 0.33;

    @Property(doubleValue=DEFAULT_MIN_SCORE)
    public static final String MIN_SCORE = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.min-score";

    /**
     * Default values for the number of results returned by search requests
     * to the geonames.org web service
     */
    private static final int DEFAULT_MAX_LOCATION_ENHANCEMENTS = 5;

    @Property(intValue=DEFAULT_MAX_LOCATION_ENHANCEMENTS)
    public static final String MAX_LOCATION_ENHANCEMENTS = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.max-location-enhancements";

    /**
     * Default value for the minimum score of search results used to also add
     * the hierarchy
     */
    private static final double DEFAULT_MIN_HIERARCHY_SCORE = 0.70;

    @Property(doubleValue=DEFAULT_MIN_HIERARCHY_SCORE)
    public static final String MIN_HIERARCHY_SCORE = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.min-hierarchy-score";

    public static final UriRef CONCEPT_GEONAMES_FEATURE = new UriRef(NamespaceEnum.geonames.toString() + "Feature");
    @Property(value=GeonamesAPIWrapper.GEONAMES_ORG_WEBSERVICE_URL)
    public static final String GEONAMES_SERVER_URL = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.serverURL";
    @Property
    public static final String GEONAMES_USERNAME = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.username";
    @Property
    public static final String GEONAMES_TOKEN = "eu.iksproject.fise.engines.geonames.locationEnhancementEngine.token";
    
    /**
     * The geonames.org API wrapper used to make service requests
     */
    protected GeonamesAPIWrapper geonamesService;

    static {
        Map<FeatureClass, Collection<UriRef>> mappings = new EnumMap<FeatureClass, Collection<UriRef>>(FeatureClass.class);
        //first add the concepts of the geonames ontology
        for (FeatureClass fc : FeatureClass.values()) {
            List<UriRef> conceptMappings = new ArrayList<UriRef>();
            conceptMappings.add(CONCEPT_GEONAMES_FEATURE); //all things are features
            conceptMappings.add(DBPEDIA_PLACE); //all things are dbPedia places
            mappings.put(fc, conceptMappings);
        }
        //now add additional mappings to the dbPedia Ontology
        UriRef populatedPlace = new UriRef(dbpedia_ont + "PopulatedPlace");
        mappings.get(FeatureClass.P).addAll(Arrays.asList(populatedPlace, new UriRef(dbpedia_ont + "Settlement")));
        mappings.get(FeatureClass.A).addAll(Arrays.asList(populatedPlace, new UriRef(dbpedia_ont + "AdministrativeRegion")));
        mappings.get(FeatureClass.H).add(new UriRef(dbpedia_ont + "BodyOfWater"));
        mappings.get(FeatureClass.R).add(new UriRef(dbpedia_ont + "Infrastructure"));
        mappings.get(FeatureClass.S).add(new UriRef(dbpedia_ont + "Building"));
        mappings.get(FeatureClass.T).add(new UriRef(dbpedia_ont + "Mountain"));
        //now write the unmodifiable static final constant
        FEATURE_CLASS_CONCEPT_MAPPINGS = Collections.unmodifiableMap(mappings);

        //Mappings for known FeatureTypes
        Map<String, Collection<UriRef>> typeMappings = new HashMap<String, Collection<UriRef>>();
        Collection<UriRef> lakeTypes = Arrays.asList(new UriRef(dbpedia_ont + "Lake"));
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

        UriRef stream = new UriRef(dbpedia_ont + " Stream");
        Collection<UriRef> canalTypes = Arrays.asList(stream, new UriRef(dbpedia_ont + "Canal"));
        typeMappings.put("H.CNL", canalTypes);
        typeMappings.put("H.CNLA", canalTypes);
        typeMappings.put("H.CNLB", canalTypes);
        typeMappings.put("H.CNLI", canalTypes);
        typeMappings.put("H.CNLD", canalTypes);
        typeMappings.put("H.CNLSB", canalTypes);
        typeMappings.put("H.CNLN", canalTypes);
        typeMappings.put("H.CNLQ", canalTypes);
        typeMappings.put("H.CNLX", canalTypes);

        Collection<UriRef> riverTypes = Arrays.asList(stream, new UriRef(dbpedia_ont + "River"));
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

        Collection<UriRef> caveTypes = Arrays.asList(new UriRef(dbpedia_ont + "Cave"));
        typeMappings.put("H.LKSB", caveTypes);
        typeMappings.put("R.TNLN", caveTypes);
        typeMappings.put("S.CAVE", caveTypes);
        typeMappings.put("S.BUR", caveTypes);

        Collection<UriRef> countryTypes = Arrays.asList(new UriRef(dbpedia_ont + "Country"));
        typeMappings.put("A.PCLI", countryTypes);

        UriRef settlement = new UriRef(dbpedia_ont + "Settlement");
        Collection<UriRef> cityTypes = Arrays.asList(settlement, new UriRef(dbpedia_ont + "City"));
        Collection<UriRef> villageTypes = Arrays.asList(settlement, new UriRef(dbpedia_ont + "Village"));
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
    protected void activate(ComponentContext ce) throws IOException {
        Dictionary<String, Object> properties = ce.getProperties();
        log.debug("activating ...");
        //NOTE: The type of the values is ensured by the default values in the
        //      @Property annotations e.g. doubleValue -> Double
        setMinScore((Double)properties.get(MIN_SCORE));
        setMaxLocationEnhancements((Integer)properties.get(MAX_LOCATION_ENHANCEMENTS));
        setMinHierarchyScore((Double)properties.get(MIN_HIERARCHY_SCORE));
        String serverUrl = (String)properties.get(GEONAMES_SERVER_URL);
        if(serverUrl != null && serverUrl.isEmpty()){
            serverUrl = null; //prevent empty serverURLs (e.g. if the user deletes an value)
        }
        String userName = (String)properties.get(GEONAMES_USERNAME);
        String token = (String)properties.get(GEONAMES_TOKEN);
        geonamesService = new GeonamesAPIWrapper(serverUrl, userName, token);
    }

    protected void deactivate(ComponentContext ce) {
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
        UriRef contentItemId = new UriRef(ci.getId());
        MGraph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        //get all the textAnnotations
        /*
         * this Map holds the name as key and all the text annotations of
         * dc:type dbPedia:Place that select this name as value
         * this map is used to avoid multiple lookups for text annotations
         * selecting the same name.
         */
        Map<String, Collection<NonLiteral>> name2placeEnhancementMap = new HashMap<String, Collection<NonLiteral>>();
        Iterator<Triple> iterator = graph.filter(null, DC_TYPE, DBPEDIA_PLACE);
        while (iterator.hasNext()) {
            NonLiteral placeEnhancement = iterator.next().getSubject(); //the enhancement annotating an place
            //this can still be an TextAnnotation of an EntityAnnotation
            //so we need to filter TextAnnotation
            Triple isTextAnnotation = new TripleImpl(placeEnhancement, RDF_TYPE, FISE_TEXTANNOTATION);
            if (graph.contains(isTextAnnotation)) {
                //now get the name
                String name = EnhancementEngineHelper.getString(graph, placeEnhancement, FISE_SELECTED_TEXT);
                if (name == null) {
                    log.warn("Unable to process TextAnnotation " + placeEnhancement
                            + " because property" + FISE_SELECTED_TEXT + " is not present");
                } else {
                    Collection<NonLiteral> placeEnhancements = name2placeEnhancementMap.get(name);
                    if (placeEnhancements == null) {
                        placeEnhancements = new ArrayList<NonLiteral>();
                        name2placeEnhancementMap.put(name, placeEnhancements);
                    }
                    placeEnhancements.add(placeEnhancement);
                }
            } else {
                //TODO: if we also ant to process EntityAnnotations with the dc:type dbPedia:Place
                //      than we need to parse the name based on the fise:entity-name property
            }
        }
        //Now we do have all the names we need to lookup
        for (Map.Entry<String, Collection<NonLiteral>> entry : name2placeEnhancementMap.entrySet()) {
            List<Toponym> results;
            try {
                results = geonamesService.searchToponyms(entry.getKey());
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
                for (Toponym result : results) {
                    log.info("process result " + result.getGeoNameId() + " " + result.getName());
                    Double score = getToponymScore(result);
                    log.info("  > score " + score);
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
                    NonLiteral locationEnhancement = writeEntityEnhancement(
                        contentItemId, graph, literalFactory, result, entry.getValue(), null,null);
                    log.info("  > " + score + " >= " + minHierarchyScore);
                    if (score != null && score >= minHierarchyScore) {
                        log.info("  > getHierarchy for " + result.getGeoNameId() + " " + result.getName());
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
                                    log.info("    - write hierarchy " + hierarchyEntry.getGeoNameId() + " " + hierarchyEntry.getName());
                                    /*
                                     * The hierarchy service dose not provide a score, because it would be 1.0
                                     * so we need to set the score to this value.
                                     * Currently is is set to the value of the suggested entry
                                     */
                                    writeEntityEnhancement(contentItemId, graph, literalFactory, hierarchyEntry,
                                            null, Collections.singletonList(locationEnhancement),score);
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
     * NOTE (2010.11.16, rw): GeoNames previously returned the score in the
     * range from [0..1]. It looks like that up from now they use the
     * range [0..100]. Therefore I created this method to make the necessary
     * adaptation.
     * see also http://code.google.com/p/iks-project/issues/detail?id=89
     *
     * @param toponym the toponym
     * @return the score in a range [0..1]
     * @throws InsufficientStyleException
     */
    private Double getToponymScore(Toponym toponym){
        return toponym.getScore() == null ? null : toponym.getScore() / 100;
    }

    /**
     * Returns the hierarchy for the parsed toponym. The planet Earth will be
     * at the first position of the list and the parsed toponum represents the
     * last element of the list
     *
     * @param toponym the toponym
     * @return The list containing the hierarchy
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
     * @return The UriRef of the created entity enhancement
     */
    private UriRef writeEntityEnhancement(UriRef contentItemId, MGraph graph,
            LiteralFactory literalFactory, Toponym toponym,
            Collection<NonLiteral> relatedEnhancements, Collection<NonLiteral> requiresEnhancements,
            Double defaultScore) {
        UriRef entityRef = new UriRef("http://sws.geonames.org/" + toponym.getGeoNameId() + '/');
        FeatureClass featureClass = toponym.getFeatureClass();
        log.debug("  > featureClass " + featureClass);
        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(graph, this, contentItemId);
        // first relate this entity annotation to the text annotation(s)
        if (relatedEnhancements != null) {
            for (NonLiteral related : relatedEnhancements) {
                graph.add(new TripleImpl(entityAnnotation, DC_RELATION, related));
            }
        }
        if (requiresEnhancements != null) {
            for (NonLiteral requires : requiresEnhancements) {
                graph.add(new TripleImpl(entityAnnotation, DC_REQUIRES, requires));
            }
        }
        graph.add(new TripleImpl(entityAnnotation, FISE_ENTITY_REFERENCE, entityRef));
        log.debug("  > name " + toponym.getName());
        graph.add(new TripleImpl(entityAnnotation, FISE_ENTITY_LABEL, literalFactory.createTypedLiteral(toponym.getName())));
        Double score = getToponymScore(toponym);
        if(score == null){ //use the default score as fallback
            score = defaultScore;
        }
        if (score != null) {
            graph.add(new TripleImpl(entityAnnotation, FISE_CONFIDENCE, literalFactory.createTypedLiteral(score)));
        }
        //now get all the entity types for the results
        Set<UriRef> entityTypes = new HashSet<UriRef>();
        //first based on the feature class
        Collection<UriRef> featureClassTypes = FEATURE_CLASS_CONCEPT_MAPPINGS.get(featureClass);
        if (featureClassTypes != null) {
            entityTypes.addAll(featureClassTypes);
        }
        //second for the feature Code
        String featureCode = toponym.getFeatureCode();
        Collection<UriRef> featureCodeTypes = FEATURE_TYPE_CONCEPT_MAPPINGS.get(featureCode);
        if (featureCodeTypes != null) {
            entityTypes.addAll(featureCodeTypes);
        }
        //third add the feature Code as additional type
        entityTypes.add(new UriRef(NamespaceEnum.geonames + featureClass.name() + '.' + featureCode));
        //finally add the type triples to the enhancement
        for (UriRef entityType : entityTypes) {
            graph.add(new TripleImpl(entityAnnotation, FISE_ENTITY_TYPE, entityType));
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
