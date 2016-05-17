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
package org.apache.stanbol.enhancer.engines.zemanta.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.createTextEnhancement;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.createTopicEnhancement;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReferences;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.SKOS_CONCEPT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_CATEGORY;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;

import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.zemanta.ZemantaOntologyEnum;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
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
@Component(immediate = true, metatype = true, inherit = true)
@Service
@Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value="zemanta")
})
public class ZemantaEnhancementEngine 
        extends AbstractEnhancementEngine<IOException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {
    
    @Property
    public static final String API_KEY_PROPERTY = "org.apache.stanbol.enhancer.engines.zemanta.key";

    public static final String DMOZ_BASE_URL = "http://www.dmoz.org/";
    public static final String ZEMANTA_DMOZ_PREFIX = "Top/";

    protected static final Set<String> SUPPORTED_MIMETYPES = 
            Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList("text/plain","text/html")));

    /**
     * The maximal prefix/suffix size used for the selection context. This is
     * required, because Zemanta does only provide the Anchor text, but not the
     * exact position within the text. So this engine creates a TextAnnotation
     * for each occurrence of the Anchor within the text and uses the surrounding
     * as context.
     */
    private static final int SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(ZemantaEnhancementEngine.class);

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT} + 10. It should run after Metaxa and LangId.
     */
    public static final Integer defaultOrder = ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT + 10;

    private String key;

    public LiteralFactory literalFactory;

    protected BundleContext bundleContext;
    /**
     * Only activate this engine in online mode
     */
    @SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode;

    @Activate
    protected void activate(ComponentContext ce) throws IOException,ConfigurationException {
        super.activate(ce);
        bundleContext = ce.getBundleContext();
        key = (String)ce.getProperties().get(API_KEY_PROPERTY);
        checkConfig();
        //init the LiteralFactory
        literalFactory = LiteralFactory.getInstance();
    }

    @Deactivate
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        literalFactory = null;
        key = null;
        bundleContext = null;
    }
    /**
     * Checks the configuration of the {@link #API_KEY_PROPERTY}
     * @throws ConfigurationException if the Zemanta key is not configured
     */
    private void checkConfig() throws ConfigurationException {
        if(key == null || key.trim().length() == 0) {
            throw new ConfigurationException(API_KEY_PROPERTY,String.format(
                "%s : please configure a Zemanta key to use this engine (e.g. by" +
                "using the 'Configuration' tab of the Apache Felix Web Console).",
                getClass().getSimpleName()));
        }
    }

    public int canEnhance(ContentItem ci) {
        if(ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null){
            return ENHANCE_ASYNC; //the ZEMANTA engine now supports async processing!
        } else {
            return CANNOT_ENHANCE;
        }
    }


    public void computeEnhancements(ContentItem ci) throws EngineException {
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if(contentPart == null){
            throw new IllegalStateException("No ContentPart with a supported Mime Type"
                + "found for ContentItem "+ci.getUri()+"(supported: '"
                + SUPPORTED_MIMETYPES+"') -> this indicates that canEnhance was" 
                + "NOT called and indicates a bug in the used EnhancementJobManager!");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            log.warn("ContentPart {} of ContentItem {} does not contain any text to enhance",
                contentPart.getKey(),ci.getUri());
            return;
        }
        Graph graph = ci.getMetadata();
        IRI ciId = ci.getUri();
        //we need to store the results of Zemanta in an temp graph
        Graph results = new SimpleGraph();
        ZemantaAPIWrapper zemanta = new ZemantaAPIWrapper(key);
        try {
            results.addAll(zemanta.enhance(text));
        } catch (IOException e) {
           throw new EngineException("Unable to get Enhancement from remote Zemanta Service",e);
        }
        //now we need to process the results and convert them into the Enhancer
        //annotation structure
        ci.getLock().writeLock().lock();
        try {
            processRecognition(results, graph, text, ciId);
            processCategories(results, graph, ciId);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }
    public Map<String, Object> getServiceProperties() {
        // TODO Auto-generated method stub
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }
    
    protected void processCategories(Graph results, Graph enhancements, IRI ciId) {
        Iterator<Triple> categories = results.filter(null, RDF_TYPE, ZemantaOntologyEnum.Category.getUri());
        //add the root Text annotation as soon as the first TopicAnnotation is added.
        IRI textAnnotation = null;
        while (categories.hasNext()) {
            BlankNodeOrIRI category = categories.next().getSubject();
            log.debug("process category " + category);
            Double confidence = parseConfidence(results, category);
            log.debug(" > confidence :" + confidence);
            //now we need to follow the Target link
            IRI target = EnhancementEngineHelper.getReference(results, category, ZemantaOntologyEnum.target.getUri());
            if (target != null) {
                //first check the used categorisation
                IRI categorisationScheme = EnhancementEngineHelper.getReference(results, target, ZemantaOntologyEnum.categorization.getUri());
                if (categorisationScheme != null && categorisationScheme.equals(ZemantaOntologyEnum.categorization_DMOZ.getUri())) {
                    String categoryTitle = EnhancementEngineHelper.getString(results, target, ZemantaOntologyEnum.title.getUri());
                    if (categoryTitle != null) {
                        if(textAnnotation == null){
                            //this is the first category ... create the TextAnnotation used
                            //to link all fise:TopicAnnotations
                            textAnnotation = createTextEnhancement(enhancements, this, ciId);
                            enhancements.add(new TripleImpl(textAnnotation,DC_TYPE,SKOS_CONCEPT));
                        }
                        //now write the TopicAnnotation
                        IRI categoryEnhancement = createTopicEnhancement(enhancements, this, ciId);
                        //make related to the EntityAnnotation
                        enhancements.add(new TripleImpl(categoryEnhancement, DC_RELATION, textAnnotation));
                        //write the title
                        enhancements.add(new TripleImpl(categoryEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(categoryTitle)));
                        //write the reference
                        if (categoryTitle.startsWith(ZEMANTA_DMOZ_PREFIX)) {
                            enhancements.add(
                                    new TripleImpl(categoryEnhancement, ENHANCER_ENTITY_REFERENCE, new IRI(DMOZ_BASE_URL + categoryTitle.substring(ZEMANTA_DMOZ_PREFIX.length()))));
                        }
                        //write the confidence
                        if (confidence != null) {
                            enhancements.add(new TripleImpl(categoryEnhancement, ENHANCER_CONFIDENCE, 
                                literalFactory.createTypedLiteral(confidence)));
                        }
                        //we need to write the fise:entity-type
                        //as of STANBOL-617 we use now both the zemanta:Category AND the skos:Concept
                        //type. dc:type is no longer used as this is only used by fise:TextAnnotations
                        // see http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine#Mapping_of_Categories
                        // for more Information
                        enhancements.add(new TripleImpl(categoryEnhancement, ENHANCER_ENTITY_TYPE, SKOS_CONCEPT));
                        //Use also Zemanta Category as type for the referred Entity
                        enhancements.add(new TripleImpl(categoryEnhancement, ENHANCER_ENTITY_TYPE, ZemantaOntologyEnum.Category.getUri()));
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
    protected void processRecognition(Graph results, Graph enhancements, String text, IRI ciId) {
        Iterator<Triple> recognitions = results.filter(null, RDF_TYPE, ZemantaOntologyEnum.Recognition.getUri());
        while (recognitions.hasNext()) {
            BlankNodeOrIRI recognition = recognitions.next().getSubject();
            log.debug("process recognition " + recognition);
            //first get everything we need for the textAnnotations
            Double confidence = parseConfidence(results, recognition);
            log.debug(" > confidence :" + confidence);
            String anchor = EnhancementEngineHelper.getString(results, recognition, ZemantaOntologyEnum.anchor.getUri());
            log.debug(" > anchor :" + anchor);
            Collection<BlankNodeOrIRI> textAnnotations = processTextAnnotation(enhancements, text, ciId, anchor, confidence);
            log.debug(" > number of textAnnotations :" + textAnnotations.size());

            //second we need to create the EntityAnnotation that represent the
            //recognition
            BlankNodeOrIRI object = EnhancementEngineHelper.getReference(results, recognition, ZemantaOntologyEnum.object.getUri());
            log.debug(" > object :" + object);
            //The targets represent the linked entities
            //  ... and yes there can be more of them!
            //TODO: can we create an EntityAnnotation with several referred entities?
            //      Should we use the owl:sameAs to decide that!
            Set<IRI> sameAsSet = new HashSet<IRI>();
            for (Iterator<IRI> sameAs = getReferences(results, object, ZemantaOntologyEnum.owlSameAs.getUri()); sameAs.hasNext(); sameAsSet.add(sameAs.next()))
                ;
            log.debug(" > sameAs :" + sameAsSet);
            //now parse the targets and look if there are others than the one
            //merged by using sameAs
            Iterator<IRI> targets = EnhancementEngineHelper.getReferences(results, object, ZemantaOntologyEnum.target.getUri());
            String title = null;
            while (targets.hasNext()) {
                //the entityRef is the URL of the target
                IRI entity = targets.next();
                log.debug("    -  target :" + entity);
                IRI targetType = EnhancementEngineHelper.getReference(results, entity, ZemantaOntologyEnum.targetType.getUri());
                log.debug("       o type :" + targetType);
                if (ZemantaOntologyEnum.targetType_RDF.getUri().equals(targetType)) {
                    String targetTitle = EnhancementEngineHelper.getString(results, entity, ZemantaOntologyEnum.title.getUri());
                    log.debug("       o title :" + targetTitle);
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
            IRI entityEnhancement = EnhancementEngineHelper.createEntityEnhancement(enhancements, this, ciId);
            if (confidence != null) {
                enhancements.add(
                        new TripleImpl(entityEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(confidence)));
            }
            for (BlankNodeOrIRI relatedTextAnnotation : textAnnotations) {
                enhancements.add(
                        new TripleImpl(entityEnhancement, DC_RELATION, relatedTextAnnotation));
            }
            for (IRI entity : sameAsSet) {
                enhancements.add(
                        new TripleImpl(entityEnhancement, ENHANCER_ENTITY_REFERENCE, entity));
            }
            enhancements.add(
                    new TripleImpl(entityEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(title)));
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
    private static Double parseConfidence(Graph tc, BlankNodeOrIRI resource) {
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
     * extractions.
     * <p>
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
    private Collection<BlankNodeOrIRI> processTextAnnotation(Graph enhancements, String text, IRI ciId, String anchor, Double confidence) {
        Collection<BlankNodeOrIRI> textAnnotations = new ArrayList<BlankNodeOrIRI>();
        int anchorLength = anchor.length();
        Literal anchorLiteral = new PlainLiteralImpl(anchor);
        //first search for existing TextAnnotations for the anchor
        Map<Integer, Collection<BlankNodeOrIRI>> existingTextAnnotationsMap = searchExistingTextAnnotations(enhancements, anchorLiteral);

        for (int current = text.indexOf(anchor); current >= 0; current = text.indexOf(anchor, current + 1)) {
            Collection<BlankNodeOrIRI> existingTextAnnotations = existingTextAnnotationsMap.get(current);
            if (existingTextAnnotations != null) {
                //use the existing once
                textAnnotations.addAll(existingTextAnnotations);
            } else {
                //we need to create an new one!
                IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(enhancements, this, ciId);
                textAnnotations.add(textAnnotation);
                //write the selection
                enhancements.add(
                        new TripleImpl(textAnnotation, ENHANCER_START, literalFactory.createTypedLiteral(current)));
                enhancements.add(
                        new TripleImpl(textAnnotation, ENHANCER_END, literalFactory.createTypedLiteral(current + anchorLength)));
                enhancements.add(
                        new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, anchorLiteral));
                //extract the selection context
                int beginPos;
                if(current <= SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
                    beginPos = 0;
                } else {
                    int start = current-SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
                    beginPos = text.indexOf(' ',start);
                    if(beginPos < 0 || beginPos >= current){ //no words
                        beginPos = start; //begin within a word
                    }
                }
                int endPos;
                if(current+anchorLength+SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= text.length()){
                    endPos = text.length();
                } else {
                    int start = current+anchorLength+SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
                    endPos = text.lastIndexOf(' ', start);
                    if(endPos <= current+anchorLength){
                        endPos = start; //end within a word;
                    }
                }
                enhancements.add(new TripleImpl(textAnnotation,ENHANCER_SELECTION_CONTEXT,new PlainLiteralImpl(text.substring(beginPos, endPos))));
                //TODO: Currently I use the confidence of the extraction, but I think this is more
                //      related to the annotated Entity rather to the selected text.
                if (confidence != null) {
                    enhancements.add(
                            new TripleImpl(textAnnotation, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(confidence)));
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
    private Map<Integer, Collection<BlankNodeOrIRI>> searchExistingTextAnnotations(Graph enhancements, Literal anchorLiteral) {
        Iterator<Triple> textAnnotationsIterator = enhancements.filter(null, ENHANCER_SELECTED_TEXT, anchorLiteral);
        Map<Integer, Collection<BlankNodeOrIRI>> existingTextAnnotationsMap = new HashMap<Integer, Collection<BlankNodeOrIRI>>();
        while (textAnnotationsIterator.hasNext()) {
            BlankNodeOrIRI subject = textAnnotationsIterator.next().getSubject();
            //test rdfType
            if (enhancements.contains(new TripleImpl(subject, RDF_TYPE, ENHANCER_TEXTANNOTATION))) {
                Integer start = EnhancementEngineHelper.get(enhancements, subject, ENHANCER_START, Integer.class, literalFactory);
                if (start != null) {
                    Collection<BlankNodeOrIRI> textAnnotationList = existingTextAnnotationsMap.get(start);
                    if (textAnnotationList == null) {
                        textAnnotationList = new ArrayList<BlankNodeOrIRI>();
                        existingTextAnnotationsMap.put(start, textAnnotationList);
                    }
                    textAnnotationList.add(subject);
                }
            }
        }
        return existingTextAnnotationsMap;
    }
}