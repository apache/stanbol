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

package org.apache.stanbol.contenthub.helper.enhancementlistener.tcmanager;

import java.util.Dictionary;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.helper.enhancementlistener.EnhancementListenerFactory;
import org.apache.stanbol.contenthub.servicesapi.enhancements.vocabulary.EnhancementGraphVocabulary;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component(metatype = true, immediate = true)
@Service
public class EnhancementListenerFactoryTCM implements EnhancementListenerFactory {

    private static final Logger logger = LoggerFactory.getLogger(EnhancementListenerFactoryTCM.class);

    @Property(name = EnhancementListenerFactory.ENTITY_HUB_PROP, value = EnhancementListenerFactory.ENTITY_HUB_VALUE)
    private String entityHubURI;

    @Reference
    private TcManager tcManager;

    private EnhancementListener enhancementListener;

    @Activate
    protected void activate(ComponentContext cc) {
        logger.debug("Activating Ehnhancement Listener Factory");
        @SuppressWarnings("rawtypes")
        Dictionary properties = cc.getProperties();
        try {
            entityHubURI = (String) properties.get(EnhancementListenerFactory.ENTITY_HUB_PROP);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Configuration problem at starting enhancement listener factory", e);
        }

        if (enhancementListener == null) {
            try {
                enhancementListener = new EnhancementListener(tcManager, entityHubURI);
                enhancementListener.listen();
                logger.debug("Listener for graph {} is created",
                    EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
            } catch (Exception e) {
                logger.warn("Problem creating listener for graph {}",
                    EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
                logger.error("Error on listener creation", e);
            }
        } else {
            enhancementListener.listen();
            logger.debug("Graph {} is already being listened, no new listener is created",
                EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        logger.debug("Deactivating Enhancement Listener Factory");
        enhancementListener.unlisten();
    }

    /**
    @Override
    public Model getEnhancementModel() {
        if (this.enhancementListener == null) {
            logger.warn("Listener for graph {} is not ready yet.",
                EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
            return null;
        }
        return this.enhancementListener.getEnhancementModel();
    }

    @Override
    public IndexLARQ getIndex() {
        if (this.enhancementListener == null) {
            logger.warn("Listener for graph {} is not ready yet.",
                EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
            return null;
        }
        return this.enhancementListener.getIndex();
    }
    */

}
