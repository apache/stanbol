package org.apache.stanbol.commons.marmotta.kiwi;
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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.marmotta.kiwi.config.CacheMode;
import org.apache.marmotta.kiwi.config.CachingBackends;
import org.apache.marmotta.kiwi.config.KiWiConfiguration;
import org.apache.marmotta.kiwi.persistence.KiWiDialect;
import org.apache.marmotta.kiwi.persistence.h2.H2Dialect;
import org.apache.marmotta.kiwi.persistence.mysql.MySQLDialect;
import org.apache.marmotta.kiwi.persistence.pgsql.PostgreSQLDialect;
import org.apache.marmotta.kiwi.sail.KiWiStore;
import org.apache.marmotta.kiwi.sparql.sail.KiWiSparqlSail;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI Component that allows to setup a Sesame {@link Repository} for a
 * KiWi TripleStore. The configured {@link Repository} is registered as
 * OSGI service so that it can be used by other components. <p>
 * All {@link ComponentContext#getProperties() configuration properties} parsed
 * to this component are also added to the registered service. This menas that
 * others can use them to listen for a specific {@link Repository} instance 
 * (by using an {@link Filter}). Especially usefull for filtering is the
 * {@link #KIWI_REP_ID}<p>
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 * @author Rupert Westenthaler - refactored to directly register the {@link Repository}
 * as OSGI service.
 */
@Component(
        configurationFactory=true, //allow multiple instances
        policy= ConfigurationPolicy.REQUIRE, //the ID is required!
        specVersion="1.1",
        metatype = true,
        immediate = true
)
public class KiWiRepositoryService {

    private static Logger log = LoggerFactory.getLogger(KiWiRepositoryService.class);

    /**
     * The '</code>org.openrdf.repository.Repository.id</code>' intended to be 
     * used to {@link Filter} for a specific {@link Repository} instance.
     */
    @Property(value="kiwi")
    public static final String REPOSITORY_ID = "org.openrdf.repository.Repository.id";
    /**
     * The '</code>org.openrdf.sail.Sail.impl</code>' intended to be 
     * used to {@link Filter} for a specific {@link Sail} implementations backing
     * the registered {@link Repository} service. <p>
     * The KiWiRepositoryService sets this property to the {@link Class#getName()} of
     * the {@link KiWiStore} for every registered {@link Repository}.
     */
    public static final String SAIL_IMPL = "org.openrdf.sail.Sail.impl";
    
    /**
     * The Database dialect
     */
    @Property(options = {
            @PropertyOption(value = '%' + KiWiRepositoryService.DB_DIALECT + ".option." 
                    + KiWiRepositoryService.DB_DIALECT_POSTGRES, name = KiWiRepositoryService.DB_DIALECT_POSTGRES),
            @PropertyOption(value = '%' + KiWiRepositoryService.DB_DIALECT + ".option." 
                    + KiWiRepositoryService.DB_DIALECT_MYSQL, name = KiWiRepositoryService.DB_DIALECT_MYSQL),
            @PropertyOption(value = '%' + KiWiRepositoryService.DB_DIALECT + ".option." 
                    + KiWiRepositoryService.DB_DIALECT_H2, name = KiWiRepositoryService.DB_DIALECT_H2)},
    value = KiWiRepositoryService.DEFAULT_DB_DIALECT)
    public static final String DB_DIALECT = "marmotta.kiwi.dialect";

    /**
     * Dialect for Postgres
     */
    public static final String DB_DIALECT_POSTGRES = "postgres";
    /**
     * Diablect for MySQL
     */
    public static final String DB_DIALECT_MYSQL = "mysql";
    /**
     * Dialect for H2
     */
    public static final String DB_DIALECT_H2 = "h2";
    
    /**
     * The default database dialect is {@link #DB_DIALECT_POSTGRES}
     */
    public static final String DEFAULT_DB_DIALECT = DB_DIALECT_H2;

    /**
     * The database URL. THis property can be used instead of configuring
     * the DB options in separate properties. If this property is present the
     * other DB configurations will get ignored.
     */
    @Property
    public static final String DB_URL = "marmotta.kiwi.dburl";

    /**
     * The database host
     */
    @Property(value = KiWiRepositoryService.DEFAULT_DB_HOST)
    public static final String DB_HOST = "marmotta.kiwi.host";

    /**
     * The default database host (a file path in case of H2)
     */
    public static final String DEFAULT_DB_HOST = "${sling.home}/marmotta/kiwi";

    /**
     * The database port
     */
    @Property(intValue = KiWiRepositoryService.DEFAULT_DB_PORT)
    public static final String DB_PORT = "marmotta.kiwi.port";

    /**
     * The default database part (<code>-1</code> to use the default)
     */
    public static final int DEFAULT_DB_PORT = -1;
    
    /**
     * The database name
     */
    @Property(value = KiWiRepositoryService.DEFAULT_DB_NAME)
    public static final String DB_NAME = "marmotta.kiwi.database";

    /**
     * The default database name
     */
    public static final String DEFAULT_DB_NAME = "kiwi";
    
    /**
     * The database user name
     */
    @Property(value = KiWiRepositoryService.DEFAULT_DB_USER)
    public static final String DB_USER = "marmotta.kiwi.user";

    /**
     * The default db user
     */
    public static final String DEFAULT_DB_USER = "kiwi";

    /**
     * The database user password
     */
    @Property(value = KiWiRepositoryService.DEFAULT_DB_PASS)
    public static final String DB_PASS = "marmotta.kiwi.password";

    /**
     * The default password
     */
    public static final String DEFAULT_DB_PASS = "kiwi";

    /**
     * Additional DB options
     */
    @Property(value=";MVCC=true;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=10")
    public static final String DB_OPTS = "marmotta.kiwi.options";

    /**
     * The cluster name 
     */
    @Property(value=KiWiRepositoryService.DEFAULT_CLUSTER)
    public static final String CLUSTER = "marmotta.kiwi.cluster";


    public static final String DEFAULT_CLUSTER = "kiwi default";

    /**
     * The cluster address 
     */
    @Property
    public static final String CLUSTER_ADDRESS = "marmotta.kiwi.cluster.address";

    /**
     * The cluster port 
     */
    @Property(intValue=-1)
    public static final String CLUSTER_PORT = "marmotta.kiwi.cluster.port";

    @Property(options={
            @PropertyOption(value='%' + KiWiRepositoryService.CACHE_MODE + ".option.local",
                    name=KiWiRepositoryService.CACHE_MODE_LOCAL),
            @PropertyOption(value='%' + KiWiRepositoryService.CACHE_MODE + ".option.replicated",
                    name=KiWiRepositoryService.CACHE_MODE_REPLICATED),
            @PropertyOption(value='%' + KiWiRepositoryService.CACHE_MODE + ".option.distributed",
                    name=KiWiRepositoryService.CACHE_MODE_DISTRIBUTED)}, 
            value=KiWiRepositoryService.DEFAULT_CACHE_MODE)
    public static final String CACHE_MODE = "marmotta.kiwi.cluster.cachemode";

    public static final String CACHE_MODE_LOCAL = "LOCAL";
    public static final String CACHE_MODE_REPLICATED = "REPLICATED";
    public static final String CACHE_MODE_DISTRIBUTED = "DISTRIBUTED";
    public static final String DEFAULT_CACHE_MODE = CACHE_MODE_LOCAL;

    
    @Property(options={
            @PropertyOption(value='%' + KiWiRepositoryService.CACHING_BACKEND + ".option.ehcache",
                    name=KiWiRepositoryService.CACHING_BACKEND_EHCACHE),
            @PropertyOption(value='%' + KiWiRepositoryService.CACHING_BACKEND + ".option.guava",
                    name=KiWiRepositoryService.CACHING_BACKEND_GUAVA),
            @PropertyOption(value='%' + KiWiRepositoryService.CACHING_BACKEND + ".option.hazelcast",
                    name=KiWiRepositoryService.CACHING_BACKEND_HAZELCAST), 
            @PropertyOption(value='%' + KiWiRepositoryService.CACHING_BACKEND + ".option.infinispan-clustered",
                    name=KiWiRepositoryService.CACHING_BACKEND_INFINISPAN_CLUSTERED), 
            @PropertyOption(value='%' + KiWiRepositoryService.CACHING_BACKEND + ".option.infinispan-hotrod",
                    name=KiWiRepositoryService.CACHING_BACKEND_INFINISPAN_HOTROD)}, 
            value=KiWiRepositoryService.DEFAULT_CACHING_BACKEND)
    public static final String CACHING_BACKEND = "marmotta.kiwi.cluster.cachingbackend";
    
    public static final String CACHING_BACKEND_EHCACHE = "EHCACHE";
    public static final String CACHING_BACKEND_GUAVA = "GUAVA";
    public static final String CACHING_BACKEND_HAZELCAST = "HAZELCAST";
    public static final String CACHING_BACKEND_INFINISPAN_CLUSTERED = "INFINISPAN_CLUSTERED";
    public static final String CACHING_BACKEND_INFINISPAN_HOTROD = "INFINISPAN_HOTROD";
    public static final String DEFAULT_CACHING_BACKEND = CACHING_BACKEND_GUAVA;
    
//    @Property(intValue = KiWiRepositoryService.DEFAULT_DB_POOL)
//    public static final String DB_POOL = "marmotta.kiwi.pool_size";
//
//    public static final int DEFAULT_DB_POOL = 20;

    /**
     * The Kiwi {@link Repository} as configured in the 
     * {@link #activate(ComponentContext)} method
     */
    private Repository repository;

    /**
     * The registration for the {@link #repository}
     */
    private ServiceRegistration repoRegistration;
    
    /**
     * The database url.
     */
    private String dbUrl;

    @Activate
    protected final void activate(ComponentContext context) throws ConfigurationException, RepositoryException {
        log.info("activate KiWi repository ...");
        if(context == null || context.getProperties() == null){
            throw new IllegalStateException("No valid"+ComponentContext.class+" parsed in activate!");
        }
        //copy the read-only configuration as we might need to change it before
        //adding it to the registered service
        final Dictionary<String, Object> config = copyConfig(context);
        final BundleContext bc = context.getBundleContext();
        //we want to substitute variables used in the dbURL with configuration, 
        //framework and system properties
        StrSubstitutor strSubstitutor = new StrSubstitutor(new StrLookup<Object>() {

            @Override
            public String lookup(String key) {
                Object val = config.get(key);
                if(val == null){
                    val = bc.getProperty(key);
                }
                return val.toString();
            }
            
        });
        String name = (String)config.get(REPOSITORY_ID);
        if(StringUtils.isBlank(name)){
            throw new ConfigurationException(REPOSITORY_ID, "The parsed Repository ID MUST NOT be NULL nor blank!");
        } else {
        	log.debug(" - name: {}", name);
        }
        KiWiDialect dialect;
        String db_type;
        if(StringUtils.equalsIgnoreCase("postgres",(String)config.get(DB_DIALECT))) {
            dialect = new PostgreSQLDialect();
            db_type = "postgresql";
        } else if(StringUtils.equalsIgnoreCase("mysql",(String)config.get(DB_DIALECT))) {
            dialect = new MySQLDialect();
            db_type = "mysql";
        } else if(StringUtils.equalsIgnoreCase("h2",(String)config.get(DB_DIALECT))) {
            dialect = new H2Dialect();
            db_type = "h2";
        } else {
            throw new ConfigurationException(DB_DIALECT,"No valid database dialect was given");
        }
        log.debug(" - dialect: {}", dialect);
        String db_url = (String)config.get(DB_URL);
        if(StringUtils.isBlank(db_url)){
            //build the db url from parameters
    
            String db_host = (String)config.get(DB_HOST);
            if(StringUtils.isBlank(db_host)){
                db_host = DEFAULT_DB_HOST;
            }
            log.debug(" - db host: {}",db_host);
            String db_name = (String)config.get(DB_NAME);
            if(StringUtils.isBlank(db_name)){
                db_name = DEFAULT_DB_NAME;
            }
            log.debug(" - db name:  {}",name);
            int    db_port;
            Object value = config.get(DB_PORT);
            if(value instanceof Number){
                db_port = ((Number)value).intValue();
            } else if(value != null && !StringUtils.isBlank(value.toString())){
                db_port = Integer.parseInt(value.toString());
            } else {
                db_port = DEFAULT_DB_PORT;
            }
            log.debug(" - db port: {}", db_port);
            String db_opts = (String)config.get(DB_OPTS);
            log.debug(" - db options: {}",db_opts);
            StringBuilder dbUrlBuilder = new StringBuilder("jdbc:").append(db_type);
            if(dialect instanceof H2Dialect){
                //H2 uses a file path and not a host so we do not need the ://
                dbUrlBuilder.append(':').append(db_host);
            } else {
                dbUrlBuilder.append("://").append(db_host);
            }
            if(db_port > 0){
                dbUrlBuilder.append(':').append(db_port);
            }
            if(!StringUtils.isBlank(db_name)){
                dbUrlBuilder.append('/').append(db_name);
            }
            if(!StringUtils.isBlank(db_opts)){
                dbUrlBuilder.append(db_opts);
            }
            dbUrl = strSubstitutor.replace(dbUrlBuilder);
        } else if(!db_url.startsWith("jdbc:")){
            throw new ConfigurationException(DB_URL, "Database URLs are expected to start with " +
                    "'jdbc:' (parsed: '"+db_url+"')!");
        } else {
            dbUrl = strSubstitutor.replace(db_url);
        }
        
        String db_user = (String)config.get(DB_USER);
        if(StringUtils.isBlank(db_user)){
            db_user = DEFAULT_DB_USER;
        } else {
            db_user = strSubstitutor.replace(db_user);
        }
        log.debug(" - db user: {}", db_user);
        String db_pass = (String)config.get(DB_PASS);
        if(StringUtils.isBlank(db_pass)){
            log.debug(" - db pwd is set to default");
            db_pass = DEFAULT_DB_PASS;
        } else {
            log.debug(" - db pwd is set to parsed value");
        }

        KiWiConfiguration configuration = new KiWiConfiguration("Marmotta KiWi",dbUrl,db_user,db_pass,dialect);
        
        //parse cluster options
        String cluster = (String)config.get(CLUSTER);
        if(!StringUtils.isBlank(cluster)){
            log.debug(" - cluster: {}", cluster);
	        configuration.setClustered(true);
	        configuration.setClusterName(cluster);
	        String clusterAddress = (String)config.get(CLUSTER_ADDRESS);
	        if(!StringUtils.isBlank(clusterAddress)){
	        	configuration.setClusterAddress(
	        	    strSubstitutor.replace(clusterAddress));
	        }
            log.debug(" - cluster address: {}", configuration.getClusterAddress());

        	Object clusterPort = config.get(CLUSTER_PORT);
        	if(clusterPort instanceof Number){
        	    int port = ((Number)clusterPort).intValue();
        	    if(port > 0){
        	        configuration.setClusterPort(port);
        	    } //else use default
        	} else if(clusterPort != null){
        	    try {
        	        int port = Integer.parseInt(
        	            strSubstitutor.replace(clusterPort));
        	        if(port > 0) {
        	            configuration.setClusterPort(port);
        	        }
        	    } catch (NumberFormatException e){
        	        throw new ConfigurationException(CLUSTER_PORT, "Unable to parse "
        	            + "Cluster Port from configured value '"+clusterPort+"'!",e);
        	    }
        	}
	      	log.debug(" - cluster port ({})", configuration.getClusterPort());
	        String cachingBackend = (String)config.get(CACHING_BACKEND);
            if(StringUtils.isBlank(cachingBackend)){
                configuration.setCachingBackend(
                    CachingBackends.valueOf(DEFAULT_CACHING_BACKEND));
            } else {
                try {
                    configuration.setCachingBackend(CachingBackends.valueOf(
                        strSubstitutor.replace(cachingBackend)));
                } catch (IllegalArgumentException e){
                    throw new ConfigurationException(CACHING_BACKEND, 
                        "Unsupported CachingBackend '" + cachingBackend 
                        + "' (supported: "+Arrays.toString(CachingBackends.values())
                        + ")!", e);
                }
            }
            log.debug(" - caching Backend: {}",configuration.getCachingBackend());
	        String cacheMode = (String)config.get(CACHE_MODE);
	        if(StringUtils.isBlank(cacheMode)){
	            cacheMode = DEFAULT_CACHE_MODE;
	        }
	        try {
	            configuration.setCacheMode(CacheMode.valueOf(
	                strSubstitutor.replace(cacheMode)));
	        } catch (IllegalArgumentException e){
	            throw new ConfigurationException(CACHE_MODE, "Unsupported CacheMode '"
	                + cacheMode + "' (supported: "+Arrays.toString(CacheMode.values())
	                + ")!");
	        }
	        log.debug(" - cache mode: {}",configuration.getCacheMode());
        } else { // not clustered
        	log.debug(" - no cluster configured");
        	configuration.setClustered(false);
        }
        log.info(" ... initialise KiWi repository: {}", dbUrl);
        KiWiStore store = new KiWiStore(configuration);
        repository = new SailRepository(new KiWiSparqlSail(store));
        repository.initialize();
        //set the repository type property to KiWiStore
        config.put(SAIL_IMPL, KiWiStore.class.getName());
        repoRegistration = context.getBundleContext().registerService(
        		Repository.class.getName(), repository, config);
        log.info("  - successfully registered KiWi Repository {}", name);
    }
        
    @Deactivate
    protected final void deactivate(ComponentContext context) throws RepositoryException {
    	log.info("> deactivate KiWi repository: database URL is {}", dbUrl);
    	if(repoRegistration != null){
    		log.info(" - unregister Service");
    		repoRegistration.unregister();
    		repoRegistration = null;
            log.info("  ... unregistered");
    	}
    	if(repository != null){
    		log.info(" - shutdown Repository");
    		repository.shutDown();
            log.info("  ... done");
            repository = null;
    	} else {
    		log.info(" - repository was not active (ignore call)");
    	}
    }
    
	/**
	 * Copies the read-only configuration parsed by 
	 * {@link ComponentContext#getProperties()} to an other {@link Dictionary}
	 * so that it can be modified.
	 * @param context the context
	 * @return the copied configuration
	 */
	private Dictionary<String, Object> copyConfig(ComponentContext context) {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        @SuppressWarnings("unchecked")
		Dictionary<String,Object> config = context.getProperties();
        for(Enumeration<String> keys = config.keys(); keys.hasMoreElements(); ){
        	String key = keys.nextElement();
        	properties.put(key, config.get(key));
        }
		return properties;
	}

}
