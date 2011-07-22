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
package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import org.semanticdesktop.aperture.extractor.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HtmlExtractorFactory.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class HtmlExtractorFactory extends
        org.semanticdesktop.aperture.extractor.html.HtmlExtractorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlExtractorFactory.class);
    
    public static String REGISTRY_CONFIGURATION = "htmlextractors.xml";
    private HtmlExtractionRegistry registry;
    private HtmlParser parser;

    public HtmlExtractorFactory() throws InstantiationException {
        this.parser = new HtmlParser();
        try {
            registry = new HtmlExtractionRegistry(REGISTRY_CONFIGURATION);
        }
        catch (InitializationException e) {
            LOG.error("Registry Initialization Error: " + e.getMessage());
            throw new InstantiationException(e.getMessage());
        }
    }
    
    @Override
    public Extractor get() {
        return new IksHtmlExtractor(registry, parser);
    }

}
