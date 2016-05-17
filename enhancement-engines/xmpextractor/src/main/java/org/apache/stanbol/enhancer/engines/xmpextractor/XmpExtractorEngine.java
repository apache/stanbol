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
package org.apache.stanbol.enhancer.engines.xmpextractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.tika.parser.image.xmp.XMPPacketScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true, metatype = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME, value="xmpextractor")
})
public class XmpExtractorEngine extends AbstractEnhancementEngine<IOException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {
    private static final Logger LOG = LoggerFactory.getLogger(XmpExtractorEngine.class);
    
    
    @Reference
    Parser parser;
  
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;


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
    	InputStream in = ci.getBlob().getStream();
    	XMPPacketScanner scanner = new XMPPacketScanner();
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	try {
			scanner.parse(in, baos);
		} catch (IOException e) {
			throw new EngineException(e);
		}
    	byte[] bytes = baos.toByteArray();
    	if (bytes.length > 0) {
	        Graph model = new IndexedGraph();
			parser.parse(model, new ByteArrayInputStream(bytes), "application/rdf+xml");
	        GraphNode gn = new GraphNode(
					new IRI("http://relative-uri.fake/"), model);
			gn.replaceWith(ci.getUri());
	        ci.getLock().writeLock().lock();
	        try { 
	            LOG.info("Model: {}",model);
	            ci.getMetadata().addAll(model);
	        } finally {
	            ci.getLock().writeLock().unlock();
	        }
    	}
    }
    
    private boolean isSupported(String mimeType) {
    	if (mimeType.startsWith("text/")) {
    		return false; //assuming text types cannot contain XMP
    	} else {
    		return true; // As there isn't a list of media types that can contain XMP
    	}
    }
    
    
}
