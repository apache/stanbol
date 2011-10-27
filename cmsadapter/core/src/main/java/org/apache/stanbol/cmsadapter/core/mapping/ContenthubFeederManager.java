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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeederException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * This class manages the {@link ContenthubFeeder} instances loaded in the OSGI environment.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = ContenthubFeederManager.class)
public class ContenthubFeederManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, target = "(component.factory="
                                                                            + ContenthubFeeder.JCR_CONTENTHUB_FEEDER_FACTORY
                                                                            + ")")
    private ComponentFactory defaultJCRContentHubFeederFactory;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, target = "(component.factory="
                                                                            + ContenthubFeeder.CMIS_CONTENTUB_FEEDER_FACTORY
                                                                            + ")")
    private ComponentFactory defaultCMISContentHubFeederFactory;

    /*
     * Holds additional ContenthubFeeder implementations if there is any.
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ContenthubFeeder.class, policy = ReferencePolicy.DYNAMIC, bind = "bindContenthubFeeder", unbind = "unbindContenthubFeeder", strategy = ReferenceStrategy.EVENT)
    List<ContenthubFeeder> boundedFeeders = new CopyOnWriteArrayList<ContenthubFeeder>();

    /*
     * Holds instances of default implementations for JCR and CMIS ContenthubFeeders for different connection
     * information. Destruction of invalid instances (instances having invalid sessions) may be implemented.
     */
    private Map<ConnectionInfo,ComponentInstance> defaultFeeders = Collections
            .synchronizedMap(new HashMap<ConnectionInfo,ComponentInstance>());

    @Reference
    RepositoryAccessManager accessManager;

    /**
     * Looks for a suitable {@link ContenthubFeeder} instance based on the provided
     * <code>connectionInfo</code>.
     * 
     * @param connectionInfo
     * @return {@link ContenthubFeeder} instance.
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     * @see #getContenthubFeeder(ConnectionInfo, List)
     */
    public ContenthubFeeder getContenthubFeeder(ConnectionInfo connectionInfo) throws RepositoryAccessException,
                                                                              ContenthubFeederException {
        return getContenthubFeeder(connectionInfo, null);
    }

    /**
     * Looks for a suitable {@link ContenthubFeeder} instance based on the provided
     * <code>connectionInfo</code>. If there is an instance of a custom implementation provided in the
     * environment suitable for the connection type, it is retrieved first. If there is no custom
     * implementations in the OSGI environment, a new instance created based on the given
     * <code>connectionInfo</code> or an already existing one is returned.
     * 
     * @param connectionInfo
     * @return {@link ContenthubFeeder} instance.
     * @throws RepositoryAccessException
     * @throws ContenthubFeederException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ContenthubFeeder getContenthubFeeder(ConnectionInfo connectionInfo, List<String> contentProperties) throws RepositoryAccessException,
                                                                                                              ContenthubFeederException {

        // check additional implementations according to their connection types
        String connectionType = connectionInfo.getConnectionType();
        for (ContenthubFeeder feeder : boundedFeeders) {
            if (feeder.canFeed(connectionType)) {
                return feeder;
            }
        }

        // check default feeder instances
        // TODO: check whether the session object is still valid
        ComponentInstance componentInstance = defaultFeeders.get(connectionInfo);
        if (componentInstance != null) {
            Object value = componentInstance.getInstance();
            if (value != null) {
                return (ContenthubFeeder) value;
            } else {
                /*
                 * Default feeder instance for this connection info is not available, so remove it from the
                 * map
                 */
                defaultFeeders.remove(connectionInfo);
            }
        }

        org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo cInfo = mapConnectionInfo(connectionInfo);
        RepositoryAccess repositoryAccess = accessManager.getRepositoryAccessor(cInfo);
        Object session = repositoryAccess.getSession(cInfo);

        final Dictionary props = new Hashtable();
        props.put(ContenthubFeeder.PROP_SESSION, session);
        props.put(ContenthubFeeder.PROP_CONTENT_PROPERTIES, contentProperties);

        if (connectionType.equals(ConnectionInfo.JCR_CONNECTION_STRING)) {
            componentInstance = defaultJCRContentHubFeederFactory.newInstance(props);
        } else if (connectionType.equals(ConnectionInfo.CMIS_CONNECTION_STRING)) {
            componentInstance = defaultCMISContentHubFeederFactory.newInstance(props);
        } else {
            throw new ContenthubFeederException(String.format("Unexpected default connection type: %s",
                connectionType));
        }

        ContenthubFeeder feeder = (ContenthubFeeder) componentInstance.getInstance();
        defaultFeeders.put(connectionInfo, componentInstance);
        return feeder;
    }

    private org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo mapConnectionInfo(ConnectionInfo connectionInfo) {
        org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo cInfo = new org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo();
        cInfo.setConnectionType(connectionInfo.getConnectionType());
        cInfo.setPassword(connectionInfo.getPassword());
        cInfo.setUsername(connectionInfo.getUsername());
        cInfo.setRepositoryURL(connectionInfo.getRepositoryURL());
        cInfo.setWorkspaceName(connectionInfo.getWorkspaceIdentifier());
        return cInfo;
    }

    protected void bindContenthubFeeder(ContenthubFeeder contenthubFeeder) {
        boundedFeeders.add(contenthubFeeder);

    }

    protected void unbindContenthubFeeder(ContenthubFeeder contenthubFeeder) {
        boundedFeeders.remove(contenthubFeeder);
    }
}
