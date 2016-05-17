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
package org.apache.stanbol.enhancer.engines.entitycomention;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.CASE_SENSITIVE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESSED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReferences;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CONTRIBUTOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.entitycomention.impl.ContentItemMentionBuilder;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * The Entity Co-Mentiaon Engine builds a local knowledge base already extracted
 * <code>fise:TextAnnotation</code>s and suggested 
 * <code>fise:EntityAnnotation</code>s. This information are then used to perform
 * an entity linking process. By doing so this engine will be able to detect
 * Co-Mentions of Entities within the processed document. <p>
 * 
 * 
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true,
    inherit = true)
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_NAME),
    @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=PROCESS_ONLY_PROPER_NOUNS_STATE, boolValue=DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=PROCESSED_LANGUAGES,
        cardinality=Integer.MAX_VALUE,
        value={"*;lmmtip;uc=LINK;prop=0.75;pprob=0.75", // link multiple matchable tokens in chunks; link upper case words
               "de;uc=MATCH", //in German all Nouns are upper case
               "es;lc=Noun", //the OpenNLP POS tagger for Spanish does not support ProperNouns
               "nl;lc=Noun"}), //same for Dutch 
    @Property(name=EntityCoMentionEngine.ADJUST_EXISTING_SUGGESTION_CONFIDENCE,
    	doubleValue=EntityCoMentionEngine.DEFAULT_CONFIDENCE_ADJUSTEMENT), 
    @Property(name=SERVICE_RANKING,intValue=0)
})
@Service(value=EnhancementEngine.class)
public class EntityCoMentionEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

	/**
	 * Property used to configure if/how confidence values of existing suggestions
	 * are modified if a co-mention is detected for a fise:TextAnnotation.<p>
	 * Values MUST be in the range [0..1) the 
	 * {@link #DEFAULT_CONFIDENCE_ADJUSTEMENT default} is <code>0.33</code> <p>
	 * Added with <a href="https://issues.apache.org/jira/browse/STANBOL-1219">STANBOL-1219</a>
	 */
	public static final String ADJUST_EXISTING_SUGGESTION_CONFIDENCE = "enhancer.engines.comention.adjustExistingConfidence";
    /**
     * Default value for {@link #ADJUST_EXISTING_SUGGESTION_CONFIDENCE}
     */
	public static final double DEFAULT_CONFIDENCE_ADJUSTEMENT = 0.33;
	/**
     * first of the post processing engines (note STANBOL-1218)
     */
    private static final Integer ENGINE_ORDERING = ServiceProperties.ORDERING_POST_PROCESSING + 80;
    private static final Map<String,Object> SERVICE_PROPERTIES = 
            Collections.unmodifiableMap(Collections.singletonMap(
                ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
                (Object)ENGINE_ORDERING));


    private final Logger log = LoggerFactory.getLogger(EntityCoMentionEngine.class);

    private final LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    @Reference
    protected NamespacePrefixService prefixService;
    
    private ServiceTracker labelTokenizerTracker;

    private double confidenceAdjustmentFactor;
    
//    private BundleContext bundleContext;
    /**
     * EntityLinking configuration used for Co-Mention extractions
     */
    private EntityLinkerConfig linkerConfig;
    /**
     * TextProcessingConfig used for Co-Mention extraction
     */
    private TextProcessingConfig textProcessingConfig;

    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public EntityCoMentionEngine() {
    }

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        log.info("activate {}[name:{}]",getClass().getSimpleName(),getName());
        Dictionary<String,Object> properties = ctx.getProperties();
//        bundleContext = ctx.getBundleContext();
        //extract TextProcessing and EnityLinking config from the provided properties
        textProcessingConfig = TextProcessingConfig.createInstance(properties);
        linkerConfig = EntityLinkerConfig.createInstance(properties,prefixService);
        //some of the confiugration is predefined
        linkerConfig.setNameField(CoMentionConstants.CO_MENTION_LABEL_FIELD);
        linkerConfig.setTypeField(CoMentionConstants.CO_MENTION_TYPE_FIELD);
        linkerConfig.setMaxSuggestions(5); //there should not be more as 5 suggestions
        linkerConfig.setMinFoundTokens(1); //a single token is enough
        linkerConfig.setMinLabelScore(0.24); //1/4 of the tokens
        linkerConfig.setMinMatchScore( //labelScore * token match factor
            linkerConfig.getMinLabelScore()*linkerConfig.getMinTokenMatchFactor());
        linkerConfig.setRedirectProcessingMode(RedirectProcessingMode.IGNORE);
        //remove all type mappings
        linkerConfig.setDefaultDcType(null);
        Set<IRI> mappedUris = new HashSet<IRI>(linkerConfig.getTypeMappings().keySet());
        for(IRI mappedUri : mappedUris){
            linkerConfig.setTypeMapping(mappedUri.getUnicodeString(), null);
        }
        //parse confidence adjustment value (STANBOL-1219)
        Object value = properties.get(ADJUST_EXISTING_SUGGESTION_CONFIDENCE);
        final double confidenceAdjustment;
        if(value == null){
        	confidenceAdjustment = DEFAULT_CONFIDENCE_ADJUSTEMENT;
        } else if(value instanceof Number){
        	confidenceAdjustment = ((Number)value).doubleValue();
        } else {
        	try {
        		confidenceAdjustment = Double.parseDouble(value.toString());
        	} catch (NumberFormatException e){
        		throw new ConfigurationException(ADJUST_EXISTING_SUGGESTION_CONFIDENCE, 
        				"The confidence adjustement value for existing suggestions "
        				+ "MUST BE a double value in the range [0..1)", e);
        	}
        }
        if(confidenceAdjustment < 0 || confidenceAdjustment >= 1){
    		throw new ConfigurationException(ADJUST_EXISTING_SUGGESTION_CONFIDENCE, 
    				"The confidence adjustement value for existing suggestions "
    				+ "MUST BE a double value in the range [0..1) (parsed: "
    				+ confidenceAdjustment +")!");
        }
        confidenceAdjustmentFactor = 1 - confidenceAdjustment;
        //get the metadata later set to the enhancement engine
        final BundleContext bc = ctx.getBundleContext();
        labelTokenizerTracker = new ServiceTracker(bc, LabelTokenizer.class.getName(), null); 
        labelTokenizerTracker.open();
    }
    /**
     * Deactivates this components. 
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        log.info("deactivate {}[name:{}]",getClass().getSimpleName(),getName());
        textProcessingConfig = null;
        linkerConfig = null;
        if(labelTokenizerTracker != null){
            labelTokenizerTracker.close();
            labelTokenizerTracker = null;
        }
        super.deactivate(ctx);
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        String language = getLanguage(this, ci, false);
        if(language == null || textProcessingConfig.getConfiguration(language) == null){
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[]{ getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        //we need a detected language, the AnalyzedText contentPart with Tokens.
        AnalysedText at = getAnalysedText(this, ci, false);
        return at != null && at.getTokens().hasNext() ?
                ENHANCE_ASYNC : CANNOT_ENHANCE;
    }
    
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        String language = getLanguage(this, ci, true);
        LanguageProcessingConfig languageConfig = textProcessingConfig.getConfiguration(language);
        if(languageConfig == null){
            throw new IllegalStateException("The language '"+language+"' is not configured "
                    + "to be processed by this Engine. As this is already checked within the "
                    + "canEnhance(..) method this may indicate an bug in the used "
                    + "EnhanceemntJobManager implementation!");
        }
        if(log.isDebugEnabled()){
            log.debug("compute co-mentions for ContentItem {} language {}  text={}", 
                new Object []{ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(at.getSpan(), 100)});
        }
        LabelTokenizer labelTokenizer = (LabelTokenizer)labelTokenizerTracker.getService();
        if(labelTokenizer == null){
            throw new EngineException(this, ci, "No LabelTokenizer available!",null);
        }

        //create the in-memory database for the mentioned Entities
        ContentItemMentionBuilder entityMentionIndex = new ContentItemMentionBuilder(
            labelTokenizer, language, linkerConfig.getDefaultLanguage());
        Graph metadata = ci.getMetadata();
        Set<IRI> textAnnotations = new HashSet<IRI>();
        ci.getLock().readLock().lock();
        try { //iterate over all TextAnnotations (mentions of Entities)
            for(Iterator<Triple> it = metadata.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION); it.hasNext();){
                IRI ta = (IRI)it.next().getSubject();
                entityMentionIndex.registerTextAnnotation(ta, metadata);
                textAnnotations.add(ta); //store the registered text annotations
            }
        } finally {
            ci.getLock().readLock().unlock();
        }
        EntityLinker entityLinker = new EntityLinker(at,language, 
            languageConfig, entityMentionIndex, linkerConfig, labelTokenizer ,entityMentionIndex);
        //process
        try {
            entityLinker.process();
        } catch (EntitySearcherException e) {
            log.error("Unable to link Entities with "+entityLinker,e);
            throw new EngineException(this, ci, "Unable to link Entities with "+entityLinker, e);
        }
        //TODO: write results
        ci.getLock().writeLock().lock();
        try {
            writeComentions(ci,entityLinker.getLinkedEntities().values(), language, textAnnotations);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    private void writeComentions(ContentItem ci,Collection<LinkedEntity> comentions, String language,
            Set<IRI> textAnnotations) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        
        Graph metadata = ci.getMetadata();
        //we MUST adjust the confidence level of existing annotations only once
        //se we need to keep track of those
        Set<BlankNodeOrIRI> adjustedSuggestions = new HashSet<BlankNodeOrIRI>();
        log.debug("Write Co-Mentions:");
        for(LinkedEntity comention : comentions){
            log.debug(" > {}",comention);
            //URIs of TextAnnotations for the initial mention of this co-mention
            Collection<IRI> initialMentions = new ArrayList<IRI>(comention.getSuggestions().size());
            for(Suggestion suggestion : comention.getSuggestions()){
                Entity entity = suggestion.getEntity();
                if(textAnnotations.contains(entity.getUri())){
//                if(entity.getData().filter(entity.getUri(),RDF_TYPE,ENHANCER_TEXTANNOTATION).hasNext()){
                    //this is a textAnnotation
                    initialMentions.add(entity.getUri());
                } //else TODO support also Entities!!
            }
            //create the TextAnnotations for the co-mention
            for(Occurrence occurrence : comention.getOccurrences()){
                Literal startLiteral = literalFactory.createTypedLiteral(occurrence.getStart());
                Literal endLiteral = literalFactory.createTypedLiteral(occurrence.getEnd());
                //search for existing text annotation
                boolean ignore = false;
                //search for textAnnotations with the same end
                IRI textAnnotation = null;
                Iterator<Triple> it = metadata.filter(null, ENHANCER_START, startLiteral);
                while(it.hasNext()){
                    Triple t = it.next();
                    Integer end = EnhancementEngineHelper.get(metadata, t.getSubject(), ENHANCER_END, Integer.class, literalFactory);
                    if(end != null && textAnnotations.contains(t.getSubject())){
                            //metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                        textAnnotation = (IRI)t.getSubject();
                        if(end > occurrence.getEnd()){
                            // there is an other TextAnnotation selecting a bigger Span
                            //so we should ignore this Occurrence
                            ignore = true;
                        }
                    }
                }
                it = metadata.filter(null, ENHANCER_END, endLiteral);
                while(it.hasNext()){
                    Triple t = it.next();
                    Integer start = EnhancementEngineHelper.get(metadata, t.getSubject(), ENHANCER_START, Integer.class, literalFactory);
                    if(start != null && textAnnotations.contains(t.getSubject())){
                            //metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                        textAnnotation = (IRI)t.getSubject();
                        if(start < occurrence.getStart()){
                            // there is an other TextAnnotation selecting a bigger Span
                            //so we should ignore this Occurrence
                            ignore = true;
                        }
                    }
                }
                if(!ignore){
                    //collect confidence values of co-mentions
                    Double maxConfidence = null; //maximum confidence of suggestions of the initial mention
                    Double maxExistingConfidence = null; //maximum confidence of existing suggestions
                    if(textAnnotation == null){ //not found ... create a new TextAnnotation for the co-mention
                        textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                        textAnnotations.add(textAnnotation); //add it to the set of TextAnnotations
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_START, 
                            startLiteral));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_END, 
                            endLiteral));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_SELECTION_CONTEXT, 
                            new PlainLiteralImpl(occurrence.getContext(),languageObject)));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_SELECTED_TEXT, 
                            new PlainLiteralImpl(occurrence.getSelectedText(),languageObject)));
                    } else { //if existing add this engine as contributor
                        metadata.add(new TripleImpl(textAnnotation, DC_CONTRIBUTOR, 
                            new PlainLiteralImpl(this.getClass().getName())));
                        //maxConfidence = EnhancementEngineHelper.get(metadata, textAnnotation, 
                        //    ENHANCER_CONFIDENCE, Double.class, literalFactory);
                    }
                    //now process initial mention(s) for the co-mention
                    Set<IRI> dcTypes = new HashSet<IRI>();
                    for(IRI initialMention : initialMentions){
                        //get the dc:type(s) of the initial mentions
                        Iterator<IRI> dcTypesIt = getReferences(metadata, initialMention, DC_TYPE);
                        while(dcTypesIt.hasNext()){
                            dcTypes.add(dcTypesIt.next());
                        }
                        //check confidence of the initial mention (fise:TextAnnotation)
                        Double confidnece = EnhancementEngineHelper.get(metadata, initialMention, 
                            ENHANCER_CONFIDENCE, Double.class, literalFactory);
                        if(confidnece != null){
                            if(maxConfidence == null){
                                maxConfidence = confidnece;
                            } else if(maxConfidence.compareTo(confidnece) <= 0){
                                maxConfidence = confidnece;
                            }
                        } //else nothing to do
                        //now we need to compare the suggestions of the initial
                        //mention(s) with the existing one. 
                        //Get information about the suggestions of the initial mention
                        Map<RDFTerm,Double> initialSuggestions = new HashMap<RDFTerm,Double>();
                        Map<RDFTerm, RDFTerm> initialSuggestedEntities = new HashMap<RDFTerm,RDFTerm>();
                        for(Iterator<Triple> suggestions = metadata.filter(null, DC_RELATION, initialMention); suggestions.hasNext();){
                            if(!textAnnotations.contains(suggestions)) {
                                BlankNodeOrIRI suggestion = suggestions.next().getSubject();
                                RDFTerm suggestedEntity = EnhancementEngineHelper.getReference(metadata, suggestion, ENHANCER_ENTITY_REFERENCE);
                                if(suggestedEntity != null){ //it has a suggestion
                                    Double confidence = EnhancementEngineHelper.get(
                                        metadata, suggestion, ENHANCER_CONFIDENCE, Double.class, literalFactory);
                                    if(maxConfidence == null){
                                        maxConfidence = confidence;
                                    } else if(confidnece != null && 
                                    		maxConfidence.compareTo(confidnece) <= 0){
                                        maxConfidence = confidnece;
                                    } //else nothing to do
                                    initialSuggestions.put(suggestion,confidence);
                                    initialSuggestedEntities.put(suggestedEntity, suggestion);
                                } //no suggestion (dc:relation to some other resource)
                            } // else ignore dc:relation to other fise:TextAnnotations
                        }
                        //now we collect existing Suggestions for this TextAnnoation where we need
                        //to adjust the confidence (quite some things to check ....)
                        Map<BlankNodeOrIRI, Double> existingSuggestions = new HashMap<BlankNodeOrIRI,Double>();
                    	if(maxConfidence != null && confidenceAdjustmentFactor < 1){
                    	    //suggestions are defined by incoming dc:releation
	                        for(Iterator<Triple> esIt = metadata.filter(null, DC_RELATION, textAnnotation);esIt.hasNext();){
	                        	BlankNodeOrIRI existingSuggestion = esIt.next().getSubject();
	                        	//but not all of them are suggestions
	                        	if(!textAnnotations.contains(existingSuggestion)) { //ignore fise:TextAnnotations
	                                Double existingConfidence = EnhancementEngineHelper.get(metadata, existingSuggestion, 
                                        ENHANCER_CONFIDENCE, Double.class, literalFactory);
	                                //ignore fise:TextAnnotations also suggested for the initial mention
                                    if(!initialSuggestions.containsKey(existingSuggestion)){
                                        RDFTerm suggestedEntity = EnhancementEngineHelper.getReference(metadata, existingSuggestion, ENHANCER_ENTITY_REFERENCE);
                                        //we might also have different fise:TextAnnotations that
                                        //fise:entity-reference to an Entity present in the
                                        //suggestions for the initial mention
                                        if(!initialSuggestedEntities.containsKey(suggestedEntity)){
                                            //finally make sure that we adjust confidences only once
                                            if(!adjustedSuggestions.contains(existingSuggestion)){
                                                existingSuggestions.put(existingSuggestion, existingConfidence);
                                            } //else confidence already adjusted
                                        } else { // different fise:EntityAnnotation, but same reference Entity
                                            //we need to check confidences to decide what to do
                                            RDFTerm initialSuggestion = initialSuggestedEntities.get(suggestedEntity);
                                            Double initialConfidence = initialSuggestions.get(initialSuggestion);
                                            if(initialConfidence == null || (existingConfidence != null && 
                                                    existingConfidence.compareTo(initialConfidence) >= 0)){
                                                //existing confidence >= initial .. keep existing
                                                initialSuggestions.remove(initialSuggestion); 
                                                if(maxExistingConfidence == null){
                                                    maxExistingConfidence = existingConfidence;
                                                } else if(maxExistingConfidence.compareTo(existingConfidence) <= 0){
                                                    maxExistingConfidence = existingConfidence;
                                                }
                                            } else { //initial has higher confidence
                                                //adjust this one (if not yet adjusted)
                                                if(!adjustedSuggestions.contains(existingSuggestion)){
                                                    existingSuggestions.put(existingSuggestion, existingConfidence);
                                                } 
                                            }
                                        }
                                    } else { //a initial mention already present
                                        //no need to process initial mention
                                        initialSuggestions.remove(existingSuggestion);
                                        if(maxExistingConfidence == null){
                                            maxExistingConfidence = existingConfidence;
                                        } else if(existingConfidence != null &&
                                        		maxExistingConfidence.compareTo(existingConfidence) <= 0){
                                            maxExistingConfidence = existingConfidence;
                                        } //else maxExistingConfidence == null (undefined)
                                    }
	                        	} //else ignore dc:relations to other fise:TextAnnotations
 	                        }
	                        for(Entry<BlankNodeOrIRI,Double> entry : existingSuggestions.entrySet()){
	                        	if(entry.getValue() != null){
	                        		double adjustedConfidence = entry.getValue() * confidenceAdjustmentFactor;
	                        		if(maxExistingConfidence == null || adjustedConfidence > maxExistingConfidence){
	                        			maxExistingConfidence = adjustedConfidence;
	                        		}
	                        		EnhancementEngineHelper.set(metadata, entry.getKey(), 
	                        				ENHANCER_CONFIDENCE, adjustedConfidence, literalFactory);
	                        		adjustedSuggestions.add(entry.getKey()); //mark as adjusted
	                        	}
	                        }
                    	}
                    	//add the suggestions of the initial mention to this one
                        for(RDFTerm suggestion : initialSuggestions.keySet()){
                            metadata.add(new TripleImpl((BlankNodeOrIRI)suggestion, DC_RELATION, textAnnotation));
    
                        }
                        //finally link the co-mentation with the initial one
                        metadata.add(new TripleImpl(textAnnotation, DC_RELATION, initialMention));
                        //metadata.add(new TripleImpl(initialMention, DC_RELATION, textAnnotation));
                    }
                    // Adapt the dc:type values of the fise:TextAnnotation
                    // - if Suggestions added by this engine do have the max confidence
                    //   use the dc:type values of the initial mention
                    // - if the original suggestions do have a higher confidence keep the
                    //   existing
                    // - in case both do have the same confidence we add all dc:types
                    boolean removeExistingDcTypes = maxConfidence != null && (maxExistingConfidence == null || 
                    		maxConfidence.compareTo(maxExistingConfidence) >= 0);
                    boolean addCoMentionDcTypes = maxExistingConfidence == null ||
                    		(maxConfidence != null && maxConfidence.compareTo(maxExistingConfidence) >= 1);
                    Iterator<IRI> existingDcTypesIt = getReferences(metadata, textAnnotation, DC_TYPE);
                    while(existingDcTypesIt.hasNext()){ //do not add existing
                    	//remove dc:type triples if they are not re-added later and
                    	//removeExistingDcTypes == true
                        if((!dcTypes.remove(existingDcTypesIt.next()) || !addCoMentionDcTypes )
                        	&& removeExistingDcTypes){
                        	existingDcTypesIt.remove(); //remove the dcType
                        }
                    }
                    if(addCoMentionDcTypes){
	                    for(IRI dcType : dcTypes){ //add missing
	                        metadata.add(new TripleImpl(textAnnotation, DC_TYPE, dcType));
	                    }
                    }
                    //TODO: support also Entities
                    if(maxConfidence != null){ //set the confidence value (if known)
                        EnhancementEngineHelper.set(metadata, textAnnotation, ENHANCER_CONFIDENCE, maxConfidence, literalFactory);
                    }
                } //else ignore this occurence
            }
        }
    }
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    
}
