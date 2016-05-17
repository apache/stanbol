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
package org.apache.stanbol.enhancer.engines.htmlextractor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.BundleURIResolver;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.ClerezzaRDFUtils;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.ExtractorException;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlExtractionRegistry;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlExtractor;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.HtmlParser;
import org.apache.stanbol.enhancer.engines.htmlextractor.impl.InitializationException;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */
@Component(immediate = true, metatype = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME, value="htmlextractor")
})
public class HtmlExtractorEngine extends AbstractEnhancementEngine<IOException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlExtractorEngine.class);
    
    /**
     * The default charset
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;

    private static final String DEFAULT_HTML_EXTRACTOR_REGISTRY = "htmlextractors.xml";

    /**
     * name of a file that defines the set of extractors for HTML documents. By default, the builtin file 'htmlextractors.xml' is used."
     */
    @Property(value=HtmlExtractorEngine.DEFAULT_HTML_EXTRACTOR_REGISTRY)
    public static final String HTML_EXTRACTOR_REGISTRY = "org.apache.stanbol.enhancer.engines.htmlextractor.htmlextractors";

    /**
     * Internally used to create additional {@link Blob} for transformed
     * versions af the original content
     */
    @Reference
    private ContentItemFactory ciFactory;
    
    BundleContext bundleContext;

    private Set<String> supportedMimeTypes = new HashSet<String>(Arrays.asList(new String[]{"text/html","application/xhtml+xml"}));
    
    private HtmlExtractionRegistry htmlExtractorRegistry;
    private HtmlParser htmlParser;
    
    private boolean singleRootRdf = true;
   
    // define the Nepomuk NIE namespace locally here
    private static final String NIE_NS = "http://www.semanticdesktop.org/ontologies/2007/01/19/nie#";

    protected void activate(ComponentContext ce) throws ConfigurationException, IOException  {
        super.activate(ce);
        this.bundleContext = ce.getBundleContext();
        BundleURIResolver.BUNDLE = this.bundleContext.getBundle();
        String htmlExtractors = DEFAULT_HTML_EXTRACTOR_REGISTRY;
        Dictionary<String, Object> properties = ce.getProperties();
        String confFile = (String)properties.get(HTML_EXTRACTOR_REGISTRY);
        if (confFile != null && confFile.trim().length() > 0) {
            htmlExtractors = confFile;
        }
        try {
            this.htmlExtractorRegistry = new HtmlExtractionRegistry(htmlExtractors);
        }
        catch (InitializationException e) {
            LOG.error("Registry Initialization Error: " + e.getMessage());
            throw new IOException(e.getMessage());
        }
        this.htmlParser = new HtmlParser();

    }

    /**
     * The deactivate method.
     *
     * @param ce the {@link ComponentContext}
     */
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        this.htmlParser = null;
        this.htmlExtractorRegistry = null;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        LOG.info("MimeType: {}", ci.getMimeType());
        if (isSupported(ci.getMimeType())) {
            return ENHANCE_ASYNC;
        }
        return CANNOT_ENHANCE;
    }
    
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        HtmlExtractor extractor = new HtmlExtractor(htmlExtractorRegistry, htmlParser);
        Graph model = new SimpleGraph();
        ci.getLock().readLock().lock();
        try {
            extractor.extract(ci.getUri().getUnicodeString(), ci.getStream(),null, ci.getMimeType(), model);
        } catch (ExtractorException e) {
            throw new EngineException("Error while processing ContentItem "
                    + ci.getUri()+" with HtmlExtractor",e);
        } finally {
            ci.getLock().readLock().unlock();
        }
        ClerezzaRDFUtils.urifyBlankNodes(model);
        // make the model single rooted
        if (singleRootRdf) {
            ClerezzaRDFUtils.makeConnected(model,ci.getUri(),new IRI(NIE_NS+"contains"));
        }
        //add the extracted triples to the metadata of the ContentItem
        ci.getLock().writeLock().lock();
        try { 
            LOG.info("Model: {}",model);
            ci.getMetadata().addAll(model);
            model = null;
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }
    
    private boolean isSupported(String mimeType) {
        return this.supportedMimeTypes.contains(mimeType);
    }
    
    
}
