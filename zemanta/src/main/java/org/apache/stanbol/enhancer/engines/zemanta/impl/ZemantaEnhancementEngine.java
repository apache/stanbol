package org.apache.stanbol.enhancer.engines.zemanta.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.zemanta.ZemantaOntologyEnum;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Apache Stanbol Enhancer Zemanta enhancement engine.
 * This enhancement engine uses the the Zemanta API for enhancing content.
 * See http://developer.zemanta.com
 * To run this engine you need a Zemanta API key configured (see README)
 * <p>
 * For detailed information on the mappings of Zemanta annotations to Stanbol
 * Enhancer enhancements see 
 * <a>http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine</a>
 * <p>
 * This implementation currently only provides Stanbol Enhancer enhancements for
 * Zemanta Recognitions.
 *
 * @author michaelmarth
 * @author Rupert Westenthaler
 */
@Component(immediate = true, metatype = true)
@Service
public class ZemantaEnhancementEngine implements EnhancementEngine {

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    protected static final String TEXT_HTML_MIMETYPE = "text/html";

    private static final Logger log = LoggerFactory.getLogger(ZemantaEnhancementEngine.class);

    protected BundleContext bundleContext;

    @Property
    public static final String API_KEY_PROPERTY = "org.apache.stanbol.enhancer.engines.zemanta.key";

    public static final String DMOZ_BASE_URL = "http://www.dmoz.org/";
    public static final String ZEMANTA_DMOZ_PREFIX = "Top/";
    private static String key;

    public LiteralFactory literalFactory;

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ce) throws IOException {
        bundleContext = ce.getBundleContext();
        if (ce != null) {
            Dictionary<String, String> properties = ce.getProperties();
            key = properties.get(API_KEY_PROPERTY);
            if (key == null) {
                warnKeyMissing();
            } else {
                log.info("found Zemanta API key: " + key);
            }
        } else {
            warnKeyMissing();
        }
        //init the LiteralFactory
        literalFactory = LiteralFactory.getInstance();
    }

    private void warnKeyMissing() {
        log.warn("No Zemanata API key configured. Zemanta engine will not work properly!");
    }

    @Deactivate
    protected void deactivate(ComponentContext ce) {
        literalFactory = null;
    }

    public int canEnhance(ContentItem ci) {
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        if (TEXT_HTML_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        String text;
        try {
            text = IOUtils.toString(ci.getStream());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            log.warn("nothing to enhance");
            return;
        }
        MGraph graph = ci.getMetadata();
        UriRef ciId = new UriRef(ci.getId());
        //we need to store the results of Zemanta in an temp graph
        MGraph results = new SimpleMGraph();
        ZemantaAPIWrapper zemanta = new ZemantaAPIWrapper(key);
        results.addAll(zemanta.enhance(text));
        //now we need to process the results and convert them into the Enhancer
        //annotation structure
        processRecognition(results, graph, text, ciId);
        processCategories(results, graph, ciId);

    }

    protected void processCategories(MGraph results, MGraph enhancements, UriRef ciId) {
        Iterator<Triple> categories = results.filter(null, Properties.RDF_TYPE, ZemantaOntologyEnum.Category.getUri());
        while (categories.hasNext()) {
            NonLiteral category = categories.next().getSubject();
            log.info("process category " + category);
            Double confidence = parseConfidence(results, category);
            log.info(" > confidence :" + confidence);
            //now we need to follow the Target link
            UriRef target = EnhancementEngineHelper.getReference(results, category, ZemantaOntologyEnum.target.getUri());
            if (target != null) {
                //first check the the used categorisation
                UriRef categorisationScheme = EnhancementEngineHelper.getReference(results, target, ZemantaOntologyEnum.categorization.getUri());
                if (categorisationScheme != null && categorisationScheme.equals(ZemantaOntologyEnum.categorization_DMOZ.getUri())) {
                    String categoryTitle = EnhancementEngineHelper.getString(results, target, ZemantaOntologyEnum.title.getUri());
                    if (categoryTitle != null) {
                        //now write the Stanbol Enhancer entity enhancement
                        UriRef categoryEnhancement = EnhancementEngineHelper.createEntityEnhancement(enhancements, this, ciId);
                        //write the title
                        enhancements.add(new TripleImpl(categoryEnhancement, Properties.ENHANCER_ENTITY_LABEL, literalFactory.createTypedLiteral(categoryTitle)));
                        //write the reference
                        if (categoryTitle.startsWith(ZEMANTA_DMOZ_PREFIX)) {
                            enhancements.add(new TripleImpl(categoryEnhancement, Properties.ENHANCER_ENTITY_REFERENCE, new UriRef(DMOZ_BASE_URL + categoryTitle.substring(ZEMANTA_DMOZ_PREFIX.length()))));
                        }
                        //write the confidence
                        if (confidence != null) {
                            enhancements.add(new TripleImpl(categoryEnhancement, Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(confidence)));
                        }
                        //we need to write the entity type and the dc:type
                        // see http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine#Mapping_of_Categories
                        // for more Information
                        enhancements.add(new TripleImpl(categoryEnhancement, Properties.DC_TYPE, TechnicalClasses.ENHANCER_CATEGORY));
                        //Use the Zemanta Category as type for the referred Entity
                        enhancements.add(new TripleImpl(categoryEnhancement, Properties.ENHANCER_ENTITY_TYPE, ZemantaOntologyEnum.Category.getUri()));
                    } else {
                        log.warn("Unable to process category " + category + " because no title is present");
                    }
                } else {
                    log.warn("Unable to process category " + category + " because categorisation scheme != DMOZ (" + categorisationScheme + " != " + ZemantaOntologyEnum.categorization_DMOZ.getUri() + ")");
                }
            } else {
                log.warn("Unable to process category " + category + " because no target node was found");
            }
        }
    }

    /**
     * Processes all Zemanta Recognitions and converts them to the according
     * FISE enhancements
     *
     * @param results      the results of the Zemanta enhancement process
     * @param enhancements the graph containing the current Stanbol Enhancer
     *                     enhancements
     * @param text         the content of the content item as string
     */
    protected void processRecognition(MGraph results, MGraph enhancements, String text, UriRef ciId) {
        Iterator<Triple> recognitions = results.filter(null, Properties.RDF_TYPE, ZemantaOntologyEnum.Recognition.getUri());
        while (recognitions.hasNext()) {
            NonLiteral recognition = recognitions.next().getSubject();
            log.info("process recognition " + recognition);
            //first get everything we need for the textAnnotations
            Double confidence = parseConfidence(results, recognition);
            log.info(" > confidence :" + confidence);
            String anchor = EnhancementEngineHelper.getString(results, recognition, ZemantaOntologyEnum.anchor.getUri());
            log.info(" > anchor :" + anchor);
            Collection<NonLiteral> textAnnotations = processTextAnnotation(enhancements, text, ciId, anchor, confidence);
            log.info(" > number of textAnnotations :" + textAnnotations.size());

            //second we need to create the EntityAnnotation that represent the
            //recognition
            NonLiteral object = EnhancementEngineHelper.getReference(results, recognition, ZemantaOntologyEnum.object.getUri());
            log.info(" > object :" + object);
            //The targets represent the linked entities
            //  ... and yes there can be more of them!
            //TODO: can we create an EntityAnnotation with several referred entities?
            //      Should we use the owl:sameAs to decide that!
            Set<UriRef> sameAsSet = new HashSet<UriRef>();
            for (Iterator<UriRef> sameAs = EnhancementEngineHelper.getReferences(results, object, ZemantaOntologyEnum.owlSameAs.getUri()); sameAs.hasNext(); sameAsSet.add(sameAs.next()))
                ;
            log.info(" > sameAs :" + sameAsSet);
            //now parse the targets and look if there are others than the one
            //merged by using sameAs
            Iterator<UriRef> targets = EnhancementEngineHelper.getReferences(results, object, ZemantaOntologyEnum.target.getUri());
            String title = null;
            while (targets.hasNext()) {
                //the entityRef is the URL of the target
                UriRef entity = targets.next();
                log.info("    -  target :" + entity);
                UriRef targetType = EnhancementEngineHelper.getReference(results, entity, ZemantaOntologyEnum.targetType.getUri());
                log.info("       o type :" + targetType);
                if (ZemantaOntologyEnum.targetType_RDF.getUri().equals(targetType)) {
                    String targetTitle = EnhancementEngineHelper.getString(results, entity, ZemantaOntologyEnum.title.getUri());
                    log.info("       o title :" + targetTitle);
                    if (sameAsSet.contains(entity)) {
                        if (title == null) {
                            title = targetTitle;
                        } else if (!title.equals(targetTitle)) {
                            log.warn("Entities marked with owl:sameAs do use different labels '" + title + "' != '" + targetTitle + "'!");
                        } //else the same label used by both -> thats expected
                    } else {
                        //maybe we should create an second entityEnhancement, but I think, that such a case should
                        //not happen. So write an warning for now
                        log.warn("Found Target with type RDF, that is not linked with owl:sameAs to the others (this: '" + entity + " | sameAs: " + sameAsSet + ")");
                        log.warn("  - no Enhancement for " + entity + " will be created");
                    }
                } //else -> do not process -> RDF Entities only
                //TODO: targetTypes are not parsed by Zemanta, therefore we can not set
                //      any entity types!
            }
            //create the entityEnhancement
            UriRef entityEnhancement = EnhancementEngineHelper.createEntityEnhancement(enhancements, this, ciId);
            if (confidence != null) {
                enhancements.add(new TripleImpl(entityEnhancement, Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(confidence)));
            }
            for (NonLiteral relatedTextAnnotation : textAnnotations) {
                enhancements.add(new TripleImpl(entityEnhancement, Properties.DC_RELATION, relatedTextAnnotation));
            }
            for (UriRef entity : sameAsSet) {
                enhancements.add(new TripleImpl(entityEnhancement, Properties.ENHANCER_ENTITY_REFERENCE, entity));
            }
            enhancements.add(new TripleImpl(entityEnhancement, Properties.ENHANCER_ENTITY_LABEL, literalFactory.createTypedLiteral(title)));
        }
    }

    /**
     * Helper method to parse the confidence property for an resource. Zemanta
     * does not the the xsd data type, because of that we need to parse the
     * double value based on the string.
     *
     * @param tc       the graph used to query for confidence value
     * @param resource the resource holding the confidence property
     *
     * @return the confidence of <code>null</code> if no confidence property is
     *         present for the parsed resource of the value can not be converted to a
     *         double value.
     * @see ZemantaOntologyEnum#confidence
     */
    private static Double parseConfidence(TripleCollection tc, NonLiteral resource) {
        String confidenceString = EnhancementEngineHelper.getString(tc, resource, ZemantaOntologyEnum.confidence.getUri());
        Double confidence;
        if (confidenceString != null) {
            try {
                confidence = Double.valueOf(confidenceString);
            } catch (NumberFormatException e) {
                log.warn("Unable to parse Float confidence for Literal value '" + confidenceString + "'");
                confidence = null;
            }
        } else {
            confidence = null;
        }
        return confidence;
    }

    /**
     * This Methods searches/creates text annotations for anchor points of Zemanta
     * extractions.<br>
     * First this method searches for text annotations that do use the anchor as
     * selected text. Second it searches for occurrences of the anchor within the
     * content of the content and checks if there is an text annotation for that
     * occurrence. If not it creates an new one.
     *
     * @param enhancements the graph containing the meta data
     * @param text         the content as string
     * @param ciId         the ID of the content item
     * @param anchor       the anchor text
     * @param confidence   the confidence to be used for newly created text annotations
     *
     * @return a collection of all existing/created text annotations for the parsed anchor
     */
    private Collection<NonLiteral> processTextAnnotation(MGraph enhancements, String text, UriRef ciId, String anchor, Double confidence) {
        Collection<NonLiteral> textAnnotations = new ArrayList<NonLiteral>();
        int anchorLength = anchor.length();
        Literal anchorLiteral = literalFactory.createTypedLiteral(anchor);
        //first search for existing TextAnnotations for the anchor
        Map<Integer, Collection<NonLiteral>> existingTextAnnotationsMap = searchExistingTextAnnotations(enhancements, anchorLiteral);

        for (int current = text.indexOf(anchor); current >= 0; current = text.indexOf(anchor, current + 1)) {
            Collection<NonLiteral> existingTextAnnotations = existingTextAnnotationsMap.get(current);
            if (existingTextAnnotations != null) {
                //use the existing once
                textAnnotations.addAll(existingTextAnnotations);
            } else {
                //we need to create an new one!
                UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(enhancements, this, ciId);
                textAnnotations.add(textAnnotation);
                //write the selection
                enhancements.add(new TripleImpl(textAnnotation, Properties.ENHANCER_START, literalFactory.createTypedLiteral(current)));
                enhancements.add(new TripleImpl(textAnnotation, Properties.ENHANCER_END, literalFactory.createTypedLiteral(current + anchorLength)));
                enhancements.add(new TripleImpl(textAnnotation, Properties.ENHANCER_SELECTED_TEXT, anchorLiteral));
                //TODO: Currently I use the confidence of the extraction, but I think this is more
                //      related to the annotated Entity rather to the selected text.
                if (confidence != null) {
                    enhancements.add(new TripleImpl(textAnnotation, Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(confidence)));
                }
                //TODO: No idea about the type of the Annotation, because we do not have an type of the entity!
                //      One would need to get the types from the referred Source
            }
        }
        return textAnnotations;
    }

    /**
     * Search for existing TextAnnotations for an given selected text and
     * returns an Map that uses the start position as an key and a list of
     * text annotations as an value.
     *
     * @param enhancements  the graph containing the enhancements to be searched
     * @param anchorLiteral the Literal representing the selected text
     *
     * @return Map that uses the start position as an key and a list of
     *         text annotations as an value.
     */
    private Map<Integer, Collection<NonLiteral>> searchExistingTextAnnotations(MGraph enhancements, Literal anchorLiteral) {
        Iterator<Triple> textAnnotationsIterator = enhancements.filter(null, Properties.ENHANCER_SELECTED_TEXT, anchorLiteral);
        Map<Integer, Collection<NonLiteral>> existingTextAnnotationsMap = new HashMap<Integer, Collection<NonLiteral>>();
        while (textAnnotationsIterator.hasNext()) {
            NonLiteral subject = textAnnotationsIterator.next().getSubject();
            //test rdfType
            if (enhancements.contains(new TripleImpl(subject, Properties.RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION))) {
                Integer start = EnhancementEngineHelper.get(enhancements, subject, Properties.ENHANCER_START, Integer.class, literalFactory);
                if (start != null) {
                    Collection<NonLiteral> textAnnotationList = existingTextAnnotationsMap.get(start);
                    if (textAnnotationList == null) {
                        textAnnotationList = new ArrayList<NonLiteral>();
                        existingTextAnnotationsMap.put(start, textAnnotationList);
                    }
                    textAnnotationList.add(subject);
                }
            }
        }
        return existingTextAnnotationsMap;
    }
}
