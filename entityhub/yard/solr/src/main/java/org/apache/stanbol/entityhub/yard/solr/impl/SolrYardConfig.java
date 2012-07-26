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
package org.apache.stanbol.entityhub.yard.solr.impl;

import java.util.Dictionary;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;

/**
 * Used for the configuration of a SolrYard. Especially if the SolrYard is not running within an OSGI context,
 * than an instance of this class must be configured and than parsed to the constructor of {@link SolrYard}.
 * <p>
 * When running within an OSGI context, the configuration is provided by the OSGI environment. I that case
 * this class is used as a wrapper for easy access to the configuration.
 * 
 * @author Rupert Westenthaler
 * 
 */
public final class SolrYardConfig extends YardConfig {

    /**
     * Creates a new config with the minimal set of required properties
     * 
     * @param id
     *            the ID of the Yard
     * @param solrServer
     *            the base URL of the {@link SolrServer}
     * @throws IllegalArgumentException
     *             if the parsed valued do not fulfil the requirements.
     */
    public SolrYardConfig(String id, String solrServer) throws IllegalArgumentException {
        super(id);
        setSolrServerLocation(solrServer);
        try {
            isValid();
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Initialise the Yard configuration based on a parsed configuration. Usually used on the context of an
     * OSGI environment in the activate method.
     * 
     * @param config
     *            the configuration usually parsed within an OSGI activate method
     * @throws ConfigurationException
     *             if the configuration is incomplete of some values are not valid
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as configuration
     */
    protected SolrYardConfig(Dictionary<String,Object> config) throws IllegalArgumentException,
                                                              ConfigurationException {
        super(config);
    }

//    /**
//     * Setter for the type of the SolrServer client to by used by the SolrYard. Setting the type to
//     * <code>null</code> will activate the default value. The default is determined based on the configured
//     * {@link #getSolrServerLocation()}
//     * 
//     * @param type
//     *            The type to use
//     */
//    public void setSolrServerType(SolrServerTypeEnum type) {
//        if (type == null) {
//            config.remove(SolrYard.SOLR_SERVER_TYPE);
//        } else {
//            config.put(SolrYard.SOLR_SERVER_TYPE, type);
//        }
//    }
//
//    public SolrServerTypeEnum getSolrServerType() {
//        Object serverType = config.get(SolrYard.SOLR_SERVER_TYPE);
//        if (serverType != null) {
//            if (serverType instanceof SolrServerTypeEnum) {
//                return (SolrServerTypeEnum) serverType;
//            } else {
//                try {
//                    return SolrServerTypeEnum.valueOf(serverType.toString());
//                } catch (IllegalArgumentException e) {
//                    // invalid value set!
//                    config.remove(SolrYard.SOLR_SERVER_TYPE);
//                }
//            }
//        }
//        // guess type based on Server Location
//        String serverLocation = getSolrServerLocation();
//        // TODO: maybe we need to improve this detection code.
//        if (serverLocation.startsWith("http")) {
//            return SolrServerTypeEnum.HTTP;
//        } else {
//            return SolrServerTypeEnum.EMBEDDED;
//        }
//    }

    /**
     * Setter for the location of the SolrServer. Might be a URL or a file.
     * 
     * @param url
     *            the base URL of the SolrServer. Required, NOT NULL.
     */
    public void setSolrServerLocation(String url) {
        if (url != null) {
            config.put(SolrYard.SOLR_SERVER_LOCATION, url);
        } else {
            config.remove(SolrYard.SOLR_SERVER_LOCATION);
        }
    }

    /**
     * Getter for the Location of the SolrServer. In case of an remote server this will be the base URL of the
     * RESTful interface. In case of an embedded Server it is the directory containing the solr.xml or the
     * directory of the core in case of a multi-core setup.
     * 
     * @return the URL or path to the SolrServer
     */
    public String getSolrServerLocation() throws IllegalStateException {
        Object value = config.get(SolrYard.SOLR_SERVER_LOCATION);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    /**
     * Setter for the multi yard index layout state.
     * <p>
     * The multi layout state. If data of multiple yards are stored in the same Solr Index, than the YardID
     * MUST be stored within all indexed documents. In addition the to all queries a fq (filterQuery) must be
     * added that restricts results to the current yard
     */
    public void setMultiYardIndexLayout(Boolean multiYardIndexLayoutState) {
        if (multiYardIndexLayoutState != null) {
            config.put(SolrYard.MULTI_YARD_INDEX_LAYOUT, multiYardIndexLayoutState);
        } else {
            config.remove(SolrYard.MULTI_YARD_INDEX_LAYOUT);
        }
    }

    /**
     * Getter for the multi yard index layout state.
     * <p>
     * If data of multiple yards are stored in the same Solr Index, than the YardID MUST be stored within all
     * indexed documents. In addition the to all queries a fq (filterQuery) must be added that restricts
     * results to the current yard.
     * <p>
     * The default value is <code>false</code>
     * 
     * @return the multi yard index layout state
     */
    public boolean isMultiYardIndexLayout() {
        Object value = config.get(SolrYard.MULTI_YARD_INDEX_LAYOUT);
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return false;
        }
    }

    /**
     * Getter for the state if this SolrYard can be initialised by using the default configuration or if it is
     * required to use a provided configuration. The default is set to <code>true</code>.
     * <p>
     * If this property is set to <code>false</code> than the SolrYard can only be initialised if the Index is
     * already available or the initial configuration is provided to the {@link SolrDirectoryManager}.
     * 
     * @return the state or <code>true</code> as default
     */
    public boolean isDefaultInitialisation() {
        Object value = config.get(SolrYard.SOLR_INDEX_DEFAULT_CONFIG);
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return SolrYard.DEFAULT_SOLR_INDEX_DEFAULT_CONFIG_STATE;
        }
    }

    /**
     * Setter for the state if this SolrYard can be initialised by using the default configuration or if it is
     * required to use a provided configuration. The default is set to <code>true</code>.
     * <p>
     * If this property is set to <code>false</code> than the SolrYard can only be initialised if the Index is
     * already available or the initial configuration is provided to the {@link SolrDirectoryManager}.
     * 
     * @param defaultInitialisationState
     *            the state or <code>null</code> to remove the current configuration. The default state is
     *            <code>true</code>.
     */
    public void setDefaultInitialisation(Boolean defaultInitialisationState) {
        if (defaultInitialisationState != null) {
            config.put(SolrYard.SOLR_INDEX_DEFAULT_CONFIG, defaultInitialisationState);
        } else {
            config.remove(SolrYard.SOLR_INDEX_DEFAULT_CONFIG);
        }
    }

    /**
     * Getter for the name of the configuration used to initialise the SolrServer. <p>
     * In case this property is not set the value of {@link #getSolrServerLocation()} 
     * is used as default.<p>
     * Please NOTE that in case <code>{@link #isDefaultInitialisation()} == true</code> 
     * the value of {@link SolrYard#DEFAULT_SOLR_INDEX_CONFIGURATION_NAME} MUST
     * BE used to initialise the SolrIndex instead of the value returned by this
     * Method!
     * @return the name of the configuration of the SolrIndex
     * @see SolrYard#SOLR_INDEX_CONFIGURATION_NAME
     * @see SolrYard#SOLR_INDEX_DEFAULT_CONFIG
     */
    public String getIndexConfigurationName() {
        Object value = config.get(SolrYard.SOLR_INDEX_CONFIGURATION_NAME);
        if (value != null) {
            return value.toString();
        } else {
            return getSolrServerLocation();
        }
    }

    /**
     * Setter for the name of the configuration used to initialise this SolrYard. Parsing <code>null</code>,
     * empty or equals to the {@link #getSolrServerLocation() Solr serve location} as 
     * name will remove this configuration.
     * 
     * @param name
     *            the name of the configuration.
     */
    public void setIndexConfigurationName(String name) {
        if (name == null || name.isEmpty()) {
            config.remove(SolrYard.SOLR_INDEX_CONFIGURATION_NAME);
        } else {
            config.put(SolrYard.SOLR_INDEX_CONFIGURATION_NAME, name);
        }
    }

    /**
     * Getter for the maximum number of boolean clauses allowed for queries
     * 
     * @return The configured number of <code>null</code> if not configured or the configured value is not an
     *         valid Integer.
     */
    public Integer getMaxBooleanClauses() {
        Object value = config.get(SolrYard.MAX_BOOLEAN_CLAUSES);
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public void setMaxBooleanClauses(Integer integer) {
        if (integer == null || integer.intValue() <= 0) {
            config.remove(SolrYard.MAX_BOOLEAN_CLAUSES);
        } else {
            config.put(SolrYard.MAX_BOOLEAN_CLAUSES, integer);
        }
    }

    public void setDocumentBoostFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            config.remove(SolrYard.DOCUMENT_BOOST_FIELD);
        } else {
            config.put(SolrYard.DOCUMENT_BOOST_FIELD, fieldName);
        }
    }

    public String getDocumentBoostFieldName() {
        Object name = config.get(SolrYard.DOCUMENT_BOOST_FIELD);
        return name == null ? null : name.toString();
    }

    public void setFieldBoosts(Map<String,Float> fieldBoosts) {
        if (fieldBoosts != null) {
            config.put(SolrYard.FIELD_BOOST_MAPPINGS, fieldBoosts);
        } else {
            config.remove(SolrYard.FIELD_BOOST_MAPPINGS);
        }
    }

    public Boolean isImmediateCommit() {
        Object value = config.get(SolrYard.IMMEDIATE_COMMIT);
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return null;
        }
    }

    public void setImmediateCommit(Boolean state) {
        if (state != null) {
            config.put(SolrYard.IMMEDIATE_COMMIT, state);
        } else {
            config.remove(SolrYard.IMMEDIATE_COMMIT);
        }
    }

    public final Integer getCommitWithinDuration() {
        Object value = config.get(SolrYard.COMMIT_WITHIN_DURATION);
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else {
            return null;
        }

    }

    public final void setCommitWithinDuration(Integer duration) {
        if (duration == null || duration.intValue() <= 0) {
            config.remove(SolrYard.COMMIT_WITHIN_DURATION);
        } else {
            config.put(SolrYard.COMMIT_WITHIN_DURATION, duration);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String,Float> getFieldBoosts() {
        Object fieldBoosts = config.get(SolrYard.FIELD_BOOST_MAPPINGS);
        if (fieldBoosts == null) {
            return null;
        } else if (fieldBoosts instanceof Map<?,?>) {
            return (Map<String,Float>) fieldBoosts;
        } else {
            // TODO: add support for parsing from String[] and Collection<String>
            return null;
        }
    }

    /**
     * checks for the {@link SolrYard#SOLR_SERVER_LOCATION}
     */
    @Override
    protected void validateConfig() throws ConfigurationException {
        try {
            String solrServer = getSolrServerLocation();
            if (solrServer == null) {
                throw new ConfigurationException(SolrYard.SOLR_SERVER_LOCATION,
                        "The URL of the Solr server MUST NOT be NULL!");
            }
        } catch (IllegalStateException e) {
            throw new ConfigurationException(SolrYard.SOLR_SERVER_LOCATION, e.getMessage(), e.getCause());
        }

    }
}
