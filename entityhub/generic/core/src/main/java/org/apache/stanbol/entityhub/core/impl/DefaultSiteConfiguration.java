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
package org.apache.stanbol.entityhub.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.site.License;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link SiteConfiguration} interface with
 * getter and setters.<p>
 * Site configurations are read only and usually set during the activation and
 * through accessible via the {@link ReferencedSite} interface they MUST NOT
 * be changed by other components. Because of this there is the possibility to
 * create readonly instances of this class. In this case all setter methods will
 * throw {@link UnsupportedOperationException}s.
 * 
 * @author Rupert Westenthaler
 *
 */
public class DefaultSiteConfiguration implements SiteConfiguration {
    /**
     * The logger
     */
    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(DefaultSiteConfiguration.class);

    /**
     * Internally used to store the configuration.
     */
    private Map<String,Object> config;
    /**
     * The readonly switch (only set by the constructor
     */
    private final boolean readonly;
    
    private DefaultSiteConfiguration(boolean readonly){
        this.readonly = readonly;
        this.config = new HashMap<String,Object>();
    }
    /**
     * Creates a configuration based on the parsed parameter. The parsed 
     * configuration is validated.<p>
     * Changes to the parsed configuration do have no affect on the state of 
     * the created instance.
     * @param readonly if <code>true</code> the configuration can not be
     * be modified after creation. Calling any setter method will result in
     * {@link UnsupportedOperationException}s to be thrown
     * @param parsed the configuration used for the initialisation.
     * @throws ConfigurationException if the parsed properties are not valid
     */
    public DefaultSiteConfiguration(boolean readonly,Map<String,Object> parsed) throws ConfigurationException {
        this(readonly);
        //now add the parsed configuration
        if(parsed != null){
            //use local variable because the field might be a read only wrapper!
            config.putAll(parsed);
        }
        //validate the configuration and transform values to the preferred
        //type (e.g. strings to enums)
        validateConfiguration();
        //if readonly replace with an unmodifiable map
        if(readonly){
            this.config = Collections.unmodifiableMap(config);
        }
    }
    /**
     * Constructs an empty read/write configuration
     */
    public DefaultSiteConfiguration(){
       this(false);
    }
    /**
     * Constructs an readonly instance based on the parsed configuration. The parsed 
     * configuration is validated.<p>
     * Changes to the parsed configuration do have no affect on the state of 
     * the created instance.
     * @param config the configuration to use
     * @throws ConfigurationException if the parsed properties are not valid
     */
    public DefaultSiteConfiguration(Map<String,Object> config) throws ConfigurationException {
        this(true,config);
    }
    /**
     * Validates if the current configuration is valid and also perform type
     * transformations on values (e.g. converting string values to the enumerated 
     * types).<p>
     * This Method requires write access to the configuration and will therefore
     * fail in case this instance is readonly. Constructors that do have an
     * config as parameter call this method to validate the configuration.
     * @throws ConfigurationException if the validation of an property fails 
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     */
    public void validateConfiguration() throws ConfigurationException, UnsupportedOperationException {
        if(getId() == null || getId().isEmpty()){
            throw new ConfigurationException(ID, "The id of a ReferencedSite configuration MUST NOT be NULL nor empty!");
        }
        //check if all the Enumerated values are valid strings and convert them
        //to enumeration instances
        try {
            setDefaultMappedEntityState(getDefaultMappedEntityState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_MAPPING_STATE,
                String.format("Unknown default MappingState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_MAPPING_STATE,config.get(DEFAULT_MAPPING_STATE),getId(),
                        Arrays.toString(MappingState.values())),e);
        }
        try {
            setDefaultSymbolState(getDefaultManagedEntityState());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(DEFAULT_SYMBOL_STATE, 
                String.format("Unknown default SymbolState (%s=%s) for Site %s! Valid values are %s ",
                    DEFAULT_SYMBOL_STATE,config.get(DEFAULT_SYMBOL_STATE),getId(),
                        Arrays.toString(ManagedEntityState.values()),e));
        }
        try {
            setCacheStrategy(getCacheStrategy());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(CACHE_STRATEGY, 
                String.format("Unknown CachStrategy (%s=%s) for Site %s! Valid values are %s ",
                    CACHE_STRATEGY,config.get(CACHE_STRATEGY),getId(),
                        Arrays.toString(CacheStrategy.values()),e));
        }
        //check if the default expire duration is a number
        try {
            setDefaultExpireDuration(getDefaultExpireDuration());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(DEFAULT_EXPIRE_DURATION, 
                String.format("Unable to parse Number for %s=%s. Will return -1 (no expire duration)",
                    DEFAULT_EXPIRE_DURATION,config.get(DEFAULT_EXPIRE_DURATION)),e);
        }
        //check if the prefixes can be converted to an String[]
        try {
            setEntityPrefixes(getEntityPrefixes());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(ENTITY_PREFIX, e.getMessage(),e);
        }
        //check if the fieldMappings can be converted to an String[]
        try {
            setFieldMappings(getFieldMappings());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(SITE_FIELD_MAPPINGS, e.getMessage(),e);
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
        //check the configured licenses and create the License array
        setLicenses(getLicenses());
    }
    /**
     * Getter for the readonly state.
     * @return if <code>true</code> this configuration is readonly and all
     * setter methods will throw {@link UnsupportedOperationException}s.
     */
    public final boolean isReadonly() {
        return readonly;
    }
    
    @Override
    public final String getAccessUri() {
        Object accessUri = config.get(ACCESS_URI);
        return accessUri == null?null:accessUri.toString();
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
    public final String getAttribution() {
        Object attribution = config.get(SITE_ATTRIBUTION);
        return attribution == null || attribution.toString().isEmpty() ?
                null : attribution.toString();
    }
    /**
     * 
     * @param attribution
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAttribution()
     */
    public final void setAttribution(String attribution) throws UnsupportedOperationException {
        if(attribution == null  || attribution.isEmpty()){
            config.remove(SITE_ATTRIBUTION);
        } else {
            config.put(SITE_ATTRIBUTION, attribution);
        }
    }
    @Override
    public String getAttributionUrl() {
        Object attribution = config.get(SITE_ATTRIBUTION_URL);
        return attribution == null || attribution.toString().isEmpty() ?
                null : attribution.toString();
    }
    /**
     * 
     * @param attribution
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getAttribution()
     */
    public final void setAttributionUrl(String attributionUrl) throws UnsupportedOperationException {
        if(attributionUrl == null || attributionUrl.isEmpty()){
            config.remove(SITE_ATTRIBUTION_URL);
        } else {
            config.put(SITE_ATTRIBUTION_URL, attributionUrl);
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
    public final long getDefaultExpireDuration() {
        Object duration = config.get(DEFAULT_EXPIRE_DURATION);
        if(duration == null){
            return 0;
        } else if(duration instanceof Long){
            return (Long) duration;
        } else {
            return Long.valueOf(duration.toString());
        }
    }
    /**
     * 
     * @param duration
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultExpireDuration()
     */
    public final void setDefaultExpireDuration(long duration) throws UnsupportedOperationException {
        if(duration <= 0){
            config.remove(DEFAULT_EXPIRE_DURATION);
        } else {
            config.put(DEFAULT_EXPIRE_DURATION, duration);
        }
    }
    
    @Override
    public final MappingState getDefaultMappedEntityState() {
        Object defaultMappingState = config.get(DEFAULT_MAPPING_STATE);
        if(defaultMappingState == null){
            return null;
        } else if(defaultMappingState instanceof MappingState){
            return (MappingState)defaultMappingState;
        } else {
            return MappingState.valueOf(defaultMappingState.toString());
        }
    }
    /**
     * Setter for the default state of Mappings created between Entities of this
     * Site and Entities managed by the Entityhub. If this configuration is not
     * present created mappings will have the default state as configured by the
     * Entityhub.
     * @param state the default state for new Entity mappings.
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultMappedEntityState()
     */
    public final void setDefaultMappedEntityState(MappingState state) throws UnsupportedOperationException {
        if(state == null){
            config.remove(DEFAULT_MAPPING_STATE);
        } else {
            config.put(DEFAULT_MAPPING_STATE, state);
        }
    }

    @Override
    public final ManagedEntityState getDefaultManagedEntityState() {
        Object defaultSymbolState = config.get(DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            return null;
        } else if(defaultSymbolState instanceof ManagedEntityState){
            return (ManagedEntityState)defaultSymbolState;
        } else {
            return ManagedEntityState.valueOf(defaultSymbolState.toString());
        }
    }
    /**
     * Setter for the default state of Entities after importing them into the
     * Entityhub. If this configuration is not present Entities will have the
     * default state set for the Entityhub.
     * @param state the state or <code>null</code> to remove this configuration
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDefaultManagedEntityState()
     */
    public final void setDefaultSymbolState(ManagedEntityState state) throws UnsupportedOperationException {
        if(state == null){
            config.remove(DEFAULT_SYMBOL_STATE);
        } else {
            config.put(DEFAULT_SYMBOL_STATE, state);
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
    public final String getDescription() {
        Object description = config.get(DESCRIPTION);
        return description == null || description.toString().isEmpty() ? 
                null : description.toString();
    }
    /**
     * Setter for the description of the {@link ReferencedSite}. If set to
     * <code>null</code> or an empty string this configuration will be removed.
     * @param description the description
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getDescription()
     */
    public final void setDescription(String description) throws UnsupportedOperationException {
        if(description == null || description.isEmpty()){
            config.remove(DESCRIPTION);
        } else {
            config.put(DESCRIPTION, description);
        }
    }
    @Override
    public String[] getFieldMappings() {
        return getStringValues(SITE_FIELD_MAPPINGS);
    }
    /**
     * Setter for the mappings of a site. This mappings are used in case an 
     * Entity of this site is imported to the Entityhub. Parsing <code>null</code>
     * or an empty array will cause all existing mappings to be removed.
     * @param mappings the mappings
     * @throws UnsupportedOperationException
     */
    public final void setFieldMappings(String[] mappings) throws UnsupportedOperationException {
        if(mappings == null || mappings.length < 1){
            config.remove(SITE_FIELD_MAPPINGS);
        } else {
            config.put(SITE_FIELD_MAPPINGS, mappings);
        }
    }
    
    @Override
    public final String[] getEntityPrefixes() {
        return getStringValues(ENTITY_PREFIX);
    }
    /**
     * Setter for the Entity prefixes (typically the namespace or the host name)
     * of the entities provided by this site. Because Sites might provide Entities
     * with different namespaces this site allows to parse an array. Setting the
     * prefixes to <code>null</code> or an empty array will cause that this site
     * is ask for all requested entities.
     * @param prefixes The array with the prefixes or <code>null</code> to 
     * remove any configured prefixes
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getEntityPrefixes()
     */
    public final void setEntityPrefixes(String[] prefixes) throws UnsupportedOperationException {
        if(prefixes == null || prefixes.length < 1){
            config.remove(ENTITY_PREFIX);
        } else {
            config.put(ENTITY_PREFIX, prefixes);
        }
    }
    
    @Override
    public final String getId() {
        Object id = config.get(ID);
        return id == null ? null : id.toString();
    }
    /**
     * Setter for the id of the referenced site
     * @param id the id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @throws IllegalArgumentException in case the parsed ID is <code>null</code> or an empty String
     * @see #getId()
     */
    public final void setId(String id) throws UnsupportedOperationException, IllegalArgumentException {
        if(id == null){
            throw new IllegalArgumentException("The ID of the Site MUST NOT be set to NULL!");
        } else if (id.isEmpty()){
            throw new IllegalArgumentException("The ID of the Site MIST NOT be set to an empty String!");
        } else {
            config.put(ID, id);
        }
    }
    @Override
    public final License[] getLicenses() {
        //get Licenses based on related keys
        int elements = 0;
        String[] names = getLicenseName();
        if(names == null){
            names = new String[]{};
        } else {
            elements = Math.max(elements, names.length);
        }
        String[] texts = getLicenseText();
        if(texts == null){
            texts = new String[]{};
        } else {
            elements = Math.max(elements, texts.length);
        }
        String[] urls = getLicenseUrl();
        if(urls == null){
            urls = new String[]{};
        } else {
            elements = Math.max(elements, urls.length);
        }
        Collection<License> licenseList = new ArrayList<License>();
        for(int i=0;i<elements;i++){
            try {
                licenseList.add(new License(
                    names.length>i?names[i]:null,
                            urls.length>i?urls[i]:null,
                                    texts.length>i?texts[i]:null));
            } catch(IllegalArgumentException e){
                //ignore if name, text and url == null and/or empty
            }
        }
        return licenseList.isEmpty()?null:licenseList.toArray(new License[licenseList.size()]);
    }
    /**
     * Setter for the {@link License} information. This method stores the name,
     * text and url of the license as strings in the according fields of the
     * configuration.
     * @param licenses the licenses to store. <code>null</code> or an empty
     * array to remove existing values
     * @throws IllegalArgumentException if the parsed array contains a <code>null</code>
     * element
     * @throws UnsupportedOperationException if the configuration is read-only
     */
    public final void setLicenses(License[] licenses) throws IllegalArgumentException, UnsupportedOperationException {
        if(licenses == null || licenses.length < 1){
            config.remove(SITE_LICENCE_NAME);
            config.remove(SITE_LICENCE_TEXT);
            config.remove(SITE_LICENCE_URL);
        } else {
            String[] names = new String[licenses.length];
            String[] texts = new String[licenses.length];
            String[] urls = new String[licenses.length];
            for(int i=0;i<licenses.length;i++){
                if(licenses[i] != null){
                    names[i] = licenses[i].getName();
                    texts[i] = licenses[i].getText();
                    urls[i] = licenses[i].getUrl();
                } else {
                    throw new IllegalArgumentException("The parsed License array" +
                    		"MUST NOT contain a NULL element! (parsed: "+
                    		Arrays.toString(licenses)+")");
                }
            }
            config.put(SITE_LICENCE_NAME, names);
            config.put(SITE_LICENCE_TEXT, texts);
            config.put(SITE_LICENCE_URL, urls);
        }
    }
    
    /**
     * Internally used to get the names of the licenses
     * @return
     */
    private String[] getLicenseName() {
        return getStringValues(SITE_LICENCE_NAME);
    }
    /**
     * Internally used to get the texts of the licenes
     * @return
     */
    private String[] getLicenseText() {
        return getStringValues(SITE_LICENCE_TEXT);
    }
    /**
     * Internally used to get the urls of the page describing the license
     * @return
     */
    private String[] getLicenseUrl() {
        return getStringValues(SITE_LICENCE_URL);
    }

    @Override
    public String getName() {
        Object name = config.get(NAME);
        //use ID as fallback!
        return name == null || name.toString().isEmpty() ? getId() : name.toString();
    }
    /**
     * Setter for the name of the Referenced Site. Note that if the name is not
     * present the {@link #getId() id} will be used as name.
     * @param name the name of the site or <code>null</code> to remove it (and
     * use the {@link #getId() id} also as name
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getName()
     */
    public final void setName(String name) throws UnsupportedOperationException {
        if(name == null || name.isEmpty()){
            config.remove(NAME);
        } else {
            config.put(NAME, name);
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
    /**
     * Provides direct access to the internal configuration
     * @return the configuration wrapped by this class
     */
    protected final Map<String,Object> getConfiguration(){
        return config;
    }
    /**
     * Internally used to parse String[] based on key values. This method
     * supports Stirng, Stirng[] and Iterables&lt;?&gt;. For Iterables&lt;?&gt;
     * the {@link Object#toString()} is used and <code>null</code> elementes are
     * kept.
     * @return
     */
    private String[] getStringValues(String key) {
        Object values = config.get(key);
        if(values == null){
            return null;
        } else if (values instanceof String[]){
            if(((String[])values).length<1){ //return null if empty
                return null;
            } else {
                return (String[]) values;
            }
        } else if (values instanceof Iterable<?>){
            Collection<String> prefixes = new ArrayList<String>();
            for(Object value : (Iterable<?>)values){
                prefixes.add(value==null ? null : value.toString());
            }
            return prefixes.toArray(new String[prefixes.size()]);
        } else if(values instanceof String) {
            return new String[]{values.toString()};
        } else {
            throw new IllegalArgumentException(
                String.format("Unable to parse Sting[] for field %s form type %s (supported are String, String[] and Iterables)",
                    key,values.getClass()));
        }
    }
}
