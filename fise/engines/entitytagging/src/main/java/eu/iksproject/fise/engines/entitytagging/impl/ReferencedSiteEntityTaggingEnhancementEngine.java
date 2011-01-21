package eu.iksproject.fise.engines.entitytagging.impl;

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
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
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

import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.EngineException;
import eu.iksproject.fise.servicesapi.EnhancementEngine;
import eu.iksproject.fise.servicesapi.EnhancementJobManager;
import eu.iksproject.fise.servicesapi.ServiceProperties;
import eu.iksproject.fise.servicesapi.helper.EnhancementEngineHelper;
import eu.iksproject.fise.servicesapi.rdf.OntologicalClasses;
import eu.iksproject.fise.servicesapi.rdf.Properties;
import eu.iksproject.fise.servicesapi.rdf.TechnicalClasses;

import static eu.iksproject.fise.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static eu.iksproject.fise.servicesapi.rdf.Properties.DC_TYPE;
import static eu.iksproject.fise.servicesapi.rdf.Properties.FISE_SELECTED_TEXT;
import static eu.iksproject.fise.servicesapi.rdf.Properties.RDF_TYPE;

/**
 * Engine that uses a {@link ReferencedSite} to search for entities for
 * existing TextAnnotations of an Content Item.
 *
 * @author ogrisel, rwesten
 */
@Component(getConfigurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE, //the baseUri is required!
        specVersion = "1.1",
        metatype = true,
        immediate = true)
@Service
public class ReferencedSiteEntityTaggingEnhancementEngine implements EnhancementEngine,
        ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value="dbPedia")
    public static final String REFERENCED_SITE_ID = "eu.iksprojet.fise.engines.entitytagging.referencedSiteId";
    @Property(boolValue=true)
    public static final String PERSON_STATE = "eu.iksprojet.fise.engines.entitytagging.personState";
    @Property(value="dbp-ont:Person")
    public static final String PERSON_TYPE = "eu.iksprojet.fise.engines.entitytagging.personType";
    @Property(boolValue=true)
    public static final String ORG_STATE = "eu.iksprojet.fise.engines.entitytagging.organisationState";
    @Property(value="dbp-ont:Organisation")
    public static final String ORG_TYPE = "eu.iksprojet.fise.engines.entitytagging.organisationType";
    @Property(boolValue=true)
    public static final String PLACE_STATE = "eu.iksprojet.fise.engines.entitytagging.placeState";
    @Property(value="dbp-ont:Place")
    public static final String PLACE_TYPE = "eu.iksprojet.fise.engines.entitytagging.placeType";
    @Property(value="rdfs:label")
    public static final String NAME_FIELD = "eu.iksprojet.fise.engines.entitytagging.nameField";

    /**
     * Service of the RICK that manages all the active referenced Site.
     * This Service is used to lookup the configured Referenced Site when we
     * need to enhance a content item.
     */
    @Reference
    protected ReferencedSiteManager siteManager;
    /**
     * This is the configured name of the referenced Site used to find entities.
     * The {@link ReferencedSiteManager} service of the RICK is used to
     * get the actual {@link ReferencedSite} instance for each request to this
     * Engine.
     */
    protected String referencedSiteID;
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;
    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_PERSON}
     * are enhanced by this engine
     */
    protected boolean personState;

    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_ORGANISATION}
     * are enhanced by this engine
     */
    protected boolean orgState;

    /**
     * State if text annotations of type {@link OntologicalClasses#DBPEDIA_PLACE}
     * are enhanced by this engine
     */
    protected boolean placeState;
    /**
     * The rdf:type constraint used to search for persons or <code>null</code>
     * if no type constraint should be used
     */
    protected String personType;

    /**
     * The rdf:type constraint used to search for organisations or <code>null</code>
     * if no type constraint should be used
     */
    protected String orgType;

    /**
     * The rdf:type constraint used to search for places or <code>null</code>
     * if no type constraint should be used
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

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        Dictionary<String, Object> config = context.getProperties();
        Object referencedSiteID = config.get(REFERENCED_SITE_ID);
        if (referencedSiteID == null) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }

        this.referencedSiteID = referencedSiteID.toString();
        if (this.referencedSiteID.isEmpty()){
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        Object state = config.get(PERSON_STATE);
        personState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(ORG_STATE);
        orgState = state == null ? true : Boolean.parseBoolean(state.toString());
        state = config.get(PLACE_STATE);
        placeState = state == null ? true : Boolean.parseBoolean(state.toString());
        Object type = config.get(PERSON_TYPE);
        personType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type.toString());
        type = config.get(ORG_TYPE);
        orgType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type.toString());
        type = config.get(PLACE_TYPE);
        placeType = type == null || type.toString().isEmpty() ? null : NamespaceEnum.getFullName(type.toString());
        Object nameField = config.get(NAME_FIELD);
        this.nameField = nameField == null || nameField.toString().isEmpty() ? NamespaceEnum.rdfs+"label" : NamespaceEnum.getFullName(nameField.toString());
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
        ReferencedSite site = siteManager.getReferencedSite(referencedSiteID);
        if(site == null){
            String msg = String.format("Unable to enhance %s because Referenced Site %s is currently not active!",
                    ci.getId(), referencedSiteID);
            log.warn(msg);
            //TODO: throwing Exceptions is currently deactivated. We need a more clear
            //policy what do to in such situations
            //throw new EngineException(msg);
            return;
        }
        UriRef contentItemId = new UriRef(ci.getId());

        MGraph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        // Retrieve the existing text annotations
        Map<UriRef, List<UriRef>> textAnnotations = new HashMap<UriRef, List<UriRef>>();
        for (Iterator<Triple> it = graph.filter(null, RDF_TYPE,
                TechnicalClasses.FISE_TEXTANNOTATION); it.hasNext();) {
            UriRef uri = (UriRef) it.next().getSubject();
            if (graph.filter(uri, Properties.DC_RELATION, null).hasNext()) {
                // this is not the most specific occurrence of this name: skip
                continue;
            }
            // This is a first occurrence, collect any subsumed annotations
            List<UriRef> subsumed = new ArrayList<UriRef>();
            for (Iterator<Triple> it2 = graph.filter(null,
                    Properties.DC_RELATION, uri); it2.hasNext();) {
                subsumed.add((UriRef) it2.next().getSubject());
            }
            textAnnotations.put(uri, subsumed);
        }

        for (Map.Entry<UriRef, List<UriRef>> entry : textAnnotations.entrySet()) {
            try {
                computeEntityRecommentations(site, literalFactory, graph,
                        contentItemId, entry.getKey(), entry.getValue());
            } catch (ReferencedSiteException e) {
                throw new EngineException(this, ci, e);
            }
        }
    }

    protected final Iterable<Sign> computeEntityRecommentations(
            ReferencedSite site, LiteralFactory literalFactory, MGraph graph,
            UriRef contentItemId, UriRef textAnnotation,
            List<UriRef> subsumedAnnotations) throws ReferencedSiteException {
        // First get the required properties for the parsed textAnnotation
        // ... and check the values
        String name = EnhancementEngineHelper.getString(graph, textAnnotation,
                FISE_SELECTED_TEXT);
        if (name == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation
                    + " because property" + FISE_SELECTED_TEXT
                    + " is not present");
            return Collections.emptyList();
        }

        UriRef type = EnhancementEngineHelper.getReference(graph,
                textAnnotation, DC_TYPE);
        if (type == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation
                    + " because property" + DC_TYPE + " is not present");
            return Collections.emptyList();
        }

        log.debug("Process TextAnnotation " + name + " type=" + type);
        FieldQuery query = site.getQueryFactory().createFieldQuery();
        //replace spaces with plus to create an AND search for all words in the name!
        query.setConstraint(nameField, new TextConstraint(name.replace(' ', '+')));
        if (OntologicalClasses.DBPEDIA_PERSON.equals(type)){
            if (personState){
                if (personType!=null){
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(personType));
                } // else no type constraint
            } else { //ignore people
                return Collections.emptyList();
            }
        } else if (DBPEDIA_ORGANISATION.equals(type)){
            if (orgState){
                if (orgType!=null){
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(orgType));
                } // else no type constraint
            } else { //ignore people
                return Collections.emptyList();
            }
        } else if(OntologicalClasses.DBPEDIA_PLACE.equals(type)){
            if (this.placeState){
                if (this.placeType!=null){
                    query.setConstraint(RDF_TYPE.getUnicodeString(), new ReferenceConstraint(placeType));
                } // else no type constraint
            } else { //ignore people
                return Collections.emptyList();
            }
        }
        query.setLimit(this.numSuggestions);
        QueryResultList<Sign> results = site.findSigns(query);

        List<NonLiteral> annotationsToRelate = new ArrayList<NonLiteral>();
        annotationsToRelate.add(textAnnotation);
        annotationsToRelate.addAll(subsumedAnnotations);

        for (Sign guess : results) {
            EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory,
                    graph, contentItemId, annotationsToRelate, guess);
        }
        return results;
    }

    public int canEnhance(ContentItem ci) {
        /*
         * This engine consumes existing enhancements because of that it can
         * enhance any type of ci! TODO: It would also be possible to check here
         * if there is an TextAnnotation and use that as result!
         */
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }
}
