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

import java.util.Arrays;
import java.util.Dictionary;

import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentFactory;

public class ReferencedSiteConfigurationImpl extends SiteConfigurationImpl implements ReferencedSiteConfiguration{

    public ReferencedSiteConfigurationImpl(Dictionary<String,Object> config) throws ConfigurationException{
        super(config);
    }
    
    @Override
    protected void validateConfiguration() throws ConfigurationException {
        super.validateConfiguration();
        //check if all the Enumerated values are valid strings and convert them
        //to enumeration instances
        try {
            setCacheStrategy(getCacheStrategy());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(CACHE_STRATEGY, 
                String.format("Unknown CachStrategy (%s=%s) for Site %s! Valid values are %s ",
                    CACHE_STRATEGY,config.get(CACHE_STRATEGY),getId(),
                        Arrays.toString(CacheStrategy.values()),e));
        }
        //check that a cacheId is set if the CacheStrategy != none
        if(CacheStrategy.none != getCacheStrategy() && getCacheId() == null){
            throw new ConfigurationException(CACHE_ID, 
                String.format("The CacheID (%s) MUST NOT be NULL nor empty if the the CacheStrategy != %s",
                    CACHE_ID,CacheStrategy.none));
        }
        //check that a accessUri and an entity dereferencer is set if the 
        //cacheStrategy != CacheStrategy.all
        if(CacheStrategy.all != getCacheStrategy()){
            if(getAccessUri() == null){
                throw new ConfigurationException(ACCESS_URI, 
                    String.format("An AccessUri (%s) MUST be configured if the CacheStrategy != %s",
                        ACCESS_URI,CacheStrategy.all));
            }
            if(getEntityDereferencerType() == null){
                throw new ConfigurationException(ENTITY_DEREFERENCER_TYPE, 
                    String.format("An EntityDereferencer (%s) MUST be configured if the CacheStrategy != %s",
                        ENTITY_DEREFERENCER_TYPE,CacheStrategy.all));
            }
        }
    }
    @Override
    public final String getAccessUri() {
        Object accessUri = config.get(ACCESS_URI);
        return accessUri == null || accessUri.toString().isEmpty() ? null : accessUri.toString();
    }
    /**
     * 
     * @param uri
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAccessUri()
     */
    public final void setAccessUri(String uri) throws UnsupportedOperationException{
        if(uri == null || uri.isEmpty()){
            config.remove(ACCESS_URI);
        } else {
            config.put(ACCESS_URI, uri);
        }
    }
    
    @Override
    public final String getCacheId() {
        Object id = config.get(CACHE_ID);
        return id == null || id.toString().isEmpty() ? 
                null : id.toString();
    }
    /**
     * 
     * @param id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getCacheId()
     */
    public final void setCacheId(String id) throws UnsupportedOperationException {
        if(id == null || id.isEmpty()){
            config.remove(CACHE_ID);
        } else {
            config.put(CACHE_ID, id);
        }
    }

    @Override
    public final CacheStrategy getCacheStrategy() {
        Object cacheStrategy = config.get(CACHE_STRATEGY);
        if(cacheStrategy == null){
            return null;
        } else if(cacheStrategy instanceof CacheStrategy){
            return (CacheStrategy)cacheStrategy;
        } else {
            return CacheStrategy.valueOf(cacheStrategy.toString());
        }
    }
    /**
     * 
     * @param strategy
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getCacheStrategy()
     */
    public final void setCacheStrategy(CacheStrategy strategy) throws UnsupportedOperationException {
        if(strategy == null){
            config.remove(CACHE_STRATEGY);
        } else {
            config.put(CACHE_STRATEGY, strategy);
        }
    }

    @Override
    public final String getEntityDereferencerType() {
        Object dereferencer = config.get(ENTITY_DEREFERENCER_TYPE);
        return dereferencer == null ||dereferencer.toString().isEmpty() ?
                null : dereferencer.toString();
    }
    /**
     * Setter for the type of the {@link EntityDereferencer} to be used by
     * this site or <code>null</code> to remove the current configuration. <p>
     * Note that the {@link EntityDereferencer} is only initialised of a valid
     * {@link #getAccessUri() access URI} is configured. If the dereferencer is
     * set to <code>null</code> dereferencing Entities will not be supported by
     * this site. Entities might still be available form a local
     * {@link #getCacheId() cache}.
     * @param entityDereferencerType the key (OSGI name) of the component used
     * to dereference Entities. This component must have an {@link ComponentFactory}
     * and provide the {@link EntityDereferencer} service-
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntityDereferencerType()
     */
    public final void setEntityDereferencerType(String entityDereferencerType) throws UnsupportedOperationException {
        if(entityDereferencerType == null){
            config.remove(entityDereferencerType);
        } else {
            config.put(ENTITY_DEREFERENCER_TYPE, entityDereferencerType);
        }
    }

    @Override
    public String getEntitySearcherType() {
        Object type = config.get(ENTITY_SEARCHER_TYPE);
        return type == null || type.toString().isEmpty() ? null : type.toString();
    }
    /**
     * Setter for the type of the {@link EntitySearcher} used to query for
     * Entities by accessing a external service available at 
     * {@link #getQueryUri()}. <p>
     * Note that the {@link EntitySearcher} will only be initialised of the
     * {@link #getQueryUri() Query URI} is defined.
     * @param entitySearcherType The string representing the {@link EntitySearcher}
     * (the name of the OSGI component) or <code>null</code> to remove this
     * configuration. The referenced component MUST have an {@link ComponentFactory}
     * and provide the {@link EntitySearcher} service.
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntitySearcherType()
     */
    public final void setEntitySearcherType(String entitySearcherType) throws UnsupportedOperationException {
        if(entitySearcherType == null){
            config.remove(ENTITY_SEARCHER_TYPE);
        } else {
            config.put(ENTITY_SEARCHER_TYPE, entitySearcherType);
        }
    }
    @Override
    public String getQueryUri() {
        Object uri = config.get(QUERY_URI);
        return uri == null || uri.toString().isEmpty() ? null : uri.toString();
    }
    /**
     * Setter for the uri of the remote service used to query for entities. If
     * set to <code>null</code> this indicates that no such external service is
     * available for this referenced site
     * @param queryUri the uri of the external service used to query for entities
     * or <code>null</code> if none.
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getQueryUri()
     */
    public final void setQueryUri(String queryUri) throws UnsupportedOperationException {
        if(queryUri == null  || queryUri.isEmpty()){
            config.remove(QUERY_URI);
        } else {
            config.put(QUERY_URI, queryUri);
        }
    }
}
