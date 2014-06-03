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
package org.apache.stanbol.commons.solr.managed.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_PUBLISH_REST;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_RANKING;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.SolrServerAdapter.SolrServerProperties;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Allows to init a {@link CoreContainer} for a Solr directory on the local
 * file system and register the {@link CoreContainer} as well as all the 
 * {@link SolrCore}s as OSGI services.<p>
 * The CoreContainer is initialised on 
 * @author westei
 *
 */
@Component(
    immediate=true,
    metatype=true,
    configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE,
    specVersion="1.1")
@Service(value=ReferencedSolrServer.class)
@Properties(value={
    @Property(name=PROPERTY_SERVER_NAME),
    @Property(name=PROPERTY_SERVER_DIR),
    @Property(name=PROPERTY_SERVER_RANKING,intValue=0),
    @Property(name=PROPERTY_SERVER_PUBLISH_REST,boolValue=false)
})
public class ReferencedSolrServer {
    
    private final Logger log = LoggerFactory.getLogger(ReferencedSolrServer.class);

    /**
     * Takes care of manageing the {@link CoreContainer} and its {@link SolrCore}s
     * as OSGI services
     */
    protected SolrServerAdapter server;
    /*
     * NOTE: one could here also get the properties of the parsed 
     * ComponentContext and directly parse the values to the constructor of the
     * SolrServerAdapter. However here the configured values are all checkted
     * to generate meaningful error messages
     */
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        log.info("Activate {}: ",getClass().getSimpleName());
        SolrServerProperties properties = null;
        Object value = context.getProperties().get(PROPERTY_SERVER_DIR);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_SERVER_DIR, "The Server directory is a " +
            		"required configuration and MUST NOT be NULL nor empty!");
        } else {
            File solrServerDir = new File(value.toString());
            if(solrServerDir.isDirectory()){
                log.info(" > solrDir = {}",solrServerDir);
                properties = new SolrServerProperties(solrServerDir);
            } else {
                throw new ConfigurationException(PROPERTY_SERVER_DIR, "The parsed Solr Server directpry '"+
                    value+"' does not exist or is not a directory!");
            }
        }
        value = context.getProperties().get(PROPERTY_SERVER_NAME);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_SERVER_NAME, "The Server Name is a required" +
            		"Configuration and MUST NOT be NULL nor empty!");
        } else {
            properties.setServerName(value.toString());
            log.info(" > Name = {}",value.toString());
        }
        value = context.getProperties().get(PROPERTY_SERVER_RANKING);
        if(value instanceof Number){
            properties.setServerRanking(((Number)value).intValue());
        } else if(value != null && !value.toString().isEmpty()){
            try {
                properties.setServerRanking(Integer.parseInt(value.toString()));
                log.info(" > Ranking = {}",properties.getServerRanking());
            }catch (NumberFormatException e) {
               throw new ConfigurationException(PROPERTY_SERVER_RANKING, "The configured Server Ranking '"+
                   value+" can not be converted to an Integer!",e);
            }
        } //else not present or empty string -> do not set a ranking!
        value = properties.get(PROPERTY_SERVER_PUBLISH_REST);
        if(value == null || value instanceof Boolean) {
            properties.setPublishREST((Boolean)value);
        } else {
            properties.setPublishREST(Boolean.parseBoolean(value.toString()));
        }
        log.info(" > publisRest = {}",properties.isPublishREST());
        try {
            server = new SolrServerAdapter(context.getBundleContext(), properties);
        } catch (SolrException e) {
            throw new ConfigurationException(PROPERTY_SERVER_DIR, "Unable to initialise " +
            		"a SolrServer based on the Directory '"+properties.getServerDir() +
            		"'!",e);
        }
        log.info(" ... SolrServer successfully initialised!");
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info(" ... deactivate referenced SolrServer "+server.getServerName());
        if(server != null){
            server.shutdown();
            server = null;
        }
    }
    
}

