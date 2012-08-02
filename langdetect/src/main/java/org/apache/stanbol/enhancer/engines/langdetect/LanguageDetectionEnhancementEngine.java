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
package org.apache.stanbol.enhancer.engines.langdetect;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

/**
 * {@link LanguageDetectionEnhancementEngine} provides functionality to enhance document
 * with their language.
 *
 * @author Walter Kasper, DFKI
 */
@Component(immediate = true, metatype = true, inherit=true)
@Service
@Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value="langdetect")
})
public class LanguageDetectionEnhancementEngine 
        extends AbstractEnhancementEngine<LangDetectException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    /**
     * a configurable value of the text segment length to check
     */
    @Property
    public static final String PROBE_LENGTH_PROP = "org.apache.stanbol.enhancer.engines.langdetect.probe-length";


    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING} - 2<p>
     * NOTE: this information is used by the default and weighed {@link Chain}
     * implementation to determine the processing order of 
     * {@link EnhancementEngine}s. Other {@link Chain} implementation do not
     * use this information.
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING - 2;

    /**
     * This contains the only MIME type directly supported by this enhancement engine.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);

    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(LanguageDetectionEnhancementEngine.class);

    private static final int PROBE_LENGTH_DEFAULT = 1000;

    /**
     * How much text should be used for testing: If the value is 0 or smaller,
     * the complete text will be used. Otherwise a text probe of the given length
     * is taken from the middle of the text. The default length is 1000.
     */
    private int probeLength = PROBE_LENGTH_DEFAULT;
    
    /**
     * The literal factory
     */
    private final LiteralFactory literalFactory = LiteralFactory.getInstance();

    
    private LanguageIdentifier languageIdentifier;
    
    /**
     * Initialize the language identifier model and load the prop length bound if
     * provided as a property.
     * 
     * @param ce
     *            the {@link ComponentContext}
     */
    protected void activate(ComponentContext ce) throws ConfigurationException, LangDetectException {
        super.activate(ce);
        if (ce != null) {
            @SuppressWarnings("unchecked")
            Dictionary<String, String> properties = ce.getProperties();
            String lengthVal = properties.get(PROBE_LENGTH_PROP);
            probeLength = lengthVal == null ? PROBE_LENGTH_DEFAULT : Integer.parseInt(lengthVal);
        }
        languageIdentifier = new LanguageIdentifier();
    }
    
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        this.languageIdentifier = null;
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        if(ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null){
            return ENHANCE_ASYNC; //Langid now supports async processing
        } else {
            return CANNOT_ENHANCE;
        }
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
        if(contentPart == null){
            throw new IllegalStateException("No ContentPart with Mimetype '"
                    + TEXT_PLAIN_MIMETYPE+"' found for ContentItem "+ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }
        String text = "";
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            log.info("No text contained in ContentPart {} of ContentItem {}",
                contentPart.getKey(),ci.getUri());
            return;
        }

        // truncate text to some piece from the middle if probeLength > 0
        int checkLength = probeLength;
        if (checkLength > 0 && text.length() > checkLength) {
            text = text.substring(text.length() / 2 - checkLength / 2, text.length() / 2 + checkLength / 2);
        }
        List<Language> languages = null;
        try {
            languages = languageIdentifier.getLanguages(text);
            log.info("language identified: {}",languages);
        }
        catch (LangDetectException e) {
            log.warn("Could not identify language");
            return;
        }
        
        // add language to metadata
        if (languages.size() > 0) {
            MGraph g = ci.getMetadata();
            ci.getLock().writeLock().lock();
            // add best hypothesis
            Language oneLang = languages.get(0);
            try {
                UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(oneLang.lang)));
                g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(oneLang.prob)));
                g.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }
    }
    
    public int getProbeLength() {
        return probeLength;
    }

    public void setProbeLength(int probeLength) {
        this.probeLength = probeLength;
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

}
