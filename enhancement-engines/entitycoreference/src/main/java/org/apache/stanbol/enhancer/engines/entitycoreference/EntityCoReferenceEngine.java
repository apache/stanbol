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
package org.apache.stanbol.enhancer.engines.entitycoreference;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.NounPhrase;
import org.apache.stanbol.enhancer.engines.entitycoreference.impl.CoreferenceFinder;
import org.apache.stanbol.enhancer.engines.entitycoreference.impl.NounPhraseFilterer;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This engine extracts references in the given text of noun phrases which point to NERs. The coreference is
 * performed based on matching several of the named entity's dbpedia/yago properties to the noun phrase
 * tokens.
 * 
 * TODO - Be able to detect possessive coreferences such as Germany's prime minister 
 * TODO - be able to detect products and their developer such as Iphone 7 and Apple's new device. 
 * TODO - provide the ability via config for the user to also allow coreferencing of 1 word noun phrases based 
 * solely on comparison with entity class type?
 * 
 * @author Cristian Petroaca
 * 
 */
@Component(immediate = true, metatype = true)
@Service(value = EnhancementEngine.class)
@Properties(value = {
                     @Property(name = EnhancementEngine.PROPERTY_NAME, value = "entity-coreference"),
                     @Property(name = EntityCoReferenceEngine.CONFIG_LANGUAGES, value = "en"),
                     @Property(name = EntityCoReferenceEngine.REFERENCED_SITE_ID, value = "entity-coref-dbpedia"),
                     @Property(name = EntityCoReferenceEngine.ENTITY_URI_BASE, value = "http://dbpedia.org/resource/"),
                     @Property(name = EntityCoReferenceEngine.MAX_DISTANCE, intValue = Constants.MAX_DISTANCE_DEFAULT_VALUE),
					 @Property(name = EntityCoReferenceEngine.SPATIAL_ATTR_FOR_PERSON, value = Constants.DEFAULT_SPATIAL_ATTR_FOR_PERSON),
					 @Property(name = EntityCoReferenceEngine.SPATIAL_ATTR_FOR_ORGANIZATION, value = Constants.DEFAULT_SPATIAL_ATTR_FOR_ORGANIZATION),
					 @Property(name = EntityCoReferenceEngine.SPATIAL_ATTR_FOR_PLACE, value = Constants.DEFAULT_SPATIAL_ATTR_FOR_PLACE),
					 @Property(name = EntityCoReferenceEngine.ORG_ATTR_FOR_PERSON, value = Constants.DEFAULT_ORG_ATTR_FOR_PERSON),
					 @Property(name = EntityCoReferenceEngine.ENTITY_CLASSES_TO_EXCLUDE, value = Constants.DEFAULT_ENTITY_CLASSES_TO_EXCLUDE)})
public class EntityCoReferenceEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    private static final Integer ENGINE_ORDERING = ServiceProperties.ORDERING_POST_PROCESSING + 91;

    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    protected static final String CONFIG_LANGUAGES = "enhancer.engine.entitycoreference.languages";

    /**
     * Referenced site configuration. Defaults to dbpedia.
     */
    protected static final String REFERENCED_SITE_ID = "enhancer.engine.entitycoreference.referencedSiteId";

    /**
     * 
     */
    protected static final String ENTITY_URI_BASE = "enhancer.engine.entitycoreference.entity.uri.base";
    
    /**
     * Maximum sentence distance between the ner and the noun phrase which mentions it. -1 means no distance
     * constraint.
     */
    protected static final String MAX_DISTANCE = "enhancer.engine.entitycoreference.maxDistance";

    /**
     * Attributes used for spatial coreference when dealing with a person entity.
     */
    protected static final String SPATIAL_ATTR_FOR_PERSON = "enhancer.engine.entitycoreference.spatial.attr.person";
    
    /**
     * Attributes used for spatial coreference when dealing with an organization entity.
     */
    protected static final String SPATIAL_ATTR_FOR_ORGANIZATION = "enhancer.engine.entitycoreference.spatial.attr.org";
    
    /**
     * Attributes used for spatial coreference when dealing with a place entity.
     */
    protected static final String SPATIAL_ATTR_FOR_PLACE = "enhancer.engine.entitycoreference.spatial.attr.place";
    
    /**
     * Attributes used for organisational membership coreference when dealing with a person entity.
     */
    protected static final String ORG_ATTR_FOR_PERSON = "enhancer.engine.entitycoreference.org.attr.person";
    
    /**
     * Entity classes which will be excluded when doing the entity class type matching 
     * because they are too general in nature.
     */
    protected static final String ENTITY_CLASSES_TO_EXCLUDE = "enhancer.engine.entitycoreference.entity.classes.excluded";
    
    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(EntityCoReferenceEngine.class);

    /**
     * Service of the Entityhub that manages all the active referenced Site. This Service is used to lookup
     * the configured Referenced Site when we need to enhance a content item.
     */
    @Reference
    protected SiteManager siteManager;

    /**
     * Used to lookup Entities if the {@link #REFERENCED_SITE_ID} property is set to "entityhub" or "local"
     */
    @Reference
    protected Entityhub entityhub;

    /**
     * Specialized class which filters out bad noun phrases based on the language.
     */
    private NounPhraseFilterer nounPhraseFilterer;

    /**
     * Performs the logic needed to find corefs based on the NERs and noun phrases in the text.
     */
    private CoreferenceFinder corefFinder;

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);

        Dictionary<String,Object> config = ctx.getProperties();

        /* Step 1 - initialize the {@link NounPhraseFilterer} with the language config */
        String languages = (String) config.get(CONFIG_LANGUAGES);

        if (languages == null || languages.isEmpty()) {
            throw new ConfigurationException(CONFIG_LANGUAGES,
                    "The Languages Config is a required Parameter and MUST NOT be NULL or an empty String!");
        }

        nounPhraseFilterer = new NounPhraseFilterer(languages.split(","));

        /* Step 2 - initialize the {@link CoreferenceFinder} */
        String referencedSiteID = null;
        Object referencedSiteIDfromConfig = config.get(REFERENCED_SITE_ID);

        if (referencedSiteIDfromConfig == null) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }

        referencedSiteID = referencedSiteIDfromConfig.toString();
        if (referencedSiteID.isEmpty()) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }

        if (Entityhub.ENTITYHUB_IDS.contains(referencedSiteID.toLowerCase())) {
            log.debug("Init NamedEntityTaggingEngine instance for the Entityhub");
            referencedSiteID = null;
        }

        int maxDistance;
        Object maxDistanceFromConfig = config.get(MAX_DISTANCE);

        if (maxDistanceFromConfig == null) {
            maxDistance = Constants.MAX_DISTANCE_DEFAULT_VALUE;
        } else if (maxDistanceFromConfig instanceof Number) {
            maxDistance = ((Number) maxDistanceFromConfig).intValue();
        } else {
            try {
                maxDistance = Integer.parseInt(maxDistanceFromConfig.toString());
            } catch (NumberFormatException nfe) {
                throw new ConfigurationException(MAX_DISTANCE, "The Max Distance parameter must be a number");
            }
        }

        if (maxDistance < -1) {
            throw new ConfigurationException(MAX_DISTANCE,
                    "The Max Distance parameter must not be smaller than -1");
        }
        
        String entityUriBase = (String) config.get(ENTITY_URI_BASE);
        if (entityUriBase == null || entityUriBase.isEmpty()) {
        	throw new ConfigurationException(ENTITY_URI_BASE, "The Entity Uri Base parameter cannot be empty");
        }
        
        String spatialAttrForPerson = (String) config.get(SPATIAL_ATTR_FOR_PERSON);
        String spatialAttrForOrg = (String) config.get(SPATIAL_ATTR_FOR_ORGANIZATION);
        String spatialAttrForPlace = (String) config.get(SPATIAL_ATTR_FOR_PLACE);
        String orgAttrForPerson = (String) config.get(ORG_ATTR_FOR_PERSON);
        String entityClassesToExclude = (String) config.get(ENTITY_CLASSES_TO_EXCLUDE);
        
        corefFinder = new CoreferenceFinder(languages.split(","), siteManager, entityhub, referencedSiteID,
                maxDistance, entityUriBase, spatialAttrForPerson, spatialAttrForOrg, 
                spatialAttrForPlace, orgAttrForPerson, entityClassesToExclude);

        log.info("activate {}[name:{}]", getClass().getSimpleName(), getName());
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) ENGINE_ORDERING));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        String language = getLanguage(this, ci, false);
        if (language == null) {
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not detected.",
                new Object[] {getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }

        if (!nounPhraseFilterer.supportsLanguage(language)) {
            log.debug("Engine {} does not support language {}.", new Object[] {getName(), language});
            return CANNOT_ENHANCE;
        }

        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        /*
         * Step 1 - Build the NER list and the noun phrase list.
         * 
         * TODO - the noun phrases need to be lemmatized.
         */
        Map<Integer,List<Span>> ners = new HashMap<Integer,List<Span>>();
        List<NounPhrase> nounPhrases = new ArrayList<NounPhrase>();
        extractNersAndNounPhrases(ci, ners, nounPhrases);

        /*
         * If there are no NERs to reference there's nothing to do but exit.
         */
        if (ners.size() == 0) {
            log.info("Did not find any NERs for which to do the coreferencing");
            return;
        }

        /*
         * Step 2 - Filter out bad noun phrases.
         */
        String language = getLanguage(this, ci, false);
        if (language == null) {
            log.info("Could not detect the language of the text");
            return;
        }

        nounPhraseFilterer.filter(nounPhrases, language);

        /*
         * If there are no good noun phrases there's nothing to do but exit.
         */
        if (nounPhrases.size() == 0) {
            log.info("Did not find any noun phrases with which to do the coreferencing");
            return;
        }

        /*
         * Step 3 - Extract corefs and write them as {@link NlpAnnotations.COREF_ANNOTATION}s in the {@link
         * Span}s
         */
        corefFinder.extractCorefs(ners, nounPhrases, language);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        log.info("deactivate {}[name:{}]", getClass().getSimpleName(), getName());

        nounPhraseFilterer = null;
        corefFinder = null;

        super.deactivate(ctx);
    }

    /**
     * Extracts the NERs and the noun phrases from the given text and puts them in the given lists.
     * 
     * @param ci
     * @param ners
     * @param nounPhrases
     */
    private void extractNersAndNounPhrases(ContentItem ci,
                                           Map<Integer,List<Span>> ners,
                                           List<NounPhrase> nounPhrases) {
        AnalysedText at = NlpEngineHelper.getAnalysedText(this, ci, true);
        Iterator<? extends Section> sections = at.getSentences();
        if (!sections.hasNext()) { // process as single sentence
            sections = Collections.singleton(at).iterator();
        }

        int sentenceCnt = 0;
        while (sections.hasNext()) {
            sentenceCnt++;
            Section section = sections.next();
            List<NounPhrase> sectionNounPhrases = new ArrayList<NounPhrase>();
            List<Span> sectionNers = new ArrayList<Span>();

            Iterator<Span> chunks = section.getEnclosed(EnumSet.of(SpanTypeEnum.Chunk));
            while (chunks.hasNext()) {
                Span chunk = chunks.next();

                Value<NerTag> ner = chunk.getAnnotation(NlpAnnotations.NER_ANNOTATION);
                if (ner != null) {
                    sectionNers.add(chunk);
                }

                Value<PhraseTag> phrase = chunk.getAnnotation(NlpAnnotations.PHRASE_ANNOTATION);
                if (phrase != null && phrase.value().getCategory() == LexicalCategory.Noun) {
                    sectionNounPhrases.add(new NounPhrase(chunk, sentenceCnt));
                }
            }

            for (NounPhrase nounPhrase : sectionNounPhrases) {
                Iterator<Span> tokens = section.getEnclosed(EnumSet.of(SpanTypeEnum.Token));

                while (tokens.hasNext()) {
                    Span token = tokens.next();

                    if (nounPhrase.containsSpan(token)) {
                        nounPhrase.addToken(token);
                    }
                }

                for (Span sectionNer : sectionNers) {
                    if (nounPhrase.containsSpan(sectionNer)) {
                        nounPhrase.addNerChunk(sectionNer);
                    }
                }
            }

            nounPhrases.addAll(sectionNounPhrases);

            if (!sectionNers.isEmpty()) {
                ners.put(sentenceCnt, sectionNers);
            }
        }
    }
}
