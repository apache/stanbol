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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private final Logger log = LoggerFactory.getLogger(SolrYardConfig.class);
    /**
     * The key used to configure the URL for the SolrServer
     */
    public static final String SOLR_SERVER_LOCATION = "org.apache.stanbol.entityhub.yard.solr.solrUri";
    /**
     * The key used to configure if data of multiple Yards are stored within the same index (
     * <code>default=false</code>)
     */
    public static final String MULTI_YARD_INDEX_LAYOUT = "org.apache.stanbol.entityhub.yard.solr.multiYardIndexLayout";
    /**
     * The maximum boolean clauses as configured in the solrconfig.xml of the SolrServer. The default value
     * for this config in Solr 1.4 is 1024.
     * <p>
     * This value is important for generating queries that search for multiple documents, because it
     * determines the maximum number of OR combination for the searched document ids.
     */
    public static final String MAX_BOOLEAN_CLAUSES = "org.apache.stanbol.entityhub.yard.solr.maxBooleanClauses";
    /**
     * This property allows to define a field that is used to parse the boost for the parsed representation.
     * Typically this will be the pageRank of that entity within the referenced site (e.g.
     * {@link Math#log1p(double)} of the number of incoming links
     */
    public static final String DOCUMENT_BOOST_FIELD = "org.apache.stanbol.entityhub.yard.solr.documentBoost";
    /**
     * Key used to configure {@link Entry Entry&lt;String,Float&gt;} for fields with the boost. If no Map is
     * configured or a field is not present in the Map, than 1.0f is used as Boost. If a Document boost is
     * present than the boost of a Field is documentBoost*fieldBoost.
     */
    public static final String FIELD_BOOST_MAPPINGS = "org.apache.stanbol.entityhub.yard.solr.fieldBoosts";
    /**
     * Key used to to enable/disable the default configuration. If this is enabled,
     * that the index will get initialised with the Default configuration.<p>
     * Notes:<ul>
     * <li> Configuration is only supported for EmbeddedSolrServers that use a
     * relative path
     * <li> If this property is enabled the value of the 
     * {@link #SOLR_INDEX_CONFIGURATION_NAME} will be ignored.
     * </ul>
     * Only applies in case a EmbeddedSolrServer is used.
     * @see SolrYardConfig#isAllowInitialisation()
     * @see SolrYardConfig#setAllowInitialisation(Boolean)
     */
    public static final String ALLOW_INITIALISATION_STATE = "org.apache.stanbol.entityhub.yard.solr.useDefaultConfig";
    /**
     * By default the use of an default configuration is disabled!
     */
    public static final boolean DEFAULT_ALLOW_INITIALISATION_STATE = false;
    /**
     * The name of the configuration use as default. 
     */
    public static final String DEFAULT_SOLR_INDEX_CONFIGURATION_NAME = "default.solrindex.zip";
    /**
     * Allows to configure the name of the index used for the configuration of the Solr Core.
     * Only applies in case of using an EmbeddedSolrServer and
     * {@link #ALLOW_INITIALISATION_STATE} is disabled.
     * As default the value of the {@link #SOLR_SERVER_LOCATION} is used.
     * @see SolrYardConfig#getIndexConfigurationName()
     * @see SolrYardConfig#setIndexConfigurationName(String)
     */
    public static final String SOLR_INDEX_CONFIGURATION_NAME = "org.apache.stanbol.entityhub.yard.solr.configName";
    /**
     * The default value for the maxBooleanClauses of SolrQueries. Set to {@value #DEFAULT_MAX_BOOLEAN_CLAUSES}
     * the default of Slor 1.4
     */
    protected static final int DEFAULT_MAX_BOOLEAN_CLAUSES = 1024;
    /**
     * Key used to enable/disable committing of update(..) and store(..) operations. Enabling this ensures
     * that indexed documents are immediately available for searches, but it will also decrease the
     * performance for updates.
     */
    public static final String IMMEDIATE_COMMIT = "org.apache.stanbol.entityhub.yard.solr.immediateCommit";
    /**
     * By default {@link #IMMEDIATE_COMMIT} is disabled (NOTE: was enabled, but changed with 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-1092">STANBOL-1092</a>)
     */
    public static final boolean DEFAULT_IMMEDIATE_COMMIT_STATE = false;
    /**
     * If {@link #IMMEDIATE_COMMIT} is deactivated, than this time is parsed to update(..) and store(..)
     * operations as the maximum time (in ms) until a commit.
     */
    public static final String COMMIT_WITHIN_DURATION = "org.apache.stanbol.entityhub.yard.solr.commitWithinDuration";
    /**
     * The default value for the {@link #COMMIT_WITHIN_DURATION} parameter is 10 sec.
     */
    public static final int DEFAULT_COMMIT_WITHIN_DURATION = 1000 * 10;

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

    /**
     * Setter for the location of the SolrServer. Might be a URL or a file.
     * 
     * @param url
     *            the base URL of the SolrServer. Required, NOT NULL.
     */
    public void setSolrServerLocation(String url) {
        if (url != null) {
            config.put(SOLR_SERVER_LOCATION, url);
        } else {
            config.remove(SOLR_SERVER_LOCATION);
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
        Object value = config.get(SOLR_SERVER_LOCATION);
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
            config.put(MULTI_YARD_INDEX_LAYOUT, multiYardIndexLayoutState);
        } else {
            config.remove(MULTI_YARD_INDEX_LAYOUT);
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
        Object value = config.get(MULTI_YARD_INDEX_LAYOUT);
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
    public boolean isAllowInitialisation() {
        Object value = config.get(ALLOW_INITIALISATION_STATE);
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return DEFAULT_ALLOW_INITIALISATION_STATE;
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
    public void setAllowInitialisation(Boolean defaultInitialisationState) {
        if (defaultInitialisationState != null) {
            config.put(ALLOW_INITIALISATION_STATE, defaultInitialisationState);
        } else {
            config.remove(ALLOW_INITIALISATION_STATE);
        }
    }

    /**
     * Getter for the name of the configuration used to initialise the SolrServer. <p>
     * In case this property is not set the value of {@link #getSolrServerLocation()} 
     * is used as default.<p>
     * Please NOTE that in case <code>{@link #isAllowInitialisation()} == true</code> 
     * the value of {@link SolrYard#DEFAULT_SOLR_INDEX_CONFIGURATION_NAME} MUST
     * BE used to initialise the SolrIndex instead of the value returned by this
     * Method!
     * @return the name of the configuration of the SolrIndex
     * @see SolrYard#SOLR_INDEX_CONFIGURATION_NAME
     * @see SolrYard#ALLOW_INITIALISATION_STATE
     */
    public String getIndexConfigurationName() {
        Object value = config.get(SOLR_INDEX_CONFIGURATION_NAME);
        if (value != null) {
            return value.toString();
        } else {
            return DEFAULT_SOLR_INDEX_CONFIGURATION_NAME;
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
            config.remove(SOLR_INDEX_CONFIGURATION_NAME);
        } else {
            config.put(SOLR_INDEX_CONFIGURATION_NAME, name);
        }
    }

    /**
     * Getter for the maximum number of boolean clauses allowed for queries
     * 
     * @return The configured number of <code>null</code> if not configured or the configured value is not an
     *         valid Integer.
     */
    public int getMaxBooleanClauses() {
        Object value = config.get(MAX_BOOLEAN_CLAUSES);
        int clauses;
        if (value != null) {
            if (value instanceof Integer) {
                clauses = ((Integer) value).intValue();
            } else {
                try {
                    clauses = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Unable to parse Integer property '" + MAX_BOOLEAN_CLAUSES
                        + "' from configured value '"+value+"'! Use default '"
                        + DEFAULT_MAX_BOOLEAN_CLAUSES + "' instead.",e);
                    clauses = DEFAULT_MAX_BOOLEAN_CLAUSES;
                }
            }
        } else {
            clauses = DEFAULT_MAX_BOOLEAN_CLAUSES;
        }
        if(clauses < 1){
            log.warn("Configured '{}={} is invalid (value MUST BE > 0). Use "
                    + "default {} instead.", new Object[]{ MAX_BOOLEAN_CLAUSES,
                            clauses, DEFAULT_MAX_BOOLEAN_CLAUSES});
            clauses = DEFAULT_MAX_BOOLEAN_CLAUSES;
        }
        return clauses;
    }

    public void setMaxBooleanClauses(Integer integer) {
        if (integer == null || integer.intValue() <= 0) {
            config.remove(MAX_BOOLEAN_CLAUSES);
        } else {
            config.put(MAX_BOOLEAN_CLAUSES, integer);
        }
    }

    public void setDocumentBoostFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            config.remove(DOCUMENT_BOOST_FIELD);
        } else {
            config.put(DOCUMENT_BOOST_FIELD, fieldName);
        }
    }

    public String getDocumentBoostFieldName() {
        Object name = config.get(DOCUMENT_BOOST_FIELD);
        return name == null ? null : name.toString();
    }

    public void setFieldBoosts(Map<String,Float> fieldBoosts) {
        if (fieldBoosts != null) {
            config.put(FIELD_BOOST_MAPPINGS, fieldBoosts);
        } else {
            config.remove(FIELD_BOOST_MAPPINGS);
        }
    }

    public boolean isImmediateCommit() {
        Object value = config.get(IMMEDIATE_COMMIT);
        if (value != null) {
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue();
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            return DEFAULT_IMMEDIATE_COMMIT_STATE;
        }
    }

    public void setImmediateCommit(Boolean state) {
        if (state != null) {
            config.put(IMMEDIATE_COMMIT, state);
        } else {
            config.remove(IMMEDIATE_COMMIT);
        }
    }

    public final int getCommitWithinDuration() {
        Object value = config.get(COMMIT_WITHIN_DURATION);
        int duration;
        if (value != null) {
            if (value instanceof Integer) {
                duration = ((Integer) value).intValue();
            } else {
                try {
                    duration = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Unable to parse Integer property '" + COMMIT_WITHIN_DURATION
                        + "' from configured value '"+value+"'! Use default "
                        + DEFAULT_COMMIT_WITHIN_DURATION + "ms instead.",e);
                    duration = DEFAULT_COMMIT_WITHIN_DURATION;
                }
            }
        } else {
            duration = DEFAULT_COMMIT_WITHIN_DURATION;
        }
        if(duration <= 0){
            log.warn("Configured '{}={}ms is invalid (value MUST BE >= 0). Use "
                + "default {}ms instead.", new Object[]{ COMMIT_WITHIN_DURATION,
                        duration, DEFAULT_COMMIT_WITHIN_DURATION});
            duration = DEFAULT_COMMIT_WITHIN_DURATION;
        }
        return duration;

    }

    public final void setCommitWithinDuration(Integer duration) {
        if (duration == null || duration.intValue() <= 0) {
            config.remove(COMMIT_WITHIN_DURATION);
        } else {
            config.put(COMMIT_WITHIN_DURATION, duration);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String,Float> getFieldBoosts() {
        Object fieldBoosts = config.get(FIELD_BOOST_MAPPINGS);
        if (fieldBoosts == null) {
            return null;
        } else if (fieldBoosts instanceof Map<?,?>) {
            return (Map<String,Float>) fieldBoosts;
        } else {
            // TODO: add support for parsing from String[] and Collection<String>
            return Collections.emptyMap();
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
                throw new ConfigurationException(SOLR_SERVER_LOCATION,
                        "The URL of the Solr server MUST NOT be NULL!");
            }
        } catch (IllegalStateException e) {
            throw new ConfigurationException(SOLR_SERVER_LOCATION, e.getMessage(), e.getCause());
        }

    }
}
