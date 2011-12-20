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
package org.apache.stanbol.enhancer.engines.langid;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.NIE_PLAINTEXTCONTENT;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.tika.language.LanguageIdentifier;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LangIdEnhancementEngine} provides functionality to enhance document
 * with their language.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
@Component(immediate = true, metatype = true)
@Service
public class LangIdEnhancementEngine implements EnhancementEngine, ServiceProperties {

    /**
     * a configurable value of the text segment length to check
     */
    @Property
    public static final String PROBE_LENGTH_PROP = "org.apache.stanbol.enhancer.engines.langid.probe-length";


    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING - 2;

    /**
     * This contains the only MIME type directly supported by this enhancement engine.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(LangIdEnhancementEngine.class);

    private static final int PROBE_LENGTH_DEFAULT = 1000;

    /**
     * How much text should be used for testing: If the value is 0 or smaller,
     * the complete text will be used. Otherwise a text probe of the given length
     * is taken from the middle of the text. The default length is 1000.
     */
    private int probeLength = PROBE_LENGTH_DEFAULT;

    /**
     * Initialize the language identifier model and load the prop length bound if
     * provided as a property.
     * 
     * @param ce
     *            the {@link ComponentContext}
     */
    protected void activate(ComponentContext ce) throws IOException {
        if (ce != null) {
            @SuppressWarnings("unchecked")
            Dictionary<String, String> properties = ce.getProperties();
            String lengthVal = properties.get(PROBE_LENGTH_PROP);
            probeLength = lengthVal == null ? PROBE_LENGTH_DEFAULT : Integer.parseInt(lengthVal);
        }
        LanguageIdentifier.initProfiles();
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }

        // TODO: check whether there is the graph contains the text
        UriRef subj = ci.getUri();
        Iterator<Triple> it = ci.getMetadata().filter(subj, NIE_PLAINTEXTCONTENT, null);
        if (it.hasNext()) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        String text = "";
        if (TEXT_PLAIN_MIMETYPE.equals(ci.getMimeType())) {
            try {
                text = IOUtils.toString(ci.getStream(),"UTF-8");
            } catch (IOException e) {
                throw new InvalidContentException(this, ci, e);
            }
        } else {
            Iterator<Triple> it = ci.getMetadata().filter(ci.getUri(), NIE_PLAINTEXTCONTENT, null);
            while (it.hasNext()) {
                text += it.next().getObject();
            }
        }
        if (text.trim().length() == 0) {
            log.warn("no text found");
            return;
        }

        // truncate text to some piece from the middle if probeLength > 0
        int checkLength = probeLength;
        if (checkLength > 0 && text.length() > checkLength) {
            text = text.substring(text.length() / 2 - checkLength / 2, text.length() / 2 + checkLength / 2);
        }
        LanguageIdentifier languageIdentifier = new LanguageIdentifier(text);
        String language = languageIdentifier.getLanguage();
        log.info("language identified as " + language);

        // add language to metadata
        MGraph g = ci.getMetadata();
        UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
        g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(language)));
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
