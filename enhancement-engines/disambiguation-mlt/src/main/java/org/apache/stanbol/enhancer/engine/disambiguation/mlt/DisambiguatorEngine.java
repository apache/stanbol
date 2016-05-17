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

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.defaults.SpecialFieldEnum;
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
 * Disambiguation Engine using Entityhub {@link SimilarityConstraint}s to disambiguate between existing
 * fise:EntityAnnotations for fise:TextAnnotations.
 * <p>
 * <b>TODOs</b>:
 * <ul>
 * <li>Configurations: currently all configurations is set to the defaults
 * <li>Context: test and improve different ways to determine the context used for disambiguation.
 * <li>URI based similarity: currently only full text similarity is used. However it would also be possible to
 * use the {@link SpecialFieldEnum#references} field to disambiguate based on URIs of already suggested
 * Entities.
 * </ul>
 * 
 * @author Kritarth Anand
 * @author Rupert Westenthaler
 */
@Component(immediate = true, metatype = true)
@Service
@Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME, value = "disambiguation-mlt")})
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

    /*
     * The following parameters describe the ratio of the original fise:confidence values and the
     * disambiguation scores contributing to the final disambiguated fise:confidence
     * 
     * TODO: make configurable
     */
    /**
     * Default ratio for Disambiguation (2.0)
     */
    public static final double DEFAULT_DISAMBIGUATION_RATIO = 2.0;
    /**
     * Default ratio for the original fise:confidence of suggested entities
     */
    public static final double DEFAULT_CONFIDNECE_RATIO = 1.0;

    /**
     * The weight for disambiguation scores <code>:= disRatio/(disRatio+confRatio)</code>
     */
    private double disambiguationWeight = DEFAULT_DISAMBIGUATION_RATIO
            / (DEFAULT_DISAMBIGUATION_RATIO + DEFAULT_CONFIDNECE_RATIO);
    /**
     * The weight for the original confidence scores <code>:= confRatio/(disRatio+confRatio)</code>
     */
    private double confidenceWeight = DEFAULT_CONFIDNECE_RATIO
            / (DEFAULT_DISAMBIGUATION_RATIO + DEFAULT_CONFIDNECE_RATIO);

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

        String textContent;
        Entry<IRI,Blob> textBlob = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if (textBlob != null) {
            try {
                textContent = ContentItemHelper.getText(textBlob.getValue());
            } catch (IOException e) {
                log.warn("Unable to retieve plain text content for ContentItem " + ci.getUri(), e);
                textContent = null;
            }
        } else {
            textContent = null;
        }

        Graph graph = ci.getMetadata();

        // (1) read the data from the content item
        String contentLangauge;
        DisambiguationData disData;
        ci.getLock().readLock().lock();
        try {
            contentLangauge = EnhancementEngineHelper.getLanguage(ci);
            // NOTE (rwesten): moved the parsing of the information from the
            // contentItem to static method of the Class holding those information
            // (similar as it already was for SavedEntity)
            // readEntities(loseConfidence, allEntities, textAnnotations, graph);
            disData = DisambiguationData.createFromContentItem(ci);
        } finally {
            ci.getLock().readLock().unlock();
        }

        // (2) Disambiguate the SavedEntities
        for (SavedEntity savedEntity : disData.textAnnotations.values()) {
            if (savedEntity.getSuggestions().size() <= 1) {
                // we need not to disambiguate if only one suggestion is present
                continue;
            }
            // NOTE: the site is determined from the
            // fise:TextAnnotation <-- dc:relation --
            // fise:EntityAnnotation -- entityhub:ste --> "{siteName}"^^xsd:string
            // data.
            // TODO: add configuration to include/exclude Sites by name
            Site site = siteManager.getSite(savedEntity.getSite());
            Collection<String> types = null; // potential types of entities
            boolean casesensitive = false; // TODO: make configurable
            String savedEntityLabel =
                    casesensitive ? savedEntity.getName() : savedEntity.getName().toLowerCase();

            // Determine the context used for disambiguation
            // TODO: make this configurable options

            String disambiguationContext;
            // (0.a) The easiest way is to just use the selection context
            // disambiguationContext = savedEntity.getContext();
            // (0.b) Calculate a context based on a moving window
            String window =
                    getDisambiguationContext(textContent, savedEntity.getName(), savedEntity.getStart(), 100);
            log.info("Use Window: '{}' for '{}'", window, savedEntity.getName());

            // (1) The contextSelections:
            // All other selected text within the selection context
            List<String> contextSelections =
                    getSelectionsInContext(savedEntity.getName(), disData.allSelectedTexts, window);
            // savedEntity.getContext());
            disambiguationContext = unionString(false, contextSelections);

            // (2) I do not understand this variant (see comment for the
            // EntitiesInRange(..) method
            // List<String> L = EntitiesInRange(disData.directoryTextAnotation,
            // (savedEntity.getStart() + savedEntity.getEnd()) / 2);
            // disambiguationContext = unionString(false,contextSelections);

            // (3) one can build a combination of the above
            // disambiguationContext = unionString(true, //unique adds
            // Collections.singleton(savedEntity.getName()), //the selected text
            // Collections.singleton(context), //the context
            // contextSelections); //other selected parsed in the context

            // or just the name of the entity AND the context
            // disambiguationContext = unionString(false,
            // Collections.singleton(savedEntity.getName()),
            // contextSelections);

            // (4) TODO: I would also like to have the possibility to disambiguate
            // using URIs of Entities suggested for other TextAnnotations
            // within the context.

            // make the similarity query on the Entityhub using the collected
            // information
            QueryResultList<Entity> results;
            log.info(" - Query '{}' for {}@{} with context '{}'", new Object[] {site.getId(),
                    savedEntityLabel, contentLangauge, disambiguationContext});
            if (!StringUtils.isBlank(disambiguationContext)) {
                try {
                    results = query(site, savedEntityLabel, contentLangauge, disambiguationContext);
                } catch (SiteException e) {
                    // TODO we could also try to catch those errors ...
                    throw new EngineException("Unable to disambiguate Mention of '" + savedEntity.getName()
                            + "' on Entityhub Site '" + site.getId() + "!", e);
                }
                log.debug(" - {} results returned by query {}", results.size(), results.getQuery());
                // match the results with the suggestions
                disambiguateSuggestions(results, savedEntity);
            } else {
                log.debug(" - not disambiguated because of empty context!");
            }
        }
        // (3) Write back the Results of the Disambiguation process
        // NOTE (rwesten): In the original version of Kritarth this was done as
        // part of (2) - disambiguation. This is now changed as in (2) the
        // disambiguation results are stored in the Suggestions and only
        // applied to the EnhancementStructure in (3). This allows to reduce the
        // coverage of the wirte lock needed to be applied to the ContentItem.
        ci.getLock().writeLock().lock();
        try {
            applyDisambiguationResults(graph, disData);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    /*
     * Is used to query the Dbpedia with a entity as main constraint and then add string of all other entities
     * detected as similarity constraints
     */

    protected QueryResultList<Entity> query(Site dbpediaSite, String savedEntityLabel, String language,
            String extractionContext) throws SiteException {
        FieldQuery query = dbpediaSite.getQueryFactory().createFieldQuery();
        if (savedEntityLabel != null && !savedEntityLabel.isEmpty()) {
            Constraint labelConstraint;
            if (language != null) {
                labelConstraint = new TextConstraint(savedEntityLabel, false, language, null);
            } else {
                labelConstraint = new TextConstraint(savedEntityLabel, false);
            }
            // TODO: what happens if a recommendation was not based on rdfs:label?
            query.setConstraint(RDFS_LABEL.getUnicodeString(), labelConstraint);
        } else {
            log.warn("parsed label {} was empty or NULL. Will use Similarity constraint only!",
                savedEntityLabel);
        }
        query.setConstraint(SpecialFieldEnum.fullText.getUri(), new SimilarityConstraint(extractionContext));
        query.setLimit(25);

        return dbpediaSite.findEntities(query);
    }

    /*
     * If for an entity the Dbpedia query results in suggestion none of which match the already present
     * ambiguations, we go with the ambiguations found earlier that is the ones we have with.
     */
    // NOTE (rwesten): The disambiguateSuggestions now reduces confidence
    // values of Suggestions that are not within the disambiguation result
    // by the #confidenceWeight. So if not a single suggestion do match with
    // the disambiguation result the ambiguation is kept but the overall
    // fise:confidence values are reduced by #confidenceWeight (ensured to be
    // less than 1)
    // protected List<Triple> unchangedConfidences(List<IRI> subsumed,
    // Graph graph,
    // List<Triple> loseConfidence) {
    // for (int i = 0; i < subsumed.size(); i++) {
    // IRI uri = subsumed.get(i);
    // Iterator<Triple> confidenceTriple = graph.filter(uri, ENHANCER_CONFIDENCE, null);
    // while (confidenceTriple.hasNext()) {
    // loseConfidence.remove(confidenceTriple.next());
    // }
    // }
    // return loseConfidence;
    // }

    /**
     * Applies the disambiguation results to the suggestions of the {@link SavedEntity}.
     * <p>
     * This method modifies the state of the {@link SavedEntity#getSuggestions()}
     * 
     * @param results
     *            the results of the disambiguation request
     * @param savedEntity
     *            the saved entity to be disambiguated
     **/
    protected void disambiguateSuggestions(QueryResultList<Entity> results, SavedEntity savedEntity) {
        // NOTE (rwesten) We should not score disambiguation results based on
        // how well the labels match.
        // Either use directly the scores of the disambiguation results OR
        // do combine the confidence of the original suggestion with the
        // scores of the disambiguation

        /*
         * Algorithm: Combine original confidence with Disambiguation results
         * 
         * Parameter(s):
         * 
         * * ratio configured as '{dr}:{cr}' where 'dr' stands for the ratio for the disambiguation score and
         * 'cr' stand for the ratio for the original fise:confidence of a suggestion (default 1:1) *
         * disambiguation weight (dw) := dr/(dr+cr) ... already calculated based on the configured ratio in
         * #disambiguationWeight * confidence weight (cw) := cw/(dr+cr) ... already calculated based on the
         * configured ratio in #confidenceWeight
         * 
         * Input(s):
         * 
         * * confidence (c): the original confidence of a suggestion (range [0..1]) * score (s): the score of
         * the disambiguation * maximum score (ms): the maximum disambiguation score
         * 
         * Output
         * 
         * * disambiguated confidence (dc): the confidence after disambiguation
         * 
         * Algorithm:
         * 
         * * normalized score (ns) := s/ms ... ensures range [0..1] for disambiguation scores * disambiguated
         * confidence = c*cw+ns*dw ... guaranteed to be [0..1]
         */
        List<Suggestion> matches = new ArrayList<Suggestion>(results.size());
        Float maxScore = null;
        Float maxSuggestedScore = null;
        Iterator<Entity> guesses = results.iterator();
        log.info("disambiguate {}: ", savedEntity.getName());
        while (guesses.hasNext()) {
            Entity guess = guesses.next();
            Float score =
                    guess.getRepresentation().getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
            if (score == null) {
                log.warn("Missing Score for Entityhub Query Result {}!", guess.getId());
                continue;
            }
            if (maxScore == null) {
                maxScore = score;
            }
            IRI uri = new IRI(guess.getId());
            Suggestion suggestion = savedEntity.getSuggestion(uri);
            if (suggestion == null) {
                log.info(" - not found {}", guess.getId());
                continue;
            }
            if (maxSuggestedScore == null) {
                maxSuggestedScore = score;
            }
            double c = suggestion.getOriginalConfidnece() == null ? 0 : suggestion.getOriginalConfidnece();
            // TODO (rwesten) we need to find out if we should normalize based on the
            // maximum score or the maximum score of an suggested one
            double ns = score / maxSuggestedScore;
            suggestion.setNormalizedDisambiguationScore(ns);
            double dc = c * confidenceWeight + ns * disambiguationWeight;
            suggestion.setDisambiguatedConfidence(dc);
            log.info("  - found {}, origConf:{}, disScore:{}, disConf:{}",
                new Object[] {suggestion.getEntityUri(), c, ns, dc});
        }
        // if at least one suggestion was also in the disambiguation result
        if (maxSuggestedScore != null) {
            // adapt the confidence of suggestions that where not part of the
            // disambiguation result
            for (Suggestion suggestion : savedEntity.getSuggestions()) {
                if (suggestion.getDisambiguatedConfidence() == null) {
                    double c =
                            suggestion.getOriginalConfidnece() == null ? 0 : suggestion
                                    .getOriginalConfidnece();
                    suggestion.setDisambiguatedConfidence(c * confidenceWeight);
                }
            }
        } else { // else keep the original results
            log.info("  - none found");
        }
    }

    /*
     * Checks if there is any common elements amongst the ambiguations amongst latest dbpedia query and intial
     * ambiguations
     */
    // NOTE (rwesten): now done as part of the disambiguateSuggestions(..)
    // method.
    // protected boolean intersectionCheck(List<Suggestion> matches,
    // List<IRI> subsumed,
    // Graph graph,
    // String contentLangauge) {
    // for (int i = 0; i < subsumed.size(); i++) {
    // IRI uri = subsumed.get(i);
    //
    // IRI uri1 = EnhancementEngineHelper.getReference(graph, uri, new IRI(NamespaceEnum.fise
    // + "entity-reference"));
    //
    // String selectedText = EnhancementEngineHelper.getString(graph, uri, ENHANCER_ENTITY_LABEL);
    //
    // if (selectedText == null) {
    // continue;
    // }
    //
    // for (int j = 0; j < matches.size(); j++) {
    // Suggestion suggestion = matches.get(j);
    // String suggestName = suggestion.getURI();
    // if (suggestName.compareToIgnoreCase(uri1.getUnicodeString()) == 0) return true;
    // }
    // }
    // return false;
    // }

    // NOTE (rwesten): one MUST NOT store information of processed ContentItems
    // as member variables, as one EnhancementEngine instance is
    // concurrently used to process multiple ContentItems. Because
    // of that member variables will have data of different
    // ContentItems!
    // All those data need to be hold in information that are local
    // to the processing of a single ContentItem (similar to
    // SavedEntity).
    // NOTE moved the DisambiguationData#directoryTextAnotation
    // public Map<Integer,String> directoryTextAnotation = new HashMap<Integer,String>();

    // TODO: make configureable
    int radii = 23;

    // Value to be configured

    public boolean toInclude(int k, int s) {
        if (Math.abs(k - s) < radii && Math.abs(k - s) > 0) {
            return true;
        }
        return false;
    }

    /*
     * TODO: rwesten I do not understand what is the intension of this Adding the fise:selection-context of
     * all entities within a range of #radii characters seams not to be a great way to build a context (or do
     * i miss something?
     */
    @Deprecated
    // for now until someone can answer the anove question
    public List<String> EntitiesInRange(NavigableMap<Integer,SavedEntity> map, int radius) {
        List<String> temp = new ArrayList<String>();
        // TODO: reimplement using subMap of the parsed NavigableMap map
        for (Entry<Integer,SavedEntity> entry : map.entrySet()) {
            Integer s = entry.getKey();
            String subs = entry.getValue().getContext();
            if (toInclude(s, radius)) {
                temp.add(subs);
            }
        }

        return temp; // if(Cal(f,k))
    }

    /**
     * Returns a list of all fise:selected-text values occurring in the parsed context (excluding the parsed
     * label if not null
     * 
     * @param label
     *            The label of the current Entity. parse <code>null</code> if the current label should not be
     *            ignored (and included in the context)
     * @param allEntities
     *            The collections with all the fise:selection-text values of all fise:TextAnnotations
     * @param context
     * @return
     */
    protected List<String> getSelectionsInContext(String label, Collection<String> allEntities, String context) {
        List<String> allEntityString = new ArrayList<String>();

        for (String selectedText : allEntities) {
            if (context.contains(selectedText) && selectedText.compareToIgnoreCase(label) != 0) {
                allEntityString.add(selectedText);
            }

        }

        return allEntityString;
    }

    public String unionString(boolean unique, Collection<?>... lists) {
        StringBuilder union = new StringBuilder();
        HashSet<String> added = new HashSet<String>();
        for (Collection<?> list : lists) {
            for (Object entry : list) {
                if (!unique || added.add(entry.toString())) {
                    union.append(entry);
                    union.append(' ');
                }
            }
        }
        return union.toString();
    }

    /*
     * Finds values the lie in intersection of both the set of disambiguations( the one intially suggested and
     * the one from dpedia). Update the confidence values of those and make the confidence values of others as
     * 0 in gainconfidence list
     */
    // NOTE (rwesten): intersection is calculated as part of the disambiguateSuggestions(..)
    // method. Results are stored in the Suggestions (member of SavedEntiy) and
    // than written back to the EnhancementStructure in a separate step
    // protected List<Triple> intersection(List<Suggestion> matches,
    // List<IRI> subsumed,
    // Graph graph,
    // List<Triple> gainConfidence,
    // String contentLangauge) {
    //
    // for (int i = 0; i < subsumed.size(); i++) {
    // boolean matchFound = false;
    // IRI uri = subsumed.get(i);
    //
    // IRI uri1 = EnhancementEngineHelper.getReference(graph, uri, new IRI(NamespaceEnum.fise
    // + "entity-reference"));
    //
    // for (int j = 0; j < matches.size(); j++) {
    // Suggestion suggestion = matches.get(j);
    // String suggestName = suggestion.getURI();
    //
    // if (suggestName != null && uri1 != null
    // && suggestName.compareToIgnoreCase(uri1.getUnicodeString()) == 0) {
    // Triple confidenceTriple = new TripleImpl(uri, ENHANCER_CONFIDENCE, LiteralFactory
    // .getInstance().createTypedLiteral(suggestion.getScore()));
    // Triple contributorTriple = new TripleImpl((IRI) confidenceTriple.getSubject(),
    // new IRI(NamespaceEnum.dc + "contributor"), LiteralFactory.getInstance()
    // .createTypedLiteral(this.getClass().getName()));
    // gainConfidence.add(confidenceTriple);
    // gainConfidence.add(contributorTriple);
    // matchFound = true;
    // }
    // }
    //
    // if (!matchFound) {
    // Triple confidenceTriple = new TripleImpl(uri, ENHANCER_CONFIDENCE, LiteralFactory
    // .getInstance().createTypedLiteral(0.0));
    // Triple contributorTriple = new TripleImpl((IRI) confidenceTriple.getSubject(), new IRI(
    // NamespaceEnum.dc + "contributor"), LiteralFactory.getInstance().createTypedLiteral(
    // this.getClass().getName()));
    // gainConfidence.add(confidenceTriple);
    // gainConfidence.add(contributorTriple);
    // }
    // }
    //
    // return gainConfidence;
    // }

    /* Removes the value in lose confidence from the graph */
    protected void removeOldConfidenceFromGraph(Graph graph, List<Triple> loseConfidence) {
        for (int i = 0; i < loseConfidence.size(); i++) {
            Triple elementToRemove = loseConfidence.get(i);
            graph.remove(elementToRemove);
        }
    }

    /**
     * Adds the disambiguation results to the enhancement structure
     * 
     * @param graph
     *            the metadata of the {@link ContentItem}
     * @param disData
     *            the disambiguation data
     */
    protected void applyDisambiguationResults(Graph graph, DisambiguationData disData) {
        for (SavedEntity savedEntity : disData.textAnnotations.values()) {
            for (Suggestion s : savedEntity.getSuggestions()) {
                if (s.getDisambiguatedConfidence() != null) {
                    if (disData.suggestionMap.get(s.getEntityAnnotation()).size() > 1) {
                        // already encountered AND disambiguated -> we need to clone!!
                        log.info("clone {} suggesting {} for {}[{},{}]({})",
                            new Object[] {s.getEntityAnnotation(), s.getEntityUri(), savedEntity.getName(),
                                    savedEntity.getStart(), savedEntity.getEnd(), savedEntity.getUri()});
                        s.setEntityAnnotation(cloneTextAnnotation(graph, s.getEntityAnnotation(),
                            savedEntity.getUri()));
                        log.info("  - cloned {}", s.getEntityAnnotation());
                    }
                    // change the confidence
                    EnhancementEngineHelper.set(graph, s.getEntityAnnotation(), ENHANCER_CONFIDENCE,
                        s.getDisambiguatedConfidence(), literalFactory);
                    EnhancementEngineHelper.addContributingEngine(graph, s.getEntityAnnotation(), this);
                }
            }
        }
    }

    /**
     * This creates a 'clone' of the fise:EntityAnnotation where the original does no longer have a
     * dc:relation to the parsed fise:TextAnnotation and the created clone does only have a dc:relation to the
     * parsed fise:TextAnnotation.
     * <p>
     * This is required by disambiguation because other engines typically only create a single
     * fise:EntityAnnotation instance if several fise:TextAnnotation do have the same fise:selected-text
     * values. So for a text that multiple times mentions the same Entity (e.g. "Paris") there will be
     * multiple fise:TextAnnotations selecting the different mentions of that Entity, but there will be only a
     * single set of suggestions - fise:EntityAnnotations (e.g. "Paris, France" and "Paris, Texas"). Now lets
     * assume a text like
     * 
     * <pre>
     *     Paris is the capital of France and it is worth a visit for sure. But
     *     one can also visit Paris without leaving the United States as there
     *     is also a city with the same name in Texas.
     * </pre>
     * 
     * Entity Disambiguation need to be able to have different fise:confidence values for the first and second
     * mention of Paris and this is only possible of the fise:TextAnnotations of those mentions do NOT refer
     * to the same set of fise:EntityAnnotations.
     * <p>
     * This methods accomplished exactly that as it
     * <ul>
     * <li>creates a clone of a fise:EntityAnnotation
     * <li>removes the dc:relation link to the 2nd mention of Paris from the original
     * <li>only adds the dc:relation of the end mention to the clone
     * </ul>
     * So in the end you will have two fise:EntityAnnotation
     * <ul>
     * <li>the original fise:EntityAnnotation with dc:relation to all fise:TextAnnotations other than the 2nd
     * mention (the one this method was called for)
     * <li>the cloned fise:EntityAnnnotation with a dc:relation to the 2nd mention.
     * </ul>
     * 
     * @param graph
     * @param entityAnnotation
     * @param textAnnotation
     * @return
     */
    public static IRI cloneTextAnnotation(Graph graph, IRI entityAnnotation, IRI textAnnotation) {
        IRI copy = new IRI("urn:enhancement-" + EnhancementEngineHelper.randomUUID());
        Iterator<Triple> it = graph.filter(entityAnnotation, null, null);
        // we can not add triples to the graph while iterating. So store them
        // in a list and add later
        List<Triple> added = new ArrayList<Triple>(32);
        while (it.hasNext()) {
            Triple triple = it.next();
            if (DC_RELATION.equals(triple.getPredicate())) {
                if (triple.getObject().equals(textAnnotation)) {
                    // remove the dc relation to the currently processed
                    // textAnnotation from the original
                    it.remove();
                    // and add it to the copy
                    added.add(new TripleImpl(copy, // use the copy as subject!
                            triple.getPredicate(), triple.getObject()));
                } // else it is not the currently processed TextAnnotation
                  // so we need to keep in in the original and NOT add
                  // it to the copy
            } else { // we can copy all other information 1:1
                added.add(new TripleImpl(copy, // use the copy as subject!
                        triple.getPredicate(), triple.getObject()));
            }
        }
        graph.addAll(added);
        return copy;
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
     * Extracts the selection context based on the content, selection and the start char offset of the
     * selection
     * 
     * @param content
     *            the content
     * @param selection
     *            the selected text
     * @param selectionStartPos
     *            the start char position of the selection
     * @param contextSize
     *            the size of the context in characters
     * @return the context
     */
    public static String getDisambiguationContext(String content, String selection, int selectionStartPos,
            int contextSize) {
        // extract the selection context
        int beginPos;
        if (selectionStartPos <= contextSize) {
            beginPos = 0;
        } else {
            int start = selectionStartPos - contextSize;
            beginPos = start;
            int c;
            do {
                c = content.codePointAt(beginPos);
                beginPos++;
            } while (beginPos <= selectionStartPos || Character.isWhitespace(c)
                    || Character.getType(c) == Character.SPACE_SEPARATOR);
            if (beginPos < 0 || beginPos >= selectionStartPos) { // no words
                beginPos = start; // begin within a word
            }
        }
        int endPos;
        if (selectionStartPos + selection.length() + contextSize >= content.length()) {
            endPos = content.length();
        } else {
            int selectionEndPos = selectionStartPos + selection.length();
            int end = selectionEndPos + contextSize;
            endPos = end;
            int c;
            do {
                c = content.codePointAt(endPos);
                endPos--;
            } while (endPos > selectionEndPos || Character.isWhitespace(c)
                    || Character.getType(c) == Character.SPACE_SEPARATOR);
            if (endPos <= selectionStartPos + selection.length()) {
                endPos = end; // end within a word;
            }
        }
        return content.substring(beginPos, endPos);
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

    // private static double levenshtein(String s1, String s2) {
    // if (s1 == null || s2 == null) {
    // throw new IllegalArgumentException("NONE of the parsed String MUST BE NULL!");
    // }
    // s1 = StringUtils.trim(s1);
    // s2 = StringUtils.trim(s2);
    // return s1.isEmpty() || s2.isEmpty() ? 0
    // : 1.0 - (((double) getLevenshteinDistance(s1, s2)) / ((double) (Math.max(s1.length(),
    // s2.length()))));
    // }

}