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
package org.apache.stanbol.enhancer.topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Helper class to factorize some common code for Solr Core tracking OSGi component
 */
public abstract class ConfiguredSolrCoreTracker {

    protected String solrCoreId;

    protected RegisteredSolrServerTracker indexTracker;

    // instance of classifierSolrServer to use if not using the OSGi service tracker (e.g. for tests)
    protected SolrServer solrServer;

    protected ComponentContext context;

    abstract public void configure(Dictionary<String,Object> config) throws ConfigurationException;

    protected String getRequiredStringParam(Dictionary<String,Object> parameters, String paramName) throws ConfigurationException {
        return getRequiredStringParam(parameters, paramName, null);
    }

    protected String getRequiredStringParam(Dictionary<String,Object> config,
                                            String paramName,
                                            String defaultValue) throws ConfigurationException {
        Object paramValue = config.get(paramName);
        if (paramValue == null) {
            if (defaultValue == null) {
                throw new ConfigurationException(paramName, paramName + " is a required parameter.");
            } else {
                return defaultValue;
            }
        }
        return paramValue.toString();
    }

    @SuppressWarnings("unchecked")
    protected List<String> getStringListParan(Dictionary<String,Object> config, String paramName) throws ConfigurationException {
        Object paramValue = config.get(paramName);
        if (paramValue == null) {
            return new ArrayList<String>();
        } else if (paramValue instanceof String) {
            return Arrays.asList(paramValue.toString().split(",\\s*"));
        } else if (paramValue instanceof String[]) {
            return Arrays.asList((String[]) paramValue);
        } else if (paramValue instanceof List) {
            return (List<String>) paramValue;
        } else {
            throw new ConfigurationException(paramName, String.format(
                "Unexpected parameter type for '%s': %s", paramName, paramValue));
        }
    }

    /**
     * @return the manually bound classifierSolrServer instance or the one tracked by the OSGi service tracker.
     */
    public SolrServer getActiveSolrServer() {
        return solrServer != null ? solrServer : indexTracker.getService();
    }

    protected void configureSolrCore(Dictionary<String,Object> config, String solrCoreProperty) throws ConfigurationException {
        if (config.get(solrCoreProperty) instanceof SolrServer) {
            // Bind a fixed Solr server client instead of doing dynamic OSGi lookup using the service tracker.
            // This can be useful both for unit-testing .
            solrServer = (SolrServer) config.get(solrCoreProperty);
        } else {
            String solrCoreId = getRequiredStringParam(config, solrCoreProperty);
            if (context == null) {
                throw new ConfigurationException(solrCoreProperty,
                        solrCoreProperty + " should be a SolrServer instance for using"
                                + " the engine without any OSGi context. Got: " + solrCoreId);
            }
            try {
                indexTracker = new RegisteredSolrServerTracker(context.getBundleContext(),
                        IndexReference.parse(solrCoreId));
                indexTracker.open();
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException(solrCoreProperty, e.getMessage(), e);
            }
        }
    }

}
