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
package org.apache.stanbol.enhancer.engines.entitylinking.engine;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CONTRIBUTOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
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
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Engine that consumes NLP processing results from the {@link AnalysedText}
 * content part of processed {@link ContentItem}s and links them with
 * Entities as provided by the configured {@link EntitySearcher} instance.
 * @author Rupert Westenthaler
 *
 */
public class EntityLinkingEngine implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(EntityLinkingEngine.class);
    /**
     * This is used to check the content type of parsed {@link ContentItem}s for
     * plain text
     */
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    protected static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
    /**
     * The default value for the Execution of this Engine.
     * This Engine creates TextAnnotations that should not be processed by other Engines.
     * Therefore it uses a lower rank than {@link ServiceProperties#ORDERING_DEFAULT}
     * to ensure that other engines do not get confused
     */
    public static final Integer DEFAULT_ORDER = ServiceProperties.ORDERING_DEFAULT - 10;
    
    private static final IRI XSD_DOUBLE = new IRI("http://www.w3.org/2001/XMLSchema#double");
    
    private static final IRI ENHANCER_ENTITY_RANKING = new IRI(NamespaceEnum.fise + "entity-ranking");
    
    /**
     * The name of this engine
     */
    protected final String name;
    /**
     * The entitySearcher used for linking
     */
    protected final EntitySearcher entitySearcher;
    /**
     * configuration for entity linking
     */
    protected final EntityLinkerConfig linkerConfig;
    /**
     * The label tokenizer
     */
    protected LabelTokenizer labelTokenizer;

    /**
     * The text processing configuration
     */
    protected final  TextProcessingConfig textProcessingConfig;
    /**
     * The literalFactory used to create typed literals
     */
    private LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link ReferencedSite} can operate
     * offline or not.
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     */
    @org.apache.felix.scr.annotations.Reference(
        cardinality = ReferenceCardinality.OPTIONAL_UNARY, 
        policy = ReferencePolicy.DYNAMIC, 
        bind = "enableOfflineMode", 
        unbind = "disableOfflineMode", 
        strategy = ReferenceStrategy.EVENT)
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
    
    /**
     * Internal Constructor used by {@link #createInstance(EntitySearcher, LanguageProcessingConfig, EntityLinkerConfig)}
     * @param entitySearcher The component used to lookup Entities
     * @param textProcessingConfig The configuration on how to use the {@link AnalysedText} content part of
     * processed {@link ContentItem}s
     * @param linkingConfig the configuration for the EntityLinker
     */
    public EntityLinkingEngine(String name, EntitySearcher entitySearcher,TextProcessingConfig textProcessingConfig, 
                                   EntityLinkerConfig linkingConfig, LabelTokenizer labelTokenizer){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed EnhancementEngine name MUST NOT be NULL!");
        }
        this.name = name;
        this.linkerConfig = linkingConfig != null ? linkingConfig : new EntityLinkerConfig();
        this.textProcessingConfig = textProcessingConfig;
        this.entitySearcher = entitySearcher;
        this.labelTokenizer = labelTokenizer;
    }
    /**
     * Getter for the {@link LabelTokenizer} used by this Engine
     * @return the labelTokenizer
     */
    public final LabelTokenizer getLabelTokenizer() {
        return labelTokenizer;
    }

    /**
     * Setter for the {@link LabelTokenizer} used by this Engine
     * @param labelTokenizer the labelTokenizer to set
     */
    public final void setLabelTokenizer(LabelTokenizer labelTokenizer) {
        this.labelTokenizer = labelTokenizer;
    }
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ENHANCEMENT_ENGINE_ORDERING,
            (Object) DEFAULT_ORDER));
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        log.trace("canEnhancer {}",ci.getUri());
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            log.warn("{} '{}' is inactive because EntitySearcher does not support Offline mode!",
                getClass().getSimpleName(),getName());
            return CANNOT_ENHANCE;
        }
        String language = getLanguage(this, ci, false);
        if(language == null || textProcessingConfig.getConfiguration(language) == null){
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[]{ getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        //we need a detected language, the AnalyzedText contentPart with
        //Tokens.
        AnalysedText at = getAnalysedText(this, ci, false);
        return at != null && at.getTokens().hasNext() ?
                ENHANCE_ASYNC : CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        log.trace(" enhance ci {}",ci.getUri());
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            throw new EngineException(this,ci,"Offline mode is not supported by the used EntitySearcher!",null);
        }
        AnalysedText at = getAnalysedText(this, ci, true);
        log.debug("  > AnalysedText {}",at);
        String language = getLanguage(this, ci, true);
        if(log.isDebugEnabled()){
            log.debug("computeEnhancements for ContentItem {} language {} text={}", 
                new Object []{ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(at.getSpan(), 100)});
        }
        log.debug("  > Language {}",language);
        LanguageProcessingConfig languageConfig = textProcessingConfig.getConfiguration(language);
        if(languageConfig == null){
            throw new IllegalStateException("The language '"+language+"' is not configured "
                    + "to be processed by this Engine. As this is already checked within the "
                    + "canEnhance(..) method this may indicate an bug in the used "
                    + "EnhanceemntJobManager implementation!");
        }
        EntityLinker entityLinker = new EntityLinker(at,language, 
            languageConfig, entitySearcher, linkerConfig, labelTokenizer);
        //process
        try {
            entityLinker.process();
        } catch (EntitySearcherException e) {
            log.error("Unable to link Entities with "+entityLinker,e);
            throw new EngineException(this, ci, "Unable to link Entities with "+entityLinker, e);
        }
        if(log.isInfoEnabled()){
            entityLinker.logStatistics(log);
        }
        //write results (requires a write lock)
        ci.getLock().writeLock().lock();
        try {
            writeEnhancements(ci, entityLinker.getLinkedEntities().values(), language,
                linkerConfig.isWriteEntityRankings());
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    /**
     * Writes the Enhancements for the {@link LinkedEntity LinkedEntities}
     * extracted from the parsed ContentItem
     * @param ci
     * @param linkedEntities
     * @param language
     */
    private void writeEnhancements(ContentItem ci, Collection<LinkedEntity> linkedEntities, 
            String language, boolean writeRankings) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        Set<IRI> dereferencedEntitis = new HashSet<IRI>();
        
        Graph metadata = ci.getMetadata();
        for(LinkedEntity linkedEntity : linkedEntities){
            Collection<IRI> textAnnotations = new ArrayList<IRI>(linkedEntity.getOccurrences().size());
            //first create the TextAnnotations for the Occurrences
            for(Occurrence occurrence : linkedEntity.getOccurrences()){
                Literal startLiteral = literalFactory.createTypedLiteral(occurrence.getStart());
                Literal endLiteral = literalFactory.createTypedLiteral(occurrence.getEnd());
                //search for existing text annotation
                Iterator<Triple> it = metadata.filter(null, ENHANCER_START, startLiteral);
                IRI textAnnotation = null;
                while(it.hasNext()){
                    Triple t = it.next();
                    if(metadata.filter(t.getSubject(), ENHANCER_END, endLiteral).hasNext() &&
                            metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                        textAnnotation = (IRI)t.getSubject();
                        break;
                    }
                }
                if(textAnnotation == null){ //not found ... create a new one
                    textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
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
                    metadata.add(new TripleImpl(textAnnotation, 
                        Properties.ENHANCER_CONFIDENCE, 
                        literalFactory.createTypedLiteral(linkedEntity.getScore())));
                } else { //if existing add this engine as contributor
                    metadata.add(new TripleImpl(textAnnotation, DC_CONTRIBUTOR, 
                        new PlainLiteralImpl(this.getClass().getName())));
                }
                //add dc:types (even to existing)
                for(IRI dcType : linkedEntity.getTypes()){
                    metadata.add(new TripleImpl(
                        textAnnotation, Properties.DC_TYPE, dcType));
                }
                textAnnotations.add(textAnnotation);
            }
            //now the EntityAnnotations for the Suggestions
            for(Suggestion suggestion : linkedEntity.getSuggestions()){
                IRI entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                //should we use the label used for the match, or search the
                //representation for the best label ... currently its the matched one
                Literal label = suggestion.getBestLabel(linkerConfig.getNameField(),language);
                Entity entity = suggestion.getEntity();
                metadata.add(new TripleImpl(entityAnnotation, Properties.ENHANCER_ENTITY_LABEL, label));
                metadata.add(new TripleImpl(entityAnnotation,ENHANCER_ENTITY_REFERENCE, entity.getUri()));
                Iterator<IRI> suggestionTypes = entity.getReferences(linkerConfig.getTypeField());
                while(suggestionTypes.hasNext()){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.ENHANCER_ENTITY_TYPE, suggestionTypes.next()));
                }
                metadata.add(new TripleImpl(entityAnnotation,
                    Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(suggestion.getScore())));
                for(IRI textAnnotation : textAnnotations){
                    metadata.add(new TripleImpl(entityAnnotation, Properties.DC_RELATION, textAnnotation));
                }
                //add origin information of the EntiySearcher
                for(Entry<IRI,Collection<RDFTerm>> originInfo : entitySearcher.getOriginInformation().entrySet()){
                    for(RDFTerm value : originInfo.getValue()){
                        metadata.add(new TripleImpl(entityAnnotation, 
                            originInfo.getKey(),value));
                    }
                }
                if(writeRankings){
                    Float ranking = suggestion.getEntity().getEntityRanking();
                    if(ranking != null){
                        metadata.add(new TripleImpl(entityAnnotation, 
                            ENHANCER_ENTITY_RANKING,
                            //write the float as double
                            new TypedLiteralImpl(ranking.toString(), XSD_DOUBLE)));
                    }
                }
                //in case dereferencing of Entities is enabled we need also to
                //add the RDF data for entities
                if(linkerConfig.isDereferenceEntitiesEnabled() &&
                        dereferencedEntitis.add(entity.getUri())){ //not yet dereferenced
                    //add all outgoing triples for this entity
                    //NOTE: do not add all triples as there might be other data in the graph
                    for(Iterator<Triple> triples = entity.getData().filter(entity.getUri(), null, null);
                            triples.hasNext();metadata.add(triples.next()));
                }
            }
        }
    }

}
