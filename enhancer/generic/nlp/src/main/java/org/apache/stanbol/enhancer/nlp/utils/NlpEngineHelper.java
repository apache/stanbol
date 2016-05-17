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
package org.apache.stanbol.enhancer.nlp.utils;

import static java.util.Collections.singleton;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link EnhancementEngine} implementations that
 * do use the {@link AnalysedText} content part
 * @author Rupert Westenthaler
 *
 */
public final class NlpEngineHelper {
    
    private static final Logger log = LoggerFactory.getLogger(NlpEngineHelper.class);

    private NlpEngineHelper(){}
    
    
    /**
     * Getter for the AnalysedText for a ContentItem
     * @param engine the EnhancementEngine calling this method (used for logging)
     * @param ci the ContentItem
     * @param exception <code>false</code> id used in {@link #canEnhance(ContentItem)}
     * and <code>true</code> when called from {@link #computeEnhancements(ContentItem)}
     * @return the AnalysedText or <code>null</code> if not found.
     * @throws IllegalStateException if exception is <code>true</code> and the
     * {@link AnalysedText} could not be retrieved from the parsed {@link ContentItem}.
     */
    public static AnalysedText getAnalysedText(EnhancementEngine engine, ContentItem ci, boolean exception) {
        AnalysedText at;
        try {
            at = AnalysedTextUtils.getAnalysedText(ci);
        }catch (RuntimeException e) {
            log.warn("Unable to retrieve AnalysedText for ContentItem "
                + ci + "because of an "+e.getClass().getSimpleName()+" with message "
                + e.getMessage(),e);
            at = null;
        }
        if(at != null){
            return at;
        }
        if(exception){
            throw new IllegalStateException("Unable to retrieve AnalysedText from ContentItem "
                    + ci+". As this is also checked in canEnhancer this may indicate an Bug in the "
                    + "used EnhancementJobManager!");
        } else {
            log.warn("The Enhancement Engine '{} (impl: {})' CAN NOT enhance "
                    + "ContentItem {} because the AnalysedText ContentPart is "
                    + "missing. Users might want to add an EnhancementEngine that "
                    + "creates the AnalysedText ContentPart such as the "
                    + "POSTaggingEngine (o.a.stanbol.enhancer.engines.opennlp.pos)!",
                    new Object[]{engine.getName(), engine.getClass().getSimpleName(),ci});
            return null;
        }
    }
    
    /**
     * Getter for the language of the content
     * @param ci the ContentItem
     * @param exception <code>false</code> id used in {@link #canEnhance(ContentItem)}
     * and <code>true</code> when called from {@link #computeEnhancements(ContentItem)}
     * @return the AnalysedText or <code>null</code> if not found.
     * @throws IllegalStateException if exception is <code>true</code> and the
     * language could not be retrieved from the parsed {@link ContentItem}.
     */
    public static String getLanguage(EnhancementEngine engine, ContentItem ci, boolean exception) {
        String language = EnhancementEngineHelper.getLanguage(ci);
        if(language != null) {
            return language;
        }
        if(exception){
            throw new IllegalStateException("Unable to retrieve the detected language for ContentItem "
                    + ci+". As this is also checked in canEnhancer this may indicate an Bug in the "
                    + "used EnhancementJobManager!");
        } else {
            log.warn("The Enhancement Engine '{} (impl: {})' CAN NOT enhance "
                    + "ContentItem {} because the langauge of "
                    + "this ContentItem is unknown. Users might want to add a "
                    + "Language Identification EnhancementEngine to the current "
                    + "EnhancementChain!",
                    new Object[]{engine.getName(), engine.getClass().getSimpleName(),ci});
            return null;
        }
    }
    /**
     * Used in {@link #canEnhance(ContentItem)} to check if a {@link ContentItem}
     * should be processed based on the language configuration of this engine.
     * @param engine the {@link EnhancementEngine} calling this method
     * @param languageConfiguration the language configuration
     * @param language the language
     * @param exception <code>false</code> id used in {@link #canEnhance(ContentItem)}
     * and <code>true</code> when called from {@link #computeEnhancements(ContentItem)}
     * @return the state
     * @throws IllegalStateException if exception is <code>true</code> and the
     * language is not configured as beeing processed.
     */
    public static boolean isLangaugeConfigured(EnhancementEngine engine, LanguageConfiguration languageConfiguration, String language, boolean exception){
        boolean state = languageConfiguration.isLanguage(language);
        if(!state && exception){
            throw new IllegalStateException("Language "+language+" is not included "
                    + "by the LanguageConfiguration of this engine (name "+ engine.getName()
                    + "). As this is also checked in canEnhancer this may indicate an Bug in the "
                    + "used EnhancementJobManager!");
        } else {
            return state;
        }
    }
    
    /**
     * Retrieves - or if not present - creates the {@link AnalysedText} content
     * part for the parsed {@link ContentItem}. If the {@link Blob} with the
     * mime type '<code>text/plain</code>' is present this method
     * throws an {@link IllegalStateException} (this method internally uses
     * {@link #getPlainText(EnhancementEngine, ContentItem, boolean)} with
     * <code>true</code> as third parameters. Users of this method should call
     * this method with <code>false</code> as third parameter in their 
     * {@link EnhancementEngine#canEnhance(ContentItem)} implementation.<p>
     * <i>NOTE:</i> This method is intended for Engines that want to create an
     * empty {@link AnalysedText} content part. Engines that assume that this
     * content part is already present (e.g. if the consume already existing
     * annotations) should use the 
     * {@link #getAnalysedText(EnhancementEngine, ContentItem, boolean)}
     * method instead.
     * @param engine the EnhancementEngine calling this method (used for logging)
     * @param analysedTextFactory the {@link AnalysedTextFactory} used to create
     * the {@link AnalysedText} instance (if not present).
     * @param ci the {@link ContentItem}
     * @return the AnalysedText
     * @throws EngineException on any exception while accessing the 
     * '<code>text/plain</code>' Blob
     * @throws IllegalStateException if no '<code>text/plain</code>' Blob is
     * present as content part of the parsed {@link ContentItem} or the parsed
     * {@link AnalysedTextFactory} is <code>null</code>. <i>NOTE</i> that 
     * {@link IllegalStateException} are only thrown if the {@link AnalysedText}
     * ContentPart is not yet present in the parsed {@link ContentItem}
     */
    public static AnalysedText initAnalysedText(EnhancementEngine engine, 
                                                AnalysedTextFactory analysedTextFactory,
                                                ContentItem ci) throws EngineException {
        AnalysedText at = AnalysedTextUtils.getAnalysedText(ci);
        if(at == null){
            if(analysedTextFactory == null){
                throw new IllegalStateException("Unable to initialise AnalysedText"
                    + "ContentPart because the parsed AnalysedTextFactory is NULL");
            }
            Entry<IRI,Blob> textBlob = getPlainText(engine, ci, true);
            //we need to create
            ci.getLock().writeLock().lock();
            try {
                //try again to retrieve (maybe an concurrent thread has created
                //the content part in the meantime
                at = AnalysedTextUtils.getAnalysedText(ci);
                if(at == null){
                    log.debug(" ... create new AnalysedText instance for Engine {}", engine.getName());
                    at = analysedTextFactory.createAnalysedText(ci, textBlob.getValue());
                }
            } catch (IOException e) {
                throw new EngineException("Unable to create AnalysetText instance for Blob "
                    + textBlob.getKey()+ " of ContentItem "+ci.getUri()+"!",e);
            } finally {
                ci.getLock().writeLock().unlock();
            }
        } else {
            log.debug(" ... use existing AnalysedText instance for Engine {}", engine.getName());
        }
        return at;
    }
    
    /**
     * Getter for the "<code>plain/text</code>" content part
     * @param ci the ContentItem
     * @param exception <code>false</code> id used in {@link #canEnhance(ContentItem)}
     * and <code>true</code> when called from {@link #computeEnhancements(ContentItem)}
     * @return the AnalysedText or <code>null</code> if not found.
     * @throws IllegalStateException if exception is <code>true</code> and the
     * language could not be retrieved from the parsed {@link ContentItem}.
     */
    public static Entry<IRI,Blob> getPlainText(EnhancementEngine engine, ContentItem ci, boolean exception) {
        Entry<IRI,Blob> textBlob = ContentItemHelper.getBlob(
            ci, singleton("text/plain"));
        if(textBlob != null) {
            return textBlob;
        }
        if(exception){
            throw new IllegalStateException("Unable to retrieve 'text/plain' ContentPart for ContentItem "
                    + ci+". As this is also checked in canEnhancer this may indicate an Bug in the "
                    + "used EnhancementJobManager!");
        } else {
            log.warn("The Enhancement Engine '{} (impl: {})' CAN NOT enhance "
                    + "ContentItem {} because no 'text/plain' ContentPart is "
                    + "present in this ContentItem. Users that need to enhance "
                    + "non-plain-text Content need to add an EnhancementEngine "
                    + "that supports the conversion of '{}' files to plain text "
                    + "to the current EnhancementChain!",
                    new Object[]{engine.getName(), engine.getClass().getSimpleName(),ci,ci.getMimeType()});
            return null;
        }
    }
    /**
     * Parsed the {@link NlpProcessingRole} typically provided by the 
     * {@link ServiceProperties#getServiceProperties()} provided by some
     * EnhancementEngines.<p>
     * This supports both {@link NlpProcessingRole} as well as String values
     * using the {@link NlpProcessingRole#name()}.
     * @param properties the properties (typically retrieved from the
     * {@link ServiceProperties#getServiceProperties()} method)
     * @return the NLP processing role or <code>null</code> if not present OR
     * an error while parsing.
     */
    public static NlpProcessingRole getNlpProcessingRole(Map<String,Object> properties){
        Object value = properties.get(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE);
        if(value instanceof NlpProcessingRole){
            return (NlpProcessingRole)value;
        } else if(value != null){
            try {
                return NlpProcessingRole.valueOf(value.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown NLP processing role {} -> return null",value);
                return null;
            }
        } else {
            return null;
        }
    }
}
