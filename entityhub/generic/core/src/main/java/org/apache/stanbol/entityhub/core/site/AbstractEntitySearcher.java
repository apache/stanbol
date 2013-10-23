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
package org.apache.stanbol.entityhub.core.site;

import static org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer.ACCESS_URI;
import static org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher.QUERY_URI;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

@Property(name=QUERY_URI)
public abstract class AbstractEntitySearcher implements EntitySearcher {

    protected final Logger log;

    protected AbstractEntitySearcher(Logger log){
        this.log = log;
           log.info("create instance of "+this.getClass().getName());
    }

    private String queryUri;
    private String baseUri;

    private Dictionary<String,?> config;
    private ComponentContext context;

    protected final String getQueryUri() {
        return queryUri;
    }
    /**
     * Getter for the base URI to be used for parsing relative URIs in responses
     * @return
     */
    protected String getBaseUri(){
        return baseUri;
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.debug("in "+AbstractEntitySearcher.class.getSimpleName()+" activate with context "+context);
        // TODO handle updates to the configuration
        if(context != null && context.getProperties() != null){
            this.context = context;
            Dictionary<String,?> properties = context.getProperties();
            Object queryUriObject = properties.get(QUERY_URI);
            Object accessUriObject = properties.get(ACCESS_URI); //use as an fallback
            if(queryUriObject != null){
                this.queryUri = queryUriObject.toString();
                //now set the new config
            } else if(accessUriObject != null){
                log.info("Using AccessUri as fallback for missing QueryUri Proerty (accessUri="+accessUriObject);
                this.queryUri = accessUriObject.toString();
            } else {
                throw new IllegalArgumentException("The property "+EntitySearcher.QUERY_URI+" must be defined");
            }
            this.baseUri = extractBaseUri(queryUri);
            this.config = properties;
        } else {
            throw new IllegalArgumentException("The property "+EntitySearcher.QUERY_URI+" must be defined");
        }

    }
    /**
     * computes the base URL based on service URLs
     * @param the URL of the remote service
     * @return the base URL used to parse relative URIs in responses.
     */
    protected static String extractBaseUri(String uri) {
        //extract the namepsace from the query URI to use it fore parsing
        //responses with relative URIs
        String baseUri;
        int index = Math.max(uri.lastIndexOf('#'),uri.lastIndexOf('/'));
        int protIndex = uri.indexOf("://")+3; //do not convert http://www.example.org
        if(protIndex < 0){
            protIndex = 0;
        }
        //do not convert if the parsed uri does not contain a local name
        if(index > protIndex && index+1 < uri.length()){
            baseUri = uri.substring(0, index+1);
        } else {
            if(!(uri.charAt(uri.length()-1) == '/' || uri.charAt(uri.length()-1) == '#')){
                baseUri = uri+'/'; //add a tailing '/' to Uris like http://www.example.org
            } else {
                baseUri = uri;
            }
        }
        return baseUri;
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.debug("in "+AbstractEntitySearcher.class.getSimpleName()+" deactivate with context "+context);
        this.config = null;
        this.queryUri = null;
        this.baseUri = null;
    }
    /**
     * The OSGI configuration as provided by the activate method
     * @return
     */
    protected final Dictionary<String,?> getSiteConfiguration() {
        return config;
    }

    protected final ComponentContext getComponentContext(){
        return context;
    }
}
