/*
 * Copyright 2012, FORMCEPT [http://www.formcept.com]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.stanbol.enhancer.engine.disambiguation.mlt;

import static org.apache.commons.lang.StringUtils.getLevenshteinDistance;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Enhancer
 * 
 * @author Kritarth
 */
@Component(immediate = true, metatype = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "entity-disambiguator")
})
public class DisambiguatorEngine extends AbstractEnhancementEngine<IOException,RuntimeException> implements
        EnhancementEngine, ServiceProperties {

    private static Logger log = LoggerFactory.getLogger(DisambiguatorEngine.class);

    /**
     * Service URL
     */
    private String serviceURL;

    /**
     * The default value for the execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_POST_PROCESSING} + 90.
     * <p>
     * This should ensure that this engines runs as one of the first engines of the post-processing phase
     */
    public static final Integer defaultOrder = ServiceProperties.ORDERING_POST_PROCESSING - 90;
    /**
     * The plain text might be required for determining the extraction context
     */
    public static final String PLAIN_TEXT_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mime type {@link #PLAIN_TEXT_MIMETYPE}
     */
    public static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(PLAIN_TEXT_MIMETYPE);

    /**
     * Used to lookup the Entityhub {@link Site} used to perform the disambiguation.
     */
    @Reference
    protected SiteManager siteManager;

    /**
     * The {@link LiteralFactory} used to create typed RDF literals
     */
    private final LiteralFactory literalFactory = LiteralFactory.getInstance();

    /**
     * Returns the properties containing the {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING}
     */
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ContentItemHelper.getText(ci.getBlob()) == null)
                || (ContentItemHelper.getText(ci.getBlob()).trim().isEmpty())) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for " + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // default enhancement is synchronous enhancement
        return ENHANCE_SYNCHRONOUS;
    }

    /*
     * This function first evaluates all the possible ambiguations of each text annotation detected. the text
     * of all entities detected is used for making a Dbpedia query with all string for MLT that contain all
     * the other entities. The results obtained are used to calcualte new confidence values which are updated
     * in the metadata.
     */
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {

        MGraph graph = ci.getMetadata();

        List<String> allEntities = new ArrayList<String>();
        Map<SavedEntity,List<UriRef>> textAnnotations = new HashMap<SavedEntity,List<UriRef>>();
        String contentLangauge = null;

        // List to contain old confidence values that are to removed
        List<Triple> loseConfidence = new ArrayList<Triple>();
        // List to contain new confidence values to be added to metadata
        List<Triple> gainConfidence = new ArrayList<Triple>();

        ci.getLock().readLock().lock();
        try {
            contentLangauge = EnhancementEngineHelper.getLanguage(ci);
            readEntities(loseConfidence, allEntities, textAnnotations, graph);
        } catch (Exception e) {

            log.info(" readEntities" + e.getMessage());

        }
        ci.getLock().readLock().unlock();

        Site dbpediaSite = null;
        try {
            dbpediaSite = siteManager.getSite("dbpedia");

            for (Entry<SavedEntity,List<UriRef>> entry : textAnnotations.entrySet()) {

                SavedEntity savedEntity = entry.getKey();
                // the selected text of the TextAnnotation to disambiguate
                String label = savedEntity.getName();
                Collection<String> types = null; // potential types of entities
                String language = contentLangauge; // the language of the analyzed text
                List<UriRef> subsumed = entry.getValue();
                boolean casesensitive = false; //TODO: make configurable
                String savedEntityLabel = casesensitive ? label : label.toLowerCase();

                if (subsumed.size() <= 1) {
                    continue;
                }

                String extractionContext = savedEntity.getContext();

                List<String> L = EntitiesInRange(directoryTextAnotation,
                    (savedEntity.getStart() + savedEntity.getEnd()) / 2);
                // the surrounding text of the TextAnnotation
                List<String> M = getEntititesSelection(label, allEntities, extractionContext); 

                extractionContext = unionString(L, M, label);
                if (extractionContext.equals("")) extractionContext = label;
                QueryResultList<Entity> results = queryDbpedia(dbpediaSite, savedEntityLabel, language,
                    extractionContext);
                log.info(" - {} results returned by query {}", results.size(), results.getQuery());

                List<Suggestion> matches = rankResults(results, casesensitive, language, savedEntityLabel);
                Collections.sort(matches);

                ci.getLock().readLock().lock();
                try {
                    if (intersectionCheck(matches, subsumed, graph, contentLangauge)) {
                        gainConfidence = intersection(matches, subsumed, graph, gainConfidence,
                            contentLangauge);
                    } else {
                        loseConfidence = unchangedConfidences(subsumed, graph, loseConfidence);
                    }
                } finally {
                    ci.getLock().readLock().unlock();
                }
            }
            ci.getLock().writeLock().lock();
            try {
                removeOldConfidenceFromGraph(graph, loseConfidence);

                addNewConfidenceToGraph(graph, gainConfidence);
            } finally {
                ci.getLock().writeLock().unlock();
            }

        }

        catch (Exception e) {
            log.info("Error " + e.getMessage());
            log.info("Error " + e.getStackTrace());

        }
    }

    /*
     * We create a data structure that stores the mapping of text annotation to List of Uri of all possible
     * amiguations of the Text. Also it fills the list loseconfidence with confidence values of all the
     * ambiguations for all entities (which will be removed eventually)
     */
    protected void readEntities(List<Triple> loseConfidence,
                                List<String> allEntities,
                                Map<SavedEntity,List<UriRef>> textAnnotations,
                                MGraph graph) {
        Iterator<Triple> it = graph.filter(null, RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION);
        while (it.hasNext()) {
            UriRef uri = (UriRef) it.next().getSubject();
            String selectText = EnhancementEngineHelper.getString(graph, uri, ENHANCER_SELECTED_TEXT);
// TODO: rwesten: do we really want to ignore fise:TextAnnotations that link to
//       to an other one (typically two TextAnnotations that select the exact same text)
//            if (graph.filter(uri, new UriRef(NamespaceEnum.dc + "relation"), null).hasNext()) {
//                continue;
//            }

            SavedEntity savedEntity = SavedEntity.createFromTextAnnotation(graph, uri);
            if (savedEntity != null) {
                allEntities.add(selectText);
                directoryTextAnotation.put(
                    (savedEntity.getStart()+savedEntity.getEnd()) / 2, selectText);

                List<UriRef> confidenceUriList = new ArrayList<UriRef>();
                for (Iterator<Triple> it2 = graph
                        .filter(null, new UriRef(NamespaceEnum.dc + "relation"), uri); it2.hasNext();) {
                    UriRef uriAmbiguations = (UriRef) it2.next().getSubject();
                    Iterator<Triple> confidenceTriple = graph.filter(uriAmbiguations, ENHANCER_CONFIDENCE,
                        null);
                    while (confidenceTriple.hasNext()) {
                        loseConfidence.add(confidenceTriple.next());
                    }

                    // UriRef textAnnotation = uri; //the URI of the processed TextAnnotation
                    // UriRef entityAnnotation = uriAmbiguations; //the URI of the original EntityAnnotation
                    // UriRef copy = new UriRef("urn:enhancement-"
                    // + EnhancementEngineHelper.randomUUID());

                    // List<Triple> triples = new ArrayList<Triple>();
                    // it = graph.filter(entityAnnotation,null,null);
                    // int refCount = 0;
                    // while(it.hasNext()){
                    // Triple triple = it.next();
                    // if(DC_RELATION.equals(triple.getPredicate())){
                    // refCount++;
                    // }
                    // if(triple!=null)
                    // triples.add(triple);
                    // }
                    /*
                     * if(refCount > 1){ for(Triple triple : triples){
                     * /*if(DC_RELATION.equals(triple.getPredicate())){
                     * if(triple.getObject().equals(textAnnotation)){ //remove the dc relation to the
                     * currently processed //textAnnotation from the original //graph.remove(triple); //
                     * RemoveConf.add(triple); //and add it to the copy // graph.add(new TripleImpl( // copy,
                     * triple.getPredicate(), triple.getObject())); //AddConf.add(triple); } //else it is not
                     * the currently processed TextAnnotation // so we need to keep in in the original and NOT
                     * add // it to the copy } else { //we can copy all other information 1:1 // graph.add(new
                     * TripleImpl(copy,triple.getPredicate(), triple.getObject())); //AddConf.add(triple); } }
                     * }
                     */

                    confidenceUriList.add(uriAmbiguations);

                }
                textAnnotations.put(savedEntity, confidenceUriList);
            }
        }
        return;
    }

    /*
     * Is used to query the Dbpedia with a entity as main constraint and then add string of all other entities
     * detected as similarity constraints
     */

    protected QueryResultList<Entity> queryDbpedia(Site dbpediaSite,
                                                   String savedEntityLabel,
                                                   String language,
                                                   String extractionContext) throws SiteException {

        FieldQuery query = dbpediaSite.getQueryFactory().createFieldQuery();
        Constraint labelConstraint;
        if (language != null) {
            labelConstraint = new TextConstraint(savedEntityLabel, false, language, null);
        } else {
            labelConstraint = new TextConstraint(savedEntityLabel, false);
        }

        query.setConstraint(RDFS_LABEL.getUnicodeString(), labelConstraint);
        log.info("Init SavedEntityTaggingEngine instance for the Entityhub");
        query.setConstraint("http://stanbol.apache.org/ontology/entityhub/query#fullText",
            new SimilarityConstraint(extractionContext));
        // query.setLimit(Math.max(20,9));

        return dbpediaSite.findEntities(query);
    }

    /*
     * If for an entity the Dbpedia query results in suggestion none of which match the already present
     * ambiguations, we go with the ambiguations found earlier that is the ones we have with.
     */

    protected List<Triple> unchangedConfidences(List<UriRef> subsumed,
                                                MGraph graph,
                                                List<Triple> loseConfidence) {
        for (int i = 0; i < subsumed.size(); i++) {
            UriRef uri = subsumed.get(i);
            Iterator<Triple> confidenceTriple = graph.filter(uri, ENHANCER_CONFIDENCE, null);
            while (confidenceTriple.hasNext()) {
                loseConfidence.remove(confidenceTriple.next());
            }
        }
        return loseConfidence;
    }

    /* To convert the values from Dbpedia into score values. */
    protected List<Suggestion> rankResults(QueryResultList<Entity> results,
                                           boolean casesensitive,
                                           String language,
                                           String savedEntityLabel) {
        List<Suggestion> matches = new ArrayList<Suggestion>(results.size());
        Float maxScore = null;
        Float maxExactScore = null;

        for (Iterator<Entity> guesses = results.iterator(); guesses.hasNext();) {
            Suggestion match = new Suggestion(guesses.next());
            Representation rep = match.getEntity().getRepresentation();
            Float score = rep.getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
            match.setURI(rep.getId());
            if (maxScore == null) {
                maxScore = score;
            }
            Iterator<Text> labels = rep.getText(RDFS_LABEL.getUnicodeString());
            while (labels.hasNext() && match.getLevenshtein() < 1.0) {
                Text label1 = labels.next();
                if (language == null || // if the content language is unknown -> accept all labels
                    label1.getLanguage() == null || // accept labels with no language
                    // and labels in the same language as the content
                    (language != null && label1.getLanguage().startsWith(language))) {
                    double actMatch = levenshtein(
                        casesensitive ? label1.getText().toLowerCase() : label1.getText(), savedEntityLabel);
                    if (actMatch > match.getLevenshtein()) {
                        match.setLevenshtein(actMatch);
                        // JOptionPane.showMessageDialog(null, "++"+label1);
                        match.setMatchedLabel(label1);
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
                log.info("No value of {} for Entity {}!", RDFS_LABEL.getUnicodeString(), match.getEntity()
                        .getId());
            }
        }
        return matches;
    }

    /*
     * Checks if there is any common elements amongst the ambiguations amongst latest dbpedia query and intial
     * ambiguations
     */

    protected boolean intersectionCheck(List<Suggestion> matches,
                                        List<UriRef> subsumed,
                                        MGraph graph,
                                        String contentLangauge) {
        for (int i = 0; i < subsumed.size(); i++) {
            UriRef uri = subsumed.get(i);

            UriRef uri1 = EnhancementEngineHelper.getReference(graph, uri, new UriRef(NamespaceEnum.fise
                                                                                      + "entity-reference"));

            String selectedText = EnhancementEngineHelper.getString(graph, uri, ENHANCER_ENTITY_LABEL);

            if (selectedText == null) {
                continue;
            }

            for (int j = 0; j < matches.size(); j++) {
                Suggestion suggestion = matches.get(j);
                String suggestName = suggestion.getURI();
                if (suggestName.compareToIgnoreCase(uri1.getUnicodeString()) == 0) return true;
            }
        }
        return false;
    }

    public Map<Integer,String> directoryTextAnotation = new HashMap<Integer,String>();

    int radii = 23;

    // Value to be configured

    public boolean toInclude(int k, int s) {
        if (Math.abs(k - s) < radii && Math.abs(k - s) > 0) {
            return true;
        }
        return false;
    }

    public List<String> EntitiesInRange(Map<Integer,String> map, int radius) {
        List<String> temp = new ArrayList<String>();

        for (Entry<Integer,String> entry : map.entrySet()) {
            Integer s = entry.getKey();
            String subs = entry.getValue();
            if (toInclude(s, radius)) {
                temp.add(subs);
            }
        }

        return temp; // if(Cal(f,k))
    }

    /* Returns a string on appended text annotations seperated by spaces */
    protected List<String> getEntititesSelection(String label, List<String> allEntities, String context) {
        List<String> allEntityString = new ArrayList<String>();

        for (int i = 0; i < allEntities.size(); i++) {

            if (label.compareToIgnoreCase(allEntities.get(i)) != 0 && (context != null)
                && (context.contains(allEntities.get(i)))) {
                allEntityString.add(allEntities.get(i));

            }

        }

        return allEntityString;
    }

    public String unionString(List<String> a, List<String> b, String h) {
        Set union = new HashSet();

        union.addAll(a);
        union.addAll(b);
        String AllString = "";

        Object[] temp = union.toArray();
        for (int i = 0; i < temp.length; i++) {
            AllString = AllString + " " + (String) temp[i];
        }
        return AllString;
    }

    /*
     * Finds values the lie in intersection of both the set of disambiguations( the one intially suggested and
     * the one from dpedia). Update the confidence values of those and make the confidence values of others as
     * 0 in gainconfidence list
     */
    protected List<Triple> intersection(List<Suggestion> matches,
                                        List<UriRef> subsumed,
                                        MGraph graph,
                                        List<Triple> gainConfidence,
                                        String contentLangauge) {

        for (int i = 0; i < subsumed.size(); i++) {
            boolean matchFound = false;
            UriRef uri = subsumed.get(i);

            UriRef uri1 = EnhancementEngineHelper.getReference(graph, uri, new UriRef(NamespaceEnum.fise
                                                                                      + "entity-reference"));

            for (int j = 0; j < matches.size(); j++) {
                Suggestion suggestion = matches.get(j);
                String suggestName = suggestion.getURI();

                if (suggestName != null && uri1 != null
                    && suggestName.compareToIgnoreCase(uri1.getUnicodeString()) == 0) {
                    Triple confidenceTriple = new TripleImpl(uri, ENHANCER_CONFIDENCE, LiteralFactory
                            .getInstance().createTypedLiteral(suggestion.getScore()));
                    Triple contributorTriple = new TripleImpl((UriRef) confidenceTriple.getSubject(),
                            new UriRef(NamespaceEnum.dc + "contributor"), LiteralFactory.getInstance()
                                    .createTypedLiteral(this.getClass().getName()));
                    gainConfidence.add(confidenceTriple);
                    gainConfidence.add(contributorTriple);
                    matchFound = true;
                }
            }

            if (!matchFound) {
                Triple confidenceTriple = new TripleImpl(uri, ENHANCER_CONFIDENCE, LiteralFactory
                        .getInstance().createTypedLiteral(0.0));
                Triple contributorTriple = new TripleImpl((UriRef) confidenceTriple.getSubject(), new UriRef(
                        NamespaceEnum.dc + "contributor"), LiteralFactory.getInstance().createTypedLiteral(
                    this.getClass().getName()));
                gainConfidence.add(confidenceTriple);
                gainConfidence.add(contributorTriple);
            }
        }

        return gainConfidence;
    }

    /* Removes the value in lose confidence from the graph */
    protected void removeOldConfidenceFromGraph(MGraph graph, List<Triple> loseConfidence) {
        for (int i = 0; i < loseConfidence.size(); i++) {
            Triple elementToRemove = loseConfidence.get(i);
            graph.remove(elementToRemove);
        }
    }

    /* adds the confidence values from gain confidence list to graph */
    protected void addNewConfidenceToGraph(MGraph graph, List<Triple> gainConfidence) {
        for (int i = 0; i < gainConfidence.size(); i++) {
            Triple elementToAdd = gainConfidence.get(i);
            graph.add(elementToAdd);
        }
    }

    /* Returns a string on appended text annotations seperated by spaces */
    protected String getEntitiesfromContext(String label, List<String> allEntities, String context) {
        String allEntityString = "";

        for (int i = 0; i < allEntities.size(); i++) {

            if (label.compareToIgnoreCase(allEntities.get(i)) != 0 && (context != null)
                && (context.contains(allEntities.get(i)))) {
                allEntityString = allEntityString + "  " + allEntities.get(i);
            }

        }

        return allEntityString;
    }

    protected String deriveSentence(String Context, int a, int b) {
        String allEntityString = "";
        String start = Context.substring(0, a);
        String end = Context.substring(b);
        int s = start.lastIndexOf('.');
        int e = end.indexOf('.');
        if (s < 0) {
            if (e < 0) return Context;
            else return Context.substring(0, b + e);
        } else {
            if (e < 0) return Context.substring(s);
            else return Context.substring(s + 1, b + e);
        }

    }

    /**
     * Activate and read the properties
     * 
     * @param ce
     *            the {@link ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException {
        try {
            super.activate(ce);
        } catch (IOException e) {
            // log
            log.error("Failed to update the configuration", e);
        }
        @SuppressWarnings("unchecked")
        Dictionary<String,Object> properties = ce.getProperties();
        // update the service URL if it is defined
        // if (properties.get(FORMCEPT_SERVICE_URL) != null) {
        // this.serviceURL = (String) properties.get(FORMCEPT_SERVICE_URL);
        // }
    }

    /**
     * Deactivate
     * 
     * @param ce
     *            the {@link ComponentContext}
     */
    @Deactivate
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
    }

    /**
     * Gets the Service URL
     * 
     * @return
     */
    public String getServiceURL() {
        return serviceURL;
    }

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