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
package org.apache.stanbol.enhancer.engines.textannotationnewmodel.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getBlob;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_PREFIX;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_SUFFIX;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(policy = ConfigurationPolicy.OPTIONAL, metatype = true, immediate = true)
@Service
@Properties(value = {
        @Property(name = EnhancementEngine.PROPERTY_NAME, value="text-annotation-new-model"),
        @Property(name = TextAnnotationsNewModelEngine.PROPERTY_PREFIX_SUFFIX_SIZE, 
                intValue=TextAnnotationsNewModelEngine.DEFAULT_PREFIX_SUFFIX_SIZE),
        @Property(name = Constants.SERVICE_RANKING, intValue=0)
})
public class TextAnnotationsNewModelEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(TextAnnotationsNewModelEngine.class);

    public static final String PROPERTY_PREFIX_SUFFIX_SIZE = "enhancer.engines.textannotationnewmodel.prefixSuffixSize";
    public static final int DEFAULT_PREFIX_SUFFIX_SIZE = EnhancementEngineHelper.DEFAULT_PREFIX_SUFFIX_LENGTH;
    
    // the order in which this engine is executed.
    public static final Integer ENGINE_ORDER = ServiceProperties.ORDERING_POST_PROCESSING - 20;

    private static final Set<String> supportedMimeTypes = Collections.singleton("text/plain");
    
    private LiteralFactory lf = LiteralFactory.getInstance();
    
    private int prefixSuffixSize;
    
    /**
     * Get the service properties (basically the engine order).
     */
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) ENGINE_ORDER));
    }

    /**
     * States whether can enhance the provided ContentItem.
     */
    @Override
    public int canEnhance(ContentItem contentItem) throws EngineException {
        if(getBlob(contentItem, supportedMimeTypes) != null){
            return ENHANCE_ASYNC;
        } else {
            return CANNOT_ENHANCE;
        }
    }

    /**
     * Computes the enhancements on the provided ContentItem.
     */
    @Override
    public void computeEnhancements(ContentItem contentItem) throws EngineException {
        Entry<UriRef,Blob> textBlob = getBlob(contentItem, supportedMimeTypes);
        if(textBlob == null){
            return;
        }
        String language = EnhancementEngineHelper.getLanguage(contentItem);
        Language lang = language == null ? null : new Language(language);
        String text;
        try {
             text = ContentItemHelper.getText(textBlob.getValue());
        } catch (IOException e) {
            throw new EngineException(this, contentItem, "Unable to read Plain Text Blob", e);
        }
        Map<NonLiteral,Literal[]> prefixSuffixMap = new HashMap<NonLiteral,Literal[]>();
        MGraph metadata = contentItem.getMetadata();
        //extract all the necessary information within a read lock
        contentItem.getLock().readLock().lock();
        try {
            Iterator<Triple> it = metadata.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
            while(it.hasNext()){
                NonLiteral ta = it.next().getSubject();
                boolean hasPrefix = metadata.filter(ta, ENHANCER_SELECTION_PREFIX, null).hasNext();
                boolean hasSuffix = metadata.filter(ta, ENHANCER_SELECTION_SUFFIX, null).hasNext();
                if(hasPrefix && hasSuffix){
                    continue; //this TextAnnotation already uses the new model
                }
                Integer start;
                if(!hasPrefix){
                    start = EnhancementEngineHelper.get(metadata, ta, ENHANCER_START, Integer.class, lf);
                    if(start == null){
                        log.debug("unable to add fise:selection-prefix to TextAnnotation {} "
                            + "because fise:start is not present",ta);
                    }
                } else {
                    start = null;
                }
                Integer  end;
                if(!hasSuffix){
                    end = EnhancementEngineHelper.get(metadata, ta, ENHANCER_END, Integer.class, lf);
                    if(end == null){
                        log.debug("unable to add fise:selection-suffix to TextAnnotation {} "
                            + "because fise:end is not present",ta);
                    }
                } else {
                    end = null;
                }
                if(end != null || start != null){
                    prefixSuffixMap.put(ta, new Literal[]{
                            start == null ? null : new PlainLiteralImpl(text.substring(
                                Math.max(0,start-prefixSuffixSize), start), lang),
                            end == null ? null : new PlainLiteralImpl(text.substring(
                                end,Math.min(text.length(), end+prefixSuffixSize)),lang)
                    });
                }
            }
        } finally {
            contentItem.getLock().readLock().unlock();
        }
        //finally write the prefix/suffix triples within a write lock
        if(!prefixSuffixMap.isEmpty()){
            contentItem.getLock().writeLock().lock();
            try {
                for(Entry<NonLiteral,Literal[]> entry : prefixSuffixMap.entrySet()){
                    if(entry.getValue()[0] != null){
                        metadata.add(new TripleImpl(entry.getKey(), 
                            ENHANCER_SELECTION_PREFIX, entry.getValue()[0]));
                    } //else prefix already present
                    if(entry.getValue()[1] != null){
                        metadata.add(new TripleImpl(entry.getKey(), 
                            ENHANCER_SELECTION_SUFFIX, entry.getValue()[1]));
                    } //else suffix already present
                }
            } finally {
                contentItem.getLock().writeLock().unlock();
            }
        }
    }

    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException, RuntimeException {
        super.activate(ctx);
        Object value = ctx.getProperties().get(PROPERTY_PREFIX_SUFFIX_SIZE);
        if(value instanceof Number){
            prefixSuffixSize = ((Number)value).intValue();
        } else if (value != null){
            try {
                prefixSuffixSize = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(PROPERTY_PREFIX_SUFFIX_SIZE, "The value MUST be an Integer", e);
            }
        } else {
            prefixSuffixSize = DEFAULT_PREFIX_SUFFIX_SIZE;
        }
        if(prefixSuffixSize < EnhancementEngineHelper.MIN_PREFIX_SUFFIX_SIZE){
            throw new ConfigurationException(PROPERTY_PREFIX_SUFFIX_SIZE, 
                "The prefixSuffixSize MUST BE >= " + EnhancementEngineHelper.MIN_PREFIX_SUFFIX_SIZE);
        }
    }

    @Override
    protected void deactivate(ComponentContext ctx) throws RuntimeException {
        prefixSuffixSize = 0;
        super.deactivate(ctx);
    }
}
