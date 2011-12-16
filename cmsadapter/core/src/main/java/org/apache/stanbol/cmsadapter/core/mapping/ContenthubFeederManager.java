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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.repository.SessionManager;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeederException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the {@link ContenthubFeeder} instances loaded in the OSGI environment. It provides
 * retrieval of suitable instance based on the provided <b>session</b> or <b>session key</b>.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = ContenthubFeederManager.class)
public class ContenthubFeederManager {

    private static final Logger log = LoggerFactory.getLogger(ContenthubFeederManager.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ContenthubFeeder.class, policy = ReferencePolicy.DYNAMIC, bind = "bindContenthubFeeder", unbind = "unbindContenthubFeeder", strategy = ReferenceStrategy.EVENT)
    List<ContenthubFeeder> boundedFeeders = new CopyOnWriteArrayList<ContenthubFeeder>();

    @Reference
    RepositoryAccessManager accessManager;

    @Reference
    SessionManager sessionManager;

    /**
     * Looks for a suitable {@link ContenthubFeeder} instance based on the provided <code>session</code>. It
     * initializes the feeder instance with the given parameters.
     * 
     * @param sessionKey
     *            Session key to retrieve previously cached session to access the repository
     * @param contentProperties
     *            Content repository object properties to be checked for the content itself e.g
     *            <b>dbpedia-owl:abstract</b>.
     * @return suitable {@link ContenthubFeeder} instance.
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     */
    public ContenthubFeeder getContenthubFeeder(String sessionKey, List<String> contentProperties) throws RepositoryAccessException,
                                                                                                  ContenthubFeederException {
        Object session = sessionManager.getSession(sessionKey);
        return getContenthubFeeder(session, contentProperties);
    }

    /**
     * Looks for a suitable {@link ContenthubFeeder} instance based on the provided <code>session</code>. It
     * initializes the feeder instance with the given parameters.
     * 
     * @param session
     *            Session object to be used to access to content repository
     * @param contentProperties
     *            Content repository object properties to be checked for the content itself e.g
     *            <b>dbpedia-owl:abstract</b>.
     * @return suitable {@link ContenthubFeeder} instance.
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ContenthubFeeder getContenthubFeeder(Object session, List<String> contentProperties) throws RepositoryAccessException,
                                                                                               ContenthubFeederException {

        ContenthubFeeder feeder = null;
        for (ContenthubFeeder f : boundedFeeders) {
            if (f.canFeedWith(session)) {
                feeder = f;
            }
        }

        if (feeder != null) {
            final Dictionary props = new Hashtable();
            props.put(ContenthubFeeder.PROP_SESSION, session);
            props.put(ContenthubFeeder.PROP_CONTENT_PROPERTIES, contentProperties != null ? contentProperties
                    : new ArrayList<String>());
            feeder.setConfigs(props);
        } else {
            log.warn("Failed to obtain a suitable ContenthubFeeder instance for session: {}", session);
            throw new ContenthubFeederException(String.format(
                "Failed to obtain a suitable ContenthubFeeder instance for session: %s", session));
        }

        return feeder;
    }

    protected void bindContenthubFeeder(ContenthubFeeder contenthubFeeder) {
        boundedFeeders.add(contenthubFeeder);

    }

    protected void unbindContenthubFeeder(ContenthubFeeder contenthubFeeder) {
        boundedFeeders.remove(contenthubFeeder);
    }
}