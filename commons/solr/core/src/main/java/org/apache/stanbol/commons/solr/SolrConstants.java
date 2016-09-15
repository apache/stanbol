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
package org.apache.stanbol.commons.solr;

import java.io.File;
import java.util.Locale;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.Version;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.osgi.framework.Constants;

/**
 * Defines the keys used to register {@link SolrCore}s as OSGI services
 * @author Rupert Westenthaler
 *
 */
public final class SolrConstants {

    private SolrConstants(){/*do not create instances*/}
    /**
     * Used as prefix for all {@link CoreContainer} related properties
     */
    private static final String PROPERTY_SOLR_SERVER = "org.apache.solr.core.CoreContainer";
    /**
     * Used as prefix for all {@link SolrCore} related properties
     */
    private static final String PROPERTY_SOLR_CORE = "org.apache.solr.core.SolrCore";
    /**
     * Property used for the human readable name of a SolrServer. This will be used
     * as alternative to the absolute file path of the solr.xml file used for the
     * initialisation of the solr server
     */
    public static final String PROPERTY_SERVER_NAME = PROPERTY_SOLR_SERVER+".name";
    /**
     * The directory of the SolrServer. Values are expected to be {@link File} 
     * objects with <code>{@link File#isDirectory()}==true</code> or {@link String}
     * values containing a file path. The {@link File#getAbsolutePath()} will be
     * used to initialise the SolrServer.
     */
    public static final String PROPERTY_SERVER_DIR = PROPERTY_SOLR_SERVER+".dir";
    /**
     * the name of the solr.xml file defining the configuration for the Solr
     * Server. If not defined {@link #SOLR_XML_NAME} is used as default
     */
    public static final String PROPERTY_SOLR_XML_NAME = PROPERTY_SOLR_SERVER+".solrXml";
    /**
     * The registered {@link SolrCore} names for this server. Values are expected
     * to be a read only collection of names.
     */
    public static final String PROPERTY_SERVER_CORES = PROPERTY_SOLR_SERVER+".cores";
    /**
     * The {@link Constants#SERVICE_RANKING service ranking} of the Solr server.
     * If not defined that '0' is used as default.<p>
     * Values are expected to be Integers. This Property uses 
     * {@link Constants#SERVICE_RANKING} as key.
     */
    public static final String PROPERTY_SERVER_RANKING = Constants.SERVICE_RANKING;
    /**
     * Allows to enable/disable the publishing of the RESTful interface of Solr
     * on the OSGI HttpService by using the value of the {@link #PROPERTY_SERVER_NAME}
     * as path.
     */
    public static final String PROPERTY_SERVER_PUBLISH_REST = PROPERTY_SOLR_SERVER+".publishREST";
    /**
     * By default the RESTful API of a SolrServer is published
     */
    public static final boolean DEFAULT_PUBLISH_REST = true;
    /**
     * Property used for the name of a solr core. This is typically set by the
     * {@link SolrServerAdapter} implementation based on the name of the 
     * cores registered with a SolrServer.
     */
    public static final String PROPERTY_CORE_NAME = PROPERTY_SOLR_CORE+".name";
    /**
     * The directory of this core. This needs to be set if the
     * core is not located within a sub-directory within the
     * {@link #PROPERTY_SERVER_DIR} with the name {@link #PROPERTY_CORE_NAME}.
     */
    public static final String PROPERTY_CORE_DIR = PROPERTY_SOLR_CORE+".dir";
    /**
     * The data directory of a core. Set by the {@link SolrServerAdapter} when
     * registering a SolrCore based on {@link SolrCore#getDataDir()}
     */
    public static final String PROPERTY_CORE_DATA_DIR = PROPERTY_SOLR_CORE+".dadadir";
    /**
     * The index directory of a core. Set by the {@link SolrServerAdapter} when
     * registering a SolrCore based on {@link SolrCore#getIndexDir()}
     */
    public static final String PROPERTY_CORE_INDEX_DIR = PROPERTY_SOLR_CORE+".indexdir";
    /**
     * The name of the "schema.xml" file defining the solr schema for this core.
     * If not defined {@link #SOLR_SCHEMA_NAME} is used as default.
     */
    public static final String PROPERTY_CORE_SCHEMA = PROPERTY_SOLR_CORE+".schema";
    /**
     * The name of the "solrconf.xml" file defining the configuration for this
     * core. If not defined {@link #SOLR_SCHEMA_NAME} is used as default.
     */
    public static final String PROPERTY_CORE_SOLR_CONF = PROPERTY_SOLR_CORE+".solrconf";
    /**
     * The {@link Constants#SERVICE_ID} of the {@link CoreContainer} this core
     * is registered with. Values are of type {@link Long}.
     */
    public static final String PROPERTY_CORE_SERVER_ID = PROPERTY_SOLR_SERVER+".id";
    /**
     * The {@link Constants#SERVICE_RANKING service ranking} of the SolrCore. 
     * The ranking of the SolrServer is used as default if not defined. If also no 
     * ServiceRanking is defined for the server than '0' is used.<p>
     * Values are expected to be Integers. This Property uses 
     * {@link Constants#SERVICE_RANKING} as key.
     */
    public static final String PROPERTY_CORE_RANKING = Constants.SERVICE_RANKING;
    
    /**
     * Default name of the solr.xml file needed for the initialisation of a
     * {@link CoreContainer Solr server} 
     */
    public static final String SOLR_XML_NAME = "solr.xml";
    /**
     * default name of the Solrconfig.xml file needed for the initialisation of 
     * a {@link SolrCore}
     */
    public static final String SOLR_CONFIG_NAME = "solrconfig.xml";
    /**
     * Defualt name of the schema.xml file needed for the initialisation of 
     * a {@link SolrCore}
     */
    public static final String SOLR_SCHEMA_NAME = "schema.xml";

    /**
     * Key used to store the name of the {@link AbstractAnalysisFactory}. This is
     * the lower case version of the actual name excluding the detected
     * suffix.<p>
     * This property is added to {@link AbstractAnalysisFactory} instanced
     * registered as OSGI services to workaround the SPI typically used by Solr 4
     * to find anayzer factory instances.
     */
    public static final String PROPERTY_ANALYZER_FACTORY_NAME = "org.apache.lucene.analysis.factory.name";
    /**
     * The full qualified name of the {@link AbstractAnalysisFactory} implementation
     * registered with the {@link #PROPERTY_ANALYZER_FACTORY_NAME name}.
     */
    public static final String PROPERTY_ANALYZER_FACTORY_IMPL = "org.apache.lucene.analysis.factory.impl";
    /**
     * The full qualified name of the {@link AbstractAnalysisFactory} interface
     * implemented by the class. One of {@link CharFilterFactory}, 
     * {@link TokenizerFactory}or {@link TokenFilterFactory}.
     */
    public static final String PROPERTY_ANALYZER_FACTORY_TYPE = "org.apache.lucene.analysis.factory.type";
    
    public static final String PROPERTY_LUCENE_MATCH_VERSION = Version.class.getName().toLowerCase(Locale.ROOT);
    
}
