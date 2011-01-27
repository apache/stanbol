/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.enhancer.clerezza;

import javax.ws.rs.core.MediaType;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.metadata.MetaDataGenerator;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.Store;


/**
 * This class generates metadata about image data.
 *
 * @author mir
 */
@Component(metatype = true)
@Service(MetaDataGenerator.class)
public class EnhancerMetaDataGenerator implements MetaDataGenerator {

    public static final Log log = LogFactory.getLog(EnhancerMetaDataGenerator.class);

    @Reference
    EnhancementJobManager jobManager;

    @Reference
    Store store;

    public void generate(GraphNode node, byte[] data, MediaType mediaType) {
        final ContentItem ci = store.create(
                ((UriRef) node.getNode()).getUnicodeString(), data,
                mediaType.toString());
        if (ci == null) {
            return;
        }
        try {
            jobManager.enhanceContent(ci);
        } catch (EngineException e) {
            // TODO: would be better wrapped as an exception accepted by the
            // MetaDataGenerator interface instead of hiding unexpected problems
            // in the logs and behave as if everything went well
            log.error(e, e);
        }
    }
}
