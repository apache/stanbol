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
package org.apache.stanbol.enhancer.engines.uimaremote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.caslight.FeatureStructure;
import org.apache.stanbol.commons.caslight.FeatureStructureListHolder;
import org.apache.stanbol.enhancer.engines.uimaremote.tools.UIMASimpleServletClient;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a remote UIMA Client Enhancement Engine that communicates
 * with UIMA SimpleServlets.
 *
 * @author Mihaly Heder
 */
@Component(immediate = true, metatype = true, inherit = true, label = "UIMA Remote Client Enhancement Engine",
description = "Connects to one or more UIMA Simpleservlets and retreives annotations")
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "uimaremote")
})
public class UIMARemoteClient extends AbstractEnhancementEngine<RuntimeException, RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Property(cardinality = 1000, value = "sourcename;http://example.com/uimaend",
    label = "UIMA source name + endpoint",
    description = "Format: <sourceName>;<URI>. Example: sourcename;http://example.com/uimaend ."
    + "The sourcename will be used for referring internally to the UIMA endpoint")
    public static final String UIMA_ENDPOINTS = "stanbol.engine.uimaremote.endpoint";
    @Property(value = "uima.apache.org", label = "Content Part URI reference",
    description = "The URI Reference of the UIMA content part to be created. This content part will "
    + "contain Annotations from all the resources above.")
    public static final String UIMA_CONTENTPART_URIREF = "stanbol.engine.uimaremote.contentpart.uriref";
    @Property(cardinality = 1000, value = "text/plain", label = "Supported Mime Types",
    description = "Mime Types supported by this client. This should be aligned to the capabilities of the "
    + "UIMA Endpoints.")
    public static final String UIMA_SUPPORTED_MIMETYPES = "stanbol.engine.uimaremote.contentpart.mimetypes";
    public static final Integer defaultOrder = ServiceProperties.ORDERING_PRE_PROCESSING;
    private Set<String> SUPPORTED_MIMETYPES;
    private List<UIMASimpleServletClient> usscList;
    private String uimaUri;

    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Dictionary<String, Object> props = ctx.getProperties();

        if (props.get(UIMA_SUPPORTED_MIMETYPES) instanceof String[]) {

            SUPPORTED_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList((String[]) props.get(UIMA_SUPPORTED_MIMETYPES))));
        } else {
            logger.warn("Got String: '" + props.get(UIMA_SUPPORTED_MIMETYPES) + "' instead of String[] from Felix for param:"+UIMA_SUPPORTED_MIMETYPES);
            SUPPORTED_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList(new String[]{(String) props.get(UIMA_SUPPORTED_MIMETYPES)})));
        }
        
        String[] endpointsA;
        if (props.get(UIMA_ENDPOINTS) instanceof String[]) {
            endpointsA = (String[]) props.get(UIMA_ENDPOINTS);
        }
        else {
            logger.warn("Got String: '" + props.get(UIMA_ENDPOINTS) + "' instead of String[] from Felix for param:"+UIMA_ENDPOINTS);
            endpointsA = new String[]{(String) props.get(UIMA_ENDPOINTS)};
        }
        usscList = new ArrayList<UIMASimpleServletClient>();

        for (String endpoint : endpointsA) {
            String[] parts = endpoint.split(";", 2);
            if (parts.length == 2) {
                UIMASimpleServletClient ussc = new UIMASimpleServletClient();
                ussc.setSourceName(parts[0]);
                ussc.setUri(parts[1]);
                usscList.add(ussc);
            } else {
                logger.error("Enpoint '" + endpoint + "' cannot be configured. Proper format: <sourcename>;<uri>");
            }
        }

        this.uimaUri = (String) props.get(UIMA_CONTENTPART_URIREF);
    }

    @Override
    protected void deactivate(ComponentContext ctx) {
        usscList = null;
        super.deactivate(ctx);
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null) {
            return ENHANCE_ASYNC;
        }
        return CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if (contentPart == null) {
            throw new IllegalStateException("No ContentPart with an supported Mimetype '"
                    + SUPPORTED_MIMETYPES + "' found for ContentItem " + ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }

        for (UIMASimpleServletClient ussc : usscList) {
            logger.info("Accessing uima source:" + ussc.getSourceName() + " endpoint:" + ussc.getUri());
            List<FeatureStructure> featureSetList = ussc.process(text);
            IRI uimaIRI = new IRI(uimaUri);

            FeatureStructureListHolder holder;
            ci.getLock().writeLock().lock();
            try {
                holder = ci.getPart(uimaIRI, FeatureStructureListHolder.class);
            } catch (NoSuchPartException e) {
                holder = new FeatureStructureListHolder();
                logger.info("Adding FeatureSet List Holder content part with uri:" + uimaUri);
                ci.addPart(uimaIRI, holder);
                logger.info(uimaUri + " content part added.");
            } finally {
                ci.getLock().writeLock().unlock();
            }

            ci.getLock().writeLock().lock();
            try {
                holder.addFeatureStructureList(ussc.getSourceName(), featureSetList);
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }

    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }
}
